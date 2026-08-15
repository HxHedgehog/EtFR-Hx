package ganymedes01.etfuturum.blocks;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.client.sound.ModSounds;
import ganymedes01.etfuturum.core.utils.Utils;
import ganymedes01.etfuturum.creative.ModdedCreativeTabs;
import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Random;

/**
 * 缠根泥土 —— 复刻官方 1.17+ RootedDirtBlock。
 * 骨粉时在下方生成 {@link BlockHangingRoots}（垂根）。
 */
public class BlockRootedDirt extends Block implements IGrowable {

	public BlockRootedDirt() {
		super(Material.ground);
		setHardness(0.5F);
		setResistance(0.5F);
		setHarvestLevel("shovel", 0);
		Utils.setBlockSound(this, ModSounds.soundRootedDirt);
		setBlockTextureName("rooted_dirt");
		setBlockName(Utils.getUnlocalisedName("rooted_dirt"));
		setCreativeTab(ModdedCreativeTabs.BUILDING_BLOCKS);
	}

	/**
	 * MCP: canFertilize —— 官方 isValidBonemealTarget：下方是空气才允许使用骨粉
	 */
	@Override
	public boolean func_149851_a(World world, int x, int y, int z, boolean isClient) {
		return y > 0 && world.isAirBlock(x, y - 1, z);
	}

	/**
	 * MCP: shouldFertilize —— 官方 isBonemealSuccess
	 */
	@Override
	public boolean func_149852_a(World world, Random rand, int x, int y, int z) {
		return true;
	}

	/**
	 * MCP: fertilize —— 官方 performBonemeal：下方放置垂根
	 */
	@Override
	public void func_149853_b(World world, Random rand, int x, int y, int z) {
		if (y > 0 && world.isAirBlock(x, y - 1, z)) {
			world.setBlock(x, y - 1, z, ModBlocks.HANGING_ROOTS.get(), 0, 2);
		}
	}

	@Override
	public boolean canSustainPlant(IBlockAccess world, int x, int y, int z, ForgeDirection direction, IPlantable plant) {
		return Blocks.dirt.canSustainPlant(world, x, y, z, direction, plant);
	}
}
