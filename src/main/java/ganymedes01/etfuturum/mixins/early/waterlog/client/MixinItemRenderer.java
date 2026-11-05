package ganymedes01.etfuturum.mixins.early.waterlog.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;

/**
 * Vanilla 1.7.10 draws the underwater overlay at alpha 0.5, which lets the noisy
 * {@code underwater.png} read as bright speckles. 26.2 draws the same texture at
 * alpha 0.1 and leans on the water fog for the underwater look instead
 * (ScreenEffectRenderer#submitWater: {@code ARGB.colorFromFloat(0.1f, ...)}).
 * Lowering the constant here dims the overlay for real water, matching that
 * look. Unconditional - it touches the shared overlay path, not any
 * waterlogged-block feature.
 */
@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer {

	@Shadow
	private Minecraft mc;

	@ModifyConstant(method = "renderWarpedTextureOverlay(F)V", constant = @Constant(floatValue = 0.5F))
	private float etfuturum$dimWaterOverlay(float original) {
		return 0.1F;
	}
}
