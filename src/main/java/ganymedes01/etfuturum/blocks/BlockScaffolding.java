package ganymedes01.etfuturum.blocks;

import ganymedes01.etfuturum.client.sound.ModSounds;
import ganymedes01.etfuturum.lib.RenderIDs;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.List;
import java.util.Random;

/**
 * Backport of the 1.14+ scaffolding block (26.2 {@code ScaffoldingBlock}).
 * <p>
 * Redstone-relevant behavior: instead of breaking on neighbor change, every
 * placement and neighbor update schedules a tick 1 game tick later; the tick
 * recomputes the stability distance and writes it back with flag 3, so a
 * distance change propagates exactly one block per game tick - the 1gt delay
 * lines used with (mod) observers.
 * <p>
 * Meta layout (4 bits): bits 0-2 = stability distance (0-7), bit 3 = bottom.
 */
public class BlockScaffolding extends BaseBlock {

	private IIcon iconTop;
	private IIcon iconSide;
	private IIcon iconBottom;

	public BlockScaffolding() {
		super(Material.wood);
		setUnlocalizedNameWithPrefix("scaffolding");
		setBlockSound(ModSounds.soundScaffolding);
		setHardness(0.0F);
		setLightOpacity(0);
	}

	public static int getDistanceFromMeta(int meta) {
		return meta & 7;
	}

	public static boolean isBottomFromMeta(int meta) {
		return (meta & 8) != 0;
	}

	public static int composeMeta(int distance, boolean bottom) {
		return (distance & 7) | (bottom ? 8 : 0);
	}

	/**
	 * 26.2 ScaffoldingBlock.getDistance: scaffolding below inherits its distance;
	 * a block with a sturdy top face below gives 0; otherwise the minimum of the
	 * horizontal scaffolding neighbours' distance + 1, defaulting to 7.
	 */
	public static int getDistance(IBlockAccess world, int x, int y, int z) {
		Block below = world.getBlock(x, y - 1, z);
		int distance = 7;
		if (below instanceof BlockScaffolding) {
			distance = getDistanceFromMeta(world.getBlockMetadata(x, y - 1, z));
		} else if (below.isSideSolid(world, x, y - 1, z, ForgeDirection.UP)) {
			return 0;
		}
		for (ForgeDirection dir : HORIZONTALS) {
			int nx = x + dir.offsetX;
			int nz = z + dir.offsetZ;
			if (world.getBlock(nx, y, nz) instanceof BlockScaffolding) {
				distance = Math.min(distance, getDistanceFromMeta(world.getBlockMetadata(nx, y, nz)) + 1);
				if (distance == 1) {
					break;
				}
			}
		}
		return distance;
	}

	private static boolean isBottom(IBlockAccess world, int x, int y, int z, int distance) {
		return distance > 0 && !(world.getBlock(x, y - 1, z) instanceof BlockScaffolding);
	}

	private static final ForgeDirection[] HORIZONTALS = {ForgeDirection.EAST, ForgeDirection.WEST, ForgeDirection.SOUTH, ForgeDirection.NORTH};

	@Override
	public int onBlockPlaced(World world, int x, int y, int z, int side, float subX, float subY, float subZ, int meta) {
		int distance = getDistance(world, x, y, z);
		return composeMeta(distance, isBottom(world, x, y, z, distance));
	}

	/**
	 * 26.2 onPlace: schedule the first stability tick 1gt after placement.
	 */
	@Override
	public void onBlockAdded(World world, int x, int y, int z) {
		world.scheduleBlockUpdate(x, y, z, this, 1);
	}

