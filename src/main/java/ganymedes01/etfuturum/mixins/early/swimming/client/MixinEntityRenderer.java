package ganymedes01.etfuturum.mixins.early.swimming.client;

import ganymedes01.etfuturum.elytra.IElytraPlayer;
import ganymedes01.etfuturum.swimming.ISwimmingPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.culling.Frustrum;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {

	@Shadow
	private Minecraft mc;

	/**
	 * The distance between the vanilla 1.7.10 camera anchor (the interpolated posY, i.e. the
	 * standing eye) and the real camera position (feet + smoothed pose eye height), recomputed
	 * once per orientCamera frame. 26.2 carries the camera on a Camera object whose position
	 * already sits at the lerped pose eye height; 1.7.10 has no such anchor, so every camera
	 * consumer is shifted by this delta instead. Zero outside the swimming poses.
	 */
	@Unique
	private float etfu$cameraDelta;

	/**
	 * The value that replaces the hardcoded 1.62F in orientCamera's anchor while a pose camera is
	 * active: f1 = yOffset - (ySize + cameraEyeHeight) yields exactly the real camera anchor.
	 * Float.NaN means "bypass" (elytra glide or non-player view): keep the vanilla constant.
	 */
	@Unique
	private float etfu$anchorExtra;

	/**
	 * 26.2 Player#tick converges the camera eye height onto the pose eye height every tick and
	 * the Camera position is derived from it; the 1.7.10 camera has no pose awareness. The delta
	 * is captured once per frame here and consumed by both injections below. Elytra gliding keeps
	 * the vanilla anchor (its pose keeps height 1.8), as do non-player render views.
	 */
	@Inject(method = "orientCamera", at = @At("HEAD"))
	private void etfu$swimCameraBegin(float partialTicks, CallbackInfo ci) {
		this.etfu$cameraDelta = 0.0F;
		this.etfu$anchorExtra = Float.NaN;
		if (this.mc.renderViewEntity instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer) this.mc.renderViewEntity;
			if (!(player instanceof IElytraPlayer && ((IElytraPlayer) player).etfu$isElytraFlying())) {
				float cameraEye = ((ISwimmingPlayer) player).etfu$getCameraEyeHeight(partialTicks);
				this.etfu$cameraDelta = player.yOffset - player.ySize - cameraEye;
				this.etfu$anchorExtra = player.ySize + cameraEye;
			}
		}
	}

	/**
	 * orientCamera derives its entire camera anchor from f1 = yOffset - 1.62 (L555): the
	 * third-person anti-clip rays start at d1 = posY - f1 (L609), the clip distance is measured
	 * from the same origin (L613), the final glTranslatef(0, f1, 0) places the camera (L645) and
	 * the cloud fog probe reuses d1 (L647). Replacing the constant with ySize + cameraEyeHeight
	 * makes f1 = yOffset - ySize - cameraEyeHeight = the real camera anchor, moving the rays, the
	 * distance reference, the camera itself and the cloud fog probe onto the smoothed pose camera
	 * in one step - exactly the 26.2 Camera semantics. This fixes both the third-person camera
	 * being dragged into the tunnel ceiling while crawling (the standing-eye ray origin was inside
	 * a block, so all 8 rays hit at ~0 distance) and the third-person x-ray when looking up from
	 * the seabed (the distance was over-counted by the full camera offset for vertical views).
	 *
	 * IMPLEMENTATION NOTE: this used to be a @Redirect on the GETFIELD of Entity;yOffset, but that
	 * scanned 0 targets both in dev and production (fail: "expected 1 invocation(s) but 0
	 * succeeded"), so the constant is anchored instead - an LDC match needs no field/class
	 * remapping at all. The rayTraceBlocks callpoint inside orientCamera must also not be touched
	 * from here: Hodgepodge and ArchaicFix each already @Redirect it (owner WorldClient - the
	 * declared type of Minecraft.theWorld), and only one mixin can own a callpoint.
	 */
	@ModifyConstant(method = "orientCamera", constant = @Constant(floatValue = 1.62F), require = 1)
	private float etfu$poseCameraAnchor(float original) {
		return Float.isNaN(this.etfu$anchorExtra) ? original : this.etfu$anchorExtra;
	}

	/**
	 * 26.2 LevelRenderer prepares the culling frustum from the real camera position. 1.7.10
	 * anchors it at the raw interpolated posY (L1256), which is the standing eye and sits above
	 * the swimming/crawling camera - sections near the low camera get mis-culled and terrain
	 * vanishes when looking up from the seabed. Shift the anchor down by the same delta.
	 */
	@Redirect(method = "renderWorld",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/culling/Frustrum;setPosition(DDD)V"),
			require = 1)
	private void etfu$frustumAtRealCamera(Frustrum frustrum, double x, double y, double z) {
		frustrum.setPosition(x, y - this.etfu$cameraDelta, z);
	}
}
