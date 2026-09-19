package ganymedes01.etfuturum.blocks.itemblocks;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.blocks.BlockScaffolding;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * 26.2 {@code ScaffoldingBlockItem} backport.
 * <p>
 * Clicking an existing scaffolding cell extends the structure: non-sneaking
 * walks up the tower (or horizontally away from the player when clicking a top
 * face), sneaking walks along the clicked face; the first replaceable
 * non-scaffolding cell along the way is the placement target (horizontal walks
 * are capped at 7 cells like 26.2). Placing into a fresh cell whose stability
 * distance would compute to 7 is refused.
 */
public class ItemBlockScaffolding extends ItemBlock {

	public ItemBlockScaffolding(Block block) {
		super(block);
	}

	@Override
	public int getMetadata(int meta) {
		return 0;
	}

	/**
	 * All placement validation lives in onItemUse (26.2 mustSurvive() = false),
	 * so the default precheck - which cannot express these rules - is bypassed.
	 */
	@Override
	public boolean func_150936_a(World world, int x, int y, int z, int side, EntityPlayer player, ItemStack stack) {
		return true;
	}

	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
		Block placed = ModBlocks.SCAFFOLDING.get();
		Block clicked = world.getBlock(x, y, z);
		int tx;
		int ty;
		int tz;

		if (clicked == placed) {
			// 26.2 canBeReplaced: clicking scaffolding with scaffolding extends the structure
			ForgeDirection dir;
			if (player.isSneaking()) {
				// 26.2 isInside(): clicking the cell the player's own box overlaps
				boolean inside = player.boundingBox.intersectsWith(AxisAlignedBB.getBoundingBox(x, y, z, x + 1.0D, y + 1.0D, z + 1.0D));
				dir = ForgeDirection.getOrientation(side);
				if (inside) {
					dir = dir.getOpposite();
				}
			} else {
				dir = side == 1 ? getHorizontalLookDirection(player) : ForgeDirection.UP;
			}

			tx = x + dir.offsetX;
			ty = y + dir.offsetY;
			tz = z + dir.offsetZ;
			int horizontalSteps = 0;
			boolean found = false;
			while (horizontalSteps < 7) {
				if (ty < 0 || ty > 255 || !world.blockExists(tx, ty, tz)) {
					return false;
				}
				Block cell = world.getBlock(tx, ty, tz);
				if (cell != placed) {
					if (cell.isReplaceable(world, tx, ty, tz)) {
						found = true;
					}
					break;
				}
				tx += dir.offsetX;
				ty += dir.offsetY;
				tz += dir.offsetZ;
				if (dir.offsetY == 0) {
					++horizontalSteps;
				}
			}
			if (!found) {
				return false;
			}
		} else {
			if (clicked.isReplaceable(world, x, y, z)) {
				tx = x;
				ty = y;
				tz = z;
			} else {
				ForgeDirection dir = ForgeDirection.getOrientation(side);
				tx = x + dir.offsetX;
				ty = y + dir.offsetY;
				tz = z + dir.offsetZ;
			}
			// 26.2 updatePlacementContext: refuse cells that would be fully unsupported
			if (ty < 0 || ty > 255 || BlockScaffolding.getDistance(world, tx, ty, tz) == 7) {
				return false;
			}
		}

		if (!player.canPlayerEdit(tx, ty, tz, side, stack)) {
			return false;
		}
		int meta = placed.onBlockPlaced(world, tx, ty, tz, side, hitX, hitY, hitZ, 0);
		if (!world.setBlock(tx, ty, tz, placed, meta, 3)) {
			return false;
		}
		if (world.getBlock(tx, ty, tz) == placed) {
			placed.onBlockPlacedBy(world, tx, ty, tz, player, stack);
			world.playSoundEffect(tx + 0.5D, ty + 0.5D, tz + 0.5D, placed.stepSound.func_150496_b(),
					(placed.stepSound.getVolume() + 1.0F) / 2.0F, placed.stepSound.getPitch() * 0.8F);
		}
		if (!world.isRemote && !player.capabilities.isCreativeMode) {
			stack.stackSize--;
		}
		return true;
	}

	/**
	 * 26.2 context.getHorizontalDirection(): 1.7.10 yaw, 0 = south (+Z).
	 */
	private static ForgeDirection getHorizontalLookDirection(EntityPlayer player) {
		int facing = MathHelper.floor_double(player.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
		switch (facing) {
			case 0:
				return ForgeDirection.SOUTH;
			case 1:
				return ForgeDirection.WEST;
			case 2:
				return ForgeDirection.NORTH;
			default:
				return ForgeDirection.EAST;
		}
	}
}
