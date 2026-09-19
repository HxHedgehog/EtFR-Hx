package ganymedes01.etfuturum.mixins.early.waterlog.client;

import ganymedes01.etfuturum.waterlog.VisualWaterlog;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 1.7.10's {@code RenderGlobal.renderSky} draws the graded sky body with fog on,
 * then calls {@code glDisable(GL_FOG)} (line 1125) before painting the
 * sunrise/sunset colour band, so that band is never fogged. When the camera sits
 * underwater (real water, or a visually waterlogged volume) the resulting hard
 * boundary between the fogged near water and the unfogged far band shows up as a
 * visible gap, most notably at sunrise/sunset. Re-enabling the fog for exactly
 * the sunrise/sunset band (from its {@code glShadeModel(GL_SMOOTH)} setup to its
 * {@code glShadeModel(GL_FLAT)} teardown) lets the band blend onto the same EXP
 * fog as the foreground, then drops the fog again before the sun/moon/stars are
 * drawn so those stay crisp, matching 1.26.2's closure of the water/sky seam.
 */
@Mixin(RenderGlobal.class)
public abstract class MixinRenderGlobal {

    @Redirect(
            method = "renderSky(F)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glShadeModel(I)V", remap = false))
    private void etfuturum$fogSunriseBandOnly(int mode) {
        if (etfuturum$isEyeInWaterForSky()) {
            if (mode == GL11.GL_SMOOTH) {
                // Sunrise/sunset band begins: fog it so it blends with the water.
                GL11.glEnable(GL11.GL_FOG);
            } else if (mode == GL11.GL_FLAT) {
                // Band ends: drop the fog so sun/moon/stars stay crisp.
                GL11.glDisable(GL11.GL_FOG);
            }
        }
        GL11.glShadeModel(mode);
    }

    private static boolean etfuturum$isEyeInWaterForSky() {
        Entity viewEntity = Minecraft.getMinecraft().renderViewEntity;
        if (viewEntity == null) {
            return false;
        }
        if (viewEntity.isInsideOfMaterial(Material.water)) {
            return true;
        }
        return viewEntity instanceof EntityLivingBase
                && VisualWaterlog.isEyeInVisualWater((EntityLivingBase) viewEntity);
    }
}