package ganymedes01.etfuturum.waterlog;

import cpw.mods.fml.common.Loader;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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

	/** 构建标签：日志中核对运行中的 jar 是否为本次构建（防旧 jar 误测）。 */
	public static final String BUILD_TAG = "20261020-A";

	// ===== 20261013-J 倒水/舀水闪烁追踪：玩家事件坐标广播 =====
	// Angelica 的 EtfrFlashProbe 每客户端 tick 反射轮询（debugTrackSeq 变化 = 新
	// 事件）→ 锁定该格并开 60 tick 观察窗，逐 tick 记录该格区块重建有没有画水。
	// 写序铁律：坐标先写、seq 最后写（volatile 写序），读方用 seq 前后一致做快照校验。
	// 客户端预测与服务端各写一次同格（序号 +2），Angelica 侧同格同类事件只续窗不重置。
	public static final int TRACK_POUR = 0;
	public static final int TRACK_SOAK = 1;

	public static volatile int debugTrackX;
	public static volatile int debugTrackY;
	public static volatile int debugTrackZ;
	public static volatile int debugTrackKind;
	public static volatile long debugTrackTick;
	public static volatile long debugTrackSeq;

	/** 玩家倒水/舀水成功路径调用（客户端预测与服务端都算，幂等）。 */
	public static void debugTrack(World world, int kind, int x, int y, int z) {
		debugTrackX = x;
		debugTrackY = y;
		debugTrackZ = z;
		debugTrackKind = kind;
		debugTrackTick = world.getTotalWorldTime();
		debugTrackSeq++; // 最后写
		debug("TRACK-ARM kind=" + (kind == TRACK_POUR ? "POUR" : "SOAK") + " " + x + "," + y + "," + z
				+ (world.isRemote ? " client" : " server") + " tick=" + debugTrackTick);
	}

	// ===== 20261018-A 客户端预测挂起 + 迟到回声过滤（26.2 BlockStatePredictionHandler
	// 轻量同构，无 ack 包，10 tick 超时替代）=====
	// 竞态根因：倒水/舀水时客户端瞬时预测写入，而服务端更早写入 flush 的 S23/S22（回声，
	// 恰好等于预测前状态）在 1-2 tick 后到达，无条件覆写预测 → DRY→WET→DRY 闪 2 tick，
	// 随后确认包 S23(预测态) 修回。TCP 有序 ⇒ 回声必先于确认包。
	// 语义：预测时登记 {坐标 → (预测前 block,meta), 过期 tick}；S23/S22 到包时该格有
	// 未过期登记 且 到来状态==预测前状态 → 丢弃该包/该格（保留登记等确认包）；否则照常
	// 应用并消费登记。超时自动失效回归原版。保守性：只丢"恰好还原预测前状态"的回声。
	// 纯 long/int 存储，无 client 类依赖；登记/过滤都过 isEnabled() 短路，功能关闭零行为。
	//
	// ===== 20261020-P 重写为 26.2 窗口收敛语义（预测状态、不比对内容）=====
	// 上版"内容比对"有两处实证缺陷：①空气格倒水的回声（==pre）被丢——但海绵吸水等
	// 服务端"把格子清回空气"的权威写入同样是 air，被误吞 → 客户端永久残水；②反过来
	// 不登记空气格则回声直接应用 → 空地倒水闪。26.2 BlockStatePredictionHandler 的
	// 真实语义（prediction/BlockStatePredictionHandler.java）不做任何内容比对：
	// retainKnownServerState 登记预测序号 → updateKnownServerState（S23/S22 到达）
	// 只更新 pending、绝不应用 → endPredictionsUpTo（ack 到达）把最后到达的权威状态
	// 一次性 syncBlockState。即"窗口内一切包延迟、窗口末应用最后一条 = 服务端终态"。
	// 1.7.10 无 ack 包，用 settle 定时近似：首个包到达时 settle=now+2（海绵吸水与
	// 倒水确认在服务端同一 tick 顺序发出，+2 足以收齐），后续包顺延 max(settle,now+1)；
	// 与 ping 无关（ping 只影响首包到达时刻，收敛窗从首包起算）。TTL 仅作断线兜底，
	// 过期静默移除（pending 不应用，下一条包回归原版行为）。
	// 应用由 MixinWorldClient.tick → pollSettledPredictions 驱动（client 包，common
	// 侧只交换 int[]，无 client 类依赖）。登记/过滤都过 isEnabled() 短路，关闭零行为。
	public static final int PREDICTION_TTL_TICKS = 60;
	private static final int SETTLE_FIRST_PACKET_DELAY = 2;
	private static final int SETTLE_NEXT_PACKET_DELAY = 1;

	private static final Map<Long, Prediction> pendingPredictions = new HashMap<>();

	private static final class Prediction {
		final int preId;
		final int preMeta;
		final long expireTick;
		int pendingId;
		int pendingMeta;
		long settleTick = -1L;

		Prediction(int preId, int preMeta, long expireTick) {
			this.preId = preId;
			this.preMeta = preMeta;
			this.pendingId = preId;
			this.pendingMeta = preMeta;
			this.expireTick = expireTick;
		}
	}

	/** 26.2 BlockPos.asLong 的打包惯例（y 12 位足够 1.7.10 的 0-255）。 */
	private static long packCoord(int x, int y, int z) {
		return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (long) (y & 0xFFF);
	}

	private static int unpackX(long packed) {
		int x = (int) (packed >> 38);
		return (x & (1 << 25)) != 0 ? x - (1 << 26) : x;
	}

	private static int unpackZ(long packed) {
		int z = (int) ((packed << 26) >> 38);
		return (z & (1 << 25)) != 0 ? z - (1 << 26) : z;
	}

	private static int unpackY(long packed) {
		return (int) (packed & 0xFFF);
	}

	/**
	 * 客户端预测写入前登记（26.2 retainKnownServerState 同构）。调用方（桶路径/
	 * MarkWaterlogged）须守卫 {@code world.isRemote}；本方法只做 isEnabled() 短路。
	 * 包处理与游戏逻辑同在客户端主线程，synchronized 仅为防御性兜底，无性能影响。
	 */
	public static void recordPrediction(int x, int y, int z, int blockId, int meta, long nowTick) {
		if (!isEnabled()) {
			return;
		}
		final long expireTick = nowTick + PREDICTION_TTL_TICKS;
		synchronized (pendingPredictions) {
			// 登记点远少于消费点的格子可能滞留过期记录，超阈值时顺手清扫（有界防泄漏）
			if (pendingPredictions.size() > 256) {
				final Iterator<Map.Entry<Long, Prediction>> it = pendingPredictions.entrySet().iterator();
				while (it.hasNext()) {
					if (it.next().getValue().expireTick < nowTick) {
						it.remove();
					}
				}
			}
			pendingPredictions.put(packCoord(x, y, z), new Prediction(blockId, meta, expireTick));
		}
		debug("PRED-REC " + x + "," + y + "," + z + " pre=" + blockId + "/" + meta + " expire=" + expireTick);
	}

	/**
	 * S23/S22 到包时的窗口处理（26.2 updateKnownServerState 同构，客户端包处理侧
	 * 调用）。返回 true 表示该包/该格延迟应用（由 pollSettledPredictions 收敛时应用）：
	 * 窗口内不比对内容、一律吞下并更新 pending——回声与权威写入在窗口内不可区分，
	 * 也无需区分，窗口末应用最后一条即服务端终态。
	 */
	public static boolean tryFilter(int x, int y, int z, int blockId, int meta, long nowTick) {
		if (!isEnabled() || pendingPredictions.isEmpty()) {
			return false;
		}
		synchronized (pendingPredictions) {
			final Long key = packCoord(x, y, z);
			final Prediction p = pendingPredictions.get(key);
			if (p == null) {
				return false;
			}
			if (nowTick > p.expireTick) {
				// 超时：回归原版无条件应用
				pendingPredictions.remove(key);
				debug("PRED-EXPIRE " + x + "," + y + "," + z + " pre " + p.preId + "/" + p.preMeta + " tick=" + nowTick);
				return false;
			}
			p.pendingId = blockId;
			p.pendingMeta = meta;
			// 首包启动收敛窗（+2 收齐同 tick 发出的回声+确认对），后续包顺延 1 tick
			p.settleTick = p.settleTick < 0L ? nowTick + SETTLE_FIRST_PACKET_DELAY : Math.max(p.settleTick, nowTick + SETTLE_NEXT_PACKET_DELAY);
			debug("PRED-HOLD " + x + "," + y + "," + z + " pkt=" + blockId + "/" + meta + " settle=" + p.settleTick + " tick=" + nowTick);
			return true;
		}
	}

	/**
	 * 26.2 endPredictionsUpTo 同构（MixinWorldClient.tick 每客户端 tick 驱动）：
	 * 取出收敛期已到的登记，交由调用方应用 pending 终态；TTL 过期的静默移除
	 * （不应用，下一条包回归原版行为）。返回 {x, y, z, blockId, meta} 数组，
	 * 纯 int 交换，无 client 类依赖。
	 */
	public static int[][] pollSettledPredictions(long nowTick) {
		if (!isEnabled() || pendingPredictions.isEmpty()) {
			return new int[0][];
		}
		ArrayList<int[]> settled = null;
		synchronized (pendingPredictions) {
			final Iterator<Map.Entry<Long, Prediction>> it = pendingPredictions.entrySet().iterator();
			while (it.hasNext()) {
				final Map.Entry<Long, Prediction> e = it.next();
				final Prediction p = e.getValue();
				if (nowTick > p.expireTick) {
					it.remove();
					continue;
				}
				if (p.settleTick >= 0L && nowTick >= p.settleTick) {
					it.remove();
					final long packed = e.getKey();
					if (settled == null) {
						settled = new ArrayList<>();
					}
					settled.add(new int[] { unpackX(packed), unpackY(packed), unpackZ(packed), p.pendingId, p.pendingMeta });
					debug("PRED-SETTLE " + unpackX(packed) + "," + unpackY(packed) + "," + unpackZ(packed)
							+ " -> " + p.pendingId + "/" + p.pendingMeta + " (pre " + p.preId + "/" + p.preMeta + ") tick=" + nowTick);
				}
			}
		}
		return settled == null ? new int[0][] : settled.toArray(new int[0][]);
	}

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
		if (world.isRemote) {
			// 20261018-A 客户端预测登记：bit16 置位是瞬时预测写，记下预测前的干状态
			// 供包侧回声过滤（family 直接点击倒水 + 邻面倒入 + 含水放置共用本口）
			recordPrediction(x, y, z, Block.getIdFromBlock(block), oldMeta, world.getTotalWorldTime());
		}
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
