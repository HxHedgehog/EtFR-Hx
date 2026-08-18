package ganymedes01.etfuturum.world.generate.decorate;

import ganymedes01.etfuturum.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 杜鹃树 —— 精确复刻官方 1.17+ TreeFeatures.AZALEA_TREE（反编译自 26.2 官方映射源码）。
 *
 * <p>官方配置（{@code TreeFeatures.java} L224）：
 * <pre>
 *   trunk:          oak_log
 *   trunk placer:   BendingTrunkPlacer(baseHeight=4, heightRandA=2, heightRandB=0, minHeightForLeaves=3, bendLength=UniformInt(1,2))
 *   foliage:        WeightedStateProvider(azalea_leaves:3, flowering_azalea_leaves:1)  // 25% 开花
 *   foliage placer: RandomSpreadFoliagePlacer(radius=3, offset=0, foliageHeight=2, leafPlacementAttempts=50)
 *   feature size:   TwoLayersFeatureSize(limit=1, lowerSize=0, upperSize=1)
 *   below trunk:    rooted_dirt
 * </pre>
 */
public class WorldGenAzaleaTree extends WorldGenAbstractTree {

	// BendingTrunkPlacer(4, 2, 0, 3, UniformInt.of(1, 2))
	private static final int BASE_HEIGHT = 4;
	private static final int HEIGHT_RAND_A = 2;
	private static final int HEIGHT_RAND_B = 0;
	private static final int MIN_HEIGHT_FOR_LEAVES = 3;

	// RandomSpreadFoliagePlacer(ConstantInt.of(3), ConstantInt.of(0), ConstantInt.of(2), 50)
	private static final int FOLIAGE_RADIUS = 3;
	private static final int FOLIAGE_HEIGHT = 2;
	private static final int LEAF_PLACEMENT_ATTEMPTS = 50;

	// TwoLayersFeatureSize(limit=1, lowerSize=0, upperSize=1)
	private static final int FEATURE_SIZE_LIMIT = 1;
	private static final int FEATURE_SIZE_LOWER = 0;
	private static final int FEATURE_SIZE_UPPER = 1;

	public WorldGenAzaleaTree(boolean doNotify) {
		super(doNotify);
	}

	@Override
	public boolean generate(World world, Random rand, int x, int y, int z) {
		// 官方 TrunkPlacer.getTreeHeight = baseHeight + nextInt(heightRandA+1) + nextInt(heightRandB+1)
		int treeHeight = BASE_HEIGHT + rand.nextInt(HEIGHT_RAND_A + 1) + rand.nextInt(HEIGHT_RAND_B + 1);

		// 官方 doPlace 边界检查
		if (y < 1 || y + treeHeight + 1 > world.getHeight()) {
			return false;
		}

		// 官方 getMaxFreeTreeHeight
		int clippedTreeHeight = getMaxFreeTreeHeight(world, treeHeight, x, y, z);
		// 官方：minClippedHeight 为空 → 高度被裁剪则失败
		if (clippedTreeHeight < treeHeight) {
			return false;
		}

		// 官方 BendingTrunkPlacer.placeTrunk（先放树干，收集树冠挂点）
		List<int[]> foliagePoints = new ArrayList<int[]>();
		placeTrunk(world, rand, x, y, z, treeHeight, foliagePoints);

		// 官方：树干放置完毕后，对每个 attachment 调用 createFoliage
		for (int[] point : foliagePoints) {
			createFoliage(world, rand, point[0], point[1], point[2]);
		}

		return true;
	}

