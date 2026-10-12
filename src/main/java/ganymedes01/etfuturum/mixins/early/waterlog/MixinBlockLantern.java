package ganymedes01.etfuturum.mixins.early.waterlog;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ganymedes01.etfuturum.blocks.BlockLantern;
import ganymedes01.etfuturum.waterlog.WaterlogFlow;
import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

/**
 * 提灯扩族（26.2 {@code LanternBlock implements SimpleWaterloggedBlock}：
 * WATERLOGGED 完整支持——getStateForPlacement L53-61 按 replacedFluidState 判定、
 * updateShape L84-92 含水 scheduleTick 蔓延、支撑丢失 → AIR 丢水）→ 26.2 忠实
 * 对象。EtFR 自有方块（普通 + 灵魂提灯都由 {@link BlockLantern} 承载），
 * {@code BlockSeaLantern} 非其子类（instanceof 不误伤）。
 *
 * <p><b>混合模式</b>（沿用梯子/活板门/墙先例）：
 * <ul>
 * <li>{@code onNeighborBlockChange} 由 BlockLantern 声明（支撑检查 → 掉落 +
 * setBlockToAir）→ {@code @Inject HEAD} 保留原体（不可 @Override 覆写），在其
 * 之前补蔓延调度。</li>
 * <li>{@code removedByPlayer} 双载 / {@code updateTick} 目标类未声明
 * （Block 基类空实现）→ {@code extends Block + @Override} 裸覆写（隐式重映射，
 * 生产 SRG 环境虚分派命中）。</li>
 * </ul>
 * 支撑丢失 = setLanternToAir 掉落 + 丢水，与 26.2 updateShape → AIR 同构。
 *
 * <p>裸 meta 剥位：BlockLantern 源码内完成（setBlockBoundsBasedOnState 挂位
 * {@code == 0}、canPlaceAbove 的邻居链/半砖/楼梯 meta 比较）——ISBRH 渲染器
 * {@code % 2} 读挂位对 bit16 天然透明，getIcon 未覆写（单贴图无需剥位）。
 * 物品侧：damageDropped 直通 meta 但提灯为单变体物品（damage 无害）、
 * ItemBlockLantern 无 placeBlockAt/onItemUse 覆写 → 放置走 MixinItemBlock
 * 标准捕获，物品永不携带 bit16。
 */
@Mixin(BlockLantern.class)
public abstract class MixinBlockLantern extends Block {

	protected MixinBlockLantern() {
		super(Material.iron);
	}

	/**
	 * 挖掘含水提灯留下水源格——双载全覆写（6 参 = 服务端挖掘路径；5 参 = GTNH
	 * 客户端挖掘预测，默认 setBlockToAir），共用 {@link WaterlogFlow#leaveSourceWater}。
	 */
	@Override
	public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest) {
		return WaterlogFlow.leaveSourceWater(world, x, y, z) || super.removedByPlayer(world, player, x, y, z, willHarvest);
	}

	@Override
	public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z) {
		return WaterlogFlow.leaveSourceWater(world, x, y, z) || super.removedByPlayer(world, player, x, y, z);
	}

	/**
	 * 蔓延调度：vanilla 支撑移除逻辑照常执行（@Inject 不取消）。
	 */
	@Inject(method = "onNeighborBlockChange(Lnet/minecraft/world/World;IIILnet/minecraft/block/Block;)V", at = @At("HEAD"))
	private void etfuturum$scheduleSpreadOnNeighbor(World world, int x, int y, int z, Block neighbor, CallbackInfo ci) {
		if (!world.isRemote && WaterlogRuntime.isWaterlogged(world, x, y, z)) {
			WaterlogFlow.scheduleSpread(world, x, y, z, (Block) (Object) this);
		}
	}

	@Override
	public void updateTick(World world, int x, int y, int z, Random random) {
		WaterlogFlow.handleFamilyTick(world, x, y, z, (Block) (Object) this);
	}
}
