package ganymedes01.etfuturum.waterlog;

import ganymedes01.etfuturum.blocks.BlockAquaticPlant;
import ganymedes01.etfuturum.mixins.early.oceanplants.MixinBlockDynamicLiquid;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * 含水格向外蔓延（26.2 语义：含水格内部是 {@code Fluids.WATER.getSource(false)}
 * 源水，{@code updateShape} 邻居变化时 {@code scheduleTick} → 源水 spread 进
 * 可流入的邻格）。实现直接复用 oceanplants 的 {@link MixinBlockDynamicLiquid}
 * {@code @Invoker} 接口驱动 vanilla 私有蔓延机器（func_149809_q / func_149808_o
 * / func_149813_h），与海泡菜的"点燃 vanilla spread"同构：下流 meta 8
 * （falling），下方堵住时按 vanilla 权重寻路横流 meta 1。
 *
 * <p>蔓延水的存续依赖 {@code waterlog.MixinBlockLiquid} 的
 * {@code func_149804_e} 虚拟源桥（含水格对水流深度计算呈现为 0）：否则流动水
 * 找不到真实水邻居 → 首个自身 tick {@code j1 < 0} 自删（"水闪现即消退"）。
 * 桥接后蔓延水沉降为静态水稳定存续，排水后按 vanilla 语义回退干涸。
 * {@code func_149811_n}（动态→静态沉降）对本虚拟源不适用——水始终是方块
 * state 的一部分，无实体水可沉降。
 */
public final class WaterlogFlow {

	private WaterlogFlow() {}

	/** 水的流体 tick 间隔（BlockLiquid.tickRate 水分支，L329）。 */
	public static final int WATER_TICK_RATE = 5;

	/**
	 * 26.2 FlowingFluid.canPassThroughWall 语义：宿主方块碰撞形状完整覆盖某方向的
	 * 格边界平面（mergedFaceOccludes）时该方向不流水——含水楼梯的底面与背面、
	 * 下半砖的底面是完整面，水只从开口部分流出；部分覆盖照常流动。1.7.10 侧用
	 * 宿主碰撞箱集合做等价判定（vanilla addCollisionBoxesToList 收集）：楼梯碰撞箱
	 * 随邻居动态拐角（func_150145_f 外角收缩台阶 / func_150144_g 内角补 1/4 块）——
	 * 直梯 = 底/顶 + 背面 2 完整面、内角 = 3 完整面（底/顶 + 两个背向）、外角 = 仅
	 * 底/顶 1 完整面，与 26.2 StairBlock 形状自动拐角同构；玻璃板等薄面无完整面。
	 * 覆盖判定用四象限采样：1.7.10 家族碰撞箱全部 0.5 对齐，四象限各被某箱覆盖
	 * ⇔ 整面覆盖（半格对齐下精确）。
	 */
	public static boolean isFaceComplete(List<AxisAlignedBB> boxes, int x, int y, int z, int side) {
		for (int quadrant = 0; quadrant < 4; ++quadrant) {
			if (!isQuadrantCovered(boxes, side, x, y, z, (quadrant & 1) != 0, (quadrant >> 1 & 1) != 0)) {
				return false;
			}
		}
		return true;
	}

	/** 收集宿主方块在整格范围内的全部碰撞箱（楼梯含动态拐角收缩/补块）。 */
	private static List<AxisAlignedBB> getHostCollisionBoxes(World world, int x, int y, int z) {
		List<AxisAlignedBB> list = new ArrayList<AxisAlignedBB>();
		world.getBlock(x, y, z).addCollisionBoxesToList(world, x, y, z,
				AxisAlignedBB.getBoundingBox(x, y, z, x + 1.0D, y + 1.0D, z + 1.0D), list, null);
		return list;
	}

	/** side：0=下(y=min) 1=上(y=max) 2=北(z=min) 3=南(z=max) 4=西(x=min) 5=东(x=max)。 */
	private static boolean isFaceCovered(List<AxisAlignedBB> boxes, int x, int y, int z, int side) {
		for (int quadrant = 0; quadrant < 4; ++quadrant) {
			if (!isQuadrantCovered(boxes, side, x, y, z, (quadrant & 1) != 0, (quadrant >> 1 & 1) != 0)) {
				return false;
			}
		}
		return true;
	}

