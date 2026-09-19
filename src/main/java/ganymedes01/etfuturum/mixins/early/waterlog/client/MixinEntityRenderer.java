package ganymedes01.etfuturum.mixins.early.waterlog.client;

import ganymedes01.etfuturum.waterlog.VisualWaterlog;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The underwater look splits across three independent vanilla checks: fog
 * colour, fog density, and FOV.
 * <ul>
 * <li>{@code updateFogColor} computes the fog RGB (the blue-green
 * {@code 0.02/0.02/0.2}), and</li>
 * <li>{@code setupFog} sets the fog mode+density (the {@code EXP} 0.1 fog that
 * actually walls off the view), and</li>
 * <li>{@code getFOVModifier} narrows the FOV to {@code 60/70} under water.</li>
 * </ul>
 * A visually waterlogged cell takes the vanilla non-water path in all three, so
 * the camera under fake water keeps normal visibility: a dimmed distance, no
 * near fog, and no underwater FOV narrowing. Redirecting the water material
 * probe in all three methods makes each take the water branch whenever the view
 * entity's eye sits inside a visually waterlogged volume, matching real water's
 * blue-green colour, walling-off density and narrowed FOV without touching the
 * breathing, movement or air-bar logic.
 */
@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {

    @Redirect(
            method = "updateFogColor(F)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getMaterial()Lnet/minecraft/block/material/Material;", ordinal = 0))
    private Material etfuturum$visualWaterFogColor(Block block) {
        return etfuturum$waterMaterialWhenEyeInVisualWater(block);
    }

    @Redirect(
            method = "setupFog(IF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getMaterial()Lnet/minecraft/block/material/Material;", ordinal = 0))
    private Material etfuturum$visualWaterFogDensity(Block block) {
        return etfuturum$waterMaterialWhenEyeInVisualWater(block);
    }

    @Redirect(
            method = "getFOVModifier(FZ)F",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getMaterial()Lnet/minecraft/block/material/Material;", ordinal = 0))
    private Material etfuturum$visualWaterFov(Block block) {
        return etfuturum$waterMaterialWhenEyeInVisualWater(block);
    }

    private static Material etfuturum$waterMaterialWhenEyeInVisualWater(Block block) {
        Material material = block.getMaterial();
        if (material != Material.water && material != Material.lava) {
            Entity viewEntity = Minecraft.getMinecraft().renderViewEntity;
            if (viewEntity instanceof EntityLivingBase
                    && VisualWaterlog.isEyeInVisualWater((EntityLivingBase) viewEntity)) {
                return Material.water;
            }
        }
        return material;
    }
}