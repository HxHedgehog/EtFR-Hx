package ganymedes01.etfuturum.blocks;
import ganymedes01.etfuturum.creative.ModdedCreativeTabs;

import cpw.mods.fml.client.FMLClientHandler;
import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.client.particle.CustomParticles;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/**
 * 1.8+ 的树叶（红树树叶、樱花树叶）。
 * 拆分后每个树叶是独立方块，注册名为官方名称（mangrove_leaves, cherry_leaves）。
 */
public class BlockModernLeaves extends BaseLeaves {

	private final boolean isCherry;

	public BlockModernLeaves(String name) {
		super(name);
		this.isCherry = "cherry".equals(name);
		setCreativeTab(ModdedCreativeTabs.BUILDING_BLOCKS);
	}

	@Override
	public int getRange(int meta) {
		return isCherry ? 7 : 4;
	}

	@Override
	public int quantityDropped(int meta, int fortune, Random random) {
		if (!isCherry) {
			return 0;
		}
		return super.quantityDropped(meta, fortune, random);
	}

	@Override
	public Item getItemDropped(int meta, Random random, int fortune) {
		if (!isCherry) {
			return null;
		}
		return ModBlocks.CHERRY_SAPLING.getItem();
	}

	@Override
	public int colorMultiplier(IBlockAccess worldIn, int x, int y, int z) {
		return isCherry ? 0xFFFFFF : super.colorMultiplier(worldIn, x, y, z);
	}

	@Override
	public int getRenderColor(int meta) {
		return isCherry ? 0xFFFFFF : 0x92C648;
	}

	@Override
	public void randomDisplayTick(World world, int x, int y, int z, Random rand) {
		if (isCherry) {
			if (FMLClientHandler.instance().getClient().gameSettings.particleSetting == 0) {
				if (world.getBlock(x, y - 1, z).getMaterial() == Material.air && rand.nextInt(10) == 0) {
					CustomParticles.spawnCherryLeaf(world, x + rand.nextFloat(), y, z + rand.nextFloat());
				}
			}
			return;
		}
		super.randomDisplayTick(world, x, y, z, rand);
	}
}
