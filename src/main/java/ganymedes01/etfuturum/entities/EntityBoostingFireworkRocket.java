package ganymedes01.etfuturum.entities;

import ganymedes01.etfuturum.elytra.IElytraPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.List;

/**
 * 26.2 FireworkRocketEntity replica for elytra boosting. The rocket is always attached
 * to its rider: it applies thrust while the rider is fall flying, follows the rider's
 * hand position, explodes on impact (entities always, blocks only when the firework
 * carries explosion stars) and deals 26.2's distance-falloff explosion damage when it
 * explodes.
 */
public class EntityBoostingFireworkRocket extends EntityFireworkRocket {
	private static final int BOOSTED_ENTITY_ID = 9;
	private EntityLivingBase boostedEntity;
	private int fireworkAge;
	private int lifetime;

	@Override
	protected void entityInit() {
		super.entityInit();
		this.dataWatcher.addObject(BOOSTED_ENTITY_ID, 0);
	}

	public EntityBoostingFireworkRocket(World world) {
		super(world);
	}

	public EntityBoostingFireworkRocket(World world, ItemStack stack, EntityLivingBase entityToBoost) {
		super(world, entityToBoost.posX, entityToBoost.posY, entityToBoost.posZ, stack == null ? null : stack.copy());
		this.dataWatcher.updateObject(BOOSTED_ENTITY_ID, entityToBoost.getEntityId());
		this.boostedEntity = entityToBoost;

		/* 26.2 FireworkRocketEntity: lifetime = 10 * (1 + flight) + rand(6) + rand(7).
		 * Re-implemented here because 1.7.10 keeps its own copy private. */
		int flight = 1;
		if (stack != null && stack.hasTagCompound()) {
			NBTTagCompound fireworks = stack.getTagCompound().getCompoundTag("Fireworks");
			if (fireworks != null && fireworks.hasKey("Flight")) {
				flight += fireworks.getByte("Flight");
			}
		}
		this.lifetime = 10 * flight + this.rand.nextInt(6) + this.rand.nextInt(7);
	}

	@Override
	public boolean isInRangeToRenderDist(double distance) {
		/* 26.2: attached rockets are never rendered */
		return false;
	}

	@Override
	public void onUpdate() {
		this.lastTickPosX = this.posX;
		this.lastTickPosY = this.posY;
		this.lastTickPosZ = this.posZ;
		this.onEntityUpdate();

		Vec3 handAngle = Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);

		if (this.isAttachedToEntity()) {
			if (this.boostedEntity == null) {
				Entity entity = this.worldObj.getEntityByID(this.dataWatcher.getWatchableObjectInt(BOOSTED_ENTITY_ID));
				if (entity instanceof EntityLivingBase) {
					this.boostedEntity = (EntityLivingBase) entity;
				}
			}

			if (this.boostedEntity != null) {
				/* 26.2 FireworkRocketEntity.tick(): thrust is applied only while the rider is
				 * fall flying; otherwise the rocket keeps following the rider without thrust
				 * until it explodes at the end of its lifetime. */
				if (this.boostedEntity instanceof IElytraPlayer && ((IElytraPlayer) this.boostedEntity).etfu$isElytraFlying()) {
					Vec3 look = this.boostedEntity.getLookVec();
					this.boostedEntity.motionX += look.xCoord * 0.1D + (look.xCoord * 1.5D - this.boostedEntity.motionX) * 0.5D;
					this.boostedEntity.motionY += look.yCoord * 0.1D + (look.yCoord * 1.5D - this.boostedEntity.motionY) * 0.5D;
					this.boostedEntity.motionZ += look.zCoord * 0.1D + (look.zCoord * 1.5D - this.boostedEntity.motionZ) * 0.5D;

					/* 26.2 Entity.getHandHoldingItemAngle(): the main hand view vector rotated
					 * 80 degrees to the right and scaled by 0.5 (1.7.10 has no offhand). */
					float angle = (this.boostedEntity.rotationYaw + 80.0F) * (float) Math.PI / 180.0F;
					handAngle = Vec3.createVectorHelper(-MathHelper.sin(angle) * 0.5D, 0.0D, MathHelper.cos(angle) * 0.5D);
				}

				/* 26.2 positions the rocket at the rider's feet (getY() is the bounding box
				 * bottom). 1.7.10 players have yOffset = 1.62, so on the client side posY
				 * is offset above the bounding box bottom - use the box bottom instead. */
				double riderFeetY = this.boostedEntity.boundingBox.minY;
				this.setPosition(this.boostedEntity.posX + handAngle.xCoord, riderFeetY + handAngle.yCoord, this.boostedEntity.posZ + handAngle.zCoord);
				this.motionX = this.boostedEntity.motionX;
				this.motionY = this.boostedEntity.motionY;
				this.motionZ = this.boostedEntity.motionZ;
			}
		} else {
			/* 1.7.10 vanilla physics, kept for non-attached fallback */
			this.motionX *= 1.15D;
			this.motionZ *= 1.15D;
			this.motionY += 0.04D;
		}

