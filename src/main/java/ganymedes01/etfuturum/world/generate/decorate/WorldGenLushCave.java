package ganymedes01.etfuturum.world.generate.decorate;

import ganymedes01.etfuturum.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.feature.WorldGenerator;

import java.util.Random;

import static net.minecraft.world.EnumSkyBlock.Sky;

/**
 * 繁茂洞穴 —— 复刻官方 lush caves 的「地表标记 + 洞穴内植被」。
 *
 * <p>官方 1.17+ 中 lush caves 是地下群系，靠 ROOT_SYSTEM（杜鹃树根系统）在地表留标记、
 * 靠 MOSS_PATCH 在洞穴地板铺苔藓。1.7.10 没有洞穴群系，因此此处以「洞穴驱动」近似：
 * 从地表向下扫描真实洞穴，找到洞穴后：
 * <ol>
 *   <li>洞穴地板铺苔藓块补丁（复刻 CaveFeatures.MOSS_PATCH，xz_radius 4~7、extraEdgeColumnChance 0.3）；</li>
 *   <li>从洞穴顶部向上种杜鹃树，并沿「洞穴顶 → 树底」填缠根泥土根柱，
 *       再在洞穴顶部附近撒垂根（复刻 CaveFeatures.ROOTED_AZALEA_TREE / RootSystemFeature）。</li>
 * </ol>
 *
 * <p>根系统参数取自官方 {@code RootSystemConfiguration}（rootRadius=3、rootPlacementAttempts=20、
 * hangingRootRadius=3、hangingRootsVerticalSpan=2、hangingRootPlacementAttempts=20）。
 */
public class WorldGenLushCave extends WorldGenerator {

	private final WorldGenAzaleaTree azaleaTree = new WorldGenAzaleaTree(false);

	// 官方 RootSystemConfiguration.ROOTED_AZALEA_TREE 参数
	private static final int ROOT_RADIUS = 3;
	private static final int ROOT_PLACEMENT_ATTEMPTS = 20;
	private static final int HANGING_ROOT_RADIUS = 3;
	private static final int HANGING_ROOT_VERTICAL_SPAN = 2;
	private static final int HANGING_ROOT_PLACEMENT_ATTEMPTS = 20;

	// 官方 MOSS_PATCH（自然生成版）参数
	private static final float MOSS_EDGE_COLUMN_CHANCE = 0.3F;

	@Override
	public boolean generate(World world, Random rand, int x, int y, int z) {
		if (!isLushCaveBiome(world.getBiomeGenForCoords(x, z))) {
			return false;
		}

		// 从地表向下扫描洞穴顶部
		int caveTop = findCaveTop(world, x, y, z);
		if (caveTop < 0) {
			return false;
		}

		// 洞穴地板铺苔藓补丁
		placeMossPatch(world, rand, x, caveTop, z);

		// 洞穴驱动：杜鹃树 + 缠根泥土根柱 + 垂根
		generateRootedAzaleaTree(world, rand, x, caveTop, z);

		return true;
	}

	/**
	 * 官方 lush caves 只出现在特定群系；1.7.10 用「桦木森林 + 平原」近似。
	 */
	private boolean isLushCaveBiome(BiomeGenBase biome) {
		return biome == BiomeGenBase.plains
				|| biome == BiomeGenBase.birchForest
				|| biome == BiomeGenBase.birchForestHills;
	}

	/**
	 * 从地表往下扫描，返回洞穴顶部（第一个地下空气块，天空光照极低），找不到返回 -1。
	 */
	private int findCaveTop(World world, int x, int surfaceY, int z) {
		for (int cy = surfaceY - 1; cy >= 15; cy--) {
			if (world.isAirBlock(x, cy, z) && world.getSavedLightValue(Sky, x, cy, z) <= 3) {
				return cy;
			}
		}
		return -1;
	}

	/**
	 * 复刻官方 RootSystemFeature.placeDirtAndTree + placeDirt + placeRootedDirt：
	 * 在洞穴上方的地表种杜鹃树，然后沿「洞穴顶 → 树底」逐层撒缠根泥土，形成根柱。
	 */
	private void generateRootedAzaleaTree(World world, Random rand, int x, int caveTop, int z) {
		int surfaceY = world.getHeightValue(x, z);
		if (surfaceY <= caveTop + 1) {
			return; // 洞穴离地表太近，没有足够根柱空间
		}

		// 树底须能支撑杜鹃树（AZALEA_GROWS_ON 的 1.7.10 等价）
		Block ground = world.getBlock(x, surfaceY - 1, z);
		if (!canAzaleaGrowOn(ground)) {
			return;
		}

		// 种杜鹃树（官方 treeFeature.place）
		if (!azaleaTree.generate(world, rand, x, surfaceY, z)) {
			return;
		}

		// 根柱：从洞穴天花板到地表实心块，逐层撒缠根泥土（官方 placeDirt）
		for (int cy = caveTop + 1; cy < surfaceY; cy++) {
			placeRootedDirt(world, rand, x, z, cy);
		}

		// 垂根：在洞穴顶部附近撒（官方 placeRoots）
		placeHangingRoots(world, rand, x, caveTop, z);
	}

	/**
	 * 官方 placeRootedDirt：在 (originX, y, originZ) 这一层，rootRadius 内随机 rootPlacementAttempts 次，
	 * 把 rootReplaceable（AZALEA_ROOT_REPLACEABLE）替换为缠根泥土。
	 */
	private void placeRootedDirt(World world, Random rand, int originX, int originZ, int y) {
		for (int i = 0; i < ROOT_PLACEMENT_ATTEMPTS; i++) {
			int px = originX + rand.nextInt(ROOT_RADIUS) - rand.nextInt(ROOT_RADIUS);
			int pz = originZ + rand.nextInt(ROOT_RADIUS) - rand.nextInt(ROOT_RADIUS);
			if (isRootReplaceable(world, px, y, pz)) {
				world.setBlock(px, y, pz, ModBlocks.ROOTED_DIRT.get(), 0, 2);
			}
		}
	}

