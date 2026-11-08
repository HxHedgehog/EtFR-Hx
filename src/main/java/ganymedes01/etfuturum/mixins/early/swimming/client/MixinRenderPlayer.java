package ganymedes01.etfuturum.mixins.early.swimming.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import ganymedes01.etfuturum.elytra.IElytraPlayer;
import ganymedes01.etfuturum.swimming.ISwimmingPlayer;
import ganymedes01.etfuturum.swimming.client.SwimmingRenderState;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderPlayer.class)
public class MixinRenderPlayer {

	/**
	 * 26.2 keeps the crouching pose separate from the swimming pose, so the vanilla
	 * "isSneak" model pose must be disabled while swimming or crawling.
	 */
	@ModifyExpressionValue(method = "doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/AbstractClientPlayer;isSneaking()Z"))
	private boolean etfu$hideSneakForSwimming(boolean original, AbstractClientPlayer player) {
		return original && !(player instanceof ISwimmingPlayer && ((ISwimmingPlayer) player).etfu$isVisuallySwimming());
	}

	/**
	 * 26.2 AvatarRenderer: when swimAmount > 0 the model is rotated around X by
	 * (-90 - pitch) * swimAmount (or -90 * swimAmount when not in water), and when the pose is
	 * SWIMMING the model is additionally translated by (0, -1, 0.3).
	 */
	@Inject(method = "rotateCorpse(Lnet/minecraft/client/entity/AbstractClientPlayer;FFF)V",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/entity/RendererLivingEntity;rotateCorpse(Lnet/minecraft/entity/EntityLivingBase;FFF)V",
					shift = At.Shift.AFTER))
	private void etfu$rotateForSwimming(AbstractClientPlayer player, float p1, float p2, float partialTicks, CallbackInfo ci) {
		SwimmingRenderState.partialTicks = partialTicks;

		if (player instanceof IElytraPlayer && ((IElytraPlayer) player).etfu$isElytraFlying()) {
			return;
		}
		if (!(player instanceof ISwimmingPlayer)) {
			return;
		}

		ISwimmingPlayer swimmingPlayer = (ISwimmingPlayer) player;
		float swimAmount = swimmingPlayer.etfu$getSwimAmount(partialTicks);
		if (swimAmount <= 0.0F) {
			return;
		}

		boolean inWater = swimmingPlayer.etfu$isSwimming() || player.isInWater();
		float target = inWater ? -90.0F - player.rotationPitch : -90.0F;
		GL11.glRotatef(target * swimAmount, 1.0F, 0.0F, 0.0F);

		if (swimmingPlayer.etfu$isVisuallySwimming()) {
			GL11.glTranslatef(0.0F, -1.0F, 0.3F);
		}
	}
}
