package ganymedes01.etfuturum.blocks;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.lib.RenderIDs;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Random;

/**
 * Base for the Update Aquatic plants that directly occupy a water block
 * (pseudo-waterlogged, the same approach used by {@link BlockBubbleColumn}).
 * <p>
 * The block uses {@link Material#water} so fluid physics, swimming drag,
 * breath, underwater fog and biome water tint all come for free, and the
 * renderer paints a full water volume in pass 1 before drawing the plant.
 * Destruction restores a still water source, matching modern waterlogging.
 * <p>
 * Shader pack contract: the painted water quads are auto-tagged with the
 * pack's water id by Angelica (sprite-based), so water programs apply on any
 * pack without pack-side configuration. For the plant crosses, the blocks are
 * registered into Angelica's {@code tag:ocean_plants} shader tag (see
 * {@link ganymedes01.etfuturum.compat.AngelicaCompat}); a pack maps the whole
 * group with one line (e.g. {@code block.41000=tag:ocean_plants}) and any mod
 * registering the same tag is covered too. Unmapped packs just render the
 * crosses with the default program.
 */
public abstract class BlockAquaticPlant extends BaseBlock {

	public BlockAquaticPlant() {
		super(Material.water);
		setLightOpacity(Blocks.water.getLightOpacity());
		setHardness(0.0F);
	}

	/**
	 * The water-material blocks added by the ocean plants feature.
	 * Used by the liquid/falling-block/sponge mixins to tell them apart from real water.
	 */
	public static boolean isOceanPlant(Block block) {
		return block == ModBlocks.SEAGRASS.get()
				|| block == ModBlocks.KELP.get()
				|| block == ModBlocks.KELP_PLANT.get()
				|| block == ModBlocks.SEA_PICKLE.get();
	}

	public static boolean isLiquidWater(Block block) {
		return block == Blocks.water || block == Blocks.flowing_water;
	}

	/**
	 * 26.2 FluidState#isFull for 1.7.10 water: sources (meta 0) and falling
	 * columns (meta 8-15) fill the cell, the flowing levels (meta 1-7) do not.
	 */
	public static boolean isFullWater(IBlockAccess world, int x, int y, int z) {
		int meta = world.getBlockMetadata(x, y, z);
		return isLiquidWater(world.getBlock(x, y, z)) && (meta == 0 || meta >= 8);
	}

	/**
	 * 26.2 SeagrassBlock#mayPlaceOn (isFaceSturdy UP; the CANNOT_SUPPORT_SEAGRASS
	 * tag has no 1.7.10 equivalent): the floor's collision box must cover a full
	 * square at the top of the cell. getCollisionBoundingBoxFromPool returns
	 * world-offset boxes, hence the y + 1 comparison. Opacity plays no role, so
	 * leaves and glass support plants like in modern versions, while slabs and
	 * farmland fail because their boxes never reach the cell top.
	 */
	public static boolean hasSolidGround(World world, int x, int y, int z) {
		return isFaceSturdy(world, x, y - 1, z, ForgeDirection.UP.ordinal());
	}

	/**
	 * 26.2 BlockBehaviour#isFaceSturdy for plain 1.7.10 collision AABBs: the box
	 * must cover a full square at the requested cell boundary. Air is rejected
	 * explicitly (its 1.7.10 default box is a false positive full cube) and
	 * liquids/plants return null boxes.
	 */
	public static boolean isFaceSturdy(World world, int x, int y, int z, int side) {
		Block block = world.getBlock(x, y, z);
		if (block.getMaterial() == Material.air) {
			return false;
		}
		AxisAlignedBB box = block.getCollisionBoundingBoxFromPool(world, x, y, z);
		if (box == null) {
			return false;
		}
		boolean fullX = box.minX <= x && box.maxX >= x + 1.0D;
		boolean fullY = box.minY <= y && box.maxY >= y + 1.0D;
		boolean fullZ = box.minZ <= z && box.maxZ >= z + 1.0D;
		switch (side) {
			case 0: return fullX && fullZ && box.minY <= y; // DOWN
			case 1: return fullX && fullZ && box.maxY >= y + 1.0D; // UP
			case 2: return fullX && fullY && box.minZ <= z; // NORTH
			case 3: return fullX && fullY && box.maxZ >= z + 1.0D; // SOUTH
			case 4: return fullY && fullZ && box.minX <= x; // WEST
			case 5: return fullY && fullZ && box.maxX >= x + 1.0D; // EAST
			default: return false;
		}
	}

	/**
	 * 26.2 BlockState#isCollisionShapeFullBlock: the collision box covers the
	 * whole cell (used by the bone meal random walk to stop at solids).
	 */
	public static boolean isCollisionShapeFullBlock(World world, int x, int y, int z) {
		Block block = world.getBlock(x, y, z);
		if (block.getMaterial() == Material.air) {
			return false;
		}
		AxisAlignedBB box = block.getCollisionBoundingBoxFromPool(world, x, y, z);
		return box != null
				&& box.minX <= x && box.maxX >= x + 1.0D
				&& box.minY <= y && box.maxY >= y + 1.0D
				&& box.minZ <= z && box.maxZ >= z + 1.0D;
	}

	@Override
	public boolean canPlaceBlockAt(World world, int x, int y, int z) {
		return isLiquidWater(world.getBlock(x, y, z)) && hasSolidGround(world, x, y, z);
	}

	@Override
	public boolean isReplaceable(IBlockAccess world, int x, int y, int z) {
		return false;
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
		return null;
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}

	@Override
	public boolean canRenderInPass(int pass) {
		return pass == 1;
	}

	@Override
	public int getRenderBlockPass() {
		return 1;
	}

	@Override
	public int getRenderType() {
		return RenderIDs.OCEAN_CROSS;
	}

	@Override
	public int quantityDropped(Random random) {
		return 0;
	}

	@Override
	public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z) {
		world.setBlock(x, y, z, Blocks.water, 0, 3);
		return true;
	}

	@Override
	public void onBlockExploded(World world, int x, int y, int z, Explosion explosion) {
		die(world, x, y, z);
	}

	/**
	 * Called when the plant loses support or becomes invalid.
	 * Drops whatever the block would normally drop (kelp drops itself; seagrass drops nothing)
	 * and restores a still water source.
	 */
	protected void die(World world, int x, int y, int z) {
		dropBlockAsItemWithChance(world, x, y, z, world.getBlockMetadata(x, y, z), 1.0F, 0);
		world.setBlock(x, y, z, Blocks.water, 0, 3);
	}
}
