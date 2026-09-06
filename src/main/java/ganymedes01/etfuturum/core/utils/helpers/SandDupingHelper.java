package ganymedes01.etfuturum.core.utils.helpers;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.core.utils.Logger;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * Lenient port of the modern (1.21+) falling block end portal duplication ("sand duping").
 * <p>
 * In modern versions a falling block touching an end portal keeps falling and lands in the original dimension
 * (the teleport force-ticks its landing logic), while a copy with the same motion spawns at the End spawn point.
 * Vanilla 1.7.10 instead teleports the block away cleanly, so the machines cannot work.
 * <p>
 * When a falling block touches an end portal in the overworld we:
 * <ul>
 * <li>Refresh the End spawn platform (destroying mismatched blocks with drops, like modern EndPlatformFeature) and
 * spawn a copy of the block at (100.5, 50, 0.5) with its motion preserved,</li>
 * <li>Park the original one block above portal level on the side it came from with its motion cleared, so it
 * appears beside the portal; whether it then falls is left to the machine layout,</li>
 * </ul>
 * Every touch duplicates: both versions check overlap only against the post-move position, and their horizontal
 * trigger windows are identical (1 block cell + 0.98 block entity width), so any sand that touches here would
 * have touched in modern versions too. High momentum sand that tunnels past the portal in one tick never covers
 * the portal cell and never reaches this code, so fast sand stays clean without a speed check.
 */
public class SandDupingHelper {

	private static final int END_SPAWN_X = 100;
	private static final int END_SPAWN_Y = 50;
	private static final int END_SPAWN_Z = 0;

	private static final Set<Entity> DUPLICATED = Collections.newSetFromMap(new WeakHashMap<Entity, Boolean>());

	/**
	 * Attempts to duplicate a falling block touching an end portal.
	 *
	 * @return true if the duplication was handled and the vanilla teleport should be suppressed
	 */
	public static boolean tryDuplicateFallingBlock(World world, int x, int y, int z, Entity entity) {
		if (!ConfigMixins.enableSandDuping || world.isRemote) {
			return false;
		}
		boolean debug = ConfigMixins.sandDupingDebug;
		if (debug) {
			Logger.info("[SandDuping] touch at portal (" + x + ", " + y + ", " + z + ") dim " + entity.dimension + ", " + entity.getClass().getSimpleName()
					+ ", pos (" + fmt(entity.posX) + ", " + fmt(entity.posY) + ", " + fmt(entity.posZ) + ")"
					+ ", motion (" + fmt(entity.motionX) + ", " + fmt(entity.motionY) + ", " + fmt(entity.motionZ) + ")"
					+ ", onGround " + entity.onGround);
		}
		if (!(entity instanceof EntityFallingBlock)) {
			return false;
		}
		if (entity.dimension != 0) {
			debug("dim " + entity.dimension + " is not the overworld, vanilla teleport");
			return false;
		}
		if (DUPLICATED.contains(entity)) {
			// A multi tick crossing touches the portal again; ignore it without teleporting the original away.
			debug("entity already duplicated, ignoring further touches");
			return true;
		}

		WorldServer endWorld = MinecraftServer.getServer().worldServerForDimension(1);
		if (endWorld == null) {
			debug("End world unavailable, vanilla teleport");
			return false;
		}
		Entity copy = EntityList.createEntityByName(EntityList.getEntityString(entity), endWorld);
		if (copy == null) {
			debug("failed to recreate the entity for the End copy, vanilla teleport");
			return false;
		}

		DUPLICATED.add(entity);

		// End side: refresh the spawn platform and spawn a copy of the block with the same state.
		WorldHelper.createEndPlatform(endWorld, END_SPAWN_X, END_SPAWN_Y - 1, END_SPAWN_Z, true);
		copy.copyDataFrom(entity, true); // NBT copy keeps the block, metadata and motion
		copy.setPosition(END_SPAWN_X + 0.5D, END_SPAWN_Y, END_SPAWN_Z + 0.5D);
		endWorld.spawnEntityInWorld(copy);
		if (debug) {
			Logger.info("[SandDuping] duplicating: End copy spawned at (" + END_SPAWN_X + ".5, " + END_SPAWN_Y + ", " + END_SPAWN_Z + ".5)"
					+ " with motion (" + fmt(copy.motionX) + ", " + fmt(copy.motionY) + ", " + fmt(copy.motionZ) + "), platform refreshed");
		}

		// Overworld side: park the original on the side of the portal it came from.
		parkBesidePortal((EntityFallingBlock) entity, x, y, z);
		return true;
	}

	/**
	 * Parks the original beside the portal on the side it came from: a sand flying in from the north (motion
	 * towards +Z) appears north of the portal, one block above portal level. No support check is done; whether
	 * the parked block then falls is the machine's business.
	 */
	private static void parkBesidePortal(EntityFallingBlock entity, int portalX, int portalY, int portalZ) {
		// Opposite of the dominant motion axis: the side the sand arrived from.
		int dx = 0;
		int dz = 0;
		if (Math.abs(entity.motionX) >= Math.abs(entity.motionZ)) {
			if (Math.abs(entity.motionX) > 1.0E-4D) {
				dx = entity.motionX >= 0 ? -1 : 1;
			}
		}
		else if (Math.abs(entity.motionZ) > 1.0E-4D) {
			dz = entity.motionZ >= 0 ? -1 : 1;
		}
		if (dx == 0 && dz == 0) {
			// No horizontal motion to derive a side from: keep the trajectory, the End copy is already placed.
			debug("no horizontal motion, keeping the original trajectory");
			return;
		}
		int parkX = portalX + dx;
		int parkY = portalY + 1;
		int parkZ = portalZ + dz;
		entity.setLocationAndAngles(parkX + 0.5D, parkY, parkZ + 0.5D, entity.rotationYaw, 0.0F);
		entity.motionX = entity.motionY = entity.motionZ = 0.0D;
		debug("parked the original at (" + parkX + ", " + parkY + ", " + parkZ + ") with zeroed motion");
	}

	private static void debug(String message) {
		if (ConfigMixins.sandDupingDebug) {
			Logger.info("[SandDuping] " + message);
		}
	}

	private static String fmt(double value) {
		return String.format("%.3f", value);
	}
}
