package ganymedes01.etfuturum.waterlog;

import net.minecraft.block.Block;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;

/**
 * 含水家族注册表（plan §3.3）：按基类 instanceof 判定可含水的方块，
 * 模组子类（模组楼梯、BaseSlab、GTNH 模组半砖等）自动继承，与上代伪含水
 * 行为一致。楼梯/半砖等 vanilla 逻辑只用低 4 位掩码（{@code &3}/{@code &4}
 * {@code &7}/{@code &8}），bit16（0x10）对家族方块的原版语义天然透明。
 *
 * <p>半砖（26.2 SlabBlock）：单双层是不同 Block 实例（double 构造参数
 * {@code field_150004_a = true} → {@code isOpaqueCube() == true}），双层是
 * 完整不透明方块、26.2 无 waterlogged 状态（合成 DOUBLE 时
 * {@code WATERLOGGED = false} 丢水）→ 用 {@code !isOpaqueCube()} 排除；
 * 单层的 {@code meta & 8} 是上/下半标志（BlockSlab.onBlockPlaced 的
 * {@code meta | 8}），与 bit16 独立——上/下半砖都可含水（26.2 SlabBlock
 * L86 对 TYPE=TOP 同样判定 waterlogged）。
 *
 * <p>玻璃板（26.2 PaneBlock）：无朝向/变体 meta（1.7.10 原版板 meta 0，
 * EtFR 染色板为独立 Block 实例）→ 无需排除项，也无 canSurvive 要求
 * （26.2 PaneBlock.canSurvive 恒真）；含水视觉 = 整格 8/9 静水体叠在
 * 薄板几何上（画水管线按本家族过滤，玻璃板自动生效）。
 */
public final class WaterlogFamilies {

	private WaterlogFamilies() {}

	public static boolean isWaterloggable(Block block) {
		return block instanceof BlockPane
				|| (block instanceof BlockStairs || block instanceof BlockSlab) && !block.isOpaqueCube();
	}
}