	private static boolean isQuadrantCovered(List<AxisAlignedBB> boxes, int side, int x, int y, int z, boolean half1, boolean half2) {
		for (AxisAlignedBB bb : boxes) {
			if (quadrantCoveredByBox(bb, side, x, y, z, half1, half2)) {
				return true;
			}
		}
		return false;
	}

	private static boolean quadrantCoveredByBox(AxisAlignedBB bb, int side, int x, int y, int z, boolean half1, boolean half2) {
		switch (side) {
			case 0: // y = min
				if (bb.minY > y + EPS) return false;
				return coversHalf(bb.minX, bb.maxX, x, half1) && coversHalf(bb.minZ, bb.maxZ, z, half2);
			case 1: // y = max
				if (bb.maxY < y + 1 - EPS) return false;
				return coversHalf(bb.minX, bb.maxX, x, half1) && coversHalf(bb.minZ, bb.maxZ, z, half2);
			case 2: // z = min
				if (bb.minZ > z + EPS) return false;
				return coversHalf(bb.minX, bb.maxX, x, half1) && coversHalf(bb.minY, bb.maxY, y, half2);
			case 3: // z = max
				if (bb.maxZ < z + 1 - EPS) return false;
				return coversHalf(bb.minX, bb.maxX, x, half1) && coversHalf(bb.minY, bb.maxY, y, half2);
			case 4: // x = min
				if (bb.minX > x + EPS) return false;
				return coversHalf(bb.minZ, bb.maxZ, z, half1) && coversHalf(bb.minY, bb.maxY, y, half2);
			default: // x = max
				if (bb.maxX < x + 1 - EPS) return false;
				return coversHalf(bb.minZ, bb.maxZ, z, half1) && coversHalf(bb.minY, bb.maxY, y, half2);
		}
	}

	/** 盒在面内轴上覆盖该轴的前半（false）/后半（true）半格区间。 */
	private static boolean coversHalf(double min, double max, int cellCoord, boolean upperHalf) {
		double q0 = cellCoord + (upperHalf ? 0.5D : 0.0D);
		double q1 = q0 + 0.5D;
		return min <= q0 + EPS && max >= q1 - EPS;
	}

	private static final double EPS = 1.0E-7D;

	/** MixinBlockDynamicLiquid 喂养重定向用：采样格 (x,y,z) 朝 side 侧的完整面判定。 */
	public static boolean isCellFaceComplete(World world, int x, int y, int z, int side) {
		return isFaceCovered(getHostCollisionBoxes(world, x, y, z), x, y, z, side);
	}

	/** dir（0=x-1, 1=x+1, 2=z-1, 3=z+1）→ Forge 侧面号（4=西, 5=东, 2=北, 3=南）。 */
	private static int sideOfDir(int dir) {
		return dir == 0 ? 4 : dir == 1 ? 5 : dir == 2 ? 2 : 3;
	}

	/**
	 * 目标格成源判定（26.2 {@code getNewLiquid} / 1.7.10 BlockDynamicLiquid
	 * L72-82 语义）：水平 ≥2 个源（含水家族邻居是虚拟源，计入）+ 下方固体或
	 * 源水。3c"无限水成源自动含水"的 target-centric 判定：真实水流向干家族
	 * 格蔓延时，若该格在空气中本会成源，则含水家族方块直接获得含水位。
	 */
	public static boolean wouldFormSource(World world, int x, int y, int z) {
		List<AxisAlignedBB> hostBoxes = getHostCollisionBoxes(world, x, y, z);
		int sources = 0;
		for (int dir = 0; dir < 4; ++dir) {
			int nx = dir == 0 ? x - 1 : dir == 1 ? x + 1 : x;
			int nz = dir == 2 ? z - 1 : dir == 3 ? z + 1 : z;
			// 26.2 getNewLiquid L174：邻居水须穿过本格朝向该邻居的面才参与喂养/源计数
			// ——干楼梯背面贴着的源水不计数
			if (isFaceComplete(hostBoxes, x, y, z, sideOfDir(dir))) {
				continue;
			}
			if (isSourceCell(world, nx, y, nz)) {
				++sources;
			}
		}
		if (sources < 2) {
			return false;
		}
		Block below = world.getBlock(x, y - 1, z);
		return below.getMaterial().isSolid() || isSourceCell(world, x, y - 1, z);
	}