	/**
	 * 官方 placeRoots：在 origin 附近（hangingRootRadius × verticalSpan）随机 hangingRootPlacementAttempts 次，
	 * 在「空气 + 上方为实心面朝下」处放垂根。
	 */
	private void placeHangingRoots(World world, Random rand, int originX, int originY, int originZ) {
		for (int i = 0; i < HANGING_ROOT_PLACEMENT_ATTEMPTS; i++) {
			int px = originX + rand.nextInt(HANGING_ROOT_RADIUS) - rand.nextInt(HANGING_ROOT_RADIUS);
			int py = originY + rand.nextInt(HANGING_ROOT_VERTICAL_SPAN) - rand.nextInt(HANGING_ROOT_VERTICAL_SPAN);
			int pz = originZ + rand.nextInt(HANGING_ROOT_RADIUS) - rand.nextInt(HANGING_ROOT_RADIUS);
			if (world.isAirBlock(px, py, pz)
					&& ModBlocks.HANGING_ROOTS.get().canPlaceBlockAt(world, px, py, pz)) {
				world.setBlock(px, py, pz, ModBlocks.HANGING_ROOTS.get(), 0, 2);
			}
		}
	}

	/**
	 * 复刻官方 MOSS_PATCH（自然生成版，xz_radius 4~7、extraEdgeColumnChance 0.3）：
	 * 在洞穴地板上铺苔藓块补丁。
	 */
	private void placeMossPatch(World world, Random rand, int x, int caveTop, int z) {
		int radius = 4 + rand.nextInt(4); // 官方 xzRadius = UniformInt(4,7)
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				boolean isXEdge = dx == -radius || dx == radius;
				boolean isZEdge = dz == -radius || dz == radius;
				if (isXEdge && isZEdge) {
					continue; // 四角跳过
				}
				if ((isXEdge || isZEdge) && rand.nextFloat() > MOSS_EDGE_COLUMN_CHANCE) {
					continue;
				}

				int px = x + dx;
				int pz = z + dz;
				int floorY = findFloor(world, px, caveTop, pz);
				if (floorY > 0 && isMossReplaceable(world, px, floorY, pz)) {
					world.setBlock(px, floorY, pz, ModBlocks.MOSS_BLOCK.get(), 0, 2);
				}
			}
		}
	}

	/**
	 * 从洞穴顶部往下找地板（第一个非空气块）。
	 */
	private int findFloor(World world, int x, int fromY, int z) {
		for (int cy = fromY; cy >= 5; cy--) {
			if (!world.isAirBlock(x, cy, z)) {
				return cy;
			}
		}
		return -1;
	}

	/**
	 * 官方 AZALEA_ROOT_REPLACEABLE（BASE_STONE_OVERWORLD + SUBSTRATE_OVERWORLD + 沙/砾/陶瓦/雪）的 1.7.10 等价。
	 */
	private boolean isRootReplaceable(World world, int x, int y, int z) {
		if (y < 0 || y >= world.getHeight()) {
			return false;
		}
		Block block = world.getBlock(x, y, z);
		if (block == Blocks.stone || block == Blocks.grass || block == Blocks.sand
				|| block == Blocks.gravel || block == Blocks.clay) {
			return true;
		}
		if (block == Blocks.dirt) {
			int meta = world.getBlockMetadata(x, y, z);
			return meta == 0 || meta == 1 || meta == 2;
		}
		if (block == ModBlocks.ROOTED_DIRT.get() || block == ModBlocks.MOSS_BLOCK.get()) {
			return true;
		}
		return block == ModBlocks.GRANITE.get() || block == ModBlocks.DIORITE.get()
				|| block == ModBlocks.ANDESITE.get() || block == ModBlocks.DEEPSLATE.get()
				|| block == ModBlocks.TUFF.get();
	}

	/**
	 * 官方 MOSS_REPLACEABLE 的 1.7.10 等价（与 {@code BlockMoss} 保持一致）。
	 */
	private boolean isMossReplaceable(World world, int x, int y, int z) {
		if (y < 0 || y >= world.getHeight()) {
			return false;
		}
		Block block = world.getBlock(x, y, z);
		if (block == Blocks.stone || block == Blocks.grass || block == ModBlocks.ROOTED_DIRT.get()) {
			return true;
		}
		if (block == Blocks.dirt) {
			int meta = world.getBlockMetadata(x, y, z);
			return meta == 0 || meta == 1 || meta == 2;
		}
		return block == ModBlocks.GRANITE.get() || block == ModBlocks.DIORITE.get()
				|| block == ModBlocks.ANDESITE.get() || block == ModBlocks.DEEPSLATE.get()
				|| block == ModBlocks.TUFF.get() || block == ModBlocks.BASALT.get()
				|| block == ModBlocks.BLACKSTONE.get();
	}

	/**
	 * 官方 AZALEA_GROWS_ON 的 1.7.10 等价：杜鹃树底须为泥土系 / 苔藓 / 黏土。
	 */
	private boolean canAzaleaGrowOn(Block block) {
		return block == Blocks.grass || block == Blocks.dirt || block == Blocks.clay
				|| block == ModBlocks.MOSS_BLOCK.get() || block == ModBlocks.ROOTED_DIRT.get();
	}
}
