package ganymedes01.etfuturum.client.renderer.item;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.core.utils.SpearEase;
import ganymedes01.etfuturum.core.utils.SpearKinetics;
import ganymedes01.etfuturum.elytra.IElytraPlayer;
import ganymedes01.etfuturum.items.ItemSpear;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.player.EntityPlayer;

/**
 * 26.2 net.minecraft.client.model.effects.SpearAnimations on the fixed-function pipeline.
 * JOML rotateAround(q, ox, oy, oz) == T(ox,oy,oz) * R * T(-ox,-oy,-oz); Axis.XN.rotationDegrees(d)
 * == glRotatef(d, 0, -1, 0) and so on for the other signs.
 *
 * UseParams mirrors UseParams.fromKineticWeapon: the phase anchors come from the per-tier
 * dismount/knockback/damage windows of Items.java L1198-1204 (already in ticks here).
 */
@SideOnly(Side.CLIENT)
public class SpearAnimations {

	public static final class UseParams {
		public final float raiseProgress;
		public final float raiseProgressStart;
		public final float raiseProgressMiddle;
		public final float raiseProgressEnd;
		public final float swayProgress;
		public final float lowerProgress;
		public final float raiseBackProgress;
		public final float swayIntensity;
		public final float swayScaleSlow;
		public final float swayScaleFast;

		private UseParams(float raiseProgress, float raiseProgressStart, float raiseProgressMiddle, float raiseProgressEnd, float swayProgress, float lowerProgress, float raiseBackProgress, float swayIntensity, float swayScaleSlow, float swayScaleFast) {
			this.raiseProgress = raiseProgress;
			this.raiseProgressStart = raiseProgressStart;
			this.raiseProgressMiddle = raiseProgressMiddle;
			this.raiseProgressEnd = raiseProgressEnd;
			this.swayProgress = swayProgress;
			this.lowerProgress = lowerProgress;
			this.raiseBackProgress = raiseBackProgress;
			this.swayIntensity = swayIntensity;
			this.swayScaleSlow = swayScaleSlow;
			this.swayScaleFast = swayScaleFast;
		}

		public static UseParams fromSpear(ItemSpear spear, float time) {
			int finishRaisingTick = spear.getDelayTicks();
			int finishSwayingTick = spear.getDismountTicks() + finishRaisingTick;
			int startSwayingTick = finishSwayingTick - 20;
			int finishLoweringTick = spear.getKnockbackTicks() + finishRaisingTick;
			int startLoweringTick = finishLoweringTick - 40;
			int finishRaisingBackTick = spear.getDamageTicks() + finishRaisingTick;
			float raiseProgress = SpearEase.progress(time, 0.0F, finishRaisingTick);
			float raiseProgressStart = SpearEase.progress(raiseProgress, 0.0F, 0.5F);
			float raiseProgressMiddle = SpearEase.progress(raiseProgress, 0.5F, 0.8F);
			float raiseProgressEnd = SpearEase.progress(raiseProgress, 0.8F, 1.0F);
			float swayProgress = SpearEase.progress(time, startSwayingTick, finishSwayingTick);
			float lowerProgress = SpearEase.outCubic(SpearEase.inOutElastic(SpearEase.progress(time - 20.0F, startLoweringTick, finishLoweringTick)));
			float raiseBackProgress = SpearEase.progress(time, finishRaisingBackTick - 5, finishRaisingBackTick);
			float swayIntensity = 2.0F * SpearEase.outCirc(swayProgress) - 2.0F * SpearEase.inCirc(raiseBackProgress);
			float swayScaleSlow = (float) Math.sin(time * 19.0F * (Math.PI / 180.0F)) * swayIntensity;
			float swayScaleFast = (float) Math.sin(time * 30.0F * (Math.PI / 180.0F)) * swayIntensity;
			return new UseParams(raiseProgress, raiseProgressStart, raiseProgressMiddle, raiseProgressEnd, swayProgress, lowerProgress, raiseBackProgress, swayIntensity, swayScaleSlow, swayScaleFast);
		}
	}