	/**
	 * 源格判定：静态水 meta 0、含水家族格（内部虚拟源）、或海洋植物格（26.2
	 * getFluidState 恒为 getSource(false) 源水——海草/海带的 meta 是株龄/半位，
	 * 不能用 meta==0 判源，须按 block 身份；枯死海泡菜是 Material.coral 天然排除）。
	 */
	public static boolean isSourceCell(World world, int x, int y, int z) {
		Block block = world.getBlock(x, y, z);
		return block.getMaterial() == Material.water && world.getBlockMetadata(x, y, z) == 0
				|| BlockAquaticPlant.isOceanPlant(block)
				|| WaterlogRuntime.isWaterlogged(world, x, y, z);
	}

	/**
	 * 26.2 updateShape→scheduleTick 的调度复刻：邻居变化/成为含水位后延迟
	 * 一个流体 tick 执行蔓延。调度去重由 WorldServer 的 NextTickListEntry
	 * isEqualTo 校验兜底；仅服务端调用。
	 */
	public static void scheduleSpread(World world, int x, int y, int z, Block hostBlock) {
		if (!world.isRemote) {
			WaterlogRuntime.debug("SCHEDULE " + x + "," + y + "," + z);
			world.scheduleBlockUpdate(x, y, z, hostBlock, WATER_TICK_RATE);
		}
	}

	/**
	 * 家族方块的调度 tick 统一入口（MixinBlockStairs / MixinBlockSlab 的
	 * updateTick 都走这里）：含水 → 蔓延一步；未含水 → 3c 的延迟成源重查
	 * （5gt 前的流尝试调度的重查，水源移走则保持干），含水后再延 5gt 蔓延。
	 */
	public static void handleFamilyTick(World world, int x, int y, int z, Block host) {
		if (world.isRemote) {
			return;
		}
		if (WaterlogRuntime.isWaterlogged(world, x, y, z)) {
			WaterlogRuntime.debug("SPREAD " + x + "," + y + "," + z);
			spreadFromSource(world, x, y, z);
			return;
		}
		if (wouldFormSource(world, x, y, z)) {
			WaterlogRuntime.markWaterlogged(world, x, y, z);
			WaterlogRuntime.debug("SOURCE-FORM " + x + "," + y + "," + z);
			scheduleSpread(world, x, y, z, host); // 含水后再延 5gt 蔓延
		}
	}

	/**
	 * 挖掘含水方块留下水源格（26.2 {@code Level.destroyBlock} =
	 * {@code setBlock(pos, fluidState.createLegacyBlock(), 3)}——含水方块的
	 * FluidState 是源水，破坏与留水在同一 setBlock 原子完成）。GTNH forge 的
	 * removedByPlayer 双载都要走这里：6 参 = 服务端挖掘路径
	 * （ItemInWorldManager.removeBlock(IIIZ)Z）；5 参 = 客户端挖掘预测
	 * （PlayerControllerMP.onPlayerDestroyBlock 直接调用，Block 默认
	 * setBlockToAir）——漏掉 5 参则客户端预测先画空气，等服务端水包到达才
	 * 回填，肉眼可见"空气闪一下"（2026-10-07 用户实证）。
	 *
	 * @return true = 已写水源格，调用方直接 return true；false = 未含水，走原逻辑
	 */
	public static boolean leaveSourceWater(World world, int x, int y, int z) {
		if (WaterlogRuntime.isWaterlogged(world, x, y, z)) {
			world.setBlock(x, y, z, Blocks.water, 0, 3);
			WaterlogRuntime.debug("DIG-SOURCE " + x + "," + y + "," + z);
			return true;
		}
		return false;
	}

