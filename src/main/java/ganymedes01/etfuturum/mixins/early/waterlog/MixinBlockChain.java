package ganymedes01.etfuturum.mixins.early.waterlog;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;

import ganymedes01.etfuturum.blocks.BlockChain;
import ganymedes01.etfuturum.waterlog.WaterlogFlow;
import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

/**
 * 锁链扩族（26.2 {@code ChainBlock extends RotatedPillarBlock implements
 * SimpleWaterloggedBlock}：WATERLOGGED 完整支持——getStateForPlacement L55-59
 * 按 replacedFluidState 判定、updateShape L62-67 含水 scheduleTick 蔓延、
 * 无 canSurvive 支撑要求）→ 26.2 忠实对象。EtFR 自有方块（{@link BlockChain}）。
 *
 * <p>1.7.10 meta 0/1/2 = 轴向（Y/X/Z）：碰撞箱、边界与 ISBRH 渲染器
 * （RenderIDs.CHAIN，BlockChainRenderer）的 {@code switch} 裸判已在源码剥位
 * {@code & 15}——default 分支 = Y 轴，含水横链 meta 17/18 裸判会竖起来；
 * BlockChain/BaseBlock 均未声明
 * onNeighborBlockChange/updateTick/removedByPlayer/damageDropped → 按墙先例
 * {@code extends Block + @Override} 四件套（隐式重映射，生产 SRG 环境虚分派
 * 命中）：
 * <ul>
 * <li>{@code onNeighborBlockChange} → 蔓延调度（无支撑判定，26.2 同）；</li>
 * <li>{@code updateTick} → 家族标准 handleFamilyTick（到点重查后蔓延）；</li>
 * <li>{@code removedByPlayer} 双载 → 挖掘留水源格；</li>
 * <li>{@code damageDropped} → 剥 bit16（vanilla 体 = {@code return meta;}，
 * {@code & 15} 对干链等价，物品永不携带含水位）。</li>
 * </ul>
 * 放置进水由 MixinItemBlock 标准捕获并入 bit16，真实水蔓延经
 * MixinBlockDynamicLiquid 成源判定调度——均按 {@code WaterlogFamilies}
 * 注册表自动生效。
 */
@Mixin(BlockChain.class)
public abstract class MixinBlockChain extends Block {

	protected MixinBlockChain() {
		super(Material.iron);
	}

	@Override
	public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest) {
		return WaterlogFlow.leaveSourceWater(world, x, y, z) || super.removedByPlayer(world, player, x, y, z, willHarvest);
	}

	@Override
	public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z) {
		return WaterlogFlow.leaveSourceWater(world, x, y, z) || super.removedByPlayer(world, player, x, y, z);
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
		if (!world.isRemote && WaterlogRuntime.isWaterlogged(world, x, y, z)) {
			WaterlogFlow.scheduleSpread(world, x, y, z, (Block) (Object) this);
		}
	}

	@Override
	public void updateTick(World world, int x, int y, int z, Random random) {
		WaterlogFlow.handleFamilyTick(world, x, y, z, (Block) (Object) this);
	}

	/**
	 * 物品侧含水位剥离：掉落与中键取块的物品 damage = damageDropped(世界 meta)，
	 * bit16 直通 → 剥离（26.2 参照：getCloneItemStack 不带任何 state）。
	 */
	@Override
	public int damageDropped(int meta) {
		return meta & 15;
	}
}
