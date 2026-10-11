package ganymedes01.etfuturum.mixins.early.spear.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ganymedes01.etfuturum.core.utils.SpearChargeTicker;
import ganymedes01.etfuturum.items.ItemSpear;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

/**
 * 26.2 LivingEntity.getCurrentSwingDuration derives the swing length from the held item's
 * SwingAnimation component; spears register SwingAnimation(STAB, attackDuration * 20) so the stab
 * plays over the full 13-23 tick attack duration (and updateSwingTime divides by it the same way
 * 1.7.10 divides swingProgress by this method). 1.7.10 hardcodes 6 ticks; keep the vanilla
 * haste/mining-fatigue adjustments (26.2 applies the identical shape on top of the component value,
 * with haste checked first). Also drives the swingItem mid-swing re-click gate, which 26.2 shares
 * (swingTime >= currentSwingDuration / 2).
 */
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase {

	@Shadow
	public abstract ItemStack getHeldItem();

	@Shadow
	public abstract boolean isPotionActive(Potion potion);

	@Shadow
	public abstract PotionEffect getActivePotionEffect(Potion potion);

	@Inject(method = "getArmSwingAnimationEnd", at = @At("HEAD"), cancellable = true)
	private void etfu$spearSwingDuration(CallbackInfoReturnable<Integer> cir) {
		ItemStack held = this.getHeldItem();
		if (held == null || !(held.getItem() instanceof ItemSpear)) {
			return;
		}
		int duration = ((ItemSpear) held.getItem()).getAttackDurationTicks();
		if (this.isPotionActive(Potion.digSpeed)) {
			cir.setReturnValue(duration - (1 + this.getActivePotionEffect(Potion.digSpeed).getAmplifier()));
			return;
		}
		if (this.isPotionActive(Potion.digSlowdown)) {
			cir.setReturnValue(duration + (1 + this.getActivePotionEffect(Potion.digSlowdown).getAmplifier()) * 2);
			return;
		}
		cir.setReturnValue(duration);
	}

	/**
	 * 26.2 resets the attack charge when an attack goes through: Player.attack/PiercingWeapon.attack
	 * call onAttack() (resetOnlyAttackStrengthTicker), and the server-side swing override
	 * (ServerPlayer.swing L1878-1882) resets it too. 1.7.10 funnels every left-click swing through
	 * EntityLivingBase.swingItem - the client's clickMouse (L1459) and the server's processAnimation
	 * (C0A packet, L800) both land here - so one reset point covers both sides. Re-clicking while
	 * the stab plays is blocked by the gate mixins before swingItem ever runs, so a blocked click
	 * never resets the charge.
	 */
	@Inject(method = "swingItem", at = @At("HEAD"))
	private void etfu$spearAttackReset(CallbackInfo ci) {
		Object self = this;
		if (!(self instanceof EntityPlayer)) {
			return; // mobs never carry the charge tickers
		}
		ItemStack held = this.getHeldItem();
		if (held != null && held.getItem() instanceof ItemSpear) {
			((SpearChargeTicker) self).etfu$resetSpearAttackCharge();
		}
	}
}