	/** 0.4 * (outQuart(1..3) - inOutSine(3..10)), the hitFeedback envelope driven by the S->C event. */
	public static float hitFeedbackAmount(float ticksSinceFeedbackStart) {
		return 0.4F * (SpearEase.outQuart(SpearEase.progress(ticksSinceFeedbackStart, 1.0F, 3.0F)) - SpearEase.inOutSine(SpearEase.progress(ticksSinceFeedbackStart, 3.0F, 10.0F)));
	}

	/**
	 * 26.2 SpearAnimations.firstPersonAttack, the SwingAnimationType.STAB branch of
	 * ItemInHandRenderer (1.7.10 ItemRenderer L516-527 equivalent). Applied in the held-item anchor
	 * space right after the T(invert * 0.56, -0.52, -0.72) shift (where vanilla puts the swing arc
	 * blocks we suppress in spear.client.MixinItemRenderer). Three-phase envelope: prepare
	 * (0-5%, inOutSine), thrust (5-20%, outBack, 0.65 blocks forward + X -70 degrees), retract
	 * (40-100%, inOutExpo).
	 */
	public static void firstPersonAttack(float attackTime, boolean rightArm) {
		int invert = rightArm ? 1 : -1;
		float startingAmount = SpearEase.inOutSine(SpearEase.progress(attackTime, 0.0F, 0.05F));
		float middleAmount = SpearEase.outBack(SpearEase.progress(attackTime, 0.05F, 0.2F));
		float endingAmount = SpearEase.inOutExpo(SpearEase.progress(attackTime, 0.4F, 1.0F));
		GL11.glTranslatef(invert * 0.1F * (startingAmount - middleAmount), -0.075F * (startingAmount - endingAmount), 0.65F * (startingAmount - middleAmount));
		GL11.glRotatef(-70.0F * (startingAmount - endingAmount), 1.0F, 0.0F, 0.0F); // Axis.XP
		GL11.glTranslatef(0.0F, 0.0F, -0.25F * (endingAmount - middleAmount));
	}

	/**
	 * 26.2 SpearAnimations.thirdPersonAttackItem, the STAB branch of ItemInHandLayer L52-54.
	 * Applied in the hand anchor space right after the -90X/180Y/translate(1/16, 2/16, -10/16)
	 * pre-transform. The +Y shift is the 0.38 forwardMovement lunge (the -90X pre-rotation makes
	 * +Y point forward). No left/right asymmetry: 26.2 does not use invert here.
	 */
	public static void thirdPersonAttackItem(float attackTime) {
		float attack = SpearEase.inQuad(SpearEase.progress(attackTime, 0.05F, 0.2F));
		float retract = SpearEase.inOutExpo(SpearEase.progress(attackTime, 0.4F, 1.0F));
		float amount = attack - retract;
		// rotateAround(XN, 70 * (attack - retract), 0, -0.125, 0.125); Axis.XN = (-1, 0, 0)
		GL11.glTranslatef(0.0F, -0.125F, 0.125F);
		GL11.glRotatef(70.0F * amount, -1.0F, 0.0F, 0.0F);
		GL11.glTranslatef(0.0F, 0.125F, -0.125F);
		GL11.glTranslatef(0.0F, SpearKinetics.FORWARD_MOVEMENT * amount, 0.0F);
	}

