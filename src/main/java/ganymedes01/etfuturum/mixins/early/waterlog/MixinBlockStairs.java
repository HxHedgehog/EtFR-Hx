package ganymedes01.etfuturum.mixins.early.waterlog;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ganymedes01.etfuturum.waterlog.WaterlogFlow;
import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

/**
 * 蔓延触发（26.2 语义：含水格内部是源水，updateShape → scheduleTick →
 * spread）。写入点已统一迁移到 MixinItemBlock.onItemUse TAIL（楼梯/半砖
 * 共用），本类只保留调度与执行：
 * <ul>
 * <li>{@code onNeighborBlockChange}：BlockStairs 未声明（基类空实现），按
 * MixinBlockSponge 的 {@code extends Block + @Override} 模式添加覆写——
 * 隐式重映射在生产环境（SRG 名）命中 vanilla 的虚分派（onBlockAdded L485 的
 * {@code this.onNeighborBlockChange(air)} 调用经合并后的覆写覆盖 setblock 直接
 * 带 bit16 的放置路径）；<b>不可用 @Unique</b>：@Unique 方法不做重映射，生产
 * 环境下是孤儿方法，虚分派不命中（2026-10-05 蔓延失效实证）。</li>
 * <li>{@code updateTick} HEAD：到点执行 {@link WaterlogFlow#handleFamilyTick}
 * （含水 → 蔓延；未含水 → 3c 延迟成源重查），并取消向基座方块的委托。</li>
 * </ul>
 */
@Mixin(BlockStairs.class)
public abstract class MixinBlockStairs extends Block {

	protected MixinBlockStairs(Material materialIn) {
		super(materialIn);
	}

	/**
	 * 挖掘含水方块留下水源格——双载双参全覆写，共用
	 * {@link WaterlogFlow#leaveSourceWater}：6 参 = 服务端挖掘路径
	 * （ItemInWorldManager.removeBlock(IIIZ)Z）；5 参 = 客户端挖掘预测
	 * （PlayerControllerMP.onPlayerDestroyBlock 直接调用，Block 默认
	 * setBlockToAir）。海洋植物不走这里：格不带 bit16，且 BlockAquaticPlant
	 * 已覆写恢复水源。
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
	 * 原子含水放置（26.2 getStateForPlacement 语义：含水状态与方块同一 setBlock
	 * 写入，双端预测/确认零闪）：onBlockPlacedBy 把 meta 重写为 facing|上下半位
	 * （旧 meta 只保留 &4），会抹掉 MixinItemBlock 在 onBlockPlaced 返回值里并入
	 * 的 bit16——在此并回。捕获标记此时仍在线程上（onItemUse RETURN 才消费），
	 * 只读不消费；非放置上下文（发射器等）标记为空，vanilla 行为不变。
	 */
	@Redirect(method = "onBlockPlacedBy(Lnet/minecraft/world/World;IIILnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockMetadataWithNotify(IIIII)Z"))
	private boolean etfuturum$keepWaterlogInPlacedMeta(World world, int x, int y, int z, int meta, int flags) {
		if (WaterlogRuntime.isEnabled() && WaterlogRuntime.isPlacementSource()) {
			meta |= WaterlogRuntime.WATERLOGGED_BIT;
		}
		return world.setBlockMetadataWithNotify(x, y, z, meta, flags);
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
		if (!world.isRemote && WaterlogRuntime.isWaterlogged(world, x, y, z)) {
			WaterlogFlow.scheduleSpread(world, x, y, z, (Block) (Object) this);
		}
	}

	@Inject(method = "updateTick(Lnet/minecraft/world/World;IIILjava/util/Random;)V", at = @At("HEAD"), cancellable = true)
	private void etfuturum$familyTick(World world, int x, int y, int z, Random random, CallbackInfo ci) {
		if (WaterlogRuntime.isEnabled()) {
			ci.cancel(); // 家族 tick 代替向基座方块 updateTick 的委托
			WaterlogFlow.handleFamilyTick(world, x, y, z, (Block) (Object) this);
		}
	}
}
