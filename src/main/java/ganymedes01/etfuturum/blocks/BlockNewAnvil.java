package ganymedes01.etfuturum.blocks;
import ganymedes01.etfuturum.creative.ModdedCreativeTabs;

import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.configuration.configs.ConfigWorld;
import ganymedes01.etfuturum.lib.GUIIDs;
import net.minecraft.block.BlockAnvil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.world.World;

import java.util.Random;

public class BlockNewAnvil extends BlockAnvil {

	public BlockNewAnvil() {
		setHardness(5.0F);
		setResistance(2000.0F);
		setStepSound(soundTypeAnvil);
		setBlockName("anvil");
		setCreativeTab(ConfigWorld.tileReplacementMode == -1 ? ModdedCreativeTabs.BUILDING_BLOCKS : null);
	}

	@Override
	public Item getItemDropped(int meta, Random rand, int fortune) {
		if (ConfigWorld.tileReplacementMode == -1)
			return Item.getItemFromBlock(ModBlocks.ANVIL.get());
		return Item.getItemFromBlock(Blocks.anvil);
	}

	@Override
	public Item getItem(World world, int x, int y, int z) {
		if (ConfigWorld.tileReplacementMode == -1)
			return Item.getItemFromBlock(ModBlocks.ANVIL.get());
		return Item.getItemFromBlock(Blocks.anvil);
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		if (world.isRemote)
			return true;
		player.openGui(EtFuturum.instance, GUIIDs.ANVIL, world, x, y, z);
		return true;
	}

}