	private int getMaxFreeTreeHeight(World world, int maxTreeHeight, int x, int y, int z) {
		for (int dy = 0; dy <= maxTreeHeight + 1; dy++) {
			int r = dy < FEATURE_SIZE_LIMIT ? FEATURE_SIZE_LOWER : FEATURE_SIZE_UPPER;
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					int px = x + dx, py = y + dy, pz = z + dz;
					if (isFree(world, px, py, pz) && world.getBlock(px, py, pz) != Blocks.vine) {
						continue; // ignoreVines = false，藤蔓算阻挡
					}
					return dy - 2;
				}
			}
		}
		return maxTreeHeight;
	}

	private void placeTrunk(World world, Random rand, int x, int y, int z, int treeHeight, List<int[]> foliagePoints) {
		// 官方：随机水平弯曲方向
		int bendX = 0, bendZ = 0;
		switch (rand.nextInt(4)) {
			case 0: bendZ = -1; break;
			case 1: bendZ = 1; break;
			case 2: bendX = -1; break;
			default: bendX = 1; break;
		}

		// 官方 placeBelowTrunkBlock：belowTrunkProvider = rooted_dirt（无条件放置）
		setBlockAndNotifyAdequately(world, x, y - 1, z, ModBlocks.ROOTED_DIRT.get(), 0);

		int logHeight = treeHeight - 1;
		int px = x, py = y, pz = z;
		for (int i = 0; i <= logHeight; i++) {
			// 官方弯曲：i + 1 >= logHeight + nextInt(2)
			if (i + 1 >= logHeight + rand.nextInt(2)) {
				px += bendX;
				pz += bendZ;
			}
			if (validTreePos(world, px, py, pz)) {
				placeLog(world, px, py, pz);
			}
			if (i >= MIN_HEIGHT_FOR_LEAVES) {
				foliagePoints.add(new int[]{px, py, pz});
			}
			py++;
		}

		// 官方弯曲延伸：bendLength = UniformInt(1,2)
		int bendLength = 1 + rand.nextInt(2);
		for (int i = 0; i <= bendLength; i++) {
			if (validTreePos(world, px, py, pz)) {
				placeLog(world, px, py, pz);
			}
			foliagePoints.add(new int[]{px, py, pz});
			px += bendX;
			pz += bendZ;
		}
	}

	private void createFoliage(World world, Random rand, int x, int y, int z) {
		for (int i = 0; i < LEAF_PLACEMENT_ATTEMPTS; i++) {
			int dx = rand.nextInt(FOLIAGE_RADIUS) - rand.nextInt(FOLIAGE_RADIUS);
			int dy = rand.nextInt(FOLIAGE_HEIGHT) - rand.nextInt(FOLIAGE_HEIGHT);
			int dz = rand.nextInt(FOLIAGE_RADIUS) - rand.nextInt(FOLIAGE_RADIUS);
			tryPlaceLeaf(world, rand, x + dx, y + dy, z + dz);
		}
	}

	private void tryPlaceLeaf(World world, Random rand, int x, int y, int z) {
		if (!validTreePos(world, x, y, z)) {
			return;
		}
		// 官方 WeightedStateProvider(azalea_leaves:3, flowering_azalea_leaves:1) → 25% 开花
		int meta = rand.nextInt(4) == 0 ? 1 : 0;
		setBlockAndNotifyAdequately(world, x, y, z, ModBlocks.AZALEA_LEAVES.get(), meta);
	}

	private void placeLog(World world, int x, int y, int z) {
		setBlockAndNotifyAdequately(world, x, y, z, Blocks.log, 0);
	}

	// 官方 validTreePos = air || REPLACEABLE_BY_TREES（含 leaves，不含 dirt/log/stone）
	private boolean validTreePos(World world, int x, int y, int z) {
		Block block = world.getBlock(x, y, z);
		return block.isAir(world, x, y, z) || isReplaceableByTrees(world, block, x, y, z);
	}

	private boolean isReplaceableByTrees(World world, Block block, int x, int y, int z) {
		if (block.isLeaves(world, x, y, z)) {
			return true;
		}
		return block == Blocks.tallgrass
				|| block == Blocks.deadbush
				|| block == Blocks.vine
				|| block == Blocks.sapling
				|| block == Blocks.yellow_flower
				|| block == Blocks.red_flower
				|| block == Blocks.double_plant
				|| block == Blocks.water
				|| block == Blocks.flowing_water
				|| (ModBlocks.HANGING_ROOTS.isEnabled() && block == ModBlocks.HANGING_ROOTS.get());
	}

	// 官方 isFree = validTreePos || LOGS
	private boolean isFree(World world, int x, int y, int z) {
		return validTreePos(world, x, y, z) || world.getBlock(x, y, z).isWood(world, x, y, z);
	}
}