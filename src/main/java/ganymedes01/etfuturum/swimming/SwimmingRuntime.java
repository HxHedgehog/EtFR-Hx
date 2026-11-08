package ganymedes01.etfuturum.swimming;

import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import ganymedes01.etfuturum.elytra.IElytraPlayer;
import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.DataWatcher;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/**
 * Shared logic for the 26.2 swimming port.
 */
public final class SwimmingRuntime {
	public static final float WIDTH = 0.6F;
	public static final float STANDING_HEIGHT = 1.8F;
	public static final float CROUCHING_HEIGHT = 1.5F;
	public static final float SWIMMING_HEIGHT = 0.6F;

	public static final float STANDING_EYE = 1.62F;
	public static final float CROUCHING_EYE = 1.27F;
	public static final float SWIMMING_EYE = 0.4F;

	public static final int POSE_KEEP = 0;
	public static final int POSE_STANDING = 1;
	public static final int POSE_CROUCHING = 2;
	public static final int POSE_SWIMMING = 3;

	private SwimmingRuntime() {
	}

	/**
	 * Dedicated watchable object id for the swimming flag, resolved on the first player
	 * construction: starts at {@link ConfigFunctions#swimmingDataWatcherSlot} and walks
	 * upward past ids other mods already claimed, so a free slot is guaranteed.
	 *
	 * <p>v7: the flag moved out of data watcher byte 0 into its own object. 1.7.10 single
	 * player syncs the whole shared byte 0 back and forth between the client and the
	 * integrated server; with two writers holding lagged copies (vanilla sprint bit 3 and
	 * the swimming bit 6), bit 3 was being stomped every few ticks without a single
	 * setSprinting call (F9 log v6: dw0 flip-flopping 0x08/0x00 while ls stayed false),
	 * which flickered the sprint FOV and deadlocked the vanilla sprint state machine
	 * (T4/T5 skip on !isSprinting(), T6 fires on the stomped bit and kills the real
	 * sprint). A dedicated object keeps byte 0 vanilla-only, which is proven stable in
	 * single player.</p>
	 */
	private static int swimmingSlot = -1;

	public static synchronized void registerSwimmingData(DataWatcher dataWatcher) {
		int id = swimmingSlot >= 0 ? swimmingSlot : ConfigFunctions.swimmingDataWatcherSlot;
		for (;;) {
			try {
				dataWatcher.addObject(id, (byte) 0);
				swimmingSlot = id;
				return;
			} catch (IllegalArgumentException duplicate) {
				if (++id > 31) {
					throw duplicate;
				}
			}
		}
	}

	public static int getSwimmingSlot() {
		return swimmingSlot >= 0 ? swimmingSlot : ConfigFunctions.swimmingDataWatcherSlot;
	}

	/**
	 * 26.2 {@code Entity#isEyeInFluid(FluidTags.WATER)} backed by the 1.7.10 water material and waterlogged blocks.
	 */
	public static boolean isWaterAt(World world, double x, double y, double z) {
		int bx = MathHelper.floor_double(x);
		int by = MathHelper.floor_double(y);
		int bz = MathHelper.floor_double(z);
		Block block = world.getBlock(bx, by, bz);
		return block.getMaterial() == Material.water || WaterlogRuntime.isWaterlogged(world, bx, by, bz);
	}

	private static float getEyeHeight(EntityPlayer player) {
		return ((ISwimmingPlayer) player).etfu$getEyeHeightAboveFeet();
	}

	public static boolean isEyeInWater(EntityPlayer player) {
		return isWaterAt(player.worldObj, player.posX, player.boundingBox.minY + getEyeHeight(player), player.posZ);
	}

	/**
	 * 26.2 {@code Entity#updateSwimming} and {@code Player#updateSwimming}.
	 *
	 * <p>Called on the server for every player and on the client only for the local player
	 * (1.7.10 remote players never run entity physics, so their {@code isInWater()} is always false).</p>
	 */
	public static void updateSwimming(EntityPlayer player) {
		ISwimmingPlayer swimmingPlayer = (ISwimmingPlayer) player;

		// 26.2 Player#updateSwimming: flying clears the swimming flag.
		if (player.capabilities.isFlying) {
			swimmingPlayer.etfu$setSwimming(false);
			return;
		}

		// Compatibility with the existing elytra port: gliding always wins.
		if (player instanceof IElytraPlayer && ((IElytraPlayer) player).etfu$isElytraFlying()) {
			swimmingPlayer.etfu$setSwimming(false);
			return;
		}

		// The integrated server syncs its packet-lagged sprint state back through data
		// watcher byte 0, stomping the sprint bit the client just wrote (F9 log: dw0
		// flip-flops 0x48/0x00 every tick while the client never changes its mind), so
		// 26.2 keeps the local player's sprint client-authoritative: the client decides
		// from the locally captured state, the server from its own C0B-driven state.
		boolean sprinting = player.worldObj.isRemote ? swimmingPlayer.etfu$isLocalSprinting() : player.isSprinting();

		boolean swimming;
		if (swimmingPlayer.etfu$isSwimming()) {
			swimming = sprinting && player.isInWater() && player.ridingEntity == null;
		} else {
			boolean underWater = isEyeInWater(player) && player.isInWater();
			swimming = sprinting && underWater && player.ridingEntity == null
					&& isWaterAt(player.worldObj, player.posX, player.boundingBox.minY, player.posZ);
		}
		swimmingPlayer.etfu$setSwimming(swimming);
	}

	/**
	 * 26.2 {@code Player#updatePlayerPose}. Returns one of the {@code POSE_*} constants.
	 */
	public static int computePose(EntityPlayer player) {
		// 26.2 only keeps the previous pose when even the smallest pose does not fit.
		// Sleeping keeps the vanilla 1.7.10 size: the bed model replaces the pose entirely.
		if (player.isPlayerSleeping()) {
			return POSE_KEEP;
		}
		if (player instanceof IElytraPlayer && ((IElytraPlayer) player).etfu$isElytraFlying()) {
			return POSE_KEEP;
		}
		if (!canFit(player, SWIMMING_HEIGHT)) {
			return POSE_KEEP;
		}

		if (((ISwimmingPlayer) player).etfu$isSwimming()) {
			return POSE_SWIMMING;
		}

		// getDesiredPose(): crouching is the desired pose while shift is held (not flying).
		if (player.isSneaking() && !player.capabilities.isFlying) {
			return canFit(player, CROUCHING_HEIGHT) ? POSE_CROUCHING : POSE_SWIMMING;
		}
		if (canFit(player, STANDING_HEIGHT)) {
			return POSE_STANDING;
		}
		if (canFit(player, CROUCHING_HEIGHT)) {
			return POSE_CROUCHING;
		}
		// 26.2 falls back to Pose.SWIMMING, which is rendered as crawling on land.
		return POSE_SWIMMING;
	}

	private static boolean canFit(EntityPlayer player, float height) {
		AxisAlignedBB box = AxisAlignedBB.getBoundingBox(
				player.posX - WIDTH / 2.0D, player.boundingBox.minY, player.posZ - WIDTH / 2.0D,
				player.posX + WIDTH / 2.0D, player.boundingBox.minY + height, player.posZ + WIDTH / 2.0D).contract(1.0E-7D, 1.0E-7D, 1.0E-7D);
		// func_147461_a is the block-only collision list (26.2 noCollision).
		return player.worldObj.func_147461_a(box).isEmpty();
	}
}
