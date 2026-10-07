package ganymedes01.etfuturum.mixins.early.backlytra.client;

import ganymedes01.etfuturum.elytra.IElytraPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {
	@Shadow
	private Minecraft mc;

	@Inject(method = "orientCamera", at = @At("TAIL"))
	private void adjustCameraForElytra(float partialTicks, CallbackInfo ci) {
		if (this.mc.renderViewEntity instanceof IElytraPlayer) {
			if (((IElytraPlayer) this.mc.renderViewEntity).etfu$isElytraFlying()) {
				/* 26.2 fall flying eye height is 0.4 (Avatar.POSES) instead of the standing 1.62,
				 * so shift the camera down by 1.22 in both first and third person. A +Y glTranslate
				 * here moves the camera down: it is applied first in world space. */
				GL11.glTranslatef(0, 1.22f, 0f);
			}
		}
	}
}
