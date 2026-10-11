package ganymedes01.etfuturum.mixins.early.spear.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ganymedes01.etfuturum.client.renderer.item.SpearAnimations;
import ganymedes01.etfuturum.items.ItemSpear;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * 26.2 ArmPose.SPEAR: holding a spear alone (ItemTags.SPEARS source of AvatarRenderer#getArmPose)
 * switches the right arm to the raised pose via HumanoidModel#poseRightArm case 10 ->
 * SpearAnimations.thirdPersonHandUse, which overwrites the vanilla arm angles. Using it stacks the
 * sway/raise terms on top. A left-click swing with a stab item keeps the SPEAR pose
 * (AvatarRenderer L136-138) and HumanoidModel#setupAttackAnimation runs AFTER poseArm, stacking
 * SpearAnimations.thirdPersonAttackHand on the raised pose; that order is mirrored here on
 * ModelBiped, whose inline onGround block (L156-176) is the same body twist + arm shift the 26.2
 * setupAttackAnimation applies before its swingAnimationType switch. attackTime mirrors 26.2
 * state.attackTime = getAttackAnim(partialTicks): the interpolated swing progress, which here is
 * the swing progress sampled at the current frame's renderPartialTicks (RendererLivingEntity L90
 * feeds exactly this value into ModelBase.onGround for the vanilla arc).
 */
@Mixin(ModelBiped.class)
public abstract class MixinModelBiped {

	@Shadow
	public ModelRenderer bipedHead;
	@Shadow
	public ModelRenderer bipedRightArm;
	@Shadow
	public ModelRenderer bipedLeftArm;
	@Shadow
	public ModelRenderer bipedBody;

	@Inject(method = "setRotationAngles", at = @At("TAIL"))
	private void etfu$spearPose(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn, CallbackInfo ci) {
		if (!(entityIn instanceof EntityPlayer))
			return; // 1.7.10 keeps the item-in-use state on EntityPlayer only
		EntityPlayer player = (EntityPlayer) entityIn;
		ItemStack main = player.getHeldItem(); // 26.2 getUseItemStackForArm(RIGHT) - 1.7.10 has one main hand
		if (main == null || !(main.getItem() instanceof ItemSpear))
			return;
		SpearAnimations.thirdPersonHandUse(this.bipedRightArm, this.bipedHead, true, player, (ItemSpear) main.getItem());
		float attackTime = player.getSwingProgress(Minecraft.getMinecraft().timer.renderPartialTicks);
		if (attackTime > 0.0F) { // 26.2 setupAttackAnimation gate: attackTime > 0
			SpearAnimations.thirdPersonAttackHand(this.bipedRightArm, this.bipedLeftArm, this.bipedBody, attackTime);
		}
	}
}
