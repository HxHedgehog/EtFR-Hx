package ganymedes01.etfuturum.mixins.early.spear.client;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.core.utils.SpearChargeTicker;
import ganymedes01.etfuturum.core.utils.SpearKinetics;
import ganymedes01.etfuturum.items.ItemSpear;
import ganymedes01.etfuturum.lib.Reference;
import ganymedes01.etfuturum.network.SpearJabMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.item.ItemStack;

/**
 * 26.2 Minecraft.startAttack: the MINIMUM_ATTACK_CHARGE gate (L1525) precedes everything - no
 * swing, no stab, no block break, no miss time while the charge is short - and the PIERCING_WEAPON
 * branch (L1530-1534) comes before the crosshair switch, so a spear left click ALWAYS jabs and
 * NEVER starts mining, no matter whether the crosshair rests on air, an entity or a block.
 *
 * 1.7.10's clickMouse (func_147116_af) swings first (L1459) and then either attacks the picked
 * entity (static ~3-block crosshair ray, the reason fast elytra dives never landed hits) or starts
 * digging, so both halves are replaced: the gate cancels undercharged clicks at HEAD, and a ready
 * spear click cancels the vanilla body, sweeps the official corridor (26.2 ProjectileUtil
 * semantics, incl. block occlusion and the elytra eye compensation) and reports the hits to the
 * server, which re-tests the charge gate before settling. swingItem() then resets the charge
 * (MixinEntityLivingBase) and broadcasts the C0A animation that drives the stab pose.
 *
 * 26.2 Minecraft.continueAttack (L1473-1495) returns right after the release reset while a
 * piercing weapon is held, so 1.7.10's hold-to-mine tick (func_147115_a, run every frame while the
 * attack key is down) is cancelled on the down frames only - a spear held to a wall never mines.
 */
@Mixin(Minecraft.class)
public abstract class MixinMinecraft {

	@Shadow
	public EntityClientPlayerMP thePlayer;

	@Inject(method = "func_147116_af", at = @At("HEAD"), cancellable = true, require = 1)
	private void etfu$spearClickMouse(CallbackInfo ci) {
		ItemStack held = this.thePlayer.getCurrentEquippedItem();
		if (held == null || !(held.getItem() instanceof ItemSpear)) {
			return;
		}
		ItemSpear spear = (ItemSpear) held.getItem();
		ci.cancel();
		float charge = ((SpearChargeTicker) this.thePlayer).etfu$spearAttackCharge(0, spear.getAttackDurationTicks());
		if (charge < 1.0F) {
			return; // 26.2 cannotAttackWithItem(heldItem, 0): nothing happens at all
		}
		List<SpearKinetics.EntityHit> hits = SpearKinetics.getHitEntitiesAlong(this.thePlayer.worldObj, this.thePlayer, 1.0F);
		int[] ids = new int[hits.size()];
		for (int i = 0; i < ids.length; i++) {
			ids[i] = hits.get(i).entity.getEntityId();
		}
		EtFuturum.networkWrapper.sendToServer(new SpearJabMessage(ids));
		// 26.2 piercingAttack(): local onAttack + swing; the whoosh plays on the attacking client
		// because 1.7.10 playSoundAtEntity cannot exclude the attacker from the server broadcast
		this.thePlayer.swingItem();
		this.thePlayer.worldObj.playSound(this.thePlayer.posX, this.thePlayer.posY, this.thePlayer.posZ, Reference.MOD_ID + ":" + spear.getSoundPrefix() + ".attack", 1.0F, 1.0F, false);
	}

	@Inject(method = "func_147115_a", at = @At("HEAD"), cancellable = true, require = 1)
	private void etfu$spearHoldMining(boolean leftClick, CallbackInfo ci) {
		if (!leftClick) {
			return; // 26.2 still clears missTime on the release frame before the piercing check
		}
		ItemStack held = this.thePlayer.getCurrentEquippedItem();
		if (held != null && held.getItem() instanceof ItemSpear) {
			ci.cancel(); // 26.2 continueAttack: piercing weapons never continue into block damage
		}
	}
}
