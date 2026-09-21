package ganymedes01.etfuturum.mixins.early.waterlog.client;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 1.7.10's {@code RenderGlobal.renderSky} draws the graded sky body with fog on,
 * then calls {@code glDisable(GL_FOG)} (line 1125) before painting the
 * sunrise/sunset colour band, so that band is never fogged. When the camera sits
 * underwater the resulting hard boundary between the fogged near water and the
 * unfogged far band shows up as a visible gap, most notably at sunrise/sunset
 * (and under Angelica, which redraws the sky body itself). Re-enabling the fog
 * for exactly the sunrise/sunset band (from its {@code glShadeModel(GL_SMOOTH)}
 * setup to its {@code glShadeModel(GL_FLAT)} teardown) lets the band blend onto
 * the same EXP fog as the foreground, then drops the fog again before the
 * sun/moon/stars are drawn so those stay crisp. This matches 26.2, where the sky
 * band is shaded with the current fog parameters (FogRenderer uploads a dedicated
 * {@code skyEnd} alongside the render-distance ends, so the band is fogged with
 * whatever fog is active - water fog underwater) and the seam never opens.
 * <p>
 * Applies to real water only; no waterlogged-block feature is involved.
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
		return viewEntity != null && viewEntity.isInsideOfMaterial(Material.water);
	}
}
