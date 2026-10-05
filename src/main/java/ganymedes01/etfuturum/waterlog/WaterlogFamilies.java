package ganymedes01.etfuturum.waterlog;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStairs;

/**
 * 含水家族注册表（plan §3.3）：按基类 instanceof 判定可含水的方块，
 * 模组子类（模组楼梯等）自动继承，与上代伪含水行为一致。
 * 楼梯/半砖等 vanilla 逻辑只用低 4 位掩码（{@code &3}/{@code &4}/{@code &7}），
 * bit16（0x10）对家族方块的原版语义天然透明。
 */
public final class WaterlogFamilies {

	private WaterlogFamilies() {}

	/** Phase 1 只做楼梯；半砖（排除双层）/玻璃板/铁栏杆/栅栏/栅栏门/梯子/活板门在扩族阶段追加。 */
	public static boolean isWaterloggable(Block block) {
		return block instanceof BlockStairs;
	}
}
