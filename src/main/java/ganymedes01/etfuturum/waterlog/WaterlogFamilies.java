package ganymedes01.etfuturum.waterlog;

import ganymedes01.etfuturum.blocks.BlockChain;
import ganymedes01.etfuturum.blocks.BlockLantern;
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
 *
 * <p>提灯（26.2 LanternBlock implements SimpleWaterloggedBlock）：WATERLOGGED
 * 完整支持（getStateForPlacement L53-61 按 replacedFluidState 判定、updateShape
 * L84-92 含水 scheduleTick 蔓延 + 支撑丢失 → AIR 丢水、lightLevel 15 与含水
 * 无关）→ 26.2 忠实对象。EtFR 自有方块（普通 + 灵魂提灯都由 {@code BlockLantern}
 * 承载，{@code BlockSeaLantern} 非其子类不误伤）：1.7.10 meta 只有挂位
 * （0 = 立于地面 / 1 = 吊挂），ISBRH 渲染器（RenderIDs.LANTERN）{@code % 2}
 * 读挂位对 bit16 天然透明，getIcon 未覆写单贴图；生命周期（onNeighborBlockChange
 * 支撑移除掉落 + 丢水保留原体、挖掘留水、蔓延调度、updateTick）走混合模式
 * mixin（声明方法 @Inject HEAD + 未声明方法 extends + @Override），裸 meta 比较
 * （setBlockBoundsBasedOnState 挂位 {@code == 0}、canPlaceAbove 邻居链/半砖/
 * 楼梯 meta 比较）在源码剥位。物品侧 damageDropped 直通 meta 但提灯为单变体
 * 物品（damage 无害）→ 不剥。
 *
 * <p>锁链（26.2 ChainBlock extends RotatedPillarBlock implements
 * SimpleWaterloggedBlock）：WATERLOGGED 完整支持（getStateForPlacement L55-59
 * 按 replacedFluidState 判定、updateShape L62-67 含水 scheduleTick 蔓延、
 * 无 canSurvive 支撑要求）→ 26.2 忠实对象。1.7.10 meta 0/1/2 = 轴向
 * （Y/X/Z）：碰撞箱、边界与 ISBRH 渲染器（RenderIDs.CHAIN）的 switch 裸判
 * 已在源码剥位 {@code & 15}——default 分支 = Y 轴，含水横链 meta 17/18 裸判
 * 会竖起来；无任何生命周期方法声明 →
 * extends + @Override 四件套（onNeighborBlockChange 蔓延调度、updateTick、
 * removedByPlayer 双载、damageDropped 剥位——锁链同为单变体物品但按家族
 * 铁律审计剥位）。
 */
public final class WaterlogFamilies {

	private WaterlogFamilies() {}

	public static boolean isWaterloggable(Block block) {
		return block instanceof BlockPane
				|| block instanceof BlockFence
				|| block instanceof BlockWall
				|| block instanceof BlockLadder
				|| block instanceof BlockTrapDoor
				|| block instanceof BlockLantern
				|| block instanceof BlockChain
				|| (block instanceof BlockStairs || block instanceof BlockSlab) && !block.isOpaqueCube();
	}
}