	/**
	 * 源格蔓延一步（调用方保证该格当前含水且在服务端线程）。对应
	 * {@code BlockDynamicLiquid.updateTick} 的 L117-168 段：下方可流入 →
	 * falling（meta 8）；否则按 vanilla 权重寻路横流（meta 1）。
	 */
	public static void spreadFromSource(World world, int x, int y, int z) {
		WaterlogRuntime.debug("SPREAD " + x + "," + y + "," + z);
		MixinBlockDynamicLiquid flow = (MixinBlockDynamicLiquid) Blocks.flowing_water;
		List<AxisAlignedBB> hostBoxes = getHostCollisionBoxes(world, x, y, z);

		// 26.2 isWaterHole：下行先过宿主底面完整面门控——楼梯/下半砖底面完整时不下行，
		// 落回横向蔓延（vanilla updateTick 的下行受阻分支）
		if (!isFaceComplete(hostBoxes, x, y, z, 0) && flow.etfuturum$canFlowInto(world, x, y - 1, z)) {
			flow.etfuturum$flowInto(world, x, y - 1, z, 8); // 源 l=0 → 下方 falling（l + 8）
			return;
		}
		// 源 l == 0：横向蔓延（vanilla else-if 的 l == 0 分支），逐方向过宿主完整面门控
		//（26.2 spreadToSides：完整面不流水，楼梯背面/半砖底面朝向的邻格不出水）
		boolean[] dirs = flow.etfuturum$findFlowDirections(world, x, y, z);
		for (int dir = 0; dir < 4; ++dir) {
			if (isFaceComplete(hostBoxes, x, y, z, sideOfDir(dir))) {
				continue; // 完整面不出水也不传导含水
			}
			int nx = dir == 0 ? x - 1 : dir == 1 ? x + 1 : x;
			int nz = dir == 2 ? z - 1 : dir == 3 ? z + 1 : z;
			Block neighbor = world.getBlock(nx, y, nz);
			if (WaterlogFamilies.isWaterloggable(neighbor) && !WaterlogRuntime.isWaterlogged(world, nx, y, nz)) {
				// 26.2 spreadToSides 扩散进相邻可含水格：仅当目标格 getNewLiquid 成源
				//（getSpread 传源流体 → canPlaceLiquid(WATER) 通过 → waterlogged）。
				// 本实现等价 = flowInto 触发 HEAD 调度，5gt 后 wouldFormSource 重查
				// 通过即含水——对角双源的楼梯圈由此 4 格全含水（用户场景）
				flow.etfuturum$flowInto(world, nx, y, nz, 1);
				continue;
			}
			if (!dirs[dir]) {
				continue;
			}
			flow.etfuturum$flowInto(world, nx, y, nz, 1);
		}
	}

	/**
	 * 26.2 ExplosionDamageCalculator#getBlockExplosionResistance 的
	 * {@code max(方块抗性, 流体抗性)} 语义：含水家族格与水生植物格的
	 * FluidState 是水（WaterFluid 抗性 100），爆炸射线每步烧掉
	 * {@code (100 + 0.3) * 0.3 ≈ 30} 点威力——TNT 级（~5 点）一格就停，
	 * 含水方块进不了破坏名单（"含水墙防爆"），海草/海带/湿海泡菜同理。
	 * 1.7.10 {@code doExplosionA} 的采样只有宿主抗性，此处补齐 fluid 那一半；
	 * 真实水/岩浆方块自身抗性已是 100，原值返回不变。
	 */
	public static float explosionResistance(World world, int x, int y, int z, Block block, float hostResistance) {
		float waterResistance = Blocks.water.getExplosionResistance((Entity) null);
		if (hostResistance >= waterResistance || !WaterlogRuntime.isEnabled()) {
			return hostResistance;
		}
		if (WaterlogRuntime.isWaterlogged(world, x, y, z) || BlockAquaticPlant.isOceanPlant(block)) {
			WaterlogRuntime.debug("EXPL-BOOST " + x + "," + y + "," + z + " id=" + Block.getIdFromBlock(block)
					+ " " + hostResistance + " -> " + waterResistance);
			return waterResistance;
		}
		if (WaterlogFamilies.isWaterloggable(block)) {
			WaterlogRuntime.debug("EXPL-SAMPLE-DRY " + x + "," + y + "," + z + " id=" + Block.getIdFromBlock(block)
					+ " meta=" + world.getBlockMetadata(x, y, z) + " " + hostResistance);
		}
		return hostResistance;
	}
}
