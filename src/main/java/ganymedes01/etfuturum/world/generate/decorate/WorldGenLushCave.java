package ganymedes01.etfuturum.world.generate.decorate;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.tileentities.TileEntityCaveVines;
import ganymedes01.etfuturum.world.generate.caves.noise.DoublePerlinNoiseSampler;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.feature.WorldGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

import static net.minecraft.world.EnumSkyBlock.Sky;

/**
 * 繁茂洞穴 —— 复刻 26.2 lush caves 的生成机制。
 *
 * <p>官方 26.2 中 lush caves 是地下生物群系：MultiNoiseBiomeSource 用气候噪声圈出繁茂区域，
 * MOSS_PATCH（地板苔藓 + MOSS_VEGETATION 植被）、LUSH_CAVES_CLAY（黏土水池）、
 * MOSS_PATCH_CEILING（顶板苔藓 + CAVE_VINE_IN_MOSS 洞穴藤蔓）在繁茂区域内按
 * EnvironmentScan 找到的实际洞穴地板/顶板放置；ROOTED_AZALEA_TREE 每区块 1~2 棵。
 *
 * <p>1.7.10 没有地下群系，此处用「腔室尺度的选择噪声」实现繁茂区域 —— 与官方气候噪声同思路：
 * <ul>
 *   <li><b>繁茂选择噪声</b>：世界种子派生的低频噪声（约 250 格波长，低于腔室尺度），
 *       噪声等值线圈出繁茂区 —— 频率低于腔室时，整个腔室天然整腔整腔地被圈进/圈出，
 *       等效「每个腔室掷一次繁茂骰子」且边界是有机曲线，不会按区块切出整齐直线；</li>
 *   <li><b>地表群系</b>：桦木森林/桦木丘陵下约一半区域繁茂（阈值 0 ≈ 面积各半），
 *       平原用更高阈值天然稀疏（模拟官方「地表 azalea 生物群系」关联）；</li>
 *   <li><b>落位</b>：每区块 2×2 象限锚点（8 格间距），噪声过阈值才装饰 ——
 *       锚点写入可越区块边界（populate 门控保证邻块地形已生成），越界写入无害。</li>
 * </ul>
 *
 * <p>选择噪声与装饰随机数均为种子派生的纯函数（装饰随机数按锚点坐标播种，
 * {@link #anchorSeededRandom}），判定与加载顺序、玩家移动方向/速度完全无关 ——
 * 同一种子无论怎么逛，繁茂洞穴的分布与内容一致。
 *
 * <p>各子特征的参数均取自官方 CaveFeatures/CavePlacements：
 * <ul>
 *   <li>MOSS_PATCH：xzRadius 4~7、extraEdgeColumnChance 0.3、vegetationChance 0.8；</li>
 *   <li>MOSS_VEGETATION：短草 50 / 苔藓地毯 25 / 杜鹃 7 / 开花杜鹃 4 / 高草 10（共 96）；</li>
 *   <li>CAVE_VINE_IN_MOSS：body 长 5/6 为 0~3、1/6 为 1~7，每节 1/5 带发光浆果；</li>
 *   <li>LUSH_CAVES_CLAY：出现频率约苔藓补丁的一半，水池含水（垂滴叶涉及含水机制，省略）；</li>
 *   <li>ROOTED_AZALEA_TREE：每区块 UniformInt(1,2) 次尝试（随机 Y + 环境扫描定位洞顶，
 *       再上溯到地表放树，含缠根泥土根柱与垂根），复刻官方完整放置链。</li>
 * </ul>
 */
public class WorldGenLushCave extends WorldGenerator {

	private final WorldGenAzaleaTree azaleaTree = new WorldGenAzaleaTree(false);

	// 繁茂选择噪声阈值（按地表群系分档）：
	// 桦木森林/丘陵 0.0 → 噪声对称分布，约一半桦木林下区域繁茂（整腔进/整腔出）；
	// 平原 0.5 → 显著更稀有，只有噪声峰值处的腔室繁茂
	private static final double LUSH_NOISE_THRESHOLD_BIRCH = 0.0D;
	private static final double LUSH_NOISE_THRESHOLD_PLAINS = 0.5D;

	// 选择噪声采样频率：0.004 ≈ 250 格波长，低于腔室尺度 → 繁茂区整腔覆盖、边界有机
	private static final double LUSH_NOISE_FREQUENCY = 0.004D;

	// 官方 RootSystemConfiguration.ROOTED_AZALEA_TREE 参数
	private static final int ROOT_RADIUS = 3;
	private static final int ROOT_PLACEMENT_ATTEMPTS = 20;
	private static final int HANGING_ROOT_RADIUS = 3;
	private static final int HANGING_ROOT_VERTICAL_SPAN = 2;
	private static final int HANGING_ROOT_PLACEMENT_ATTEMPTS = 20;

