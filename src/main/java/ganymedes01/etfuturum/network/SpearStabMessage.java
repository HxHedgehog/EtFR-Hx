package ganymedes01.etfuturum.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.DamageSource;
import net.minecraft.stats.StatList;
import net.minecraft.network.play.server.S2APacketParticles;
import net.minecraft.world.WorldServer;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.network.NetworkRegistry;
import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.core.utils.SpearKinetics;
import ganymedes01.etfuturum.items.ItemSpear;

/**
 * Client -> Server stab report. 26.2 settles spear stabs server side because the server knows the
 * player speed (ServerPlayer#getKnownSpeed = last reported client movement). The 1.7.10 server
 * never learns player motion, so the local client projects the look-aligned speeds of the attacker
 * and every raycast target (KineticWeapon L74-75) and reports them; the server re-tests the
 * KineticWeapon conditions and the reach sanity window, then mirrors Player#stabAttack.
 */
public class SpearStabMessage implements IMessage {

	private int ticksUsed; // already past the wound-up delay, like KineticWeapon.damageEntities
	private float attackerSpeed; // look . motion, blocks/second
	private int[] targetIds;
	private float[] relativeSpeeds;

	public SpearStabMessage() {
	}

	public SpearStabMessage(int ticksUsed, float attackerSpeed, int[] targetIds, float[] relativeSpeeds) {
		this.ticksUsed = ticksUsed;
		this.attackerSpeed = attackerSpeed;
		this.targetIds = targetIds;
		this.relativeSpeeds = relativeSpeeds;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		ticksUsed = buf.readInt();
		attackerSpeed = buf.readFloat();
		int count = buf.readByte();
		targetIds = new int[count];
		relativeSpeeds = new float[count];
		for (int i = 0; i < count; i++) {
			targetIds[i] = buf.readInt();
			relativeSpeeds[i] = buf.readFloat();
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(ticksUsed);
		buf.writeFloat(attackerSpeed);
		buf.writeByte(targetIds.length);
		for (int i = 0; i < targetIds.length; i++) {
			buf.writeInt(targetIds[i]);
			buf.writeFloat(relativeSpeeds[i]);
		}
	}

	public static class Handler implements IMessageHandler<SpearStabMessage, IMessage> {

		private static final SpearKinetics.StabCooldowns COOLDOWNS = new SpearKinetics.StabCooldowns();

		@Override
		public IMessage onMessage(SpearStabMessage message, MessageContext ctx) {
			final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
			final ItemStack stack = player.getItemInUse();
			if (!player.isUsingItem() || !(stack.getItem() instanceof ItemSpear))
				return null;
			if (message.ticksUsed < 0)
				return null;
			final ItemSpear spear = (ItemSpear) stack.getItem();
			final long now = player.worldObj.getTotalWorldTime();
			COOLDOWNS.pruneOlderThan(now, 200L);
			for (int i = 0; i < message.targetIds.length; i++) {
				Entity target = player.worldObj.getEntityByID(message.targetIds[i]);
				float relativeSpeed = message.relativeSpeeds[i];
				if (!(target instanceof EntityLivingBase) || Double.isNaN(relativeSpeed))
					continue;
				if (!SpearKinetics.canHitEntity(player, target))
					continue;
				// INTENTIONAL DEVIATION from 26.2 KineticWeapon (fixes MC-310858): the official code
				// remembers the target BEFORE testing the speed conditions, so a target merely
				// entering the corridor eats the whole 10t contact window even when nothing lands.
				// Here only a real settle consumes the window, keeping further attempts open.
				if (COOLDOWNS.wasRecentlyStabbed(player, target, SpearKinetics.CONTACT_COOLDOWN_TICKS, now))
					continue;
				// Generous reach sanity window: 26.2 settles from the authoritative server position.
				// Spear window is 4.5 survival / 6.5 creative (+ hitbox margin), same +3 headroom.
				double reachLimit = (player.capabilities.isCreativeMode ? SpearKinetics.RANGE_MAX_CREATIVE : SpearKinetics.RANGE_MAX) + SpearKinetics.RANGE_MARGIN + 3.0D;
				double distSq = player.getDistanceSq(target.posX, target.posY, target.posZ);
				if (distSq > reachLimit * reachLimit)
					continue;
				// 26.2 Condition.test with the player actionFactor of 1.0
				boolean dismounts = SpearKinetics.testCondition(message.ticksUsed, spear.getDismountTicks(), message.attackerSpeed, spear.getDismountMinSpeed(), relativeSpeed, 0.0D, 1.0D);
				boolean knockback = SpearKinetics.testCondition(message.ticksUsed, spear.getKnockbackTicks(), message.attackerSpeed, spear.getKnockbackMinSpeed(), relativeSpeed, 0.0D, 1.0D);
				boolean damage = SpearKinetics.testCondition(message.ticksUsed, spear.getDamageTicks(), message.attackerSpeed, 0.0D, relativeSpeed, spear.getDamageMinRelativeSpeed(), 1.0D);
				if (!dismounts && !knockback && !damage)
					continue;
				// 26.2: base attack damage 1.0 + floor(relativeSpeed * damageMultiplier)
				float baseDamage = (float) (1.0D + MathHelper.floor_double(relativeSpeed * spear.getDamageMultiplier()));
				boolean settled = stabAttack(player, stack, target, baseDamage, damage, knockback, dismounts);
				if (settled) {
					COOLDOWNS.rememberStabbedEntity(player, target, now); // MC-310858: window only consumed on a real settle
					// 26.2 broadcastEntityEvent(attacker, 2): hit feedback to every watcher
					EtFuturum.networkWrapper.sendToAllAround(new SpearHitFeedbackMessage(player.getEntityId()), new NetworkRegistry.TargetPoint(player.dimension, player.posX, player.posY, player.posZ, 64.0D));
				}
			}
			return null;
		}

		/**
		 * 26.2 Player#stabAttack: no attack-strength scaling while using, fixed 0.4 wound-up
		 * knockback plus half the enchant knockback, dismount on request, durability, exhaustion.
		 * Package-private so SpearJabMessage.Handler settles left-click jabs through the same path.
		 */
		static boolean stabAttack(EntityPlayerMP attacker, ItemStack stack, Entity target, float baseDamage, boolean dealsDamage, boolean dealsKnockback, boolean dismounts) {
			if (!target.canAttackWithItem() || target.hitByEntity(attacker)) // 26.2 cannotAttack
				return false;
			DamageSource source = DamageSource.causePlayerDamage(attacker);
			float magicBoost = target instanceof EntityLivingBase ? EnchantmentHelper.getEnchantmentModifierLiving(attacker, (EntityLivingBase) target) : 0.0F;
			float totalDamage = dealsDamage ? baseDamage + magicBoost : 0.0F;
			float oldHealth = target instanceof EntityLivingBase ? ((EntityLivingBase) target).getHealth() : 0.0F;
			boolean wasHurt = dealsDamage && target.attackEntityFrom(source, totalDamage);
			if (dealsKnockback) {
				// 26.2 causeExtraKnockback; the attacker momentum x0.6 recoil is skipped because the
				// 1.7.10 server never knows player motion
				if (target instanceof EntityLivingBase) {
					EntityLivingBase living = (EntityLivingBase) target;
					float sin = MathHelper.sin(attacker.rotationYaw * (float) Math.PI / 180.0F);
					float cos = -MathHelper.cos(attacker.rotationYaw * (float) Math.PI / 180.0F);
					living.knockBack(attacker, 0.4F, sin, cos);
					float enchantKnockback = EnchantmentHelper.getKnockbackModifier(attacker, living) / 2.0F; // 26.2 getKnockback divides by 2
					if (enchantKnockback > 0.0F)
						living.knockBack(attacker, enchantKnockback, sin, cos);
				}
			}
			boolean dismounted = false;
			if (dismounts && target.ridingEntity != null) { // 26.2 target.isPassenger()
				dismounted = true;
				target.mountEntity(null);
			}
			if (!(wasHurt || dealsKnockback || dismounted))
				return false;
			if (magicBoost > 0.0F)
				attacker.onEnchantmentCritical(target); // 26.2 attackVisualEffects: magic crit only, no attack sound for stabs
			if (target instanceof EntityLivingBase)
				stack.damageItem(1, attacker); // 26.2 itemAttackInteraction -> hurtEnemy
			if (wasHurt)
				EnchantmentHelper.func_151385_b(attacker, target); // 26.2 doPostAttackEffects (thorns)
			if (target instanceof EntityLivingBase) { // 26.2 damageStatsAndHearts
				float actualDamage = oldHealth - ((EntityLivingBase) target).getHealth();
				attacker.addStat(StatList.damageDealtStat, Math.round(actualDamage * 10.0F));
				if (actualDamage > 2.0F && attacker.worldObj instanceof WorldServer) {
					// DAMAGE_INDICATOR burst: crit particles around the hit point
					WorldServer world = (WorldServer) attacker.worldObj;
					S2APacketParticles particles = new S2APacketParticles("crit", (float) target.posX, (float) (target.posY + target.height * 0.5D), (float) target.posZ, 0.1F, 0.0F, 0.1F, 0.2F, (int) (actualDamage * 0.5F));
					for (Object o : world.playerEntities)
						if (o instanceof EntityPlayerMP)
							((EntityPlayerMP) o).playerNetServerHandler.sendPacket(particles);
				}
			}
			if (!attacker.capabilities.isCreativeMode) // 26.2 causeFoodExhaustion(0.1)
				attacker.getFoodStats().addExhaustion(0.1F);
			return true;
		}
	}
}
