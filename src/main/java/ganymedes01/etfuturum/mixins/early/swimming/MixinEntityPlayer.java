package ganymedes01.etfuturum.mixins.early.swimming;

import com.mojang.authlib.GameProfile;
import ganymedes01.etfuturum.swimming.ISwimmingPlayer;
import ganymedes01.etfuturum.swimming.SwimmingRuntime;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer extends EntityLivingBase implements ISwimmingPlayer {

	@Unique
	private boolean etfu$visualSwimming;
	@Unique
	private float etfu$swimAmount;
	@Unique
	private float etfu$swimAmountO;
	@Unique
	private float etfu$cameraEyeHeight = SwimmingRuntime.STANDING_EYE;
	@Unique
	private float etfu$cameraEyeHeightO = SwimmingRuntime.STANDING_EYE;
	@Unique
	private boolean etfu$localSprinting;

	public MixinEntityPlayer(World world) {
		super(world);
	}

	/**
	 * v7: registers the dedicated swimming watchable object (a whole byte, not a bit of
	 * the shared flags byte 0 - see {@link SwimmingRuntime#registerSwimmingData}).
	 * EntityPlayer has a single constructor every subclass chains through, so this is the
	 * one registration point for client, server and remote players alike.
	 */
	@Inject(method = "<init>(Lnet/minecraft/world/World;Lcom/mojang/authlib/GameProfile;)V", at = @At("TAIL"))
	private void etfu$registerSwimmingDataWatcher(World world, GameProfile profile, CallbackInfo ci) {
		SwimmingRuntime.registerSwimmingData(this.getDataWatcher());
	}

	@Override
	public boolean etfu$isSwimming() {
		return this.getDataWatcher().getWatchableObjectByte(SwimmingRuntime.getSwimmingSlot()) != 0;
	}

	@Override
	public void etfu$setSwimming(boolean swimming) {
		this.getDataWatcher().updateObject(SwimmingRuntime.getSwimmingSlot(), Byte.valueOf((byte) (swimming ? 1 : 0)));
	}

	@Override
	public boolean etfu$isLocalSprinting() {
		return this.etfu$localSprinting;
	}

	@Override
	public void etfu$setLocalSprinting(boolean sprinting) {
		this.etfu$localSprinting = sprinting;
	}

	@Override
	public boolean etfu$isVisuallySwimming() {
		return this.etfu$visualSwimming;
	}

	@Override
	public boolean etfu$isVisuallyCrawling() {
		return this.etfu$visualSwimming && !this.etfu$isSwimming();
	}

	@Override
	public float etfu$getEyeHeightAboveFeet() {
		// 26.2 Entity#getEyeHeight / Player#getEyeHeight: the immediate pose value, no smoothing.
		// Feeds eye-in-water checks and the eye ray (EntityRenderer#getMouseOver), never the camera.
		return this.height == SwimmingRuntime.SWIMMING_HEIGHT ? SwimmingRuntime.SWIMMING_EYE
				: this.height == SwimmingRuntime.CROUCHING_HEIGHT ? SwimmingRuntime.CROUCHING_EYE
				: SwimmingRuntime.STANDING_EYE;
	}

	/**
	 * 26.2 LivingEntity#getEyeHeight returns the current pose eye height (0.4 swimming/crawling, 1.27
	 * crouching, 1.62 standing), while 1.7.10 freezes the constructor's standing 1.62 forever. Vanilla
	 * consumers therefore probe the standing eye even when the pose shrank: the suffocation probe
	 * (Entity#isEntityInsideOpaqueBlock) trips the renderer's inside-block dense fog that blacks out
	 * the first-person view while crawling in a 1-block-high tunnel; the underwater fog probe
	 * (Entity#isInsideOfMaterial), EntityLivingBase#canEntityBeSeen and the AI look
	 * helpers aim at the standing eye.
	 *
	 * CAUTION - 1.7.10 semantics (root cause of the v3 "pick 1.5 blocks above the crosshair" bug):
	 * EntityPlayer#getEyeHeight is NOT an absolute feet-to-eye distance, it is an offset on top of
	 * posY, and the player's posY already IS the standing eye (posY = bb.minY + yOffset - ySize =
	 * feet + 1.62; EntityLivingBase#rayTrace's floor(posY - 0.2 - yOffset) only lands on the block
	 * under the feet with that convention). Forge therefore defaults the field to
	 * getDefaultEyeHeight() == 0.12. Every consumer consumes it as that offset:
	 *  - EntityPlayer#getPosition (feeding both the EntityLivingBase#rayTrace block pick and the
	 *    EntityRenderer#getMouseOver entity pick) computes posY + (getEyeHeight - getDefaultEyeHeight);
	 *  - Entity#isEntityInsideOpaqueBlock / #isInsideOfMaterial and EntityLivingBase#canEntityBeSeen
	 *    probe posY + getEyeHeight.
	 * So the injected value must be getDefaultEyeHeight + poseEyeAboveFeet - (yOffset - ySize):
	 * standing yields the vanilla 0.12 (all consumers byte-identical to vanilla), swimming -1.10
	 * (pick ray from feet + 0.40, suffocation band feet + 0.52 clears the tunnel ceiling), crouching
	 * -0.23 (pick ray feet + 1.27). The elytra pose keeps height 1.8, so gliding stays vanilla.
	 */
	@Inject(method = "getEyeHeight", at = @At("HEAD"), cancellable = true)
	private void etfu$poseEyeHeight(CallbackInfoReturnable<Float> cir) {
		cir.setReturnValue(0.12F + this.etfu$getEyeHeightAboveFeet() - this.yOffset + this.ySize);
	}

	@Override
	public float etfu$getCameraEyeHeight(float partialTicks) {
		return this.etfu$cameraEyeHeightO + (this.etfu$cameraEyeHeight - this.etfu$cameraEyeHeightO) * partialTicks;
	}

	@Override
	public float etfu$getSwimAmount(float partialTicks) {
		return this.etfu$swimAmountO + (this.etfu$swimAmount - this.etfu$swimAmountO) * partialTicks;
	}

	/**
	 * 26.2 Player#tick calls updatePlayerPose() at the end of the tick, after the movement.
	 */
	@Inject(method = "onUpdate", at = @At("TAIL"))
	private void etfu$updatePoseAndAnimation(CallbackInfo ci) {
		int pose = SwimmingRuntime.computePose((EntityPlayer) (Object) this);

		if (pose != SwimmingRuntime.POSE_KEEP) {
			float targetHeight = pose == SwimmingRuntime.POSE_SWIMMING ? SwimmingRuntime.SWIMMING_HEIGHT
					: pose == SwimmingRuntime.POSE_CROUCHING ? SwimmingRuntime.CROUCHING_HEIGHT
					: SwimmingRuntime.STANDING_HEIGHT;
			if (this.height != targetHeight) {
				this.setSize(SwimmingRuntime.WIDTH, targetHeight);
			}
			this.etfu$visualSwimming = pose == SwimmingRuntime.POSE_SWIMMING;
		}

		this.etfu$swimAmountO = this.etfu$swimAmount;
		this.etfu$swimAmount = this.etfu$visualSwimming
				? Math.min(1.0F, this.etfu$swimAmount + 0.09F)
				: Math.max(0.0F, this.etfu$swimAmount - 0.09F);

		// 26.2 Camera.setup: eyeHeightOld = eyeHeight; eyeHeight += (target - eyeHeight) * 0.5.
		// Snapping straight to the pose value jolts the camera 1.22 blocks in one tick when the
		// pose flips; converging at 50% per tick keeps the transition smooth like modern versions.
		this.etfu$cameraEyeHeightO = this.etfu$cameraEyeHeight;
		float eyeTarget = this.etfu$getEyeHeightAboveFeet();
		this.etfu$cameraEyeHeight += (eyeTarget - this.etfu$cameraEyeHeight) * 0.5F;
	}
}
