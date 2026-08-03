package ganymedes01.etfuturum.blocks;
import ganymedes01.etfuturum.creative.ModdedCreativeTabs;

import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.core.utils.Utils;
import net.minecraft.block.BlockButtonStone;
import net.minecraft.util.IIcon;

public class BlockPolishedBlackstoneButton extends BlockButtonStone {

	public BlockPolishedBlackstoneButton() {
		super();
		setHardness(0.5F);
		setResistance(0.5F);
		setBlockName(Utils.getUnlocalisedName("polished_blackstone_button"));
		setCreativeTab(ModdedCreativeTabs.BUILDING_BLOCKS);
	}

	/**
	 * Gets the block's texture. Args: side, meta
	 */
	@Override
	public IIcon getIcon(int side, int meta) {
		return ModBlocks.BLACKSTONE.get().getIcon(0, 1);
	}
}
