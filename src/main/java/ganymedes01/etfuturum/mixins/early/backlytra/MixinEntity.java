package ganymedes01.etfuturum.mixins.early.backlytra;

import ganymedes01.etfuturum.elytra.IElytraPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

/**
 * 26.2 fires the water splash effect (doWaterSplashEffect, 13 bubble + 13 splash particles and a speed
 * graded splash sound) only once per real water entry, on the !wasTouchingWater edge of
 * Entity#updateFluidInteraction, and has no effect on leaving water. 1.7.10's handleWaterMovement fires
 * the same burst on every single re-entry of the bounding box, and since the Depth Strider balance
 * feature keeps the fall flying glide physics underwater, surface skimming re-triggers the burst on
 * every crossing, flooding the water with bubbles. A glide also lacks the water damping that keeps
 * 26.2 from oscillating across the surface at all, so while fall flying the vanilla burst is debounced
 * here to at most one per {@link #etfu$SPLASH_COOLDOWN} ticks. Leaving the water while fall flying is
 * answered with a quieter exit splash, an addition beyond 26.2 which has no leaving-water effect.
 */
@Mixin(Entity.class)
public abstract class MixinEntity {

	@Unique
	private static final int etfu$SPLASH_COOLDOWN = 10;

	@Shadow
	public World worldObj;

	@Shadow
	public double posX;

	@Shadow
	public double posZ;

	@Shadow
	public double motionX;

	@Shadow
	public double motionY;

	@Shadow
	public double motionZ;

	@Shadow
	public float width;

	@Shadow
	public AxisAlignedBB boundingBox;

	@Shadow
	protected Random rand;

	@Shadow
	protected boolean inWater;

	@Shadow
	protected abstract String getSplashSound();

	@Shadow
	public abstract void playSound(String name, float volume, float pitch);

	@Unique
	private int etfu$splashCooldown;

	@Unique
	private boolean etfu$splashBurstPlayed;

	@Unique
	private boolean etfu$wasTouchingWater;

	@Unique
	private static boolean etfu$isElytraFlying(Entity entity) {
		return entity instanceof IElytraPlayer && ((IElytraPlayer) entity).etfu$isElytraFlying();
	}

	@Redirect(method = "handleWaterMovement",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;playSound(Ljava/lang/String;FF)V"), require = 1)
	private void etfu$debounceSplashSound(Entity entity, String name, float volume, float pitch) {
		if (!etfu$isElytraFlying(entity) || etfu$splashCooldown <= 0) {
			entity.playSound(name, volume, pitch);
			etfu$splashBurstPlayed = true;
		}
	}

	@Redirect(method = "handleWaterMovement",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnParticle(Ljava/lang/String;DDDDDD)V"), require = 2)
	private void etfu$debounceSplashParticles(World world, String particle, double x, double y, double z, double motionX, double motionY, double motionZ) {
		if (!etfu$isElytraFlying((Entity) (Object) this) || etfu$splashCooldown <= 0) {
			world.spawnParticle(particle, x, y, z, motionX, motionY, motionZ);
			etfu$splashBurstPlayed = true;
		}
	}

	@Inject(method = "handleWaterMovement", at = @At("TAIL"))
	private void etfu$waterEdgeEffects(CallbackInfoReturnable<Boolean> cir) {
		Entity entity = (Entity) (Object) this;
		if (etfu$isElytraFlying(entity)) {
			if (etfu$splashBurstPlayed) {
				etfu$splashBurstPlayed = false;
				etfu$splashCooldown = etfu$SPLASH_COOLDOWN;
			} else if (etfu$wasTouchingWater && !this.inWater && etfu$splashCooldown <= 0) {
				etfu$playExitSplash();
				etfu$splashCooldown = etfu$SPLASH_COOLDOWN;
			}
		}
		etfu$wasTouchingWater = this.inWater;
		if (etfu$splashCooldown > 0)
			etfu$splashCooldown--;
	}

	/**
	 * Addition beyond 26.2 (which has no leaving-water effect): surface droplets carrying the entity's
	 * motion, positioned and shaped like the vanilla entry burst, with a quiet splash sound matching
	 * the 0.2 self volume modifier of 26.2's doWaterSplashEffect, so the glide exiting the water reads
	 * once without flooding the surface.
	 */
	@Unique
	private void etfu$playExitSplash() {
		float y = (float) MathHelper.floor_double(this.boundingBox.minY) + 1.0F;
		int i;

		for (i = 0; (float) i < 1.0F + this.width * 20.0F; ++i) {
			float xOffset = (this.rand.nextFloat() * 2.0F - 1.0F) * this.width;
			float zOffset = (this.rand.nextFloat() * 2.0F - 1.0F) * this.width;
			this.worldObj.spawnParticle("splash", this.posX + (double) xOffset, (double) y, this.posZ + (double) zOffset, this.motionX, this.motionY, this.motionZ);
		}

		this.playSound(this.getSplashSound(), 0.2F, 1.0F + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.4F);
	}
}
