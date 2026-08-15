package ganymedes01.etfuturum.blocks;

import ganymedes01.etfuturum.creative.ModdedCreativeTabs;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.client.sound.ModSounds;
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
 * 苔藓块 —— 复刻官方 1.17+ MossBlock（BonemealableFeaturePlacerBlock + moss_patch_bonemeal）。
 * <p>
 * 骨粉（IGrowable，vegetation_patch）：
 * - xz_radius：UniformInt(1,2) 采样后 + 1 → 2~3，方形区域逐柱判定：四角 0%、边缘 75%（extraEdgeColumnChance）、内部 100%；
 * - 从苔藓块上方一格开始两段式扫描（vertical_range = 5）：
 *   ① 向下穿过空气；② 向上穿过实心方块，最终停在"最上方实心块之上的一格空气"；
 * - 该空气格下方须为完整方块（isFaceSturdy 向上），且方块为 moss_replaceable（或已是苔藓块）→ 替换为苔藓块；
 * - 每个 surface 列按 vegetationChance(60%) 在苔藓块上方生成植被；
 * - 植被权重（moss_vegetation）：开花杜鹃 4 / 杜鹃 7 / 苔藓地毯 25 / 草丛 50 / 高草 10。
 * 官方苔藓块无自然蔓延，只能靠骨粉。
 */
public class BlockMoss extends BaseBlock implements IGrowable {

	public BlockMoss() {
		super(Material.grass);
		setHardness(0.1F);
		setResistance(0.1F);
		setNames("moss_block");
		setHarvestLevel("hoe", 0);
		setBlockSound(ModSounds.soundMoss);
		setCreativeTab(ModdedCreativeTabs.BUILDING_BLOCKS);
	}

	/**
	 * MCP: canFertilize —— 官方 isFertilizable：上方是空气才允许使用骨粉
	 */
	@Override
	public boolean func_149851_a(World world, int x, int y, int z, boolean isClient) {
		return world.isAirBlock(x, y + 1, z);
	}

	/**
	 * MCP: shouldFertilize —— 官方 canGrow
	 */
	@Override
	public boolean func_149852_a(World world, Random rand, int x, int y, int z) {
		return true;
	}

	/**
	 * MCP: fertilize —— 官方 grow：生成植被补丁（moss_patch_bonemeal）
	 */
	@Override
	public void func_149853_b(World world, Random rand, int x, int y, int z) {
		// 官方 place()：xz_radius = UniformInt(1,2).sample() + 1 → 2~3
		int xRadius = 2 + rand.nextInt(2);
		int zRadius = 2 + rand.nextInt(2);
		for (int dx = -xRadius; dx <= xRadius; dx++) {
			for (int dz = -zRadius; dz <= zRadius; dz++) {
				// 官方 placeGroundPatch：四角跳过；边缘列仅按 extraEdgeColumnChance(0.75) 通过；内部 100%
				boolean isXEdge = dx == -xRadius || dx == xRadius;
				boolean isZEdge = dz == -zRadius || dz == zRadius;
				if (isXEdge && isZEdge) continue;
				if ((isXEdge || isZEdge) && rand.nextFloat() > 0.75F) continue;

				// 官方两段式扫描（vertical_range = 5），从苔藓块上方一格开始
				int posY = y + 1;
				int moves = 0;
				// ① 向下穿过空气
				while (moves < 5 && world.isAirBlock(x + dx, posY, z + dz)) {
					posY--;
					moves++;
				}
				// ② 向上穿过实心方块
				moves = 0;
				while (moves < 5 && !world.isAirBlock(x + dx, posY, z + dz)) {
					posY++;
					moves++;
				}
				// 最终须停在空气格，且其下方为完整方块（isFaceSturdy 向上的近似）
				if (!world.isAirBlock(x + dx, posY, z + dz)) continue;
				int groundY = posY - 1;
				if (groundY < 0 || !world.getBlock(x + dx, groundY, z + dz).isOpaqueCube()) continue;

				// 官方 placeGround（depth = 1，extraBottomBlockChance = 0）：仅替换 moss_replaceable，已是苔藓则跳过
				Block below = world.getBlock(x + dx, groundY, z + dz);
				if (below == ModBlocks.MOSS_BLOCK.get()) {
					// 已是苔藓块：仍作为 surface（官方同样加入 surface 集合）
				} else if (isMossReplaceable(world, x + dx, groundY, z + dz)) {
					world.setBlock(x + dx, groundY, z + dz, ModBlocks.MOSS_BLOCK.get(), 0, 2);
				} else {
					continue;
				}
				// 官方 distributeVegetation：vegetationChance(0.6) 在苔藓块上方生成植被
				if (rand.nextFloat() < 0.6F) {
					placeVegetation(world, rand, x + dx, posY, z + dz);
				}
			}
		}
	}