	/**
	 * 26.2 SpearAnimations.thirdPersonAttackHand, the STAB case of HumanoidModel.setupAttackAnimation.
	 * The inline 1.7.10 swing block (ModelBiped L156-176) is the 26.2 WHACK arc inlined; the raise
	 * pose overwrite already removed its xRot/yRot terms from the right arm, so what is left to
	 * mirror is the left-arm twist cancellation (26.2 subtracts body.yRot from both arms, our right
	 * arm is already clean), the cancellation of the absolute swing zRot set (ModelBiped L175), and
	 * the xRot envelope on the attacking (right) arm, stacked on the raised pose like the official
	 * poseArm -> setupAttackAnimation order.
	 */
	public static void thirdPersonAttackHand(ModelRenderer rightArm, ModelRenderer leftArm, ModelRenderer body, float attackTime) {
		leftArm.rotateAngleY -= body.rotateAngleY;
		leftArm.rotateAngleX -= body.rotateAngleY;
		rightArm.rotateAngleZ += (float) Math.sin(attackTime * Math.PI) * 0.4F; // cancel ModelBiped L175
		float prepare = SpearEase.inOutSine(SpearEase.progress(attackTime, 0.0F, 0.05F));
		float attack = SpearEase.inQuad(SpearEase.progress(attackTime, 0.05F, 0.2F));
		float retract = SpearEase.inOutExpo(SpearEase.progress(attackTime, 0.4F, 1.0F));
		rightArm.rotateAngleX += (90.0F * prepare - 120.0F * attack + 30.0F * retract) * ((float) Math.PI / 180.0F);
	}

	/**
	 * 26.2 SpearAnimations.firstPersonUse, applied to the current GL matrix right after the
	 * T(invert * 0.56, -0.52, -0.72) arm-space shift of the SPEAR branch.
	 */
	public static void firstPersonUse(float ticksSinceKineticHitFeedback, float timeHeld, ItemSpear spear, boolean rightArm) {
		UseParams p = UseParams.fromSpear(spear, timeHeld);
		int invert = rightArm ? 1 : -1;
		GL11.glTranslatef(invert * (p.raiseProgress * 0.15F + p.raiseProgressEnd * -0.05F + p.swayProgress * -0.1F + p.swayScaleSlow * 0.005F), p.raiseProgress * -0.075F + p.raiseProgressMiddle * 0.075F + p.swayScaleFast * 0.01F, p.raiseProgressStart * 0.05F + p.raiseProgressEnd * -0.05F + p.swayScaleSlow * 0.005F);
		// rotateAround(XP, -65*inOutBack(raise) - 35*lower + 100*raiseBack - 0.5*swayFast, 0, 0.1, 0)
		GL11.glTranslatef(0.0F, 0.1F, 0.0F);
		GL11.glRotatef(-65.0F * SpearEase.inOutBack(p.raiseProgress) - 35.0F * p.lowerProgress + 100.0F * p.raiseBackProgress - 0.5F * p.swayScaleFast, 1.0F, 0.0F, 0.0F);
		GL11.glTranslatef(0.0F, -0.1F, 0.0F);
		// rotateAround(YN, invert * (-90*progress(raise,0.5,0.55) + 90*sway + 2*swaySlow), invert*0.15, 0, 0)
		float yn = invert * (-90.0F * SpearEase.progress(p.raiseProgress, 0.5F, 0.55F) + 90.0F * p.swayProgress + 2.0F * p.swayScaleSlow);
		GL11.glTranslatef(invert * 0.15F, 0.0F, 0.0F);
		GL11.glRotatef(yn, 0.0F, -1.0F, 0.0F);
		GL11.glTranslatef(-invert * 0.15F, 0.0F, 0.0F);
		GL11.glTranslatef(0.0F, -hitFeedbackAmount(ticksSinceKineticHitFeedback), 0.0F);
	}

