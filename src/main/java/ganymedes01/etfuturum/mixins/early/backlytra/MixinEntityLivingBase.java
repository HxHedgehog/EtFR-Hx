package ganymedes01.etfuturum.mixins.early.backlytra;

import ganymedes01.etfuturum.ModEnchantments;
import ganymedes01.etfuturum.configuration.configs.ConfigEnchantsPotions;
import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.elytra.IElytraPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase extends Entity {
	@Shadow
	public abstract boolean isClientWorld();

	@Shadow
	protected abstract boolean isAIEnabled();

	@Shadow
	public abstract float getAIMoveSpeed();

	@Shadow
	public abstract boolean isOnLadder();

	@Shadow
	public float prevLimbSwingAmount;

	@Shadow
	public float limbSwingAmount;

	@Shadow
	public float limbSwing;

	private static final DamageSource flyIntoWall = (new DamageSource("flyIntoWall")).setDamageBypassesArmor();

	public MixinEntityLivingBase(World worldIn) {
		super(worldIn);
	}

	@Inject(method = "moveEntityWithHeading", at = @At("HEAD"), cancellable = true)
	private void moveElytra(float p_70612_1_, float p_70612_2_, CallbackInfo ci) {
		/* method is named incorrectly in these older mappings, it's really isServerWorld */
		if (this.isClientWorld() && (!this.isInWater() || (ConfigEnchantsPotions.elytraOceanEnchant && ModEnchantments.getElytraWaterWalkerLevel((EntityLivingBase) (Object) this) > 0)) && !this.handleLavaMovement()) {
			if (this instanceof IElytraPlayer && ((IElytraPlayer) this).etfu$isElytraFlying()) {
				if (this.motionY > -0.5D) {
					this.fallDistance = 1.0F;
				}

				Vec3 vec3d = this.getLookVec();
				float f = this.rotationPitch * 0.017453292F;
				double d6 = Math.sqrt(vec3d.xCoord * vec3d.xCoord + vec3d.zCoord * vec3d.zCoord);
				double d8 = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
				double d1 = vec3d.lengthVector();
				float f4 = MathHelper.cos(f);
				f4 = (float) ((double) f4 * (double) f4 * Math.min(1.0D, d1 / 0.4D));
				this.motionY += -0.08D + f4 * 0.06D;

				if (this.motionY < 0.0D && d6 > 0.0D) {
					double d2 = this.motionY * -0.1D * f4;
					this.motionY += d2;
					this.motionX += vec3d.xCoord * d2 / d6;
					this.motionZ += vec3d.zCoord * d2 / d6;
				}

				if (f < 0.0F && d6 > 0.0D) {
					double d9 = d8 * (-MathHelper.sin(f)) * 0.04D;
					this.motionY += d9 * 3.2D;
					this.motionX -= vec3d.xCoord * d9 / d6;
					this.motionZ -= vec3d.zCoord * d9 / d6;
				}

				if (d6 > 0.0D) {
					this.motionX += (vec3d.xCoord / d6 * d8 - this.motionX) * 0.1D;
					this.motionZ += (vec3d.zCoord / d6 * d8 - this.motionZ) * 0.1D;
				}

				this.motionX *= 0.9900000095367432D;
				this.motionY *= 0.9800000190734863D;
				this.motionZ *= 0.9900000095367432D;

				/* 26.2 applies the underwater speed penalty through the Depth Strider slowDown of
				   travelInWater (0.8 swimming drag, blended towards 0.546 by WATER_MOVEMENT_EFFICIENCY,
				   LivingEntity#travelInWater). The balance feature keeps the air glide physics
				   underwater instead and scales its performance: the base 0.99/0.98 glide drag is
				   raised so the drag balanced glide speed settles at 35% of the air speed with Depth
				   Strider I, 55% with II and 75% with III, keeping flight over water the faster
				   option. The boosting firework rocket scales its thrust by the same factor
				   (EntityBoostingFireworkRocket). */
				if (this.isInWater()) {
					double performance = ModEnchantments.getElytraWaterWalkerPerformance((EntityLivingBase) (Object) this);
					this.motionX *= (1.0D - 0.01D / performance) / 0.9900000095367432D;
					this.motionY *= (1.0D - 0.02D / performance) / 0.9800000190734863D;
					this.motionZ *= (1.0D - 0.01D / performance) / 0.9900000095367432D;
				}

				this.moveEntity(this.motionX, this.motionY, this.motionZ);

				if (this.isCollidedHorizontally && !this.worldObj.isRemote) {
					double d10 = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
					double d3 = d8 - d10;
					float f5 = (float) (d3 * 10.0D - 3.0D);

					if (f5 > 0.0F) {
						this.playSound((int) f5 > 4 ? "game.player.hurt.fall.big" : "game.player.hurt.fall.small", 1.0F, 1.0F);
						this.attackEntityFrom(flyIntoWall, f5);
					}
				}

				if (this.onGround && !this.worldObj.isRemote) {
					((IElytraPlayer) this).etfu$setElytraFlying(false);
				}
				ci.cancel();
			}
		}
	}

	/**
	 * 26.2 water travel with Depth Strider (travelInWater), replicated on top of the
	 * vanilla 1.7.10 water branch of {@code moveEntityWithHeading}. Only takes over
	 * when the entity is in the vanilla water branch, is not elytra gliding (the
	 * glide physics above already cancel the whole method) and the equipped boots
	 * carry Depth Strider. Without this handler, the unenchanted vanilla formula
	 * runs unchanged; Depth Strider then reduces the water drag and boosts the
	 * swim acceleration exactly like modern versions (level 3 = no penalty).
	 * This is NOT gated by elytraOceanEnchant: the enchantment works on boots always.
	 */
	@Inject(method = "moveEntityWithHeading", at = @At("HEAD"), cancellable = true)
	private void waterWalkerSwim(float p_70612_1_, float p_70612_2_, CallbackInfo ci) {
		if (this instanceof IElytraPlayer && ((IElytraPlayer) this).etfu$isElytraFlying()) {
			return;
		}
		// Vanilla water branch condition (flying players are excluded there too).
		Object self = this;
		if (!this.isInWater() || self instanceof EntityPlayer && ((EntityPlayer) self).capabilities.isFlying) {
			return;
		}
		int level = ModEnchantments.getBootWaterWalkerLevel((EntityLivingBase) (Object) this);
		if (level <= 0) {
			return;
		}

		// 26.2 LivingEntity#travelInWater: waterMovementEfficiency = level / 3, halved airborne.
		float waterWalker = Math.min(level, 3) * (1.0F / 3.0F);
		if (!this.onGround) {
			waterWalker *= 0.5F;
		}
		// Keep the vanilla 1.7.10 base values, then blend them towards the modern targets.
		float speed = this.isAIEnabled() ? 0.04F : 0.02F;
		// 26.2 LivingEntity#travelInWater: sprinting uses 0.9 instead of getWaterSlowDown() (0.8).
		float slowDown = ConfigMixins.enableSwimming && this.isSprinting() ? 0.9F : 0.8F;
		if (waterWalker > 0.0F) {
			slowDown += (0.54600006F - slowDown) * waterWalker;
			speed += (this.getAIMoveSpeed() - speed) * waterWalker;
		}

		double d0 = this.posY;
		this.moveFlying(p_70612_1_, p_70612_2_, speed);
		this.moveEntity(this.motionX, this.motionY, this.motionZ);
		// 26.2 travelInWater L2399-2401: the waterlogged-ladder climb (0.2) is set
		// BEFORE the drag, so the y*0.8 damping and water gravity apply to it -
		// steady state 0.2*0.8-0.005 = 0.155/t, matching official speed exactly.
		// The waterlog.MixinEntityLivingBase ladder bridge lives inside the vanilla
		// branch that this handler cancels from HEAD, so it is replicated here.
		if (this.isCollidedHorizontally && this.isOnLadder()) {
			this.motionY = 0.20000000298023224D;
		}
		// 26.2 keeps the vertical drag at 0.8; Depth Strider only eases X/Z.
		this.motionX *= slowDown;
		this.motionY *= 0.800000011920929D;
		this.motionZ *= slowDown;
		// 26.2 getFluidFallingAdjustedMovement: the water sink term is skipped entirely
		// while sprinting; otherwise it is baseGravity/16 = 0.005 (waterBuoyancy config)
		// or the vanilla 1.7.10 0.02. Matches swimming.MixinEntityLivingBase.etfu$swimGravity,
		// which owns the same constant in the vanilla branch this handler replaces.
		this.motionY -= this.isSprinting() ? 0.0D : ConfigFunctions.waterBuoyancy ? 0.005D : 0.02D;

		// Official priority (jumpOutOfFluid runs last in travelInWater): the surface
		// jump 0.3 overwrites the damped ladder 0.2 when both conditions coexist.
		if (this.isCollidedHorizontally && this.isOffsetPositionInLiquid(this.motionX, this.motionY + 0.6000000238418579D - this.posY + d0, this.motionZ)) {
			this.motionY = 0.30000001192092896D;
		}

		// The vanilla water branch lives inside moveEntityWithHeading, whose tail
		// (shared by every branch) advances the limb-swing animation from the actual
		// displacement. Cancelling the whole method from HEAD would freeze the legs,
		// so the tail is replicated here after the replicated water branch.
		this.prevLimbSwingAmount = this.limbSwingAmount;
		double d2 = this.posX - this.prevPosX;
		double d3 = this.posZ - this.prevPosZ;
		float f6 = MathHelper.sqrt_double(d2 * d2 + d3 * d3) * 4.0F;
		if (f6 > 1.0F) {
			f6 = 1.0F;
		}
		this.limbSwingAmount += (f6 - this.limbSwingAmount) * 0.4F;
		this.limbSwing += this.limbSwingAmount;
		ci.cancel();
	}
}
