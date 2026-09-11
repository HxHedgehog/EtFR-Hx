package ganymedes01.etfuturum.blocks.itemblocks;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.blocks.BlockAquaticPlant;
import ganymedes01.etfuturum.blocks.BlockSeaPickle;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * Places wet pickles when clicked into a water block on solid ground,
 * dry pickles when clicked on land, and adds to an existing pickle colony
 * when clicking one that is not yet full.
 */
public class ItemBlockSeaPickle extends ItemBlock {

	public ItemBlockSeaPickle(Block block) {
		super(block);
	}

	@Override
	public int getMetadata(int meta) {
		return 0;
	}

	/**
	 * Forge's client-side placement precheck: the default ItemBlock impl delegates to
	 * World.canPlaceEntityOnSide -> Block.canPlaceBlockAt, which for the wet pickle block
	 * requires the target to be water. That check runs BEFORE onItemUse in
	 * PlayerControllerMP, so right-clicking dry ground never reached onItemUse at all.
	 * Our placement rules (wet pickles into water, dry pickles onto land) live entirely
	 * in onItemUse, so let the precheck pass unconditionally.
	 */
	@Override
	public boolean func_150936_a(World world, int x, int y, int z, int side, EntityPlayer player, ItemStack stack) {
		return true;
	}

	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
		Block clicked = world.getBlock(x, y, z);

		// 26.2 canBeReplaced: clicking a cluster adds a pickle unless sneaking or the
		// cluster is already full - those fall through to placing behind the face
		// (on top of or beside the existing cluster)
		if (clicked instanceof BlockSeaPickle && !player.isSneaking()
				&& BlockSeaPickle.getPickleCount(world.getBlockMetadata(x, y, z)) < 4) {
			if (!player.canPlayerEdit(x, y, z, side, stack)) {
				return false;
			}
			return BlockSeaPickle.addToCluster(world, player, stack, x, y, z, world.getBlockMetadata(x, y, z), clicked);
		}

		ForgeDirection dir = ForgeDirection.getOrientation(side);
		int tx = x;
		int ty = y;
		int tz = z;
		if (!clicked.isReplaceable(world, x, y, z)) {
			tx += dir.offsetX;
			ty += dir.offsetY;
			tz += dir.offsetZ;
		}

		Block target = world.getBlock(tx, ty, tz);
		boolean wetTarget = BlockAquaticPlant.isLiquidWater(target);
		boolean dryTarget = target.isAir(world, tx, ty, tz);
		if (!wetTarget && !dryTarget) {
			return false;
		}
		// 26.2 mayPlaceOn: UP collision face of the cell below (leaves and glass
		// qualify; isSideSolid would demand opacity like vanilla isNormalCube)
		if (!BlockSeaPickle.mayPlaceOn(world, tx, ty - 1, tz)) {
			return false;
		}
		if (!player.canPlayerEdit(tx, ty, tz, side, stack)) {
			return false;
		}

		Block placed = wetTarget ? ModBlocks.SEA_PICKLE.get() : ModBlocks.DEAD_SEA_PICKLE.get();
		if (!world.setBlock(tx, ty, tz, placed, 0, 3)) {
			return false;
		}
		if (world.getBlock(tx, ty, tz) == placed) {
			placed.onBlockPlacedBy(world, tx, ty, tz, player, stack);
			playPlaceSound(world, placed, tx, ty, tz);
		}
		if (!world.isRemote && !player.capabilities.isCreativeMode) {
			stack.stackSize--;
		}
		return true;
	}

	private void playPlaceSound(World world, Block block, int x, int y, int z) {
		world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D,
				block.stepSound.func_150496_b(), (block.stepSound.getVolume() + 1.0F) / 2.0F, block.stepSound.getPitch() * 0.8F);
	}
}