	/**
	 * 官方 moss_replaceable 标签（1.17+）：stone, deepslate, granite, diorite, andesite,
	 * tuff, basalt, blackstone, dirt, coarse_dirt, grass_block, podzol, rooted_dirt。
	 * 1.7.10 中 dirt 的 meta 0/1/2 分别对应 dirt/coarse_dirt/podzol。
	 */
	private boolean isMossReplaceable(World world, int x, int y, int z) {
		if (y < 0 || y >= world.getHeight()) return false;
		Block block = world.getBlock(x, y, z);
		if (block == Blocks.stone) return true;
		if (block == Blocks.grass) return true;
		if (block == Blocks.dirt) {
			int meta = world.getBlockMetadata(x, y, z);
			return meta == 0 || meta == 1 || meta == 2;
		}
		if (block == ModBlocks.ROOTED_DIRT.get()) return true;
		if (block == ModBlocks.GRANITE.get() || block == ModBlocks.DIORITE.get() || block == ModBlocks.ANDESITE.get()) return true;
		if (block == ModBlocks.DEEPSLATE.get() || block == ModBlocks.TUFF.get()) return true;
		if (block == ModBlocks.BASALT.get() || block == ModBlocks.BLACKSTONE.get()) return true;
		return false;
	}

	/**
	 * 在苔藓块上方按官方 moss_vegetation 权重生成植被。
	 */
	private void placeVegetation(World world, Random rand, int x, int y, int z) {
		if (!world.isAirBlock(x, y, z)) return;
		int roll = rand.nextInt(96);
		if (roll < 4) {
			// flowering_azalea（mod AZALEA meta 1）
			if (ModBlocks.AZALEA.get() != null && ModBlocks.AZALEA.get().canBlockStay(world, x, y, z)) {
				world.setBlock(x, y, z, ModBlocks.AZALEA.get(), 1, 2);
			}
		} else if (roll < 11) {
			// azalea（mod AZALEA meta 0）
			if (ModBlocks.AZALEA.get() != null && ModBlocks.AZALEA.get().canBlockStay(world, x, y, z)) {
				world.setBlock(x, y, z, ModBlocks.AZALEA.get(), 0, 2);
			}
		} else if (roll < 36) {
			// moss_carpet
			if (ModBlocks.MOSS_CARPET.get() != null && ModBlocks.MOSS_CARPET.get().canBlockStay(world, x, y, z)) {
				world.setBlock(x, y, z, ModBlocks.MOSS_CARPET.get(), 0, 2);
			}
		} else if (roll < 86) {
			// 草丛（1.7.10 tallgrass meta 1）
			if (Blocks.tallgrass.canBlockStay(world, x, y, z)) {
				world.setBlock(x, y, z, Blocks.tallgrass, 1, 2);
			}
		} else {
			// 高草（1.7.10 double_plant meta 2，两格高）
			if (Blocks.double_plant.canBlockStay(world, x, y, z)) {
				Blocks.double_plant.func_149889_c(world, x, y, z, 2, 2);
			}
		}
	}

	@Override
	public boolean canSustainPlant(IBlockAccess world, int x, int y, int z, ForgeDirection direction, IPlantable plant) {
		return Blocks.dirt.canSustainPlant(world, x, y, z, direction, plant);
	}
}
