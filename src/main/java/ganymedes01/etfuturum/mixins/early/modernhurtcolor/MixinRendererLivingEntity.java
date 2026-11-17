package ganymedes01.etfuturum.mixins.early.modernhurtcolor;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RendererLivingEntity.class)
public abstract class MixinRendererLivingEntity {

	/**
	 * 26.2 OverlayTexture packs the hurt overlay as the constant row 0xB2FF0000 - pure red at
	 * alpha 178/255, mixed over the lit body regardless of scene lighting. 1.7.10 instead
	 * repaints the model with glColor4f(getBrightness(), 0, 0, 0.4), which scales the flash
	 * with light level and reads far darker and duller than modern versions.
	 *
	 * <p>The two red call sites (body repaint and the armor/back-item repaints) are the only
	 * glColor4f calls in doRender passing green == blue == 0 with alpha 0.4, so they are
	 * selected by argument shape instead of a fragile ordinal. The flash is also rendered
	 * unlit so the colour stays constant like 26.2; lighting is restored when the overlay
	 * block closes (its final glDepthFunc call, ordinal 3 - after the two glint-block and
	 * block-entry glDepthFunc sites).</p>
	 */
	private static final float ETFU$HURT_ALPHA = 178.0F / 255.0F;

	@WrapOperation(method = "doRender",
			at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glColor4f(FFFF)V"),
			require = 2)
	private void etfu$modernHurtColor(float red, float green, float blue, float alpha, Operation<Void> original) {
		if (green == 0.0F && blue == 0.0F && alpha == 0.4F) {
			GL11.glDisable(GL11.GL_LIGHTING);
			original.call(1.0F, 0.0F, 0.0F, ETFU$HURT_ALPHA);
		} else {
			original.call(red, green, blue, alpha);
		}
	}

	@Inject(method = "doRender",
			at = @At(value = "INVOKE",
					target = "Lorg/lwjgl/opengl/GL11;glDepthFunc(I)V",
					ordinal = 3,
					shift = At.Shift.BEFORE),
			require = 1)
	private void etfu$restoreLighting(EntityLivingBase entity, double x, double y, double z, float yaw,
			float partialTicks, CallbackInfo ci) {
		if (entity.hurtTime > 0 || entity.deathTime > 0) {
			GL11.glEnable(GL11.GL_LIGHTING);
		}
	}
}