		/* 26.2: raycast along the movement vector; entity hits always trigger the
		 * explosion, block hits only when the firework carries explosion stars. */
		MovingObjectPosition hit = !this.worldObj.isRemote && !this.isDead ? this.findImpact() : null;

		if (this.isAttachedToEntity()) {
			/* 26.2 updateRotation(): direct orientation from the movement */
			float f = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionZ * this.motionZ);
			this.rotationYaw = (float) (Math.atan2(this.motionX, this.motionZ) * 180.0D / Math.PI);
			this.rotationPitch = (float) (Math.atan2(this.motionY, f) * 180.0D / Math.PI);
		} else {
			this.moveEntity(this.motionX, this.motionY, this.motionZ);
			this.updateVanillaRotation();
		}

		if (hit != null) {
			if (hit.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY || this.hasExplosions()) {
				this.explode();
			}
		}

		if (this.fireworkAge == 0) {
			this.worldObj.playSoundAtEntity(this, "fireworks.launch", 3.0F, 1.0F);
		}

		++this.fireworkAge;

		/* 26.2 trail particle spawns at the rocket's exact position (1.7.10 vanilla
		 * uses posY - 0.3 instead) */
		if (this.worldObj.isRemote && this.fireworkAge % 2 < 2) {
			this.worldObj.spawnParticle("fireworksSpark", this.posX, this.posY, this.posZ, this.rand.nextGaussian() * 0.05D, -this.motionY * 0.5D, this.rand.nextGaussian() * 0.05D);
		}

		if (!this.worldObj.isRemote && !this.isDead && this.fireworkAge > this.lifetime) {
			this.explode();
		}
	}

	/* 1.7.10 vanilla EntityFireworkRocket rotation smoothing, kept for the non-attached fallback */
	private void updateVanillaRotation() {
		float f = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionZ * this.motionZ);
		this.rotationYaw = (float) (Math.atan2(this.motionX, this.motionZ) * 180.0D / Math.PI);

		for (this.rotationPitch = (float) (Math.atan2(this.motionY, f) * 180.0D / Math.PI); this.rotationPitch - this.prevRotationPitch < -180.0F; this.prevRotationPitch -= 360.0F) {
			;
		}

		while (this.rotationPitch - this.prevRotationPitch >= 180.0F) {
			this.prevRotationPitch += 360.0F;
		}

		while (this.rotationYaw - this.prevRotationYaw < -180.0F) {
			this.prevRotationYaw -= 360.0F;
		}

		while (this.rotationYaw - this.prevRotationYaw >= 180.0F) {
			this.prevRotationYaw += 360.0F;
		}

		this.rotationPitch = this.prevRotationPitch + (this.rotationPitch - this.prevRotationPitch) * 0.2F;
		this.rotationYaw = this.prevRotationYaw + (this.rotationYaw - this.prevRotationYaw) * 0.2F;
	}

	/* 26.2 ProjectileUtil.getHitResultOnMoveVector(): raycast from the current position
	 * along the movement, taking the closest of the block and entity hits */
	private MovingObjectPosition findImpact() {
		Vec3 from = Vec3.createVectorHelper(this.posX, this.posY, this.posZ);
		Vec3 to = from.addVector(this.motionX, this.motionY, this.motionZ);
		MovingObjectPosition blockHit = this.worldObj.func_147447_a(from, to, false, true, false);
		if (blockHit != null) {
			to = blockHit.hitVec;
		}

		/* 26.2 ProjectileUtil.computeMargin(): the entity margin grows from 0 to 0.3
		 * during the first 8 ticks of existence */
		float margin = Math.max(0.0F, Math.min(0.3F, (this.ticksExisted - 2) / 20.0F));

		MovingObjectPosition closest = blockHit;
		double closestDist = blockHit != null ? from.squareDistanceTo(blockHit.hitVec) : Double.MAX_VALUE;

		@SuppressWarnings("unchecked")
		List<Entity> entities = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, this.boundingBox.addCoord(this.motionX, this.motionY, this.motionZ).expand(1.0D, 1.0D, 1.0D));
		for (Entity entity : entities) {
			/* 26.2 canHitEntity(): the attached rider is the projectile's owner and is excluded */
			if (entity == this.boostedEntity || entity.isDead || !entity.canBeCollidedWith()) {
				continue;
			}
			MovingObjectPosition intercept = entity.boundingBox.expand(margin, margin, margin).calculateIntercept(from, to);
			if (intercept == null) {
				continue;
			}
			double dist = from.squareDistanceTo(intercept.hitVec);
			if (dist < closestDist) {
				intercept.entityHit = entity;
				closest = intercept;
				closestDist = dist;
			}
		}
		return closest;
	}

	/* 26.2 FireworkRocketEntity.explode(): firework display, damage, then remove.
	 * The client shows the display via the inherited handleHealthUpdate(byte 17). */
	private void explode() {
		this.worldObj.setEntityState(this, (byte) 17);
		this.dealExplosionDamage();
		this.setDead();
	}

	private void dealExplosionDamage() {
		int explosions = this.getExplosionCount();
		if (explosions <= 0) {
			return;
		}

		float damageAmount = 5.0F + explosions * 2.0F;
		DamageSource source = new DamageSource("fireworks").setExplosion();

		/* 26.2: the boosted rider always takes the full damage */
		if (this.boostedEntity != null) {
			this.boostedEntity.attackEntityFrom(source, damageAmount);
		}

		/* 26.2: nearby entities take damage with sqrt distance falloff, requiring line of sight */
		@SuppressWarnings("unchecked")
		List<EntityLivingBase> targets = this.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, this.boundingBox.expand(5.0D, 5.0D, 5.0D));
		for (EntityLivingBase target : targets) {
			if (target == this.boostedEntity) {
				continue;
			}
			double dx = this.posX - target.posX;
			double dy = this.posY - target.posY;
			double dz = this.posZ - target.posZ;
			double distSq = dx * dx + dy * dy + dz * dz;
			if (distSq > 25.0D) {
				continue;
			}
			if (!this.canSeeFromRocket(target)) {
				continue;
			}
			float damage = damageAmount * (float) Math.sqrt((5.0D - Math.sqrt(distSq)) / 5.0D);
			target.attackEntityFrom(source, damage);
		}
	}

	/* 26.2 checks line of sight to two sample points on the target (feet and middle) */
	private boolean canSeeFromRocket(EntityLivingBase target) {
		for (int testStep = 0; testStep < 2; testStep++) {
			Vec3 to = Vec3.createVectorHelper(target.posX, target.posY + target.height * 0.5D * testStep, target.posZ);
			Vec3 from = Vec3.createVectorHelper(this.posX, this.posY, this.posZ);
			if (this.worldObj.func_147447_a(from, to, false, true, false) == null) {
				return true;
			}
		}
		return false;
	}

	private int getExplosionCount() {
		ItemStack stack = this.dataWatcher.getWatchableObjectItemStack(8);
		if (stack == null || !stack.hasTagCompound()) {
			return 0;
		}
		NBTTagCompound fireworks = stack.getTagCompound().getCompoundTag("Fireworks");
		if (fireworks != null && fireworks.hasKey("Explosions", 9)) {
			return fireworks.getTagList("Explosions", 10).tagCount();
		}
		return 0;
	}

	private boolean hasExplosions() {
		return this.getExplosionCount() > 0;
	}

	public boolean isAttachedToEntity() {
		return this.dataWatcher.getWatchableObjectInt(BOOSTED_ENTITY_ID) > 0;
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound tagCompound) {
		super.writeEntityToNBT(tagCompound);
		tagCompound.setInteger("BoostLife", this.fireworkAge);
		tagCompound.setInteger("BoostLifeTime", this.lifetime);
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound tagCompund) {
		super.readEntityFromNBT(tagCompund);
		if (tagCompund.hasKey("BoostLifeTime")) {
			this.fireworkAge = tagCompund.getInteger("BoostLife");
			this.lifetime = tagCompund.getInteger("BoostLifeTime");
		} else {
			/* rockets saved by older versions only have the vanilla fields */
			this.fireworkAge = tagCompund.getInteger("Life");
			this.lifetime = tagCompund.getInteger("LifeTime");
		}
	}
}
