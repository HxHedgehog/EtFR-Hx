package ganymedes01.etfuturum.client.handler;

import java.util.ArrayList;
import java.util.List;

import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.core.utils.SpearKinetics;
import ganymedes01.etfuturum.items.ItemSpear;
import ganymedes01.etfuturum.network.SpearStabMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;

/**
 * Local-player stab detection, the 1.7.10 stand-in for 26.2's server-side KineticWeapon.damageEntities.
 * Runs every use tick after the wound-up delay: projects the attacker speed along the look vector,
 * raycasts the spear AttackRange, computes the relative speed per target and reports the lot; the
 * server re-tests everything before settling. The per-target contact cooldown mirrors 26.2
 * rememberStabbedEntity (one condition attempt per 10 ticks window).
 */
public class SpearClientTickHandler {

	private static final SpearKinetics.StabCooldowns COOLDOWNS = new SpearKinetics.StabCooldowns();

	public static void onSpearUseTick(ItemSpear spear, ItemStack stack, EntityPlayer player, int count) {
		Minecraft mc = Minecraft.getMinecraft();
		if (!(player instanceof EntityPlayerSP) || player != mc.thePlayer)
			return;
		// 26.2 damageEntities: ticksUsed = useDuration - remaining, then past the delay window
		int ticksUsed = ItemSpear.USE_DURATION - count - spear.getDelayTicks();
		if (ticksUsed < 0)
			return;
		Vec3 look = player.getLook(1.0F);
		// 26.2 attackerSpeedProjection = look . speed; sample last tick's full displacement
		// (motionX at this point is only the post-friction residual, underreading sprint 5.6 -> 3.1)
		double attackerSpeed = SpearKinetics.projection(look, SpearKinetics.getTravelVelocity(player));
		List<SpearKinetics.EntityHit> hits = SpearKinetics.getHitEntitiesAlong(player.worldObj, player, 1.0F);
		List<Integer> targetIds = new ArrayList<>();
		List<Float> relativeSpeeds = new ArrayList<>();
		long now = player.worldObj.getTotalWorldTime();
		COOLDOWNS.pruneOlderThan(now, 200L);
		for (SpearKinetics.EntityHit hit : hits) {
			Entity target = hit.entity;
			if (COOLDOWNS.wasRecentlyStabbed(player, target, SpearKinetics.CONTACT_COOLDOWN_TICKS, now))
				continue;
			double targetSpeed = SpearKinetics.projection(look, SpearKinetics.getMotion(target));
			double relativeSpeed = Math.max(0.0D, attackerSpeed - targetSpeed);
			// player actionFactor is 1.0
			boolean dismounts = SpearKinetics.testCondition(ticksUsed, spear.getDismountTicks(), attackerSpeed, spear.getDismountMinSpeed(), relativeSpeed, 0.0D, 1.0D);
			boolean knockback = SpearKinetics.testCondition(ticksUsed, spear.getKnockbackTicks(), attackerSpeed, spear.getKnockbackMinSpeed(), relativeSpeed, 0.0D, 1.0D);
			boolean damage = SpearKinetics.testCondition(ticksUsed, spear.getDamageTicks(), attackerSpeed, 0.0D, relativeSpeed, spear.getDamageMinRelativeSpeed(), 1.0D);
			if (!dismounts && !knockback && !damage)
				continue;
			// INTENTIONAL DEVIATION from 26.2 KineticWeapon (fixes MC-310858): the official code
			// remembers the target BEFORE the speed tests, so a target merely entering the corridor
			// eats the whole 10t contact window even when nothing lands - the "spear keeps missing"
			// feel. Here only an actual reported attempt consumes the window.
			COOLDOWNS.rememberStabbedEntity(player, target, now);
			targetIds.add(Integer.valueOf(target.getEntityId()));
			relativeSpeeds.add(Float.valueOf((float) relativeSpeed));
		}
		if (targetIds.isEmpty())
			return;
		int[] ids = new int[targetIds.size()];
		float[] speeds = new float[relativeSpeeds.size()];
		for (int i = 0; i < ids.length; i++) {
			ids[i] = targetIds.get(i).intValue();
			speeds[i] = relativeSpeeds.get(i).floatValue();
		}
		EtFuturum.networkWrapper.sendToServer(new SpearStabMessage(ticksUsed, (float) attackerSpeed, ids, speeds));
	}

	private SpearClientTickHandler() {
	}
}
