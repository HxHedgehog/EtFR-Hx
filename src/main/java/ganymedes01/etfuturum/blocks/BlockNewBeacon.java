package ganymedes01.etfuturum.blocks;
import ganymedes01.etfuturum.creative.ModdedCreativeTabs;

import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.configuration.configs.ConfigWorld;
import ganymedes01.etfuturum.tileentities.TileEntityNewBeacon;
import net.minecraft.block.BlockBeacon;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.util.Random;

public class BlockNewBeacon extends BlockBeacon {

	public BlockNewBeacon() {
		setLightLevel(1.0F);
		setBlockTextureName("beacon");
		setBlockName("beacon");
		setCreativeTab(ConfigWorld.tileReplacementMode == -1 ? ModdedCreativeTabs.BUILDING_BLOCKS : null);
	}

	@Override
	public Item getItemDropped(int meta, Random rand, int fortune) {
		if (ConfigWorld.tileReplacementMode == -1)
			return Item.getItemFromBlock(ModBlocks.BEACON.get());
		return Item.getItemFromBlock(Blocks.beacon);
	}

	@Override
	public Item getItem(World world, int x, int y, int z) {
		if (ConfigWorld.tileReplacementMode == -1)
			return Item.getItemFromBlock(ModBlocks.BEACON.get());
		return Item.getItemFromBlock(Blocks.beacon);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityNewBeacon();
	}
}