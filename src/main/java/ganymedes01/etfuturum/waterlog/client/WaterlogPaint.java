package ganymedes01.etfuturum.waterlog.client;

/**
 * pass-1 画水调度（vanilla 管线 = MixinWorldRenderer 门控；celeritas 管线 =
 * MixinAngelicaChunkBuilderMeshingTask 门控）：
 * <ol>
 * <li>门控为含水家族格打开 pass-1 槽位，并在 renderBlock 内、紧邻
 * renderBlockByRenderType 之前置位/捕获；</li>
 * <li>挂点（MixinRenderBlocks 在 renderBlockByRenderType HEAD）消费，
 * 用整格静水画水替代第二份方块几何拷贝（方块本体已在 pass 0 画过）。</li>
 * </ol>
 * 两个 ThreadLocal：celeritas 并发构建线程各自置位/消费互不串扰；两条管线
 * 的置位互斥（plugin 按 angelicaLoaded 二选一注册）。
 */
public final class WaterlogPaint {

	private static final ThreadLocal<Boolean> PAINT_NEXT_CALL = ThreadLocal.withInitial(() -> Boolean.FALSE);

	/** celeritas 管线 renderBlock 内捕获的当前 pass（该线程最近一次 renderBlock 的 pass）。 */
	private static final ThreadLocal<Integer> CELERITAS_PASS = new ThreadLocal<>();

	private WaterlogPaint() {}

	/** 门控点置位；与消费点在同一次 renderBlock 内、同线程相邻执行。 */
	public static void flagNextCall() {
		PAINT_NEXT_CALL.set(Boolean.TRUE);
	}

	/** 门控点防御性复位（每次门控判定清残留）。 */
	public static void clearFlag() {
		PAINT_NEXT_CALL.set(Boolean.FALSE);
	}

	/** 挂点快速预检：已置位才取消 vanilla 几何（含全剔除时的空返回）。 */
	public static boolean isFlagged() {
		return PAINT_NEXT_CALL.get();
	}

	/** celeritas renderBlock 内 canRenderInPass 旁路捕获 pass（纯捕获，行为不变）。 */
	public static void captureCeleritasPass(int pass) {
		CELERITAS_PASS.set(pass);
	}

	/** 当前线程正处于 celeritas pass 1 的 renderBlock 中。 */
	public static boolean isCeleritasPassOne() {
		Integer pass = CELERITAS_PASS.get();
		return pass != null && pass == 1;
	}
}
