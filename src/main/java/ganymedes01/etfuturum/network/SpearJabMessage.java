package ganymedes01.etfuturum.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.network.NetworkRegistry;
import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.core.utils.SpearChargeTicker;
import ganymedes01.etfuturum.core.utils.SpearKinetics;
import ganymedes01.etfuturum.items.ItemSpear;

/**
 * Client -> Server left-click jab, the 1.7.10 stand-in for 26.2's ServerboundPlayerActionPacket
 * STAB action. The official settle runs fully server side (PiercingWeapon.attack): the corridor
 * attack range sweeps from eye+2.0 to eye+maxRange plus the movement component, every entity on it
 * takes the flat attack-damage attribute (1.0 + material bonus, knockback always, never dismounts).
 * The 1.7.10 server cannot rebuild that corridor (GTNH clients anchor posY at the eyes, and elytra
 * drops the visual camera a further 1.22 blocks), so the client reports the hits found with
 * SpearKinetics and the server re-tests the charge gate (tolerance 5, like 26.2
 * cannotAttackWithItem(itemInHand, 5)) plus a reach sanity window on every target before settling
 * through the shared stabAttack.
 */
public class SpearJabMessage implements IMessage {

	private int[] targetIds;

	public SpearJabMessage() {
	}

	public SpearJabMessage(int[] targetIds) {
		this.targetIds = targetIds;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		int count = buf.readByte();
		targetIds = new int[count];
		for (int i = 0; i < count; i++)
			targetIds[i] = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeByte(targetIds.length);
		for (int i = 0; i < targetIds.length; i++)
			buf.writeInt(targetIds[i]);
	}

	public static class Handler implements IMessageHandler<SpearJabMessage, IMessage> {

		@Override
		public IMessage onMessage(SpearJabMessage message, MessageContext ctx) {
			EntityPlayerMP player = ctx.getServerHandler().playerEntity;
			ItemStack stack = player.getCurrentEquippedItem(); // 26.2 MAIN_HAND, not the item in use
			if (stack == null || !(stack.getItem() instanceof ItemSpear))
				return null;
			ItemSpear spear = (ItemSpear) stack.getItem();
			// 26.2 ServerGamePacketListenerImpl STAB case: optimistic latency tolerance of 5 ticks
			if (((SpearChargeTicker) player).etfu$spearAttackCharge(5, spear.getAttackDurationTicks()) < 1.0F)
				return null;
			// same sanity window as the kinetic settle: reach + hitbox margin + 3 headroom
			double reachLimit = (player.capabilities.isCreativeMode ? SpearKinetics.RANGE_MAX_CREATIVE : SpearKinetics.RANGE_MAX) + SpearKinetics.RANGE_MARGIN + 3.0D;
			double limitSq = reachLimit * reachLimit;
			// 26.2 PiercingWeapon.attack: damage = getAttributeValue(ATTACK_DAMAGE) - the held-item
			// attribute modifier (ItemSpear) puts the material bonus and strength potions in
			float baseDamage = (float) player.getEntityAttribute(SharedMonsterAttributes.attackDamage).getAttributeValue();
			boolean hitSomething = false;
			for (int i = 0; i < message.targetIds.length; i++) {
				Entity target = player.worldObj.getEntityByID(message.targetIds[i]);
				boolean canHit = target != null && SpearKinetics.canHitEntity(player, target);
				boolean inReach = canHit && player.getDistanceSq(target.posX, target.posY, target.posZ) <= limitSq;
				// dealsDamage and dealsKnockback are unconditional, dismounts is false
				boolean hurt = inReach && SpearStabMessage.Handler.stabAttack(player, stack, target, baseDamage, true, true, false);
				hitSomething |= hurt;
			}
			if (hitSomething) {
				// 26.2: one makeHitSound after the loop, broadcast to every watcher
				EtFuturum.networkWrapper.sendToAllAround(new SpearHitFeedbackMessage(player.getEntityId()), new NetworkRegistry.TargetPoint(player.dimension, player.posX, player.posY, player.posZ, 64.0D));
			}
			return null;
		}
	}
}
