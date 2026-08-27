package ganymedes01.etfuturum.blocks;

import ganymedes01.etfuturum.client.sound.ModSounds;
import ganymedes01.etfuturum.core.utils.Utils;
import ganymedes01.etfuturum.creative.ModdedCreativeTabs;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.ItemShears;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Random;

/**
 * 垂根 —— 复刻官方 1.17+ HangingRootsBlock。
 * 从方块底部垂下，无碰撞箱，上方方块被破坏时掉落。
 */
public class BlockHangingRoots extends Block {

	public BlockHangingRoots() {
		super(Material.plants);
		setHardness(0.0F);
		Utils.setBlockSound(this, ModSounds.soundHangingRoots);
		setBlockName(Utils.getUnlocalisedName("hanging_roots"));
		setBlockTextureName("hanging_roots");
		setCreativeTab(ModdedCreativeTabs.BUILDING_BLOCKS);
		// 官方 SHAPE = Block.column(12.0, 10.0, 16.0)：x/z 2~14，y 10~16
		setBlockBounds(0.125F, 0.625F, 0.125F, 0.875F, 1.0F, 0.875F);
	}

	@Override
	public boolean canPlaceBlockAt(World world, int x, int y, int z) {
		return world.getBlock(x, y + 1, z).isSideSolid(world, x, y + 1, z, ForgeDirection.DOWN);
	}

	@Override
	public boolean canBlockStay(World world, int x, int y, int z) {
		return canPlaceBlockAt(world, x, y, z);
	}

	@Override
	public int quantityDropped(int meta, int fortune, Random random) {
		// 官方 1.17+：空手/其他工具不掉落，仅剪刀采集时掉落自身。
		// 无玩家破坏（方块更新/自然脱落）时 harvesters 为 null，也不掉落。
		if (harvesters.get() == null || harvesters.get().getHeldItem() == null
				|| !(harvesters.get().getHeldItem().getItem() instanceof ItemShears)) {
			return 0;
		}
		// 耐久消耗统一由 MixinItemShears 处理（对齐 26.2：除火外每破坏一个方块扣 1），
		// 此处不再手动扣，避免双重消耗。
		return 1;
	}

	@Override
	public int quantityDropped(Random random) {
		// 无玩家破坏时默认不掉落（与 quantityDropped(meta, fortune, random) 保持一致）
		return 0;
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
		super.onNeighborBlockChange(world, x, y, z, neighbor);
		if (!canBlockStay(world, x, y, z)) {
			dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
			world.setBlockToAir(x, y, z);
		}
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World worldIn, int x, int y, int z) {
		return null;
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}

	@Override
	public int getRenderType() {
		return 1;
	}
}
