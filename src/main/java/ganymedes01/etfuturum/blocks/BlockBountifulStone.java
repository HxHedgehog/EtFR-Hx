package ganymedes01.etfuturum.blocks;
import ganymedes01.etfuturum.creative.ModdedCreativeTabs;

import ganymedes01.etfuturum.EtFuturum;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

/**
 * 1.8+ 的石头变体（花岗岩、闪长岩、安山岩及其磨制版本）。
 * 拆分为独立方块后，每个变体用一个 BlockBountifulStone 实例，
 * 注册名为官方名称（granite, polished_granite 等）。
 */
public class BlockBountifulStone extends BaseBlock {

	public BlockBountifulStone(String name) {
		super(Material.rock);
		setNames(name);
		setHardness(1.5F);
		setResistance(6.0F);
		setCreativeTab(ModdedCreativeTabs.BUILDING_BLOCKS);
	}

	@Override
	public boolean isReplaceableOreGen(World world, int x, int y, int z, Block target) {
		return this == target || target == Blocks.stone;
	}
}