	// 官方 MOSS_PATCH（自然生成版）参数
	private static final float MOSS_EDGE_COLUMN_CHANCE = 0.3F;
	private static final float MOSS_VEGETATION_CHANCE = 0.8F;

	// 官方 CLAY_POOL_WITH_DRIPLEAVES：extraEdgeColumnChance = 0.1（边缘列仅 10% 保留，池形圆润）
	private static final float CLAY_EDGE_COLUMN_CHANCE = 0.1F;

	// 官方 CLAY_POOL_WITH_DRIPLEAVES：verticalRange = 5（表面与锚点表面差超过 5 格的列不进盆）
	private static final int CLAY_POOL_VERTICAL_RANGE = 5;

	// 官方 depth = ConstantInt(3) + extraBottomBlockChance 0.8（黏土柱深 3，80% 概率再 +1）
	private static final int CLAY_POOL_DEPTH = 3;
	private static final float CLAY_POOL_EXTRA_DEPTH_CHANCE = 0.8F;

	// 官方 MOSS_PATCH_CEILING 的 vegetationChance = 0.08：CAVE_VINE_IN_MOSS 作为顶板苔藓补丁的"植被"，
	// 仅 8% 的补丁列会挂藤蔓（不是补丁边缘概率 0.3）
	private static final float CEILING_VINE_CHANCE = 0.08F;

	// 繁茂装饰仅限真正的地下洞穴：洞穴顶板须距真实地表（地形面）至少 8 格。
	// 官方 lush caves 是纯地下生物群系 —— 树冠/地表坑洼/洞口的低光空气不属于洞穴。
	private static final int MIN_CAVE_DEPTH = 8;

	// 每区块 2×2 象限锚点（8 格间距）：与补丁半径 4~7 衔接，繁茂区内近似铺满（官方 125 次/区块）
	// 锚点可越界写入邻区块（装饰与邻区块判定独立，越界写入只会更繁茂，不产生不一致）
	private static final int[][] ANCHOR_OFFSETS = {
			{4, 4}, {12, 4}, {4, 12}, {12, 12}
	};

	// 水池独立撒点：官方 LUSH_CAVES_CLAY 是独立撒点（62/125），水池位置不与苔藓补丁耦合，
	// 在整个繁茂噪声区域内独立分布。4 个采样角落在世界坐标系 8×8 网格瓦片内，
	// 中心再按种子随机全幅抖动（0~7），避免水池中心卡在网格角/边界被切成方形。
	private static final int[][] POOL_ANCHOR_TILES = {
			{0, 0}, {8, 0}, {0, 8}, {8, 8}
	};

	// 每个水池采样点的出现概率（官方 62/125 ≈ 苔藓补丁密度的一半，对齐 4 采样点/区块）
	private static final float CLAY_POOL_ANCHOR_CHANCE = 0.5F;

	// 水池随机流的盐：与苔藓网格错开，避免同一坐标两套装饰共享随机序列
	private static final long POOL_RANDOM_SALT = 0x5EEDBEEFL;

	// 官方 ROOTED_AZALEA_TREE（CavePlacements）放置链：每区块 UniformInt(1,2) 次尝试 →
	// 随机 (x,z) + 随机 Y → EnvironmentScan 向上找实心(≤12) → origin = 实心下方空气格 →
	// RootSystemFeature 从 origin 上溯到地表放树。多数尝试失败（随机 Y 大多落在岩体内部
	// 或空腔深处 12 格内见不到顶），实际成树率约 0.05~0.15 棵/区块，几十区块上方只有稀疏几颗。
	// 本模组调整为官方尝试数的两倍（每区块 2~4 次），成树率约为官方两倍。
	private static final long TREE_RANDOM_SALT = 0x4711BEEFL;
	private static final int TREE_ATTEMPTS_MIN = 2;   // 官方 UniformInt(1,2)，此处翻倍
	private static final int TREE_ATTEMPTS_MAX = 4;
	private static final int TREE_SCAN_MAX_STEPS = 12; // 官方 EnvironmentScan 步数上限
	private static final int ROOT_COLUMN_MAX_HEIGHT = 100; // 官方 rootColumnMaxHeight

	@Override
	public boolean generate(World world, Random rand, int x, int y, int z) {
		return decorateChunk(world, x >> 4, z >> 4);
	}

	// 繁茂选择噪声的按世界缓存（每世界一个采样器实例，种子派生，与 WorldGenLushCave 实例无关）
	private static final Map<World, DoublePerlinNoiseSampler> NOISE_CACHE = new WeakHashMap<>();

