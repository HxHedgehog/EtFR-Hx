package ganymedes01.etfuturum.core.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**
 * Physics/raycast helpers shared by the client stab detector and the server stab settlement,
 * mirroring 26.2's KineticWeapon/AttackRange/PiercingWeapon trio.
 *
 * 26.2 source values (Item.Properties.spear):
 * AttackRange(minReach 2.0, maxReach 4.5, hitboxMargin 0.125), KineticWeapon contact cooldown 10
 * ticks, forwardMovement 0.38.
 */
public class SpearKinetics {

	public static final float FORWARD_MOVEMENT = 0.38F;
	public static final int CONTACT_COOLDOWN_TICKS = 10;
	public static final float RANGE_MIN = 2.0F;
	public static final float RANGE_MAX = 4.5F;
	public static final float RANGE_MARGIN = 0.125F;
	// 26.2 spear AttackRange(2.0, 4.5, 2.0, 6.5, 0.125, 0.5): the creative branch
	// (AttackRange#effectiveMinRange/effectiveMaxRange) widens the reach window to 6.5.
	public static final float RANGE_MIN_CREATIVE = 2.0F;
	public static final float RANGE_MAX_CREATIVE = 6.5F;

	/**
	 * 26.2 KineticWeapon.getMotion: per-tick velocity scaled to blocks/second; non-player riders
	 * resolve to the root vehicle like Entity#getKnownSpeed.
	 */
	public static double[] getMotion(Entity entity) {
		if (!(entity instanceof EntityPlayer) && entity.ridingEntity != null) {
			entity = entity.ridingEntity;
		}
		return new double[] { entity.motionX * 20.0D, entity.motionY * 20.0D, entity.motionZ * 20.0D };
	}

	/**
	 * Last tick's full displacement as blocks/second. onUsingTick runs at the head of
	 * EntityPlayer.onUpdate, before this tick's movement, so motionX is only last tick's
	 * post-friction residual (sprint reads ~3.1 instead of 5.6); posX - prevPosX spans the whole
	 * previous tick and matches what 26.2's getKnownSpeed reports.
	 */
	public static double[] getTravelVelocity(Entity entity) {
		return new double[] { (entity.posX - entity.prevPosX) * 20.0D, (entity.posY - entity.prevPosY) * 20.0D, (entity.posZ - entity.prevPosZ) * 20.0D };
	}

	/** 26.2: attackerSpeedProjection = look . motion; speeds are blocks/second. */
	public static double projection(Vec3 look, double[] motion) {
		return look.xCoord * motion[0] + look.yCoord * motion[1] + look.zCoord * motion[2];
	}

	/**
	 * 26.2 Condition.test: within the duration window AND fast enough (speeds in blocks/second).
	 * entityFactor scales the thresholds: players 1.0, non-player wielders 0.2 (actionFactor).
	 */
	public static boolean testCondition(int ticksUsed, int maxDurationTicks, double attackerSpeed, double minAttackerSpeed, double relativeSpeed, double minRelativeSpeed, double entityFactor) {
		return ticksUsed <= maxDurationTicks && attackerSpeed >= minAttackerSpeed * entityFactor && relativeSpeed >= minRelativeSpeed * entityFactor;
	}

	/**
	 * Visual eye position of the wielder. Grounded: posY + getEyeHeight() - 1.7.10 players carry
	 * posY at eye level (EntityPlayer.getEyeHeight() = 0.12, yOffset 1.62). During elytra flight
	 * the camera sits 1.22 blocks lower (MixinEntityRenderer, simulating 26.2's 0.4 fall-flying
	 * eye height; the stance packet forbids touching getEyeHeight() directly), so the stab corridor
	 * must follow the camera - otherwise spears fly out 1.22 above where the player is aiming.
	 */
	public static Vec3 getEyePosition(EntityLivingBase attacker) {
		double eyeY = attacker.posY + attacker.getEyeHeight();
		if (attacker instanceof ganymedes01.etfuturum.elytra.IElytraPlayer && ((ganymedes01.etfuturum.elytra.IElytraPlayer) attacker).etfu$isElytraFlying())
			eyeY -= 1.22D;
		return Vec3.createVectorHelper(attacker.posX, eyeY, attacker.posZ);
	}

