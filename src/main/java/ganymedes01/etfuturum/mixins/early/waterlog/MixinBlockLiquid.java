package ganymedes01.etfuturum.mixins.early.waterlog;

import net.minecraft.block.BlockLiquid;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ganymedes01.etfuturum.waterlog.WaterlogRuntime;

/**
 * 含水格虚拟源桥（3a 蔓延的存续根基）：{@code BlockLiquid.func_149804_e} 是
 * 水流深度计算的唯一入口（流动水 updateTick 的邻居喂养判定、源计数、上方水
 * 检测全走它），非水材质返回 -1——含水楼梯（rock 材质）因此对水流系统不可
 * 见，蔓延写入的流动水成为"无源之水"：首个自身 tick 邻居深度全 -100 →
 * {@code j1 < 0} → setBlockToAir 自删，宏观表现为水闪现即消退。
 *
 * <p>桥接后含水格返回深度 0（26.2：含水格内部是 {@code getSource(false)}
 * 源水）：蔓延水被虚拟源喂养 → {@code j1 == l} → 沉降为静态水稳定存续；
 * vanilla 的源计数（{@code field_149815_a}）随之把含水邻居计入，与 3c 的
 * wouldFormSource 口径一致；海绵/桶排水后桥自动失效，周边水按 vanilla 语义
 * 回退干涸。岩浆流不受影响——水域判定用实例身份（Blocks.water /
 * flowing_water），<b>不可 @Shadow 父类 Block 的 getMaterial()</b>：@Shadow
 * 不解析继承成员，目标类字节码里找不到直接 apply 失败（2026-10-05 启动崩溃
 * 实证，func_149688_o not located in BlockLiquid）。
 */
@Mixin(BlockLiquid.class)
public abstract class MixinBlockLiquid {

	@Inject(method = "func_149804_e(Lnet/minecraft/world/World;III)I", at = @At("HEAD"), cancellable = true)
	private void etfuturum$waterloggedVirtualSourceDepth(World world, int x, int y, int z, CallbackInfoReturnable<Integer> cir) {
		if (((Object) this == Blocks.water || (Object) this == Blocks.flowing_water) && WaterlogRuntime.isWaterlogged(world, x, y, z)) {
			cir.setReturnValue(0);
		}
	}
}
