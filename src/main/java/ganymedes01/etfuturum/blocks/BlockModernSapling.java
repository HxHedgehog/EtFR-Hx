package ganymedes01.etfuturum.blocks;
import ganymedes01.etfuturum.creative.ModdedCreativeTabs;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.core.utils.Utils;
import ganymedes01.etfuturum.world.generate.decorate.WorldGenCherryTrees;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSapling;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraftforge.event.terraingen.TerrainGen;

import java.util.List;
import java.util.Random;

/**
 * 1.8+ 的树苗（红树胎生苗、樱花树苗）。
 * 拆分后每个树苗是独立方块，注册名为官方名称（mangrove_propagule, cherry_sapling）。
 */
public class BlockModernSapling extends BlockSapling {

	private final String name;
	private WorldGenAbstractTree treeGen;
	private IIcon icon;

	public BlockModernSapling(String name) {
		super();
		this.name = name;
		setStepSound(Block.soundTypeGrass);
		setBlockName(Utils.getUnlocalisedName(name));
		setBlockTextureName(name);
		setCreativeTab(ModdedCreativeTabs.BUILDING_BLOCKS);
	}

	private WorldGenAbstractTree getTreeGen() {
		if (treeGen == null) {
			if ("cherry_sapling".equals(name)) {
				treeGen = new WorldGenCherryTrees(true);
			}
		}
		return treeGen;
	}

	/**
	 * MCP name: {@code growTree}
	 */
	@Override
	public void func_149878_d(World p_149878_1_, int p_149878_2_, int p_149878_3_, int p_149878_4_, Random p_149878_5_) {
		if (!TerrainGen.saplingGrowTree(p_149878_1_, p_149878_5_, p_149878_2_, p_149878_3_, p_149878_4_)) {
			return;
		}

		WorldGenAbstractTree tree = getTreeGen();
		if (tree != null) {
			Block block = p_149878_1_.getBlock(p_149878_2_, p_149878_3_, p_149878_4_);
			int meta = p_149878_1_.getBlockMetadata(p_149878_2_, p_149878_3_, p_149878_4_);
			p_149878_1_.setBlock(p_149878_2_, p_149878_3_, p_149878_4_, Blocks.air);
			boolean success = tree.generate(p_149878_1_, p_149878_5_, p_149878_2_, p_149878_3_, p_149878_4_);
			if (!success) {
				p_149878_1_.setBlock(p_149878_2_, p_149878_3_, p_149878_4_, block, meta, 2);
			}
		}
	}

	@Override
	public IIcon getIcon(int side, int meta) {
		return icon;
	}

	/**
	 * 覆盖父类 BlockSapling.getSubBlocks 的硬编码 meta 0-5 行为。
	 * BlockSapling 为 6 种原版树苗硬编码了 6 个变体，
	 * 但本 mod 的每种树苗是独立方块，只需要 1 个条目。
	 */
	@Override
	public void getSubBlocks(Item item, CreativeTabs tab, List<ItemStack> list) {
		list.add(new ItemStack(item, 1, 0));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister reg) {
		icon = reg.registerIcon(name);
	}
}