	/**
	 * 26.2 ProjectileUtil.getHitEntitiesAlong with the spear AttackRange. The test segment runs from
	 * effectiveMinRange to effectiveMaxRange + max(0, movementComponent), where movementComponent is
	 * the attacker's last-tick travel projected on the look vector (blocks/tick) - the official
	 * sweep compensation for fast wielders, without which an elytra diver (~1.7 blocks/tick)
	 * tunnels past targets between two static ray samples. Spear ranges: min 2.0 in both modes,
	 * max 4.5 survival / 6.5 creative; mobFactor 0.5 only affects mob wielders, which 1.7.10 has
	 * none of.
	 *
	 * The official variant also clips blocks first (ClipContext.Block.COLLIDER): a wall between
	 * the eye and the corridor start masks the whole stab, otherwise the segment is truncated at
	 * the wall so targets behind cover are unreachable - this is what keeps the effective range
	 * honest, so the clip runs here too.
	 */
	public static List<EntityHit> getHitEntitiesAlong(World world, EntityLivingBase attacker, float partial) {
		Vec3 origin = getEyePosition(attacker);
		Vec3 look = attacker.getLook(partial);
		// 26.2 AttackRange#effectiveMinRange/effectiveMaxRange pick the creative window when seen
		boolean creative = attacker instanceof EntityPlayer && ((EntityPlayer) attacker).capabilities.isCreativeMode;
		double minRange = creative ? RANGE_MIN_CREATIVE : RANGE_MIN;
		double maxRange = creative ? RANGE_MAX_CREATIVE : RANGE_MAX;
		double movementComponent = Math.max(0.0D, projection(look, getTravelVelocity(attacker)) / 20.0D);
		Vec3 from = origin.addVector(look.xCoord * minRange, look.yCoord * minRange, look.zCoord * minRange);
		double toDist = maxRange + movementComponent;
		Vec3 to = origin.addVector(look.xCoord * toDist, look.yCoord * toDist, look.zCoord * toDist);
		// world.func_147447_a = rayTraceBlocks(from, to, stopOnLiquid, ignoreNoBoundingBox, returnLastUncollidable),
		// the 26.2 ClipContext.Block.COLLIDER equivalent (slopes, walls and slabs stop the ray).
		// The clip start is nudged 1.0E-4 along the look ray: the classic tracer tests the start
		// cell first and AxisAlignedBB.calculateIntercept returns the start point itself (t=0 face
		// hit) when the eye rests exactly on a block boundary plane, which used to mask the whole
		// stab as FULL-BLOCKED. 26.2's voxel clip has no touching-start artifact; a wall genuinely
		// over the eye still blocks from the nudged start.
		MovingObjectPosition blockHit = world.func_147447_a(origin.addVector(look.xCoord * 1.0E-4D, look.yCoord * 1.0E-4D, look.zCoord * 1.0E-4D), to, false, true, false);
		if (blockHit != null && blockHit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
			if (origin.squareDistanceTo(blockHit.hitVec) < origin.squareDistanceTo(from))
				return new ArrayList<>(); // covered before the corridor even starts
			to = blockHit.hitVec; // truncate the swept segment at the wall
		}
		AxisAlignedBB path = AxisAlignedBB.getBoundingBox(Math.min(from.xCoord, to.xCoord), Math.min(from.yCoord, to.yCoord), Math.min(from.zCoord, to.zCoord), Math.max(from.xCoord, to.xCoord), Math.max(from.yCoord, to.yCoord), Math.max(from.zCoord, to.zCoord)).expand(1.0D, 1.0D, 1.0D);
		List<?> broadPhase = world.getEntitiesWithinAABBExcludingEntity(attacker, path);
		List<EntityHit> hits = new ArrayList<>();
		for (Object o : broadPhase) {
			Entity target = (Entity) o;
			if (!canHitEntity(attacker, target))
				continue;
			Vec3 hitVec = getEntityHitResult(world, target, from, to);
			if (hitVec == null)
				continue;
			// the segment [from, to] already encodes the range window; dist is nearest-first sorting
			hits.add(new EntityHit(target, hitVec, origin.distanceTo(hitVec)));
		}
		hits.sort((a, b) -> Double.compare(a.dist, b.dist));
		return hits;
	}

	/**
	 * 26.2 PiercingWeapon.canHitEntity: alive, collidable targets, PVP-gated for players,
	 * excluding attackers sharing a vehicle with the target. The official set also covers
	 * interaction entities and multi-part bosses (EnderDragonPart resolves to the parent); the
	 * 1.7.10 mirror keeps EntityDragonPart and friends attackable, which Entity.canAttackWithItem
	 * (default true) already gates downstream - knockback and durability stay guarded to living
	 * targets inside stabAttack.
	 */
	public static boolean canHitEntity(EntityLivingBase attacker, Entity target) {
		if (target == null || !target.isEntityAlive() || target.isEntityInvulnerable() || !target.canBeCollidedWith())
			return false;
		if (target.riddenByEntity == attacker || target.ridingEntity == attacker) // 26.2 isPassengerOfSameVehicle
			return false;
		if (target instanceof EntityPlayer && attacker instanceof EntityPlayer && !((EntityPlayer) attacker).canAttackPlayer((EntityPlayer) target)) // 26.2 canHarmPlayer
			return false;
		return true;
	}

