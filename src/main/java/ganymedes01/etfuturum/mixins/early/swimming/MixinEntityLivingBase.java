package ganymedes01.etfuturum.mixins.early.swimming;

import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import ganymedes01.etfuturum.swimming.ISwimmingPlayer;
import ganymedes01.etfuturum.swimming.SwimmingRuntime;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase extends Entity {

	@Shadow
	protected boolean isJumping;

	public MixinEntityLivingBase(World world) {
		super(world);
	}

	/**
	 * 26.2 Player#travel applies the look-based vertical control before super.travel(), i.e.
	 * before the water drag and gravity. Injecting before moveEntityWithHeading() means this
	 * still runs when the backlytra mixin cancels the vanilla water branch from its HEAD.
	 *
	 * <p>26.2 LocalPlayer#aiStep also sinks the player -0.04 while sneaking in water
	 * (goDownInWater, gated by isAffectedByFluids() = !flying) right before the travel pull.
	 * That sink cannot be injected at the onLivingUpdate super call - javac emits it as an
	 * INVOKESPECIAL on the direct superclass and Mixin's @At("INVOKE") does not match super
	 * calls - so it runs here instead, ahead of the pull, preserving the 26.2 order.</p>
	 *
	 * <p>26.2 Player#travel L1291 applies the pull when {@code lookY <= 0.0 || jumping
	 * || fluidState at feet+0.9 is NOT empty} - i.e. looking up pulls the player UP while
	 * there is still fluid above, and the pull only stops once the feet+0.9 probe leaves
	 * the fluid (so the player is not yanked back down when surfacing).</p>
	 */
	@Inject(method = "onLivingUpdate",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/entity/EntityLivingBase;moveEntityWithHeading(FF)V",
					shift = At.Shift.BEFORE))
	private void etfu$swimVerticalControl(CallbackInfo ci) {
		Entity self = (Entity) (Object) this;
		if (!(self instanceof EntityPlayer)) {
			return;
		}
		EntityPlayer player = (EntityPlayer) self;

		if (player.isSneaking() && self.isInWater() && !player.capabilities.isFlying) {
			// 26.2 LocalPlayer#goDownInWater.
			this.motionY -= 0.04D;
		}

		if (!((ISwimmingPlayer) self).etfu$isSwimming()) {
			return;
		}

		Vec3 look = this.getLookVec();
		double lookY = look.yCoord;
		double mult = lookY < -0.2D ? 0.085D : 0.06D;

		if (lookY <= 0.0D || this.isJumping
				|| SwimmingRuntime.isWaterAt(this.worldObj, this.posX, this.boundingBox.minY + 0.9D, this.posZ)) {
			this.motionY += (lookY - this.motionY) * mult;
		}
	}

	/**
	 * 26.2 LivingEntity#getFluidFallingAdjustedMovement: the water sink term
	 * (baseGravity/16 = 0.005) is skipped entirely while sprinting, so a swimming player
	 * keeps the vertical speed computed by Player#travel instead of sinking every tick.
	 *
	 * <p>This is the ONLY modifier of the water-branch 0.02D constant: the waterlog
	 * buoyancy handler was folded in here because Mixin allows a single
	 * {@code @ModifyConstant} per constant - a second one silently fails to apply.
	 * It also covers {@code ConfigFunctions.waterBuoyancy} when the waterlog mixins are
	 * not registered (they defer to this one while swimming is enabled).</p>
	 */
	@ModifyConstant(method = "moveEntityWithHeading",
			constant = @Constant(doubleValue = 0.02D, ordinal = 0))
	private double etfu$swimGravity(double original) {
		Entity self = (Entity) (Object) this;
		if (self.isSprinting()) {
			return 0.0D;
		}
		return ConfigFunctions.waterBuoyancy ? 0.005D : original;
	}

	/**
	 * 26.2 LivingEntity#travelInWater: {@code slowDown = isSprinting() ? 0.9 : getWaterSlowDown()}
	 * - keyed on sprinting for every entity, so surface sprinting (eye above water, flag off)
	 * still gets the modern faster drag, exactly like the 26.2 behaviour.
	 */
	@ModifyConstant(method = "moveEntityWithHeading",
			constant = @Constant(doubleValue = 0.800000011920929D, ordinal = 0))
	private double etfu$swimFrictionX(double original) {
		return ((Entity) (Object) this).isSprinting() ? 0.9D : original;
	}

	@ModifyConstant(method = "moveEntityWithHeading",
			constant = @Constant(doubleValue = 0.800000011920929D, ordinal = 2))
	private double etfu$swimFrictionZ(double original) {
		return ((Entity) (Object) this).isSprinting() ? 0.9D : original;
	}
}
