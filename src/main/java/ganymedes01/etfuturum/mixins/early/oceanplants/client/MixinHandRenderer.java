package ganymedes01.etfuturum.mixins.early.oceanplants.client;

import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Angelica's shader hand pipeline classifies held items by their block's
 * vanilla render pass: pass-1 blocks (our pseudo-waterlogged ocean plants)
 * are routed to the translucent hand pass (enableBlend + depthMask(false)),
 * which shatters their flat 2D icon cards into the pixel-slice mesh of
 * renderItemIn2D. Flat sprite items (items atlas) are opaque cutout cards
 * regardless of the block's world render pass, so exclude them from the
 * translucency check.
 * <p>
 * Targets Angelica's {@code net.coderbot.iris.pipeline.HandRenderer} by name;
 * registered only when Angelica is present. No compile-time dependency.
 */
@Mixin(targets = "net.coderbot.iris.pipeline.HandRenderer", remap = false)
public abstract class MixinHandRenderer {

	@Inject(method = "isItemTranslucent", at = @At("HEAD"), cancellable = true, remap = false)
	private void etfuturum$flatIconBlocksAreNotTranslucent(ItemStack heldItem, CallbackInfoReturnable<Boolean> cir) {
		if (heldItem != null && heldItem.getItemSpriteNumber() != 0) {
			cir.setReturnValue(Boolean.FALSE);
		}
	}
}
