package ganymedes01.etfuturum.mixins.early.spear.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ganymedes01.etfuturum.core.utils.SpearChargeTicker;
import ganymedes01.etfuturum.items.ItemSpear;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;

/**
 * 26.2 ItemInHandRenderer switches the first-person swing animation on the held item's
 * SwingAnimation type: case WHACK keeps the vanilla arc, case STAB (spears) calls
 * SpearAnimations.firstPersonAttack instead. The 1.7.10 arc lives in two getSwingProgress-driven
 * blocks of renderItemInFirstPerson: the swing translation before the arm-space shift (L428-431,
 * ordinal 2) and the swing rotation after the 45Y rotate (L437-442, ordinal 3). Zeroing both while
 * a spear is held leaves the plain held-item anchor space, and ItemSpearRenderer applies
 * SpearAnimations.firstPersonAttack there (the map branch ordinals 0/1 and the empty-hand branch
 * 4/5 are untouched). Ordinals per the byte-verified errata: 0/1 = map branch, 2 = item swing
 * translate, 3 = item swing rotate, 4/5 = empty hand.
 */
@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer {

	@Shadow
	private Minecraft mc;
	@Shadow
	private float prevEquippedProgress;
	@Shadow
	private float equippedProgress;
	@Shadow
	private int equippedItemSlot;
	@Shadow
	private ItemStack itemToRender;

	@Redirect(method = "renderItemInFirstPerson(F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityClientPlayerMP;getSwingProgress(F)F", ordinal = 2), require = 1)
	private float etfu$spearStabTranslate(EntityClientPlayerMP player, float partialTicks) {
		return etfu$spearSwingProgress(player, partialTicks);
	}

	@Redirect(method = "renderItemInFirstPerson(F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityClientPlayerMP;getSwingProgress(F)F", ordinal = 3), require = 1)
	private float etfu$spearStabRotate(EntityClientPlayerMP player, float partialTicks) {
		return etfu$spearSwingProgress(player, partialTicks);
	}

	private static float etfu$spearSwingProgress(EntityClientPlayerMP player, float partialTicks) {
		ItemStack held = player.getHeldItem();
		if (held != null && held.getItem() instanceof ItemSpear) {
			return 0.0F; // the stab replaces the arc; run in ItemSpearRenderer's anchor space
		}
		return player.getSwingProgress(partialTicks);
	}

	/**
	 * 26.2 ItemInHandRenderer.tick L565-566 re-times the equip raise: the rise target is
	 * getItemSwapScale(1)^3 instead of 1.0, and since itemSwapTicker resets on a main-hand item
	 * change, drawing a spear (whose swap delay equals its attack duration) stretches the vanilla
	 * ~2.5 tick raise over the full 13-23 tick stab length - the draw animation that re-arms the
	 * player after swapping mid-recovery. The spear path re-runs the vanilla updateEquippedItem
	 * skeleton (same-slot/same-reference flag, NBT refresh adoption, the 0.4/tick clamp and the
	 * < 0.1 visible-item swap) with only the rise target replaced; every other item keeps the
	 * untouched vanilla method. Attacks never replay it: the swing reset in MixinEntityLivingBase
	 * clears only the attack charge (26.2 resetOnlyAttackStrengthTicker).
	 */
	@Inject(method = "updateEquippedItem", at = @At("HEAD"), cancellable = true, require = 1)
	private void etfu$spearEquipRaise(CallbackInfo ci) {
		EntityClientPlayerMP player = this.mc.thePlayer;
		ItemStack held = player.inventory.getCurrentItem();
		if (held == null || !(held.getItem() instanceof ItemSpear)) {
			return; // vanilla path handles every other item
		}
		ci.cancel();

		this.prevEquippedProgress = this.equippedProgress;
		boolean flag = this.equippedItemSlot == player.inventory.currentItem && held == this.itemToRender;

		if (this.itemToRender != null && held != this.itemToRender && held.getItem() == this.itemToRender.getItem() && held.getItemDamage() == this.itemToRender.getItemDamage()) {
			this.itemToRender = held; // same item + damage after an NBT refresh: adopt the new reference
			flag = true;
		}

		float step = 0.4F;
		float swapScale = ((SpearChargeTicker) player).etfu$spearSwapCharge(((ItemSpear) held.getItem()).getAttackDurationTicks());
		float target = flag ? swapScale * swapScale * swapScale : 0.0F;
		this.equippedProgress += MathHelper.clamp_float(target - this.equippedProgress, -step, step);

		if (this.equippedProgress < 0.1F) {
			this.itemToRender = held;
			this.equippedItemSlot = player.inventory.currentItem;
		}
	}
}
