package ganymedes01.etfuturum.blocks;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.creative.ModdedCreativeTabs;

public class BlockNetheriteStairs extends BaseStairs {

	public BlockNetheriteStairs() {
		super(ModBlocks.NETHERITE_BLOCK.get(), 0);
		setUnlocalizedNameWithPrefix("netherite");
		this.setCreativeTab(ModdedCreativeTabs.TEMPORARY);
	}
}