	/**
	 * 指定世界的列 (x, z) 是否属于繁茂噪声区域（纯函数，与加载顺序无关）。
	 * 供事件处理器在 populate 前做与繁茂装饰完全一致的区域判定（如拦截繁茂区内的岩浆湖）。
	 */
	public static boolean isLushColumnStatic(World world, int x, int z) {
		DoublePerlinNoiseSampler noise = NOISE_CACHE.get(world);
		if (noise == null) {
			noise = DoublePerlinNoiseSampler.create(new Random(world.getSeed() ^ 0x1C5A11L), -4, 1.0D);
			NOISE_CACHE.put(world, noise);
		}
		BiomeGenBase biome = world.getBiomeGenForCoords(x, z);
		if (!isLushCaveBiome(biome)) {
			return false;
		}
		double threshold = biome == BiomeGenBase.plains ? LUSH_NOISE_THRESHOLD_PLAINS : LUSH_NOISE_THRESHOLD_BIRCH;
		return noise.sample(x * LUSH_NOISE_FREQUENCY, 0.0D, z * LUSH_NOISE_FREQUENCY) >= threshold;
	}

	/**
	 * 锚点位置派生的确定性随机流：装饰内容只依赖世界种子与锚点绝对坐标，与调用时机无关。
	 * salt 用于让不同装饰子系统（苔藓/水池）在同一坐标拥有互相独立的随机序列。
	 */
	private static Random anchorSeededRandom(long worldSeed, long salt, int x, int z) {
		return new Random(worldSeed ^ salt ^ ((long) x * 341873128712L + (long) z * 132897987541L));
	}

	/**
	 * 对区块执行装饰：苔藓补丁走自己的 2×2 锚点网格，水池走错开的独立网格 ——
	 * 官方 LUSH_CAVES_CLAY 与 MOSS_PATCH 是两套独立撒点，位置互不耦合。
	 * 锚点越区块边界写入无害：邻区块的装饰判定与其无关，越界写入只会让繁茂区
	 * 边界处更饱满，不产生不一致。
	 */
	private boolean decorateChunk(World world, int chunkX, int chunkZ) {
		boolean decorated = false;
		// 水池先于苔藓：池面落成后苔藓/植被不会种在水上（水不可替换、不承重）；
		// 跨区块先铺的植被由 clearVegetationAbove 兜底清除
		Random poolRand = anchorSeededRandom(world.getSeed(), POOL_RANDOM_SALT, chunkX << 4, chunkZ << 4);
		for (int[] tile : POOL_ANCHOR_TILES) {
			// 池中心在世界坐标系 8×8 网格瓦片内全幅抖动（0~7，种子确定）：
			// 均匀覆盖整个繁茂噪声区域，不与区块/瓦片边界产生周期性条带；
			// 池体写入按世界坐标自由越过区块边界
			int px = (chunkX << 4) + tile[0] + poolRand.nextInt(8);
			int pz = (chunkZ << 4) + tile[1] + poolRand.nextInt(8);
			if (decoratePoolAnchor(world, px, pz)) {
				decorated = true;
			}
		}
		for (int[] offset : ANCHOR_OFFSETS) {
			if (decorateAnchor(world, (chunkX << 4) + offset[0], (chunkZ << 4) + offset[1])) {
				decorated = true;
			}
		}

		// 官方 ROOTED_AZALEA_TREE 是独立于苔藓补丁的撒点（自有 Count + InSquare + EnvironmentScan），
		// 不挂在锚点概率上：每区块 1~2 次尝试，随机列 + 随机 Y 环境扫描定位洞顶再上溯放树。
		// 该放置链天然导致大部分尝试失败，成树稀疏（与官方观感一致）。
		Random treeRand = anchorSeededRandom(world.getSeed(), TREE_RANDOM_SALT, chunkX << 4, chunkZ << 4);
		int attempts = TREE_ATTEMPTS_MIN + treeRand.nextInt(TREE_ATTEMPTS_MAX - TREE_ATTEMPTS_MIN + 1);
		for (int i = 0; i < attempts; i++) {
			int tx = (chunkX << 4) + treeRand.nextInt(16);
			int tz = (chunkZ << 4) + treeRand.nextInt(16);
			if (!isLushColumnStatic(world, tx, tz)) {
				continue; // 官方 BiomeFilter：仅繁茂区
			}
			int originY = scanUpForSolid(world, tx, treeRand.nextInt(world.getHeight()), tz, TREE_SCAN_MAX_STEPS);
			if (originY < 0) {
				continue; // 环境扫描未命中（起点在岩体内/空腔深处 12 格内无顶）
			}
			if (generateRootedAzaleaTree(world, treeRand, tx, originY, tz)) {
				decorated = true;
			}
		}
		return decorated;
	}

