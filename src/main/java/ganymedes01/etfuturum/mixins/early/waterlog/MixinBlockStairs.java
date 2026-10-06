package ganymedes01.etfuturum.mixins.early.waterlog;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ganymedes01.etfuturum.waterlog.WaterlogFlow;
import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * 含水写入点 + 向外蔓延触发（26.2 语义：含水格内部是源水，updateShape →
 * scheduleTick → spread）。
 *
 * <p><b>写入点</b>：{@code onBlockPlacedBy}（vanilla 方法，TAIL 锚点稳定）用
 * {@code facing | (meta & 4)} 重写 metadata，bit16 必丢——在其后按
 * {@code MixinItemBlock} 捕获的源判定补写 bit16，并调度蔓延 tick。onBlockPlacedBy
 * 只在 placeBlockAt 成功路径被调用（{@code getBlock(x,y,z) == block}）。
 *
 * <p><b>蔓延</b>：26.2 的 updateShape→scheduleTick 映射为两处触发——
 * <ul>
 * <li>{@code onNeighborBlockChange}：BlockStairs 未声明（基类空实现），按
 * MixinBlockSponge 的 {@code extends Block + @Override} 模式添加覆写——
 * 隐式重映射在生产环境（SRG 名）命中 vanilla 的虚分派（onBlockAdded L485 的
 * {@code this.onNeighborBlockChange(air)} 调用经合并后的覆写覆盖 setblock 直接
 * 带 bit16 的放置路径）；<b>不可用 @Unique</b>：@Unique 方法不做重映射，生产
 * 环境下是孤儿方法，虚分派不命中（2026-10-05 蔓延失效实证）。</li>
 * <li>{@code updateTick} HEAD：到点执行蔓延（经 invoker 接口点燃 vanilla
 * spread），并取消向基座方块的委托。</li>
 * </ul>
 * 调度统一走 {@link WaterlogFlow#scheduleSpread}（服务端专用、5gt、
 * NextTickListEntry 去重）。模组楼梯若 override onBlockPlacedBy 且不调
 * super 则不会含水，Phase 4 扩族回归时再处理。
 */
@Mixin(BlockStairs.class)
public abstract class MixinBlockStairs extends Block {

	protected MixinBlockStairs(Material materialIn) {
		super(materialIn);
	}

	@Inject(method = "onBlockPlacedBy(Lnet/minecraft/world/World;IIILnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;)V", at = @At("TAIL"))
	private void etfuturum$waterlogAfterOrientation(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack, CallbackInfo ci) {
		if (WaterlogRuntime.consumePlacementSource()) {
			int meta = world.getBlockMetadata(x, y, z);
			WaterlogRuntime.debug("WRITE " + x + "," + y + "," + z + " meta=" + meta + " -> " + (meta | 0x10));
			WaterlogRuntime.markWaterlogged(world, x, y, z);
			WaterlogFlow.scheduleSpread(world, x, y, z, (Block) (Object) this);
		}
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
		if (!world.isRemote && WaterlogRuntime.isWaterlogged(world, x, y, z)) {
			WaterlogFlow.scheduleSpread(world, x, y, z, (Block) (Object) this);
		}
	}

	@Inject(method = "updateTick(Lnet/minecraft/world/World;IIILjava/util/Random;)V", at = @At("HEAD"), cancellable = true)
	private void etfuturum$spreadFromSource(World world, int x, int y, int z, Random random, CallbackInfo ci) {
		if (!world.isRemote && WaterlogRuntime.isWaterlogged(world, x, y, z)) {
			ci.cancel(); // 蔓延代替向基座方块 updateTick 的委托
			WaterlogFlow.spreadFromSource(world, x, y, z);
			return;
		}
		// 3c 延迟成源重查（海泡菜模式）：5gt 前的水流尝试调度——到点时水源仍在
		// 才含水（水源移走则保持干），含水后再延 5gt 蔓延
		if (!world.isRemote && WaterlogFlow.wouldFormSource(world, x, y, z)) {
			ci.cancel();
			WaterlogRuntime.markWaterlogged(world, x, y, z);
			WaterlogRuntime.debug("SOURCE-FORM " + x + "," + y + "," + z);
			WaterlogFlow.scheduleSpread(world, x, y, z, (Block) (Object) this);
		}
	}
}