	/**
	 * 26.2 updateShape: any neighbor change re-schedules the stability tick,
	 * giving exactly one block of propagation per game tick.
	 */
	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
		world.scheduleBlockUpdate(x, y, z, this, 1);
	}

	/**
	 * 26.2 tick: recompute distance; already-unsupported scaffolding turns into
	 * a falling block, newly-unsupported scaffolding breaks with drops, and any
	 * distance/bottom change is written back with flag 3 (which also feeds the
	 * observer mixins).
	 */
	@Override
	public void updateTick(World world, int x, int y, int z, Random rand) {
		if (world.isRemote) {
			return;
		}
		int meta = world.getBlockMetadata(x, y, z);
		int oldDistance = getDistanceFromMeta(meta);
		int distance = getDistance(world, x, y, z);
		if (distance == 7) {
			if (oldDistance == 7) {
				// 26.2 FallingBlockEntity.fall: spawn the entity holding the recomputed state;
				// like vanilla falling sand, the entity clears the origin cell itself on its
				// first update (the origin block must stay so the entity can verify it).
				// Spawn at the cell CENTRE like BlockFalling: EntityFallingBlock sets
				// yOffset = height/2 which centres the bbox on posY, and 1.7.10 runs the
				// first moveEntity BEFORE the origin check - spawning at y would leave the
				// bbox half outside the cell (no collision), let posY sink to y-0.04 so
				// floor(posY) = y-1, and the entity would die without ever clearing the
				// origin block (floating scaffolds / never-falling distance-7 bridges).
				int fallMeta = composeMeta(7, isBottom(world, x, y, z, 7));
				world.spawnEntityInWorld(new EntityFallingBlock(world, x + 0.5D, y + 0.5D, z + 0.5D, this, fallMeta));
			} else {
				// 26.2 destroyBlock(pos, true): fires the levelEvent(2001) break
				// particles + sound, drops the item, then removes the block. 1.7.10's
				// World.func_147480_a is the same sequence via playAuxSFX(2001) - using
				// it (instead of a bare drop + setBlockToAir) is what gives the cascade
				// breaks the same break effect as a directly mined scaffold.
				world.func_147480_a(x, y, z, true);
			}
		} else {
			int newMeta = composeMeta(distance, isBottom(world, x, y, z, distance));
			if (newMeta != meta) {
				world.setBlock(x, y, z, this, newMeta, 3);
			}
		}
	}

	/**
	 * 26.2 getCollisionShape: entity-aware, dynamic. Entities above the block top
	 * (sneaking players excepted - they descend through) collide with the stable
	 * shape (top plate + 4 corner pillars); everyone else falls through the open
	 * centre, except onto the bottom plate of an unsupported base segment.
	 * {@code addCollisionBoxesToList} carries the colliding entity in 1.7.10.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, List list, Entity entity) {
		int meta = world.getBlockMetadata(x, y, z);
		boolean bottom = isBottomFromMeta(meta);
		int distance = getDistanceFromMeta(meta);

		// 26.2 EntityCollisionContext: entityBottom = feet Y, isDescending = sneaking player.
		// A null-entity query (e.g. explosion density) is the "empty context" whose
		// isAbove returns true, i.e. the stable shape.
		boolean above = entity == null || entity.boundingBox.minY > y + 1.0D - 1.0E-5D;
		boolean descending = entity instanceof EntityPlayer && entity.isSneaking();

		if (above && !descending) {
			addCollisionBox(list, mask, x, y, z, 0.0D, 0.875D, 0.0D, 1.0D, 1.0D, 1.0D); // top plate
			for (double[] p : PILLARS) {
				addCollisionBox(list, mask, x, y, z, p[0], 0.0D, p[1], p[0] + 0.125D, 1.0D, p[1] + 0.125D);
			}
		} else if (distance != 0 && bottom && entity != null && entity.boundingBox.minY > y - 1.0E-5D) {
			addCollisionBox(list, mask, x, y, z, 0.0D, 0.0D, 0.0D, 1.0D, 0.125D, 1.0D); // bottom plate
		}
	}

	private static final double[][] PILLARS = {{0.0D, 0.0D}, {0.0D, 0.875D}, {0.875D, 0.875D}, {0.875D, 0.0D}};

	@SuppressWarnings("unchecked")
	private static void addCollisionBox(List list, AxisAlignedBB mask, int x, int y, int z, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		AxisAlignedBB box = AxisAlignedBB.getBoundingBox(x + minX, y + minY, z + minZ, x + maxX, y + maxY, z + maxZ);
		if (mask.intersectsWith(box)) {
			list.add(box);
		}
	}

	/**
	 * 26.2 isPlacement -> Shapes.empty(): placement is never blocked by entities,
	 * so scaffolding can be stacked underneath yourself mid-air.
	 */
	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
		return null;
	}

	/**
	 * 26.2 getInteractionShape (and getShape while holding the item): full block.
	 */
	@Override
	public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z) {
		return AxisAlignedBB.getBoundingBox(x, y, z, x + 1.0D, y + 1.0D, z + 1.0D);
	}

	/**
	 * 26.2 CLIMBABLE tag.
	 */
	@Override
	public boolean isLadder(IBlockAccess world, int x, int y, int z, EntityLivingBase entity) {
		return true;
	}

	@Override
	public int getMobilityFlag() {
		return 1; // pushReaction DESTROY
	}

	@Override
	public boolean isFlammable(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
		return true;
	}

	@Override
	public int getFlammability(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
		return 60;
	}

	@Override
	public int getFireSpreadSpeed(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
		return 60;
	}

	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public int getRenderType() {
		return RenderIDs.SCAFFOLDING;
	}

	@Override
	public void registerBlockIcons(IIconRegister reg) {
		iconTop = reg.registerIcon("scaffolding_top");
		iconSide = reg.registerIcon("scaffolding_side");
		iconBottom = reg.registerIcon("scaffolding_bottom");
		blockIcon = iconTop; // 26.2 particle = top
	}

	@Override
	public IIcon getIcon(int side, int meta) {
		return side == 1 ? iconTop : side == 0 ? iconBottom : iconSide;
	}
}
