package ganymedes01.etfuturum.mixins.early.waterlog.client;

import ganymedes01.etfuturum.waterlog.VisualWaterlog;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The stateless visual waterlogging paints believable water, but it is not
 * real water, so {@code isInsideOfMaterial(Material.water)} never reports a
 * waterlogged stair, slab or thin block. This funnels the visually
 * waterlogged cells through the vanilla underwater-overlay path instead of
 * drawing a separate quad: redirecting the water check in
 * {@code renderOverlays} makes a waterlogged cell trigger the exact same
 * {@code renderWarpedTextureOverlay} the real water uses, under the same
 * perspective projection, so the two can never diverge in scale or tiling.
 * <p>
 * A second injection dims the overlay itself. Vanilla 1.7.10 draws it at
 * alpha 0.5, which lets the noisy {@code underwater.png} read as bright
 * speckles; 26.2 draws the same texture at alpha 0.1 and leans on the water
 * fog for the underwater look instead (the fog is what the EntityRenderer
 * mixin restores for fake water). Lowering the constant here dims both the
 * real and the visually waterlogged overlay together, keeping them in lock
 * step and removing the speckle.
 */
@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer {

    @Redirect(
            method = "renderOverlays(F)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isInsideOfMaterial(Lnet/minecraft/block/material/Material;)Z"))
    private boolean etfuturum$visualWaterCountsAsWater(Entity entity, Material material) {
        if (material == Material.water && entity instanceof EntityLivingBase
                && VisualWaterlog.isEyeInVisualWater((EntityLivingBase) entity)) {
            return true;
        }
        return entity.isInsideOfMaterial(material);
    }

    @ModifyConstant(method = "renderWarpedTextureOverlay(F)V", constant = @Constant(floatValue = 0.5F))
    private float etfuturum$dimWaterOverlay(float original) {
        return 0.1F;
    }
}