	/**
	 * 对单个苔藓锚点执行装饰：顶板苔藓 + 藤蔓、地板苔藓 + 植被。
	 * 杜鹃树由独立的每区块放置链生成（见 {@link #decorateChunk}），不挂在锚点上。
	 * 前置门限：选择噪声在该列达到群系阈值（{@link #isLushColumnStatic}）+ 下方有洞穴。
	 */
	private boolean decorateAnchor(World world, int x, int z) {
		if (!isLushColumnStatic(world, x, z)) {
			return false;
		}

		Random rand = anchorSeededRandom(world.getSeed(), 0L, x, z);
		int surfaceY = world.getHeightValue(x, z);
		int caveTop = findCaveTop(world, x, surfaceY, z);
		if (caveTop < 1) {
			return false;
		}

		placeCeilingMossPatch(world, rand, x, caveTop, z);   // 顶板苔藓 + 洞穴藤蔓
		placeMossPatch(world, rand, x, caveTop, z);          // 地板苔藓 + 植被
		return true;
	}

	/**
	 * 对单个水池锚点执行装饰：以整个繁茂噪声区域为范围独立分布，
	 * 与苔藓补丁位置无关（独立网格 + 独立盐的随机流 + 独立概率掷骰）。
	 */
	private boolean decoratePoolAnchor(World world, int x, int z) {
		if (!isLushColumnStatic(world, x, z)) {
			return false;
		}

		Random rand = anchorSeededRandom(world.getSeed(), POOL_RANDOM_SALT, x, z);
		if (rand.nextFloat() >= CLAY_POOL_ANCHOR_CHANCE) {
			return false;
		}

		int surfaceY = world.getHeightValue(x, z);
		int caveTop = findCaveTop(world, x, surfaceY, z);
		if (caveTop < 1) {
			return false;
		}
		return placeClayPool(world, rand, x, caveTop, z);    // 黏土水池（垂滴叶省略）
	}

	/**
	 * 官方 lush caves 只出现在特定群系；1.7.10 用「桦木森林 + 平原」近似。
	 */
	private static boolean isLushCaveBiome(BiomeGenBase biome) {
		return biome == BiomeGenBase.plains
				|| biome == BiomeGenBase.birchForest
				|| biome == BiomeGenBase.birchForestHills;
	}

	/**
	 * 从地表往下扫描，返回洞穴顶部（第一个地下空气块，天空光照极低），找不到返回 -1。
	 *
	 * <p>1.7.10 的 {@code getHeightValue} 会把树叶计入高度（树叶 getLightOpacity=1），
	 * 桦木森林里树冠顶会被当成"地表"。若直接以它为起点，树冠下被遮出低光的空气
	 * 会被误判成洞穴，苔藓/黏土水池就会铺到林地上。因此先定位<b>真实地形面</b>
	 * （首个非树实心块，见 {@link #findGroundSurface}），从它下方开始扫描；并额外要求：
	 * <ul>
	 *   <li>空气格正上方为地形块（树冠/树干/悬空板不算）—— 排除洞口、树下开口；</li>
	 *   <li>顶板距地形面至少 {@link #MIN_CAVE_DEPTH} 格 —— 只装饰真正的地下洞穴，
	 *       与官方 lush caves 纯地下群系的语义一致；</li>
	 *   <li>洞穴横向封闭（见 {@link #isSealedCave}）—— 排除悬崖/山体侧面突起下
	 *       侧向敞开直通天空的"伪洞穴"。</li>
	 * </ul>
	 */
	private int findCaveTop(World world, int x, int surfaceY, int z) {
		int groundY = findGroundSurface(world, x, surfaceY, z);
		if (groundY < 0) {
			return -1;
		}
		for (int cy = groundY - 1; cy >= 15; cy--) {
			if (world.isAirBlock(x, cy, z)
					&& world.getSavedLightValue(Sky, x, cy, z) <= 3
					&& groundY - cy >= MIN_CAVE_DEPTH
					&& isTerrainBlock(world, x, cy + 1, z)
					&& isSealedCave(world, x, cy, z)) {
				return cy;
			}
		}
		return -1;
	}

	/**
	 * 洞穴是否"封闭"：洞穴顶板空气格四周（同层）不得存在能望见天空的开口。
	 * <p>悬崖/山体侧面的突起（岩架、岩坎、鼓包）下方，空气会横向敞开直通崖面外的天空
	 * （{@code canBlockSeeTheSky} 为 true）—— 这类"伪洞穴"在此被排除；
	 * 真正的洞穴四周要么是岩壁，要么是同样被地形封顶的地下空气。
	 * 深度检查挡不住中段悬崖的厚岩架（其上方岩体直通崖顶），必须靠这道横向检查兜底。
	 */
	private boolean isSealedCave(World world, int x, int y, int z) {
		return !isSkyExposed(world, x - 1, y, z)
				&& !isSkyExposed(world, x + 1, y, z)
				&& !isSkyExposed(world, x, y, z - 1)
				&& !isSkyExposed(world, x, y, z + 1);
	}

