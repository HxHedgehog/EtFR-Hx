package ganymedes01.etfuturum.waterlog;

import ganymedes01.etfuturum.blocks.BlockChain;
import ganymedes01.etfuturum.blocks.BlockCopperGrate;
import ganymedes01.etfuturum.blocks.BlockLantern;
import ganymedes01.etfuturum.blocks.BlockScaffolding;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockEnderChest;
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
 *
 * <p>铜格栅（26.2 CopperGrateBlock extends WaterloggedTransparentBlock
 * implements SimpleWaterloggedBlock）：WATERLOGGED 完整支持（getStateForPlacement
 * L45-48 按 replacedFluidState 判定、updateShape L51-56 含水 scheduleTick 蔓延、
 * getFluidState L59-64 源水）→ 26.2 忠实对象。EtFR 自有方块 → 全部源码级
 * 改动无 mixin：① 裸 meta 剥位 = {@code getCopperMeta}/{@code getFinalCopperMeta}
 * 的 {@code meta > 3} 氧化阶段判定（bit16 直通 → 铜 meta 20-27，氧化/打蜡
 * 全失真且含水格栅永不氧化）；② {@code setCopperBlock} 源码覆写保留 bit16
 * （氧化/打蜡 setBlock 重写 meta 会清含水，26.2 属性保留语义）；③ 生命周期
 * 四件套走源码覆写（onNeighborBlockChange 蔓延调度、updateTick = 家族
 * handleFamilyTick + super 氧化 tick、removedByPlayer 双载）；④ 物品侧审计：
 * damageDropped = meta % 8、getIcon = meta % 4，8/4 均整除 16 → 对 bit16
 * 取模透明零改动（与 BaseWall 类型数不整除 16 的取模错位不同构）；
 * hardness/explosionResistance 变体表未填充 → 查表 miss 落默认值无害。
 * 渲染 = 标准方块渲染（renderType 0、非不透明、cutout 贴图，与玻璃同管线），
 * 通用画水门控（canRenderInPass redirect / celeritas gate）按格 bit16 判定
 * 自动覆盖。
 *
 * <p>脚手架（26.2 ScaffoldingBlock implements SimpleWaterloggedBlock）：
 * WATERLOGGED 完整支持（getStateForPlacement L84-89、updateShape L99-107
 * 含水 scheduleTick 蔓延 + 自身 1gt 稳定性 tick、getFluidState L144-149）→
 * 26.2 忠实对象。EtFR 自有方块 → 全部源码级改动无 mixin：① meta = 距离
 * {@code &7} + 底部 {@code &8}，全部掩码读取对 bit16 天然透明，但
 * {@code composeMeta} 全新组 meta 丢 bit16——updateTick 的 setBlock(newMeta,3)
 * 与 fallMeta（坠落实体）两处并回 bit16（26.2 tick 的 setBlock(newState,3)
 * 与 FallingBlockEntity.fall(newState) 均属性保留语义；漏并 = 含水脚手架
 * 首个稳定 tick 静默脱水 + newMeta != meta 每 tick 空转重写）；② 新失去支撑
 * 的破坏走 26.2 destroyBlock(pos, true) = 掉落 + 留水源
 * （fluidState.createLegacyBlock）——1.7.10 func_147480_a 丢水 → 破坏前读
 * 含水位、破坏后补水源格；坠落实体首 tick 清原格由通用
 * {@code waterlog.MixinEntityFallingBlock} 留水（26.2 FallingBlockEntity
 * setBlock(fluidState.createLegacyBlock) 同款）；③ 放置 = ItemBlockScaffolding
 * 完全覆写 onItemUse（虚分派不经过 ItemBlock.onItemUse）→ MixinItemBlock
 * 捕获对脚手架是死字节码，含水放置（isWaterCell 判定 + bit16 原子并入
 * setBlock meta）在源码内联；④ damageDropped 剥位（vanilla 体 = return meta
 * 直通）；⑤ 渲染 = ISBRH（RenderIDs.SCAFFOLDING）掩码读取 {@code &8} 对
 * bit16 透明，通用画水门控自动覆盖（与提灯/锁链 ISBRH 同路径）。
 *
 * <p>箱子/末影箱（26.2 ChestBlock/EnderChestBlock：WATERLOGGED 完整支持，
 * getStateForPlacement L238/L88 按 replacedFluidState 判定、updateShape 蔓延
 * L186·L253/L155·L163 scheduleTick）→ 26.2 忠实对象。1710 两者 meta = 朝向
 * 2-5（陷阱箱 = Blocks.trappedChest 独立实例不占 meta 位；末影箱无双箱合并），
 * 与楼梯同构的基类通用解：@Mixin(BlockChest) —— mod 木箱子类（含陷阱箱实例）
 * 自动继承；直接继承 BlockContainer 的容器（铁箱扩展等）不在家族，26.2 亦
 * 不含。裸 meta 重写点全部保留式并回 bit16（箱子会写邻居格，邻居可能自带
 * 含水，不用楼梯的 isPlacementSource 并位）：BlockChest.onBlockPlacedBy
 * （放置朝向 + 双箱合并的邻居格写）与 func_149954_e（onBlockAdded 双箱朝向
 * 修正）、BlockEnderChest.onBlockPlacedBy → @Redirect setBlockMetadataWithNotify
 * 并入目标格 &amp; bit16；func_149954_e 邻居 meta 裸比较（j1==2/j1==4）与两个
 * TESR（TileEntityChestRenderer/EnderChestRenderer 裸 getBlockMetadata 朝向
 * switch）调用点 &amp; 15 剥离。damageDropped 未声明（基类默认 0）→ 物品侧
 * 干净；放置/倒水/舀水/挖掘留水/蔓延/海绵/爆炸走通用路径；渲染 = GTNH 空
 * ISBRH（BlockChestRenderer renderId 22，世界内不画本体，箱体走 TESR），
 * 含水画水走通用 pass-1 管线（renderBlockByRenderType HEAD 挂点对 ISBRH
 * 分派天然可达，与格栅/链同链路）。
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
				|| block instanceof BlockCopperGrate
				|| block instanceof BlockScaffolding
				|| block instanceof BlockChest
				|| block instanceof BlockEnderChest
				|| (block instanceof BlockStairs || block instanceof BlockSlab) && !block.isOpaqueCube();
	}
}
