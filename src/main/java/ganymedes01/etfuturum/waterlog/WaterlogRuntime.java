package ganymedes01.etfuturum.waterlog;

import cpw.mods.fml.common.Loader;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * 真含水运行时谓词（plan §3.1）。编码：{@code waterloggedMeta = baseMeta | 0x10}，
 * 低 4 位永远保留原版语义；bit16 的持久化（区块存储/NBT/网络）完全依赖
 * NotEnoughIDs 的 16-bit meta 补丁，无任何编译依赖。
 *
 * <p>门控 = NEIDs 已装 && {@code disableWaterlogging=false}，二者缺一不可；
 * 门控关闭时所有判定短路返回 false → 零行为、零渲染、零写入，旧存档中
 * 的 bit16 方块自动按干方块表现（nibble {@code &15} 完美退化），无需迁移。
 */
public final class WaterlogRuntime {

	public static final int WATERLOGGED_BIT = 0x10;

	/**
	 * 测试期管线日志总闸（放置捕获/写入、pass-1 门控、画水结果探针）。
	 * 惯例：联调新管线（Angelica/扩族）时临时开，验收后关闭；含水系列
	 * 全部收尾时连同探针一并清理。
	 */
	public static final boolean WATERLOG_DEBUG = false;

	public static void debug(String msg) {
		if (WATERLOG_DEBUG) {
			System.out.println("[EtFuturumWaterlog] " + msg);
		}
	}

	/**
	 * 放置链路的"目标格放置前是水源"标记：MixinItemBlock 在 onBlockPlaced
	 * 调用点捕获（此时格子还是水），MixinBlockStairs 在 onBlockPlacedBy TAIL
	 * 消费（metadata 重写完成）。线程隔离：客户端预测与服务端执行互不串扰。
	 */
	private static final ThreadLocal<Boolean> placementSource = new ThreadLocal<>();

	private static boolean enabled;
	private static boolean resolved;

	private WaterlogRuntime() {}

	/** 配置不能游戏中途变更，首次调用后缓存（客户端渲染线程与服务端线程并发首调结果是确定的，无需同步）。 */
	public static boolean isEnabled() {
		if (!resolved) {
			enabled = Loader.isModLoaded("neid") && !ConfigMixins.disableWaterlogging;
			resolved = true;
		}
		return enabled;
	}

	public static boolean isWaterlogged(IBlockAccess world, int x, int y, int z) {
		if (!isEnabled()) {
			return false;
		}
		Block block = world.getBlock(x, y, z);
		return WaterlogFamilies.isWaterloggable(block) && (world.getBlockMetadata(x, y, z) & WATERLOGGED_BIT) != 0;
	}

	/**
	 * 26.2 只认精确水源（{@code is(Fluids.WATER)}）：1.7.10 对应静态水
	 * Blocks.water 且 meta 0（流水 meta 1-7/跌水 meta 8 不算——26.2 中放置进
	 * 真流水同样先干、由流体 tick 回填，与成源重查延迟语义一致）。
	 *
	 * <p>例外：{@code BlockDynamicLiquid} meta 0 是 1.7.10 特有的"满格流水
	 * 沉降等待"态——两股源水收敛先写入 Dynamic meta 0，下一流体 tick 才沉降
	 * 为静态源；26.2 收敛直接写 source state，无此中间态。连放时目标格恰好
	 * 处于该窗口会误判干放（2026-10-07 日志实证：CAPTURE cell=
	 * BlockDynamicLiquid/0 -> wasSource=false → 干板可见约 1s 后才
	 * SOURCE-FORM），按 26.2 等价语义视作源。视觉上满格流水与静态源同为
	 * 8/9 水面，无法区分；沉降后本就是源。
	 */
	public static boolean isSourceWater(IBlockAccess world, int x, int y, int z) {
		Block block = world.getBlock(x, y, z);
		return (block == Blocks.water || block == Blocks.flowing_water) && world.getBlockMetadata(x, y, z) == 0;
	}

	/** 写入含水位。只允许在 {@link #isEnabled()} 为真时调用（无 NEIDs 时写入会被 nibble 静默截断，禁止走到）。 */
	public static void markWaterlogged(World world, int x, int y, int z) {
		world.setBlockMetadataWithNotify(x, y, z, world.getBlockMetadata(x, y, z) | WATERLOGGED_BIT, 2);
	}

	/** 放置捕获：无条件覆盖（自动清除上次 placeBlockAt 失败残留的标记）。 */
	public static void capturePlacementSource(boolean wasSource) {
		if (wasSource) {
			placementSource.set(Boolean.TRUE);
		} else {
			placementSource.remove();
		}
	}

	/** 放置写入端：取出并清空捕获标记。 */
	public static boolean consumePlacementSource() {
		Boolean wasSource = placementSource.get();
		placementSource.remove();
		return Boolean.TRUE.equals(wasSource);
	}

	/**
	 * 放置捕获的前置检查（只读不消费）：{@code onBlockPlacedBy} 等在捕获与
	 * RETURN 消费之间执行的 meta 重写钩子用它并回 bit16，保证原子含水放置。
	 */
	public static boolean isPlacementSource() {
		return Boolean.TRUE.equals(placementSource.get());
	}
}
