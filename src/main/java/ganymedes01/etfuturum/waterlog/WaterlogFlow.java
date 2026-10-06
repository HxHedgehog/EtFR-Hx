package ganymedes01.etfuturum.waterlog;

import ganymedes01.etfuturum.blocks.BlockAquaticPlant;
import ganymedes01.etfuturum.mixins.early.oceanplants.MixinBlockDynamicLiquid;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

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
	 * 目标格成源判定（26.2 {@code getNewLiquid} / 1.7.10 BlockDynamicLiquid
	 * L72-82 语义）：水平 ≥2 个源（含水家族邻居是虚拟源，计入）+ 下方固体或
	 * 源水。3c"无限水成源自动含水"的 target-centric 判定：真实水流向干家族
	 * 格蔓延时，若该格在空气中本会成源，则含水家族方块直接获得含水位。
	 */
	public static boolean wouldFormSource(World world, int x, int y, int z) {
		int sources = 0;
		for (int dir = 0; dir < 4; ++dir) {
			int nx = dir == 0 ? x - 1 : dir == 1 ? x + 1 : x;
			int nz = dir == 2 ? z - 1 : dir == 3 ? z + 1 : z;
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
	 * 源格蔓延一步（调用方保证该格当前含水且在服务端线程）。对应
	 * {@code BlockDynamicLiquid.updateTick} 的 L117-168 段：下方可流入 →
	 * falling（meta 8）；否则按 vanilla 权重寻路横流（meta 1）。
	 */
	public static void spreadFromSource(World world, int x, int y, int z) {
		WaterlogRuntime.debug("SPREAD " + x + "," + y + "," + z);
		MixinBlockDynamicLiquid flow = (MixinBlockDynamicLiquid) Blocks.flowing_water;

		if (flow.etfuturum$canFlowInto(world, x, y - 1, z)) {
			flow.etfuturum$flowInto(world, x, y - 1, z, 8); // 源 l=0 → 下方 falling（l + 8）
			return;
		}
		// 源 l == 0：横向蔓延（vanilla else-if 的 l == 0 分支）
		boolean[] dirs = flow.etfuturum$findFlowDirections(world, x, y, z);
		for (int dir = 0; dir < 4; ++dir) {
			if (!dirs[dir]) {
				continue;
			}
			int nx = dir == 0 ? x - 1 : dir == 1 ? x + 1 : x;
			int nz = dir == 2 ? z - 1 : dir == 3 ? z + 1 : z;
			flow.etfuturum$flowInto(world, nx, y, nz, 1); // j1 = l + 1 = 1
		}
	}
}
