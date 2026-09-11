package ganymedes01.etfuturum.blocks;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.client.sound.ModSounds;
import ganymedes01.etfuturum.core.utils.Utils;
import ganymedes01.etfuturum.lib.RenderIDs;
import ganymedes01.etfuturum.mixins.early.oceanplants.MixinBlockDynamicLiquid;
import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Sea pickles. Wet pickles occupy a water block (pseudo-waterlogged like
 * {@link BlockAquaticPlant}), glow, can be stacked to 4 per block and spread
 * underwater with bone meal. Dry pickles sit on land ({@link Material#coral},
 * which naturally makes water flow around them and stops falling blocks).
 * Metadata low 2 bits store pickle count - 1.
 */
public class BlockSeaPickle extends BaseBlock implements IGrowable {

	private final boolean wet;

	public BlockSeaPickle(boolean wet) {
		super(wet ? Material.water : Material.coral);
		this.wet = wet;
		setNames("sea_pickle");
		Utils.setBlockSound(this, ModSounds.soundSlime);
		setHardness(0.0F);
		setLightOpacity(wet ? Blocks.water.getLightOpacity() : 0);
		setBlockBounds(0.375F, 0.0F, 0.375F, 0.625F, 0.375F, 0.625F);
		if (wet) {
			setTickRandomly(true);
		}
	}

	public static int getPickleCount(int meta) {
		return (meta & 3) + 1;
	}

	public boolean isWet() {
		return wet;
	}

	@Override
	public int getRenderType() {
		return RenderIDs.SEA_PICKLE;
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
		return wet ? pass == 1 : super.canRenderInPass(pass);
	}

	@Override
	public int getRenderBlockPass() {
		return wet ? 1 : 0;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public String getItemIconName() {
		return "sea_pickle";
	}

	@Override
	public boolean isReplaceable(IBlockAccess world, int x, int y, int z) {
		return false;
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
		// 26.2: SEA_PICKLE has no noCollision() and no getCollisionShape override, so
		// the collision shape is the same pickle column as the outline (SHAPE_ONE..FOUR:
		// 4/10/12 pixel wide, 6-7 pixel tall) - entities bump into pickles and
		// pathfinding treats the cell as an obstacle, wet or dry
		setBlockBoundsBasedOnState(world, x, y, z);
		return AxisAlignedBB.getBoundingBox(x + minX, y + minY, z + minZ, x + maxX, y + maxY, z + maxZ);
	}

	@Override
	public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z) {
		setBlockBoundsBasedOnState(world, x, y, z);
		return AxisAlignedBB.getBoundingBox(x + minX, y + minY, z + minZ, x + maxX, y + maxY, z + maxZ);
	}

	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
		int count = getPickleCount(world.getBlockMetadata(x, y, z));
		float halfWidth = count <= 1 ? 0.125F : count == 2 ? 0.3125F : 0.375F;
		float height = count == 4 ? 0.4375F : 0.375F;
		setBlockBounds(0.5F - halfWidth, 0.0F, 0.5F - halfWidth, 0.5F + halfWidth, height, 0.5F + halfWidth);
	}

	@Override
	public int getLightValue(IBlockAccess world, int x, int y, int z) {
		return wet ? 3 + 3 * getPickleCount(world.getBlockMetadata(x, y, z)) : 0;
	}

	@Override
	public boolean canPlaceBlockAt(World world, int x, int y, int z) {
		Block target = world.getBlock(x, y, z);
		boolean targetValid = wet ? BlockAquaticPlant.isLiquidWater(target) : target.isAir(world, x, y, z);
		return targetValid && mayPlaceOn(world, x, y - 1, z);
	}

	@Override
	public void onBlockAdded(World world, int x, int y, int z) {
		super.onBlockAdded(world, x, y, z);
		if (wet && !world.isRemote) {
			// Covers bucket-filling (setBlock DEAD -> SEA_PICKLE) and direct placement
			scheduleFluidTick(world, x, y, z);
		} else if (!wet && !world.isRemote && BlockAquaticPlant.isLiquidWater(world.getBlock(x, y + 1, z))) {
			// Water still overhead when the cell turns dry (sponge scoop under a
			// waterfall) re-fills after one fluid tick, like 26.2 where the
			// neighbouring fluid re-waterlogs the cell on its next fluid update
			scheduleFluidTick(world, x, y, z);
		}
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
		if (!mayPlaceOn(world, x, y - 1, z)) {
			dropBlockAsItemWithChance(world, x, y, z, world.getBlockMetadata(x, y, z), 1.0F, 0);
			world.setBlock(x, y, z, wet ? Blocks.water : Blocks.air, 0, 3);
		} else if (wet && !world.isRemote) {
			// Like vanilla static water's setNotStationary: re-spread when a
			// neighbour changes so dug-out cells get flooded again
			scheduleFluidTick(world, x, y, z);
		} else if (!wet && !world.isRemote && BlockAquaticPlant.isLiquidWater(world.getBlock(x, y + 1, z))) {
			// Water arriving above a dry pickle (waterfall) fills after one fluid
			// tick. Pickle-side hook only: the flow maths never consults dry
			// pickles, so currents keep pathing around the Material.coral dam
			// exactly as before.
			scheduleFluidTick(world, x, y, z);
		}
	}

	/**
	 * 26.2 {@code SeaPickleBlock#mayPlaceOn}: the cell below needs a collision shape
	 * whose UP face is not empty (a sturdy top face implies the same). A plain 1.7.10
	 * AABB always has a non-empty UP face, so having any collision box is the exact
	 * equivalent - unlike the vanilla {@code isSideSolid} fallback this does not
	 * require opacity, letting pickles sit on leaves and glass like in modern
	 * versions. Air must be rejected explicitly (its 1.7.10 default full AABB is a
	 * false positive); liquids and the plants themselves return null boxes.
	 */
	public static boolean mayPlaceOn(World world, int x, int y, int z) {
		Block below = world.getBlock(x, y, z);
		if (below.getMaterial() == Material.air) {
			return false;
		}
		return below.getCollisionBoundingBoxFromPool(world, x, y, z) != null;
	}

	/**
	 * One vanilla fluid tick (5 game ticks) later: wet pickles re-spread, dry
	 * pickles re-validate and take the delayed environmental fill.
	 */
	private void scheduleFluidTick(World world, int x, int y, int z) {
		world.scheduleBlockUpdate(x, y, z, this, Blocks.flowing_water.tickRate(world));
	}

	/**
	 * Mirror of the backfill mixin's spreader condition, re-checked at fill time:
	 * a source-level water cell (raw meta 0) or a wet pickle anchor adjacent on
	 * the level plane. Partial flow (1-7) and falling water (8-15) still never
	 * fill dry pickles from the sides.
	 */
	private boolean hasBackfillNeighbor(World world, int x, int y, int z) {
		int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
		for (int[] dir : dirs) {
			int nx = x + dir[0];
			int nz = z + dir[1];
			Block block = world.getBlock(nx, y, nz);
			if (block == ModBlocks.SEA_PICKLE.get()
					|| (BlockAquaticPlant.isLiquidWater(block) && world.getBlockMetadata(nx, y, nz) == 0)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * In-place dry -> wet conversion keeping the pickle count (splash + flag 3
	 * so neighbours and clients sync). The converted cell's onBlockAdded
	 * schedules the spread one fluid tick later.
	 */
	private void rehydrate(World world, int x, int y, int z) {
		world.setBlock(x, y, z, ModBlocks.SEA_PICKLE.get(), world.getBlockMetadata(x, y, z), 3);
		world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "random.splash", 0.5F, 1.0F);
	}

	@Override
	public void updateTick(World world, int x, int y, int z, Random random) {
		if (world.isRemote) {
			return;
		}
		if (!wet) {
			// Delayed environmental fill, one fluid tick after the water reached
			// the cell (26.2: waterlogging happens on the fluid update, not the
			// instant the flow arrives). Re-checking here lets water that moved
			// on during the delay leave the pickle dry.
			if (BlockAquaticPlant.isLiquidWater(world.getBlock(x, y + 1, z)) || hasBackfillNeighbor(world, x, y, z)) {
				rehydrate(world, x, y, z);
			}
			return;
		}
		// Ignite the vanilla spread as if this cell were a level-0 water source -
		// the same state a bucket-placed flowing_water block starts from (meta 0
		// flows down as 8, sideways as 1). The placed water then continues
		// entirely through vanilla flow logic: self-scheduling, decay refresh,
		// settling to static water and receding once this anchor disappears.
		MixinBlockDynamicLiquid flow = (MixinBlockDynamicLiquid) Blocks.flowing_water;
		if (flow.etfuturum$canFlowInto(world, x, y - 1, z)) {
			flow.etfuturum$flowInto(world, x, y - 1, z, 8);
		} else {
			boolean[] dirs = flow.etfuturum$findFlowDirections(world, x, y, z);
			if (dirs[0]) {
				flow.etfuturum$flowInto(world, x - 1, y, z, 1);
			}
			if (dirs[1]) {
				flow.etfuturum$flowInto(world, x + 1, y, z, 1);
			}
			if (dirs[2]) {
				flow.etfuturum$flowInto(world, x, y, z - 1, 1);
			}
			if (dirs[3]) {
				flow.etfuturum$flowInto(world, x, y, z + 1, 1);
			}
		}
	}

	@Override
	public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z) {
		if (wet) {
			world.setBlock(x, y, z, Blocks.water, 0, 3);
			return true;
		}
		return super.removedByPlayer(world, player, x, y, z);
	}

	@Override
	public void onBlockExploded(World world, int x, int y, int z, Explosion explosion) {
		if (wet) {
			dropBlockAsItemWithChance(world, x, y, z, world.getBlockMetadata(x, y, z), 1.0F, 0);
			world.setBlock(x, y, z, Blocks.water, 0, 3);
		} else {
			super.onBlockExploded(world, x, y, z, explosion);
		}
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		ItemStack held = player.getHeldItem();
		int meta = world.getBlockMetadata(x, y, z);
		// Modern pickupBlock: an empty bucket un-waterlogs the pickle, leaving the
		// pickles themselves (now dry) and handing back a water bucket. Handled here
		// because vanilla ItemBucket would scoop the whole cell (material == water
		// && meta == 0 matches single-pickle stacks).
		if (wet && held != null && held.getItem() == Items.bucket) {
			if (!world.isRemote) {
				world.setBlock(x, y, z, ModBlocks.DEAD_SEA_PICKLE.get(), meta, 3);
				world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "random.splash", 0.5F, 1.0F);
			}
			// The inventory change must run on both sides, like vanilla
			// ItemBucket#func_150910_a does: a server-only change gets its slot
			// sync swallowed by isChangingQuantityOnly during C08 processing
			// (the server mirror is already updated, so no later
			// detectAndSendChanges resends it), leaving an invisible ghost water
			// bucket when scooping from a stacked empty bucket. Single buckets
			// only replace the held slot, which the explicit end-of-packet
			// SetSlot check happens to resync.
			if (!player.capabilities.isCreativeMode) {
				if (--held.stackSize <= 0) {
					player.inventory.mainInventory[player.inventory.currentItem] = new ItemStack(Items.water_bucket);
				} else if (!player.inventory.addItemStackToInventory(new ItemStack(Items.water_bucket))) {
					player.dropPlayerItemWithRandomChoice(new ItemStack(Items.water_bucket), false);
				}
			}
			return true;
		}
		// Already waterlogged: 26.2 placeLiquid fails and the bucket stays; return
		// true so vanilla doesn't place the water behind the pickle instead
		if (wet && held != null && held.getItem() == Items.water_bucket) {
			return true;
		}
		// Modern waterlogging: a water bucket fills the dry pickle's cell (re-hydrating
		// it) instead of placing water in the neighbouring cell and letting it flow around.
		if (!wet && held != null && held.getItem() == Items.water_bucket) {
			if (!world.isRemote) {
				world.setBlock(x, y, z, ModBlocks.SEA_PICKLE.get(), meta, 3);
				world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "random.splash", 0.5F, 1.0F);
				if (!player.capabilities.isCreativeMode) {
					if (--held.stackSize <= 0) {
						player.inventory.mainInventory[player.inventory.currentItem] = new ItemStack(Items.bucket);
					} else if (!player.inventory.addItemStackToInventory(new ItemStack(Items.bucket))) {
						player.dropPlayerItemWithRandomChoice(new ItemStack(Items.bucket), false);
					}
				}
			}
			return true;
		}
		// 26.2 canBeReplaced: a held pickle adds to a cluster unless sneaking -
		// sneaking falls through to the item placement behind the clicked face
		if (!player.isSneaking() && held != null && held.getItem() == Item.getItemFromBlock(ModBlocks.SEA_PICKLE.get()) && getPickleCount(meta) < 4) {
			return addToCluster(world, player, held, x, y, z, meta, this);
		}
		return false;
	}

	/**
	 * Shared "add one pickle to this cluster" routine for the right-click and
	 * item-placement paths (26.2 canBeReplaced): bumps the cluster count, plays
	 * the placement sound and consumes one pickle outside creative mode. Always
	 * returns true so vanilla does not process the click further.
	 */
	public static boolean addToCluster(World world, EntityPlayer player, ItemStack stack, int x, int y, int z, int meta, Block clusterBlock) {
		if (!world.isRemote) {
			world.setBlockMetadataWithNotify(x, y, z, meta + 1, 3);
			world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D,
					clusterBlock.stepSound.func_150496_b(), (clusterBlock.stepSound.getVolume() + 1.0F) / 2.0F, clusterBlock.stepSound.getPitch() * 0.8F);
			if (!player.capabilities.isCreativeMode) {
				stack.stackSize--;
			}
		}
		return true;
	}

	@Override
	public Item getItemDropped(int meta, Random random, int fortune) {
		return Item.getItemFromBlock(ModBlocks.SEA_PICKLE.get());
	}

	@Override
	public int quantityDropped(int meta, int fortune, Random random) {
		return getPickleCount(meta);
	}

	@Override
	public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z, EntityPlayer player) {
		// Modern SeaPickleBlock#getCloneItemStack: pick block yields the unified sea pickle
		// item for both wet and dry blocks (same pattern as BlockKelpPlant). The default impl
		// would return DEAD_SEA_PICKLE's plain ItemBlock - a separate item whose placement
		// rules only allow land.
		return new ItemStack(Item.getItemFromBlock(ModBlocks.SEA_PICKLE.get()));
	}

	@Override
	public boolean func_149851_a(World world, int x, int y, int z, boolean isClient) {
		// 26.2 isValidBonemealTarget: alive (not dead) and a coral block below.
		// There are no coral blocks here yet, so the shared placement support
		// check stands in for the CORAL_BLOCKS floor.
		return wet && mayPlaceOn(world, x, y - 1, z);
	}

	@Override
	public boolean func_149852_a(World world, Random rand, int x, int y, int z) {
		return true;
	}

	@Override
	public void func_149853_b(World world, Random rand, int x, int y, int z) {
		// 26.2 SeaPickleBlock#performBonemeal: each cell of a manhattan diamond
		// (radius 2, y-1..y) has a 1/6 chance to grow a new cluster of 1-4
		// pickles onto a water cell whose floor supports them, then the clicked
		// cluster is topped up to 4. 26.2 only targets vanilla water cells over
		// CORAL_BLOCKS; no coral exists here yet, so mayPlaceOn (the placement
		// support check) stands in for the floor requirement.
		int zSpan = 1;
		int zOffset = 0;
		for (int i = 0; i < 5; ++i) {
			int cx = x - 2 + i;
			for (int dz = 0; dz < zSpan; ++dz) {
				int cz = z - zOffset + dz;
				for (int dy = y - 1; dy <= y; ++dy) {
					if ((cx == x && cz == z && dy == y) || rand.nextInt(6) != 0
							|| !BlockAquaticPlant.isLiquidWater(world.getBlock(cx, dy, cz))
							|| !mayPlaceOn(world, cx, dy - 1, cz)) {
						continue;
					}
					world.setBlock(cx, dy, cz, this, rand.nextInt(4), 3);
				}
			}
			if (i < 2) {
				zSpan += 2;
				zOffset++;
			} else {
				zSpan -= 2;
				zOffset--;
			}
		}
		world.setBlockMetadataWithNotify(x, y, z, 3, 2);
	}
}
