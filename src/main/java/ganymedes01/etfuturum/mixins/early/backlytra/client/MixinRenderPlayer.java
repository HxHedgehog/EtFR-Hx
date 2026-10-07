package ganymedes01.etfuturum.mixins.early.backlytra.client;

import ganymedes01.etfuturum.elytra.IElytraPlayer;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderPlayer.class)
public class MixinRenderPlayer {
	@Inject(method = "rotateCorpse(Lnet/minecraft/client/entity/AbstractClientPlayer;FFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RendererLivingEntity;rotateCorpse(Lnet/minecraft/entity/EntityLivingBase;FFF)V", shift = At.Shift.AFTER))
	private void rotateForElytra(AbstractClientPlayer player, float p_77043_2_, float p_77043_3_, float partialTicks, CallbackInfo ci) {
		if (player instanceof IElytraPlayer iep && iep.etfu$isElytraFlying()) {
			float f = iep.etfu$getTicksElytraFlying() + partialTicks;
			float f1 = MathHelper.clamp_float(f * f / 100.0F, 0.0F, 1.0F);
			GL11.glRotatef(f1 * (-90.0F - player.rotationPitch), 1.0F, 0.0F, 0.0F);

			/* 26.2 AvatarRenderer.extractFlightData: the movement is interpolated between
			 * the previous and current tick before comparing it with the look vector,
			 * the threshold is 1.0E-5 on the horizontal distance squared, and the dot
			 * product is passed through Math.abs so gliding backwards doesn't flip the
			 * model more than 90 degrees. */
			double motionX = iep.etfu$getPrevMotionX() + (player.motionX - iep.etfu$getPrevMotionX()) * partialTicks;
			double motionZ = iep.etfu$getPrevMotionZ() + (player.motionZ - iep.etfu$getPrevMotionZ()) * partialTicks;
			Vec3 vec3d = player.getLook(partialTicks);
			double d0 = motionX * motionX + motionZ * motionZ;
			double d1 = vec3d.xCoord * vec3d.xCoord + vec3d.zCoord * vec3d.zCoord;

			if (d0 > 1.0E-5D && d1 > 1.0E-5D) {
				double d2 = (motionX * vec3d.xCoord + motionZ * vec3d.zCoord) / (Math.sqrt(d0) * Math.sqrt(d1));
				double d3 = motionX * vec3d.zCoord - motionZ * vec3d.xCoord;
				GL11.glRotatef((float) (Math.signum(d3) * Math.acos(Math.min(1.0, Math.abs(d2)))) * 180.0F / (float) Math.PI, 0.0F, 1.0F, 0.0F);
			}
		}
	}
}