	/**
	 * 该空气格是否侧向敞开直通天空：同层为空气，且高度 ≥ 该列高度图（能望见天空）。
	 * 实心邻居天然不敞开，直接返回 false。
	 */
	private boolean isSkyExposed(World world, int x, int y, int z) {
		return world.isAirBlock(x, y, z) && world.canBlockSeeTheSky(x, y, z);
	}

	/**
	 * 真实地形面：从 fromY 向下找第一个地形块（非树的实心方块）。
	 * 树冠/树干、植物、水、空气等都被跳过，落在实际地表上。
	 */
	private int findGroundSurface(World world, int x, int fromY, int z) {
		for (int cy = fromY; cy > 0; cy--) {
			if (isTerrainBlock(world, x, cy, z)) {
				return cy;
			}
		}
		return -1;
	}

	/**
	 * 是否为地形块：实心立方，且非树的方块。树叶（快速图像下为实心）与原木虽为实心，
	 * 但不是地形 —— 树冠/树干不能充当洞穴顶板或地表。
	 */
	private boolean isTerrainBlock(World world, int x, int y, int z) {
		Block block = world.getBlock(x, y, z);
		return block.isOpaqueCube()
				&& block != Blocks.leaves && block != Blocks.leaves2
				&& block != Blocks.log && block != Blocks.log2;
	}

	/**
	 * 官方 EnvironmentScanPlacement(UP, solid, ONLY_IN_AIR, maxSteps) 的 1.7.10 等价：
	 * 从 (x, y, z) 向上扫描，跳过空气，直到命中实心（≤ maxSteps 格）。
	 * 返回实心块下方空气格的 Y（即 RootSystemFeature 的 origin），找不到返回 -1。
	 * 官方该扫描的 targetPredicate 为 solid()（完整立方碰撞箱），此处用 isOpaqueCube 近似。
	 */
	private int scanUpForSolid(World world, int x, int y, int z, int maxSteps) {
		for (int i = 0; i < maxSteps; i++) {
			Block block = world.getBlock(x, y, z);
			if (world.isAirBlock(x, y, z)) {
				y++;
				continue;
			}
			// 第一个非空气块：须为实心立方，且其下方是空气（origin 必须为空气格）
			if (block.isOpaqueCube() && world.isAirBlock(x, y - 1, z)) {
				return y - 1;
			}
			return -1; // 命中非实心非空气（如水/植物），官方同样失败
		}
		return -1; // maxSteps 格内全是空气（起点在空腔深处/天空），失败
	}