	/**
	 * 26.2 SpearAnimations.thirdPersonUseItem, applied to the current GL matrix in the hand anchor
	 * space right before the ItemInHandLayer -90X/180Y pre-transform. attackTime is the swing
	 * progress (0 for the right-click wind-up; the STAB left-click attack wires it later).
	 */
	public static void thirdPersonUseItem(float timeHeld, float attackTime, float ticksSinceKineticHitFeedback, ItemSpear spear, boolean rightArm) {
		UseParams p = UseParams.fromSpear(spear, timeHeld);
		int invert = rightArm ? 1 : -1;
		float attack = SpearEase.inQuad(SpearEase.progress(attackTime, 0.05F, 0.2F));
		float retract = SpearEase.inOutExpo(SpearEase.progress(attackTime, 0.4F, 1.0F));
		float raiseProgressModified = 1.0F - SpearEase.outBack(1.0F - p.raiseProgress);
		float hitFeedback = hitFeedbackAmount(ticksSinceKineticHitFeedback);
		GL11.glTranslatef(0.0F, -hitFeedback * 0.4F, -SpearKinetics.FORWARD_MOVEMENT * (raiseProgressModified - p.raiseBackProgress) + hitFeedback);
		// rotateAround(XN, 70*(raise - raiseBack) - 40*(attack - retract), 0, -0.03125, 0.125)
		float xn = 70.0F * (p.raiseProgress - p.raiseBackProgress) - 40.0F * (attack - retract);
		GL11.glTranslatef(0.0F, -0.03125F, 0.125F);
		GL11.glRotatef(xn, -1.0F, 0.0F, 0.0F); // Axis.XN = (-1,0,0)
		GL11.glTranslatef(0.0F, 0.03125F, -0.125F);
		// rotateAround(YP, invert * 90 * (raise - sway + 3*retract + attack), 0, 0, 0.125)
		float yp = invert * 90.0F * (p.raiseProgress - p.swayProgress + 3.0F * retract + attack);
		GL11.glTranslatef(0.0F, 0.0F, 0.125F);
		GL11.glRotatef(yp, 0.0F, 1.0F, 0.0F);
		GL11.glTranslatef(0.0F, 0.0F, -0.125F);
	}

	/**
	 * 26.2 SpearAnimations.thirdPersonHandUse on a ModelBiped arm: the raised-spear base pose
	 * applies whenever the spear is held, the sway/raise terms only while using it (MAIN_HAND in
	 * 1.7.10). No swim pose exists here, so only the fall-flying drop is kept.
	 */
	public static void thirdPersonHandUse(ModelRenderer arm, ModelRenderer head, boolean rightArm, EntityPlayer entity, ItemSpear spear) {
		int invert = rightArm ? 1 : -1;
		arm.rotateAngleY = -0.1F * invert + head.rotateAngleY;
		arm.rotateAngleX = -1.5707964F + head.rotateAngleX + 0.8F;
		if (entity instanceof IElytraPlayer && ((IElytraPlayer) entity).etfu$isElytraFlying()) { // 26.2 isFallFlying
			arm.rotateAngleX -= 0.9599311F;
		}
		arm.rotateAngleY = (float) Math.PI / 180.0F * clamp(57.295776F * arm.rotateAngleY, -60.0F, 60.0F);
		arm.rotateAngleX = (float) Math.PI / 180.0F * clamp(57.295776F * arm.rotateAngleX, -120.0F, 30.0F);
		if (!entity.isUsingItem()) // 26.2 gate: base pose only outside use
			return;
		float timeHeld = ItemSpear.USE_DURATION - entity.getItemInUseCount();
		UseParams p = UseParams.fromSpear(spear, timeHeld);
		arm.rotateAngleY += -invert * p.swayScaleFast * ((float) Math.PI / 180.0F) * p.swayIntensity * 1.0F;
		arm.rotateAngleZ += -invert * p.swayScaleSlow * ((float) Math.PI / 180.0F) * p.swayIntensity * 0.5F;
		arm.rotateAngleX += (float) Math.PI / 180.0F * (-40.0F * p.raiseProgressStart + 30.0F * p.raiseProgressMiddle - 20.0F * p.raiseProgressEnd + 20.0F * p.lowerProgress + 10.0F * p.raiseBackProgress + 0.6F * p.swayScaleSlow * p.swayIntensity);
	}

	private static float clamp(float x, float min, float max) {
		return x < min ? min : x > max ? max : x;
	}
}
