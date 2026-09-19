package ganymedes01.etfuturum.waterlog;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.blocks.BlockAquaticPlant;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;

import java.util.ArrayList;
import java.util.List;

/**
 * The visual-waterlogging registry: every {@link WaterlogFamily} registers
 * here and the mixins dispatch through it instead of instanceof-checking
 * concrete block classes - the hooks are the generic mechanism (pass gates,
 * water shell suppression, neighbour invalidation, the Angelica meshing
 * path) and the families own the policy. Also home to the water predicates
 * and face-offset helpers shared by every family's qualification.
 * <p>
 * The families register in this class's static initializer, which first
 * runs when a hook dispatches - during client chunk building or the
 * client-side invalidation sweep. Dedicated servers never load it: every
 * call site sits behind an isRemote guard that short-circuits first.
 */
public class VisualWaterlog {

	private static final List<WaterlogFamily> FAMILIES = new ArrayList<WaterlogFamily>();

	static {
		register(StairWaterFamily.INSTANCE);
		register(ThinBlockWaterFamily.INSTANCE);
	}

	public static void register(WaterlogFamily family) {
		FAMILIES.add(family);
	}

	/** The family the block belongs to, or null for non-participating blocks. */
	public static WaterlogFamily familyOf(Block block) {
		for (WaterlogFamily family : FAMILIES) {
			if (family.isMember(block)) {
				return family;
			}
		}
		return null;
	}

	/**
	 * Whether the cell holds a family member that currently qualifies for
	 * painted water - membership plus the stateless waterlog test.
	 */
	public static boolean isWaterlogged(IBlockAccess world, int x, int y, int z) {
		WaterlogFamily family = familyOf(world.getBlock(x, y, z));
		return family != null && family.shouldRenderWater(world, x, y, z);
	}

	/** Whether any registered family's config flag is on. */
	public static boolean anyEnabled() {
		for (WaterlogFamily family : FAMILIES) {
			if (family.isEnabled()) {
				return true;
			}
		}
		return false;
	}

	/** Any water-material cell, at any flow level (static water, flowing water, ocean plants). */
	public static boolean isWaterBlock(Block block) {
		return block != null && block.getMaterial() == Material.water;
	}

	/** Cells that render as a full still water volume: sources, ocean plants and wet sea pickles. */
	public static boolean isStillWaterBlock(Block block, int meta) {
		if (!isWaterBlock(block)) {
			return false;
		}
		// Ocean plants and pickles carry their own metadata (age/count), but their
		// renderers paint a full still water cell, so they count as still water
		return meta == 0 || BlockAquaticPlant.isOceanPlant(block) || block == ModBlocks.SEA_PICKLE.get();
	}

	/**
	 * The flow level of the real water feeding a visually waterlogged member
	 * cell - used as the fallback corner height when every sample around a
	 * corner is solid (an embedded member, which vanilla's averaging cannot
	 * express because it skips solid cells and would divide by zero). Water
	 * directly above means a falling column (level 8); otherwise the
	 * horizontal neighbour with the highest surface wins. Beware the unit:
	 * getLiquidHeightPercent returns the AIR GAP above the surface (source
	 * 1/9, flow end 8/9), so the highest surface is the SMALLEST percent and
	 * the initial value must sit above every possible gap - initialising it
	 * to percent(0) (the smallest gap there is) makes the comparison
	 * unreachable and every member read as fed by a source. Only real water
	 * cells are consulted - members never qualify each other - so the lookup
	 * cannot recurse and stays O(1).
	 */
	public static int sourceLiquidMeta(IBlockAccess world, int x, int y, int z) {
		if (isWaterBlock(world.getBlock(x, y + 1, z))) {
			return 8;
		}
		int bestMeta = 0;
		float bestPercent = 1.0F; // above every possible air gap: the deepest water neighbour must win
		for (int dir = 2; dir <= 5; dir++) {
			int nx = x + offsetX(dir);
			int nz = z + offsetZ(dir);
			Block neighbour = world.getBlock(nx, y, nz);
			if (!isWaterBlock(neighbour)) {
				continue;
			}
			int meta = world.getBlockMetadata(nx, y, nz);
			if (BlockAquaticPlant.isOceanPlant(neighbour)) {
				meta = 0; // plants carry age/count metadata, not flow levels
			}
			float percent = BlockLiquid.getLiquidHeightPercent(meta);
			if (percent < bestPercent) {
				bestPercent = percent;
				bestMeta = meta;
			}
		}
		return bestMeta;
	}

	/**
	 * Whether the eye point is inside a visually waterlogged cell's painted
	 * water volume: floor the point to its cell, resolve the family, and let
	 * the family decide against its exact sub-volume. Used by the underwater
	 * overlay so the screen dims only when the camera actually sits inside the
	 * painted water, mirroring how real water's {@code isInsideOfMaterial}
	 * works without any gameplay coupling.
	 */
	public static boolean isEyeSubmerged(IBlockAccess world, double eyeX, double eyeY, double eyeZ) {
		int x = MathHelper.floor_double(eyeX);
		int y = MathHelper.floor_double(eyeY);
		int z = MathHelper.floor_double(eyeZ);
		WaterlogFamily family = familyOf(world.getBlock(x, y, z));
		return family != null && family.isEyeSubmerged(world, x, y, z, eyeX, eyeY, eyeZ);
	}

	/**
	 * Sampled over the entity's eye, matching how real water uses the body
	 * width: eight points spread across the eye height and the horizontal
	 * body extent catch cells the camera straddles at a boundary, where a
	 * single floored point could land just outside the painted volume.
	 */
	public static boolean isEyeInVisualWater(EntityLivingBase entity) {
		if (entity == null || entity.worldObj == null) {
			return false;
		}
		double eyeY = entity.posY + entity.getEyeHeight();
		for (int i = 0; i < 8; i++) {
			float offX = ((float) ((i & 1) * 2 - 1)) * entity.width * 0.4F;
			float offY = ((float) (((i >> 1) & 1) * 2 - 1)) * 0.05F;
			float offZ = ((float) (((i >> 2) & 1) * 2 - 1)) * entity.width * 0.4F;
			if (isEyeSubmerged(entity.worldObj, entity.posX + offX, eyeY + offY, entity.posZ + offZ)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Set by the vanilla pass gate (MixinWorldRenderer) for the single
	 * renderBlockByRenderType call that follows: a qualifying member's pass-1
	 * slot paints the water volume instead of the block's own geometry copy.
	 * Vanilla chunk building runs on the render thread, so no synchronization
	 * is needed; the gate clears the flag on every other canRenderInPass call.
	 */
	public static boolean paintNextCall;

	public static int offsetX(int dir) {
		return dir == 4 ? -1 : dir == 5 ? 1 : 0;
	}

	public static int offsetY(int dir) {
		return dir == 0 ? -1 : dir == 1 ? 1 : 0;
	}

	public static int offsetZ(int dir) {
		return dir == 2 ? -1 : dir == 3 ? 1 : 0;
	}
}
