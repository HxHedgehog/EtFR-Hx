package ganymedes01.etfuturum.blocks;
import ganymedes01.etfuturum.creative.ModdedCreativeTabs;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.client.sound.ModSounds;
import ganymedes01.etfuturum.core.utils.Utils;
import ganymedes01.etfuturum.lib.RenderIDs;
import ganymedes01.etfuturum.world.generate.decorate.WorldGenAzaleaTree;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.EnumPlantType;

import java.util.List;
import java.util.Random;

public class BlockAzalea extends BlockBush implements ISubBlocksBlock, IGrowable {

	public IIcon[] sideIcons;
	public IIcon[] topIcons;
	public int meta;

	private final String[] types = new String[]{"azalea", "flowering_azalea"};

	public BlockAzalea() {
		super(Material.wood);
		setHardness(0.0F);
		setResistance(0.0F);
		Utils.setBlockSound(this, ModSounds.soundAzalea);
		setBlockName(Utils.getUnlocalisedName("azalea"));
		setBlockTextureName("azalea");
		setCreativeTab(ModdedCreativeTabs.BUILDING_BLOCKS);
		setBlockBounds(0, 0, 0, 1, 1, 1);
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public EnumPlantType getPlantType(IBlockAccess world, int x, int y, int z) {
		return EnumPlantType.Plains;
	}

	@Override
	public boolean canBlockStay(World world, int x, int y, int z) {
		Block below = world.getBlock(x, y - 1, z);
		return below.getMaterial() == Material.clay || below == ModBlocks.MOSS_BLOCK.get() || super.canBlockStay(world, x, y, z);
	}

	@Override
	public void addCollisionBoxesToList(World worldIn, int x, int y, int z, AxisAlignedBB mask, List<AxisAlignedBB> list, Entity collider) {
		setBlockBounds(0.0F, 0.5F, 0.0F, 1.0F, 1.0F, 1.0F);
		super.addCollisionBoxesToList(worldIn, x, y, z, mask, list, collider);
		setBlockBounds(0.4375F, 0.5F, 0.4375F, 0.5625F, 1.0F, 0.5625F);
		super.addCollisionBoxesToList(worldIn, x, y, z, mask, list, collider);

		this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
	}

	@Override
	public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z) {
		return AxisAlignedBB.getBoundingBox(x + 0.0F, y + 0.5F, z + 0.0F, x + 1.0F, y + 1.0F, z + 1.0F);
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
		return AxisAlignedBB.getBoundingBox(x + 0.0F, y + 0.5F, z + 0.0F, x + 1.0F, y + 1.0F, z + 1.0F);
	}

	@Override
	public void getSubBlocks(Item item, CreativeTabs tab, List<ItemStack> list) {
		for (int i = 0; i < getTypes().length; i++) {
			list.add(new ItemStack(item, 1, i));
		}
	}

	@Override
	public boolean isReplaceable(IBlockAccess world, int x, int y, int z) {
		return false;
	}

	@Override
	public int getRenderType() {
		return RenderIDs.AZALEA;
	}

	@Override
	public boolean shouldSideBeRendered(IBlockAccess worldIn, int x, int y, int z, int side) {
		return side != 0 && super.shouldSideBeRendered(worldIn, x, y, z, side);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister reg) {
		this.blockIcon = reg.registerIcon(this.getTextureName() + "_plant");

		sideIcons = new IIcon[2];
		topIcons = new IIcon[2];
		sideIcons[0] = reg.registerIcon(this.getTextureName() + "_side");
		sideIcons[1] = reg.registerIcon("flowering_" + this.getTextureName() + "_side");
		topIcons[0] = reg.registerIcon(this.getTextureName() + "_top");
		topIcons[1] = reg.registerIcon("flowering_" + this.getTextureName() + "_top");
	}

	@Override
	public int damageDropped(int meta) {
		return meta % getTypes().length;
	}

	@Override
	public IIcon[] getIcons() {
		return sideIcons;
	}

	@Override
	public IIcon getIcon(int side, int meta) {
		if (side == 0) {
			return this.blockIcon;
		}
		if (side == 1) {
			return topIcons[meta % topIcons.length];
		}
		return sideIcons[meta % topIcons.length];
	}

	@Override
	public String[] getTypes() {
		return types;
	}

	@Override
	public String getNameFor(ItemStack stack) {
		return getTypes()[stack.getItemDamage() % types.length];
	}

	@Override
	public MapColor getMapColor(int meta) {
		return MapColor.grassColor;
	}

	// === IGrowable：骨粉催熟成杜鹃树（复刻官方 AzaleaBlock 的 BonemealableBlock） ===

	@Override
	public boolean func_149851_a(World world, int x, int y, int z, boolean isClient) {
		// 官方 isValidBonemealTarget：仅服务端；上方有足够生长空间且流体为空
		if (isClient) {
			return false;
		}
		// 官方 minHeight = TreeGrower.AZALEA.getMinimumHeight() = trunkPlacer.baseHeight = 4
		// → pos.above(minHeight + 2) = pos.above(6) 需在世界高度内
		if (y + 6 >= world.getHeight()) {
			return false;
		}
		Block above = world.getBlock(x, y + 1, z);
		return above.getMaterial() != Material.water && above.getMaterial() != Material.lava;
	}

	@Override
	public boolean func_149852_a(World world, Random rand, int x, int y, int z) {
		// 官方 isBonemealSuccess：45% 概率
		return (double) rand.nextFloat() < 0.45D;
	}

	@Override
	public void func_149853_b(World world, Random rand, int x, int y, int z) {
		// 官方 performBonemeal：生长为杜鹃树（AzaleaTreeGrower.growTree）
		int meta = world.getBlockMetadata(x, y, z);
		world.setBlockToAir(x, y, z);
		if (!new WorldGenAzaleaTree(true).generate(world, rand, x, y, z)) {
			world.setBlock(x, y, z, this, meta, 3); // 空间不足则恢复灌木
		}
	}
}