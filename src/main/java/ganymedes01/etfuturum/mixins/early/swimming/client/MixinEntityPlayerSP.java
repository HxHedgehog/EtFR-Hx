package ganymedes01.etfuturum.mixins.early.swimming.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import ganymedes01.etfuturum.swimming.ISwimmingPlayer;
import ganymedes01.etfuturum.swimming.SwimmingRuntime;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.potion.Potion;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP extends AbstractClientPlayer implements ISwimmingPlayer {

	@Shadow
	public net.minecraft.util.MovementInput movementInput;

	@Shadow
	public int sprintingTicksLeft;

	@Shadow
	protected int sprintToggleTimer;

	/**
	 * Local-authoritative swimming state. The server copy of this player recomputes the flag
	 * one or two ticks behind the client (its sprint/position come from packets) and syncs it
	 * back through data watcher byte 0, which would stomp the shared bit mid-transition and
	 * flip-flop the pose, camera and ray origin. 26.2 keeps the local player's swimming state
	 * client-authoritative, so this field bypasses the sync for all local reads.
	 */
	@Unique
	private boolean etfu$localSwimming;

	public MixinEntityPlayerSP(World world, GameProfile profile) {
		super(world, profile);
	}

	@Override
	public boolean etfu$isSwimming() {
		return this.etfu$localSwimming;
	}

	@Override
	public void etfu$setSwimming(boolean swimming) {
		this.etfu$localSwimming = swimming;
		this.getDataWatcher().updateObject(SwimmingRuntime.getSwimmingSlot(), Byte.valueOf((byte) (swimming ? 1 : 0)));
	}

	/**
	 * 26.2 LocalPlayer computes the swimming flag on the client as well, so the local player
	 * does not have to wait for the server round trip.
	 */
	@Inject(method = "onLivingUpdate", at = @At("HEAD"))
	private void etfu$clientSwimming(CallbackInfo ci) {
		SwimmingRuntime.updateSwimming((EntityPlayer) (Object) this);
	}

	/**
	 * Captures every real client-side sprint change into the local-authoritative field.
	 * The integrated server stomps data watcher byte 0 (including the sprint bit at
	 * bit 3) with its packet-lagged copy, so the client's vanilla isSprinting()
	 * flip-flops during swimming transitions; SwimmingRuntime reads the captured value
	 * on the client instead (26.2 keeps the local sprint client-authoritative).
	 */
	@Inject(method = "setSprinting(Z)V", at = @At("TAIL"))
	private void etfu$captureLocalSprint(boolean sprinting, CallbackInfo ci) {
		((ISwimmingPlayer) (Object) this).etfu$setLocalSprinting(sprinting);
	}

	/**
	 * 26.2 has no sprint time limit - shouldStopSwimSprinting() is the only stop condition.
	 * 1.7.10 EntityPlayerSP#setSprinting arms a 600-tick timer (L626) whose expiry forces
	 * setSprinting(false) at onLivingUpdate L116, the 1st setSprinting call in the method
	 * (ordinal 0), which kills a held sprint-swim 30 s in. Redirect it away while swimming
	 * and disarm the timer so it cannot refire.
	 */
	@Redirect(method = "onLivingUpdate",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;setSprinting(Z)V", ordinal = 0))
	private void etfu$noSprintTimeout(EntityPlayerSP instance, boolean sprinting) {
		if (sprinting || !((ISwimmingPlayer) (Object) this).etfu$isSwimming()) {
			instance.setSprinting(sprinting);
		} else {
			this.sprintingTicksLeft = 0;
		}
	}

	/**
	 * 26.2 LocalPlayer#aiStep skips the double-space fly toggle while swimming
	 * (jumpTriggerTime is kept but the toggle branch is not entered).
	 */
	@ModifyExpressionValue(method = "onLivingUpdate",
			at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerCapabilities;allowFlying:Z", ordinal = 1))
	private boolean etfu$noFlyToggleWhileSwimming(boolean original) {
		return original && !this.etfu$localSwimming;
	}

	/**
	 * 26.2 canStartSprinting() has no onGround requirement, so double tapping W while
	 * underwater starts sprinting and therefore swimming. The 1.7.10 double tap branch
	 * (first onGround read in onLivingUpdate) is gated on being on the ground.
	 */
	@Redirect(method = "onLivingUpdate",
			at = @At(value = "FIELD", target = "Lnet/minecraft/client/entity/EntityPlayerSP;onGround:Z", ordinal = 0))
	private boolean etfu$sprintInWater(EntityPlayerSP instance) {
		return instance.onGround || instance.isInWater();
	}

	// NOTE: 26.2's goDownInWater() runs right before super.aiStep(). The bytecode super call
	// here is an INVOKESPECIAL on the direct superclass (javac emits AbstractClientPlayer as
	// the owner), and Mixin's @At("INVOKE") does not match super-call INVOKESPECIALs - that
	// injector silently found 0 targets and fatally dropped this whole mixin before. The
	// sink is therefore applied in swimming.MixinEntityLivingBase right before the
	// moveEntityWithHeading call, ahead of the look-Y pull, matching the 26.2 order.

	/**
	 * 26.2 LocalPlayer#shouldStopSwimSprinting ignores horizontal collisions and only requires
	 * a forward impulse when the player is neither on the ground nor holding shift.
	 * The 4th setSprinting call in onLivingUpdate (L228) is the vanilla sprint cancellation.
	 */
	@WrapOperation(method = "onLivingUpdate",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;setSprinting(Z)V", ordinal = 3))
	private void etfu$stopSwimSprint(EntityPlayerSP instance, boolean sprinting, Operation<Void> original) {
		if (!sprinting && ((ISwimmingPlayer) (Object) this).etfu$isSwimming()) {
			boolean hasForward = this.movementInput.moveForward > 0.0F;
			boolean canSprint = ((float) this.getFoodStats().getFoodLevel() > 6.0F || this.capabilities.allowFlying)
					&& !this.isPotionActive(Potion.blindness);
			boolean stop = !canSprint || !this.isInWater()
					|| (!hasForward && !this.onGround && !this.movementInput.sneak);
			if (stop) {
				original.call(instance, false);
			}
			return;
		}
		original.call(instance, sprinting);
	}
}
