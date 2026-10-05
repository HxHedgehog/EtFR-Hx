package ganymedes01.etfuturum.waterlog.client;

import ganymedes01.etfuturum.waterlog.WaterlogFamilies;
import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;

/**
 * vanilla 管线的 pass-1 画水调度（镜像上代伪含水架构，判定换成 bit16）：
 * <ol>
 * <li>门控（MixinWorldRenderer）为含水家族格打开 pass-1 槽位，并置
 * {@link #paintNextCall}；</li>
 * <li>挂点（MixinRenderBlocks）在 {@code renderBlockByRenderType} HEAD 消费
 * 该标志，用整格静水画水替代第二份方块几何拷贝（方块本体已在 pass 0 画过，
 * pass 1 再画一份只会产生冗余的不透明几何）。</li>
 * </ol>
 */
public final class WaterlogPaint {

	/** 门控与挂点之间的"下一个 renderBlockByRenderType 调用要画水"信号。 */
	public static boolean paintNextCall;

	private WaterlogPaint() {}

	/**
	 * 若该格是真含水家族方块则画整格静水水体并返回是否有内容（六面全剔除
	 * 时返回 false，pass 1 不得声明有内容）；否则返回 false 让调用方跳过
	 * pass-1 几何拷贝。
	 */
	public static boolean paintIfWaterlogged(IBlockAccess world, Block block, int x, int y, int z) {
		if (!WaterlogFamilies.isWaterloggable(block) || !WaterlogRuntime.isWaterlogged(world, x, y, z)) {
			return false;
		}
		return StairWaterRenderer.renderStillWaterVolume(world, x, y, z);
	}
}