	/**
	 * 26.2 ProjectileUtil.getManyEntityHitResult (L160-186), ported stage by stage with identical
	 * hit/no-hit semantics:
	 * 1. the corridor start inside the EXACT box counts as a hit at the start (includeFromEntity);
	 * 2. an exact-box entry along the segment is a hit;
	 * 3. the hitboxMargin-inflated box is only a fallback and the segment must ENTER it from
	 *    outside - a target lying wholly before the corridor start (the 26.2 close-range dead
	 *    zone) stays unhittable even when its inflated shell crosses the start, because 26.2's
	 *    AABB.clip returns nothing for a segment beginning inside it;
	 * 3b. a margin graze additionally requires a clear block line-of-sight from the shell entry
	 *     toward the target center before the exact-box surface hit counts.
	 */
	private static Vec3 getEntityHitResult(World world, Entity target, Vec3 from, Vec3 to) {
		AxisAlignedBB box = target.boundingBox;
		if (box.isVecInside(from))
			return from; // stage 1: contains() on the exact box, hit recorded at the start
		Vec3 exact = rayIntersect(from, to, box); // stage 2: exact-box entry
		if (exact != null)
			return exact;
		if (RANGE_MARGIN <= 0.0F)
			return null;
		AxisAlignedBB inflated = box.expand(RANGE_MARGIN, RANGE_MARGIN, RANGE_MARGIN);
		if (inflated.isVecInside(from))
			return null; // stage 3 dead zone: 26.2 AABB.clip from inside the inflated shell = empty
		Vec3 outsideHit = rayIntersect(from, to, inflated);
		if (outsideHit == null)
			return null;
		// stage 3b: block line-of-sight re-check from the shell entry toward the target center
		Vec3 center = Vec3.createVectorHelper((box.minX + box.maxX) / 2.0D, (box.minY + box.maxY) / 2.0D, (box.minZ + box.maxZ) / 2.0D);
		MovingObjectPosition los = world.func_147447_a(outsideHit, center, false, true, false);
		Vec3 towards = los == null ? center : los.hitVec;
		return rayIntersect(outsideHit, towards, box); // exact-box surface hit, or nothing
	}

	/**
	 * Slab-method segment vs AABB; returns the entry point, or the segment start when it begins
	 * inside the box (26.2 getManyEntityHitResult includeFromEntity keeps such targets), or null.
	 */
	private static Vec3 rayIntersect(Vec3 from, Vec3 to, AxisAlignedBB box) {
		double tMin = 0.0D;
		double tMax = 1.0D; // t beyond 1 lies past the segment end
		int axis = -1;
		double[] o = { from.xCoord, from.yCoord, from.zCoord };
		double[] d = { to.xCoord - from.xCoord, to.yCoord - from.yCoord, to.zCoord - from.zCoord };
		double[] lo = { box.minX, box.minY, box.minZ };
		double[] hi = { box.maxX, box.maxY, box.maxZ };
		for (int i = 0; i < 3; i++) {
			if (Math.abs(d[i]) < 1.0E-8D) {
				if (o[i] < lo[i] || o[i] > hi[i])
					return null;
				continue;
			}
			double inv = 1.0D / d[i];
			double tA = (lo[i] - o[i]) * inv;
			double tB = (hi[i] - o[i]) * inv;
			if (tA > tB) {
				double tmp = tA;
				tA = tB;
				tB = tmp;
			}
			if (tA > tMin) {
				tMin = tA;
				axis = i;
			}
			if (tB < tMax)
				tMax = tB;
			if (tMin > tMax)
				return null;
		}
		if (axis < 0)
			return from; // segment start inside the box: hit at the start point
		return from.addVector(d[0] * tMin, d[1] * tMin, d[2] * tMin);
	}

	/**
	 * Per-attacker per-target contact cooldowns, mirroring 26.2 LivingEntity#recentKineticEnemies
	 * (gameTime of the last stab, entries expire by themselves). One instance for the client
	 * detector, one for the server settlement.
	 */
	public static class StabCooldowns {
		private final Map<EntityPlayer, Map<Integer, Long>> entries = new WeakHashMap<>();

		public boolean wasRecentlyStabbed(EntityPlayer attacker, Entity target, int allowedTime, long gameTime) {
			Map<Integer, Long> perAttacker = entries.get(attacker);
			if (perAttacker == null)
				return false;
			Long last = perAttacker.get(Integer.valueOf(target.getEntityId()));
			return last != null && gameTime - last.longValue() < allowedTime;
		}

		public void rememberStabbedEntity(EntityPlayer attacker, Entity target, long gameTime) {
			Map<Integer, Long> perAttacker = entries.get(attacker);
			if (perAttacker == null) {
				perAttacker = new HashMap<>();
				entries.put(attacker, perAttacker);
			}
			perAttacker.put(Integer.valueOf(target.getEntityId()), Long.valueOf(gameTime));
		}

		public void pruneOlderThan(long gameTime, long maxAge) {
			Iterator<Map<Integer, Long>> outer = entries.values().iterator();
			while (outer.hasNext()) {
				Map<Integer, Long> perAttacker = outer.next();
				Iterator<Long> inner = perAttacker.values().iterator();
				while (inner.hasNext()) {
					if (gameTime - inner.next().longValue() >= maxAge)
						inner.remove();
				}
				if (perAttacker.isEmpty())
					outer.remove();
			}
		}
	}

	public static class EntityHit {
		public final Entity entity;
		public final Vec3 hitVec;
		public final double dist;

		EntityHit(Entity entity, Vec3 hitVec, double dist) {
			this.entity = entity;
			this.hitVec = hitVec;
			this.dist = dist;
		}
	}
}
