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
	public static final boolean WATERLOG_DEBUG = true;

	/** 构建标签：日志中核对运行中的 jar 是否为本次构建（防旧 jar 误测）。 */
	public static final String BUILD_TAG = "20261013-D";

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
			boolean neid = Loader.isModLoaded("neid");
			enabled = neid && !ConfigMixins.disableWaterlogging;
			resolved = true;
			debug("isEnabled resolved: neid=" + neid + " disableWaterlogging=" + ConfigMixins.disableWaterlogging
					+ " -> " + enabled + " build=" + BUILD_TAG);
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
	 * 放置含水判定（26.2 getStateForPlacement 的 {@code getFluidState(pos).is(Fluids.WATER)}）：
	 * <b>WaterFluid.isSame 覆写为"源与流水同属水"</b>（rfg 佐证：FlowingFluid 内部
	 * 全部 {@code getType().isSame(this)}）→ 任何水（静态/流水 meta 1-7/满格流水
	 * 沉降窗）均算——旧实现只认 meta 0，一格流水坑放置活板门不含水（2026-10-10
	 * 用户实证）。只认水体方块本体（含水植物格是虚拟水，不在放置语义内）。
	 */
	public static boolean isWaterCell(IBlockAccess world, int x, int y, int z) {
		Block block = world.getBlock(x, y, z);
		return block == Blocks.water || block == Blocks.flowing_water;
	}

	/** 写入含水位。只允许在 {@link #isEnabled()} 为真时调用（无 NEIDs 时写入会被 nibble 静默截断，禁止走到）。 */
	public static void markWaterlogged(World world, int x, int y, int z) {
		Block block = world.getBlock(x, y, z);
		int oldMeta = world.getBlockMetadata(x, y, z);
		world.setBlockMetadataWithNotify(x, y, z, oldMeta | WATERLOGGED_BIT, 2);
		debug("MARK " + x + "," + y + "," + z + " " + (world.isRemote ? "client" : "server")
				+ " " + block.getClass().getSimpleName() + " meta " + oldMeta + " -> " + (oldMeta | WATERLOGGED_BIT));
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