	/**
	 * 复刻官方 MOSS_PATCH_CEILING + CAVE_VINE_IN_MOSS：
	 * 把洞穴顶板换成苔藓块补丁，并在下方垂挂带浆果的洞穴藤蔓。
	 */
	private void placeCeilingMossPatch(World world, Random rand, int x, int caveTop, int z) {
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
				if (!world.isAirBlock(px, caveTop, pz)) {
					continue; // 石柱或洞壁，非顶板
				}

				// 从锚点高度向上找该列的天花板（空气上方的第一个实心块）
				int ceilingY = findCeiling(world, px, caveTop, pz);
				if (ceilingY <= 0) {
					continue; // 该列向上 24 格仍是空气，视为无顶板
				}
				if (isMossReplaceable(world, px, ceilingY, pz)) {
					world.setBlock(px, ceilingY, pz, ModBlocks.MOSS_BLOCK.get(), 0, 2);
				}

				// 洞穴藤蔓（官方 CAVE_VINE_IN_MOSS，顶板苔藓的伴生植被）
				if (ModBlocks.CAVE_VINE_PLANT.isEnabled() && ModBlocks.CAVE_VINE.isEnabled()
						&& rand.nextFloat() < CEILING_VINE_CHANCE
						&& world.isAirBlock(px, ceilingY - 1, pz)) {
					placeCaveVine(world, rand, px, ceilingY - 1, pz);
				}
			}
		}
	}

	/**
	 * 沿天花板找该列顶板：从 fromY 向上扫过空气，返回第一个非空气块的 y；超过 24 格仍为空气返回 -1。
	 */
	private int findCeiling(World world, int x, int fromY, int z) {
		int cy = fromY;
		while (cy < fromY + 24 && world.isAirBlock(x, cy, z)) {
			cy++;
		}
		return cy < fromY + 24 ? cy : -1;
	}

	/**
	 * 复刻官方 CAVE_VINE_IN_MOSS 的 BLOCK_COLUMN：
	 * body 层长 5/6 为 0~3、1/6 为 1~7（另有 1 格 head 生长端），每节 1/5 概率带浆果发光。
	 * head 的 AGE 为 UniformInt(23,25)（满 25 停止生长）—— 自然生成的藤蔓接近被修剪状态，
	 * 生成后最多再长 0~2 格；玩家种植的藤蔓仍按随机上限正常生长。
	 */
	private void placeCaveVine(World world, Random rand, int x, int topY, int z) {
		int length = rand.nextInt(6) == 0 ? 1 + rand.nextInt(7) : rand.nextInt(4);

		for (int i = 0; i < length; i++) {
			int py = topY - i;
			if (!world.isAirBlock(x, py, z)) {
				return;
			}
			world.setBlock(x, py, z, ModBlocks.CAVE_VINE_PLANT.get(), rand.nextInt(5) == 0 ? 1 : 0, 2);
		}
		int headY = topY - length;
		if (world.isAirBlock(x, headY, z)) {
			world.setBlock(x, headY, z, ModBlocks.CAVE_VINE.get(), rand.nextInt(5) == 0 ? 1 : 0, 2);
			// 官方自然生成 head AGE = 23~25，剩余生长量 0~2 格：
			// maxLength 收敛为当前总长（body + head）+ 0~2，防止自然藤蔓持续疯长
			TileEntity te = world.getTileEntity(x, headY, z);
			if (te instanceof TileEntityCaveVines) {
				((TileEntityCaveVines) te).setMaxLength(length + 1 + rand.nextInt(3));
			}
		}
	}

	/**
	 * 复刻官方 MOSS_PATCH + MOSS_VEGETATION：
	 * 在洞穴地板上铺苔藓块补丁，并按官方权重撒植被。
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
					if (rand.nextFloat() < MOSS_VEGETATION_CHANCE && world.isAirBlock(px, floorY + 1, pz)) {
						placeMossVegetation(world, rand, px, floorY + 1, pz);
					}
				}
			}
		}
	}

	/**
	 * 官方 MOSS_VEGETATION 加权（共 96）：短草 50 / 苔藓地毯 25 / 杜鹃 7 / 开花杜鹃 4 / 高草 10。
	 */
	private void placeMossVegetation(World world, Random rand, int x, int y, int z) {
		int pick = rand.nextInt(96);
		if (pick < 50) {
			world.setBlock(x, y, z, Blocks.tallgrass, 1, 2); // 短草
		} else if (pick < 75) {
			world.setBlock(x, y, z, ModBlocks.MOSS_CARPET.get(), 0, 2);
		} else if (pick < 82) {
			world.setBlock(x, y, z, ModBlocks.AZALEA.get(), 0, 2);
		} else if (pick < 86) {
			world.setBlock(x, y, z, ModBlocks.AZALEA.get(), 1, 2); // 开花杜鹃
		} else if (world.isAirBlock(x, y + 1, z)) {
			world.setBlock(x, y, z, Blocks.double_plant, 2, 2); // 高草（下半）
			world.setBlock(x, y + 1, z, Blocks.double_plant, 8 | 2, 2); // 高草（上半）
		} else {
			world.setBlock(x, y, z, Blocks.tallgrass, 1, 2); // 上方空间不足退化为短草
		}
	}

	/**
	 * 复刻官方 CLAY_POOL_WITH_DRIPLEAVES（垂滴叶省略）—— 对齐 WaterloggedVegetationPatchFeature 语义：
	 * <ul>
	 *   <li>每列独立扫表面（与锚点表面差 ≤ verticalRange 5 格才进盆）；</li>
	 *   <li>表面实心块向下替换为黏土柱（深 3，80% 概率 +1）—— 池盆地面即黏土；</li>
	 *   <li>封闭的表面块换为水：水面与周围地面齐平（水替换表面块本身，不是其上方空气格）。
	 *       邻列表面无论高低，同层必为实心岩壁或同为池格，不会敞开；邻列更低的列其水面
	 *       也在更低层，两列间自然形成黏土台阶 —— 阶梯状水池，正是官方观感；</li>
	 *   <li>可替换块含苔藓块（官方 LUSH_GROUND_REPLACEABLE 含 moss_block/moss_carpet/azalea），
	 *       与先铺的苔藓补丁不冲突 —— 苔藓洞底上正常成池。</li>
	 * </ul>
	 */
	private boolean placeClayPool(World world, Random rand, int x, int caveTop, int z) {
		int centerFloor = findFloor(world, x, caveTop, z);
		if (centerFloor <= 0) {
			return false;
		}
		// 中心地板必须落在实心岩体上（下方非空气）：悬挑/单层岩板不能作为池底
		if (world.isAirBlock(x, centerFloor - 1, z)) {
			return false;
		}

		int radius = 4 + rand.nextInt(4); // 官方 xzRadius = UniformInt(4,7)
		// 第一遍：盘点池盆列 —— findFloor 空腔感知（墙/柱列返回 -1，杜绝铺进墙里），
		// 高差 > verticalRange 的列跳过
		List<int[]> poolCells = new ArrayList<>();
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				boolean isXEdge = dx == -radius || dx == radius;
				boolean isZEdge = dz == -radius || dz == radius;
				if (isXEdge && isZEdge) {
					continue; // 四角跳过
				}
				if ((isXEdge || isZEdge) && rand.nextFloat() > CLAY_EDGE_COLUMN_CHANCE) {
					continue;
				}

				int px = x + dx;
				int pz = z + dz;
				int floorY = findFloor(world, px, caveTop, pz);
				if (floorY <= 0 || Math.abs(floorY - centerFloor) > CLAY_POOL_VERTICAL_RANGE) {
					continue;
				}
				Block block = world.getBlock(px, floorY, pz);
				if (block != Blocks.clay && block != ModBlocks.MOSS_BLOCK.get() && !isPoolReplaceable(world, px, floorY, pz)) {
					continue;
				}
				poolCells.add(new int[]{px, floorY, pz});
			}
		}
		if (poolCells.isEmpty()) {
			return false;
		}

		// 第二遍：黏土柱（表面块向下 depth 格，遇不可替换块截断 —— 官方 placeGround 同款）
		for (int[] cell : poolCells) {
			int px = cell[0];
			int floorY = cell[1];
			int pz = cell[2];
			int depth = CLAY_POOL_DEPTH + (rand.nextFloat() < CLAY_POOL_EXTRA_DEPTH_CHANCE ? 1 : 0);
			for (int i = 0; i < depth; i++) {
				int py = floorY - i;
				Block b = world.getBlock(px, py, pz);
				if (b == Blocks.clay) {
					continue;
				}
				if (b != ModBlocks.MOSS_BLOCK.get() && !isPoolReplaceable(world, px, py, pz)) {
					break;
				}
				world.setBlock(px, py, pz, Blocks.clay, 0, 2);
			}
		}

		// 第三遍：封闭的表面块换水（四邻 + 下方为实心或水；植被不算支撑）。
		// 邻列若为更低的洼地，本列该层是空气 → 不蓄水，退化为黏土岸（梯田的干边）。
		boolean placed = false;
		for (int[] cell : poolCells) {
			int px = cell[0];
			int floorY = cell[1];
			int pz = cell[2];
			if (isWaterEnclosed(world, px, floorY, pz)) {
				world.setBlock(px, floorY, pz, Blocks.water, 0, 2);
				clearVegetationAbove(world, px, floorY, pz); // 清掉漂在水面上的草/杜鹃
				placed = true;
			}
		}
		return placed;
	}

	/**
	 * 水格四邻 + 正下方均为<b>实心方块或水</b>才安全蓄水。对齐官方 isExposed（N/E/S/W/DOWN +
	 * isFaceSturdy）：植被（草/杜鹃/苔藓地毯）不提供支撑 —— 否则水贴着植物蓄水，
	 * 视觉上"水旁边是空气、草、杜鹃"。正下方是空气（悬挑/单层岩板）同样不蓄水。
	 */
	private boolean isWaterEnclosed(World world, int x, int y, int z) {
		return isSolidOrWater(world, x - 1, y, z) && isSolidOrWater(world, x + 1, y, z)
				&& isSolidOrWater(world, x, y, z - 1) && isSolidOrWater(world, x, y, z + 1)
				&& isSolidOrWater(world, x, y - 1, z);
	}

	private boolean isSolidOrWater(World world, int x, int y, int z) {
		Block block = world.getBlock(x, y, z);
		return block == Blocks.water || block.isOpaqueCube();
	}

	/**
	 * 清掉蓄水格上方的植被：苔藓补丁的草/杜鹃/苔藓地毯若已铺在池底上，池化后会漂在水面。
	 */
	private void clearVegetationAbove(World world, int x, int y, int z) {
		Block above = world.getBlock(x, y + 1, z);
		if (above == Blocks.tallgrass || above == Blocks.double_plant
				|| above == ModBlocks.MOSS_CARPET.get() || above == ModBlocks.AZALEA.get()) {
			world.setBlock(x, y + 1, z, Blocks.air, 0, 2);
			if (above == Blocks.double_plant && world.getBlock(x, y + 2, z) == Blocks.double_plant) {
				world.setBlock(x, y + 2, z, Blocks.air, 0, 2); // 高草上半
			}
		}
	}

	/**
	 * 从洞穴顶部往下找地板（第一个非空气块）。仅接受与 fromY 同一空腔连通的列：
	 * 先向下越过最多 {@link #CLAY_POOL_VERTICAL_RANGE} 格倾斜顶板找到空气（同一洞），
	 * 找不到（实心层过厚 = 墙/石柱）返回 -1 —— 否则会把墙块当"地板"，
	 * 把黏土/水铺进墙里，切出卡在墙内的方形残池。
	 */
	private int findFloor(World world, int x, int fromY, int z) {
		int cy = fromY;
		int ceilingDescent = 0;
		while (cy > 5 && !world.isAirBlock(x, cy, z)) {
			if (++ceilingDescent > CLAY_POOL_VERTICAL_RANGE) {
				return -1; // 实心层过厚：该列在墙/柱内，不属于本空腔
			}
			cy--;
		}
		while (cy > 5 && world.isAirBlock(x, cy, z)) {
			cy--;
		}
		return cy > 5 ? cy : -1;
	}

	/**
	 * 复刻官方 RootSystemFeature.placeDirtAndTree + placeDirt + placeRootedDirt：
	 * 在洞穴上方的地表种杜鹃树，然后沿「origin（环境扫描到的洞顶空气格）→ 树底」逐层撒缠根泥土，形成根柱。
	 *
	 * @return 是否成功种下一棵树（官方 placeDirtAndTree 的布尔返回值）
	 */
	private boolean generateRootedAzaleaTree(World world, Random rand, int x, int originY, int z) {
		int surfaceY = world.getHeightValue(x, z);
		if (surfaceY <= originY + 1) {
			return false; // 洞穴离地表太近，没有足够根柱空间
		}
		if (surfaceY - originY > ROOT_COLUMN_MAX_HEIGHT) {
			return false; // 官方 rootColumnMaxHeight：origin 距地表超过 100 格则放弃
		}

		// 树底须能支撑杜鹃树（AZALEA_GROWS_ON 的 1.7.10 等价）
		Block ground = world.getBlock(x, surfaceY - 1, z);
		if (!canAzaleaGrowOn(ground)) {
			return false;
		}

		// 种杜鹃树（官方 treeFeature.place）
		if (!azaleaTree.generate(world, rand, x, surfaceY, z)) {
			return false;
		}

		// 根柱：官方 placeDirt 从 origin（= 环境扫描到的洞顶空气格）逐层填到 targetHeight = A-1（不含），
		// 即最顶层是 A-2（树干基座 A 下方第 2 格）—— A-1 那格只有树底单块缠根泥土
		//（由 BendingTrunkPlacer.placeBelowTrunkBlock 在树干正下方放置，x/z 与橡木相同），
		// 不参与 ±3 扩散。起点取 originY 而非 originY+1，与官方 origin 语义一致，
		// 根柱底端在洞内顶板露出缠根泥土。
		for (int cy = originY; cy < surfaceY - 1; cy++) {
			placeRootedDirt(world, rand, x, z, cy);
		}

		// 垂根：在 origin 附近撒（官方 placeRoots）
		placeHangingRoots(world, rand, x, originY, z);
		return true;
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
	 * 官方 AZALEA_ROOT_REPLACEABLE（BASE_STONE_OVERWORLD + SUBSTRATE_OVERWORLD + 沙/砾/陶瓦/雪）的 1.7.10 等价。
	 */
	private boolean isRootReplaceable(World world, int x, int y, int z) {
		if (y < 0 || y >= world.getHeight()) {
			return false;
		}
		// 注意：不含 clay —— 官方 AZALEA_ROOT_REPLACEABLE 不该把水池黏土换成缠根泥土，
		// 否则杜鹃树的根会把已成型的水池边缘“吃掉”
		Block block = world.getBlock(x, y, z);
		if (block == Blocks.stone || block == Blocks.grass || block == Blocks.sand
				|| block == Blocks.gravel) {
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
	 * 水池专用替换检查：{@link #isMossReplaceable} 基础上放宽允许替换矿物 ——
	 * 否则矿脉穿过洞底的水池会出现池面缺口（矿物格不进池）和黏土柱截断。
	 * 仅用于黏土水池，苔藓补丁仍走矿物保留的白名单。
	 */
	private boolean isPoolReplaceable(World world, int x, int y, int z) {
		if (isMossReplaceable(world, x, y, z)) {
			return true;
		}
		Block block = world.getBlock(x, y, z);
		return block == Blocks.coal_ore || block == Blocks.iron_ore || block == Blocks.gold_ore
				|| block == Blocks.redstone_ore || block == Blocks.lapis_ore || block == Blocks.diamond_ore
				|| block == Blocks.emerald_ore || block == ModBlocks.COPPER_ORE.get()
				|| block == ModBlocks.DEEPSLATE_COAL_ORE.get() || block == ModBlocks.DEEPSLATE_IRON_ORE.get()
				|| block == ModBlocks.DEEPSLATE_COPPER_ORE.get() || block == ModBlocks.DEEPSLATE_GOLD_ORE.get()
				|| block == ModBlocks.DEEPSLATE_REDSTONE_ORE.get() || block == ModBlocks.DEEPSLATE_LAPIS_ORE.get()
				|| block == ModBlocks.DEEPSLATE_DIAMOND_ORE.get() || block == ModBlocks.DEEPSLATE_EMERALD_ORE.get();
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
