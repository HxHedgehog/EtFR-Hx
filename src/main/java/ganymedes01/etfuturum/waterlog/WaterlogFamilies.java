package ganymedes01.etfuturum.waterlog;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.BlockWall;

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
 * <p>栅栏（26.2 FenceBlock extends CrossCollisionBlock——与玻璃板/铁栏杆同基类）：
 * WATERLOGGED 完整支持（getStateForPlacement L96 按 replacedFluidState 判定、
 * updateShape L101-103 含水 scheduleTick 蔓延）→ 26.2 忠实对象；1.7.10 无 meta
 * （连接状态靠邻居查询），EtFR 木栅栏/新木栅栏子类自动继承。注意 26.2 的
 * FenceGateBlock（栅栏门）state definition 只有 FACING/OPEN/POWERED/IN_WALL、
 * **无 WATERLOGGED 属性 → 26.2 栅栏门不可含水**，不在本家族（用户决策项）。
 * <p>梯子/活板门（26.2 LadderBlock/TrapDoorBlock）：WATERLOGGED 完整支持
 * （放置判定、updateShape/neighborChanged 蔓延调度）→ 26.2 忠实对象；1.7.10
 * 梯子 meta = 朝向 2-5、活板门 meta = 朝向&3 + bit2 开 + bit3 上/下半，均无
 * 清 bit16 的重写（开关为 XOR/AND/OR 位操作）；两者的 onNeighborBlockChange
 * 为支撑移除逻辑（掉落 + setBlockToAir），mixin 必须 super 调用保留。
 *
 * <p>墙（26.2 WallBlock implements SimpleWaterloggedBlock）：WATERLOGGED 完整
 * 支持（getStateForPlacement L133 按 replacedFluidState 判定、updateShape
 * L139-141 含水 scheduleTick 蔓延、getFluidState L226）→ 26.2 忠实对象；
 * 1.7.10 连接状态靠 canConnectWallTo 邻居查询不存 meta（与栅栏同构），meta
 * 只有变体位（vanilla bit0 normal/mossy；BaseWall 为多变体取模），且无支撑
 * 判定、无 onNeighborBlockChange/updateTick/removedByPlayer 声明 → 与栅栏
 * 同款 extends + @Override 三件套。裸 meta 污染点已全部剥位：vanilla
 * getIcon/damageDropped（common/client mixin @Inject）、BaseWall 的
 * getIcon/damageDropped/getBlockHardness/getExplosionResistance/getMapColor
 * 取模与映射键（源码剥位——类型数不整除 16 时 bit16 会取模错位）、
 * BlockStoneWall/BlockBlackstoneWall getIcon 裸比较（源码剥位）。
 */
public final class WaterlogFamilies {

	private WaterlogFamilies() {}

	public static boolean isWaterloggable(Block block) {
		return block instanceof BlockPane
				|| block instanceof BlockFence
				|| block instanceof BlockWall
				|| block instanceof BlockLadder
				|| block instanceof BlockTrapDoor
				|| (block instanceof BlockStairs || block instanceof BlockSlab) && !block.isOpaqueCube();
	}
}
