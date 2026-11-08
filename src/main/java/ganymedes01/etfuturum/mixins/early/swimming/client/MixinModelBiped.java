package ganymedes01.etfuturum.mixins.early.swimming.client;

import ganymedes01.etfuturum.elytra.IElytraPlayer;
import ganymedes01.etfuturum.swimming.ISwimmingPlayer;
import ganymedes01.etfuturum.swimming.client.SwimmingRenderState;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBiped.class)
public class MixinModelBiped {

	@Shadow
	public ModelRenderer bipedHead;
	@Shadow
	public ModelRenderer bipedHeadwear;
	@Shadow
	public ModelRenderer bipedRightArm;
	@Shadow
	public ModelRenderer bipedLeftArm;
	@Shadow
	public ModelRenderer bipedRightLeg;
	@Shadow
	public ModelRenderer bipedLeftLeg;

	/**
	 * 26.2 HumanoidModel#setupAnim swim block. Injected at RETURN so it overrides the vanilla
	 * (and elytra) angles; the elytra flight pose wins when it is active.
	 */
	@Inject(method = "setRotationAngles", at = @At("RETURN"))
	private void etfu$setSwimAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
									float headPitch, float scaleFactor, Entity entityIn, CallbackInfo ci) {
		if (entityIn instanceof IElytraPlayer && ((IElytraPlayer) entityIn).etfu$isElytraFlying()) {
			return;
		}
		if (!(entityIn instanceof ISwimmingPlayer) || !(entityIn instanceof EntityLivingBase)) {
			return;
		}

		float swimAmount = ((ISwimmingPlayer) entityIn).etfu$getSwimAmount(SwimmingRenderState.partialTicks);
		if (swimAmount <= 0.0F) {
			return;
		}

		float swimPos = limbSwing % 26.0F;

		this.bipedHead.rotateAngleX = rotLerpRad(swimAmount, this.bipedHead.rotateAngleX, -0.7853982F);
		this.bipedHeadwear.rotateAngleX = this.bipedHead.rotateAngleX;

		if (!((EntityPlayer) entityIn).isUsingItem()) {
			if (swimPos < 14.0F) {
				float q = quadraticArmUpdate(swimPos) / quadraticArmUpdate(14.0F);

				this.bipedLeftArm.rotateAngleX = rotLerpRad(swimAmount, this.bipedLeftArm.rotateAngleX, 0.0F);
				this.bipedRightArm.rotateAngleX = lerp(swimAmount, this.bipedRightArm.rotateAngleX, 0.0F);
				this.bipedLeftArm.rotateAngleY = rotLerpRad(swimAmount, this.bipedLeftArm.rotateAngleY, (float) Math.PI);
				this.bipedRightArm.rotateAngleY = lerp(swimAmount, this.bipedRightArm.rotateAngleY, (float) Math.PI);
				this.bipedLeftArm.rotateAngleZ = rotLerpRad(swimAmount, this.bipedLeftArm.rotateAngleZ, (float) Math.PI + 1.8707964F * q);
				this.bipedRightArm.rotateAngleZ = lerp(swimAmount, this.bipedRightArm.rotateAngleZ, (float) Math.PI - 1.8707964F * q);
			} else if (swimPos < 22.0F) {
				float part = (swimPos - 14.0F) / 8.0F;

				this.bipedLeftArm.rotateAngleX = rotLerpRad(swimAmount, this.bipedLeftArm.rotateAngleX, 1.5707964F * part);
				this.bipedRightArm.rotateAngleX = lerp(swimAmount, this.bipedRightArm.rotateAngleX, 1.5707964F * part);
				this.bipedLeftArm.rotateAngleY = rotLerpRad(swimAmount, this.bipedLeftArm.rotateAngleY, (float) Math.PI);
				this.bipedRightArm.rotateAngleY = lerp(swimAmount, this.bipedRightArm.rotateAngleY, (float) Math.PI);
				this.bipedLeftArm.rotateAngleZ = rotLerpRad(swimAmount, this.bipedLeftArm.rotateAngleZ, 5.012389F - 1.8707964F * part);
				this.bipedRightArm.rotateAngleZ = lerp(swimAmount, this.bipedRightArm.rotateAngleZ, 1.2707963F + 1.8707964F * part);
			} else if (swimPos < 26.0F) {
				float part = (swimPos - 22.0F) / 4.0F;

				this.bipedLeftArm.rotateAngleX = rotLerpRad(swimAmount, this.bipedLeftArm.rotateAngleX, 1.5707964F - 1.5707964F * part);
				this.bipedRightArm.rotateAngleX = lerp(swimAmount, this.bipedRightArm.rotateAngleX, 1.5707964F - 1.5707964F * part);
				this.bipedLeftArm.rotateAngleY = rotLerpRad(swimAmount, this.bipedLeftArm.rotateAngleY, (float) Math.PI);
				this.bipedRightArm.rotateAngleY = lerp(swimAmount, this.bipedRightArm.rotateAngleY, (float) Math.PI);
				this.bipedLeftArm.rotateAngleZ = rotLerpRad(swimAmount, this.bipedLeftArm.rotateAngleZ, (float) Math.PI);
				this.bipedRightArm.rotateAngleZ = lerp(swimAmount, this.bipedRightArm.rotateAngleZ, (float) Math.PI);
			}
		}

		this.bipedLeftLeg.rotateAngleX = lerp(swimAmount, this.bipedLeftLeg.rotateAngleX, 0.3F * MathHelper.cos(limbSwing * 0.33333334F + (float) Math.PI));
		this.bipedRightLeg.rotateAngleX = lerp(swimAmount, this.bipedRightLeg.rotateAngleX, 0.3F * MathHelper.cos(limbSwing * 0.33333334F));
	}

	@Unique
	private static float lerp(float delta, float start, float end) {
		return start + (end - start) * delta;
	}

	/**
	 * 26.2 AnimationUtils#rotLerpRad: wraps the delta to the [-180, 180) degree range.
	 */
	@Unique
	private static float rotLerpRad(float delta, float start, float end) {
		float f = end - start;
		while (f < -(float) Math.PI) {
			f += (float) Math.PI * 2.0F;
		}
		while (f >= (float) Math.PI) {
			f -= (float) Math.PI * 2.0F;
		}
		return start + delta * f;
	}

	@Unique
	private static float quadraticArmUpdate(float x) {
		return -65.0F * x + x * x;
	}
}
