package ganymedes01.etfuturum.mixins.early.waterlog;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ganymedes01.etfuturum.waterlog.WaterlogFamilies;
import ganymedes01.etfuturum.waterlog.WaterlogFlow;
import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDynamicLiquid;
import net.minecraft.world.World;

/**
 * 无限水成源自动含水（3c，项目必做项）：1.7.10 的水不会流入家族方块格
 * （{@code func_149809_q} 对 rock 材质为否），家族格因此永远错过 vanilla 的
 * 成源收敛。挂点 = {@code BlockDynamicLiquid.func_149813_h}（水流写入目标格
 * 的唯一咽喉，源格下流与横流都经过；SRG 名双端一致）。
 *
 * <p><b>海泡菜式延迟语义</b>：流尝试只调度 5gt 重查，不立即含水——家族方块
 * 自己的 updateTick（MixinBlockStairs）到点重查 {@link WaterlogFlow#wouldFormSource}，
 * 水源已移走则保持干。与 26.2 的 scheduleTick(getTickDelay) 和海泡菜环境补水
 * 的"延迟一个流体 tick 后重查"时序一致（用户实测立即含水偏快）。vanilla 本体
 * 对不可流入格本就无事可做，无需 cancel。
 */
@Mixin(BlockDynamicLiquid.class)
public abstract class MixinBlockDynamicLiquid {

	@Inject(method = "func_149813_h(Lnet/minecraft/world/World;IIII)V", at = @At("HEAD"))
	private void etfuturum$scheduleFamilySourceFormation(World world, int x, int y, int z, int meta, CallbackInfo ci) {
		Block block = world.getBlock(x, y, z);
		// wet 早退：已含水格无需成源调度（蔓延重调度由 onNeighborBlockChange
		// 负责）——否则邻水的每次流动尝试都会空转调度，SCHEDULE 刷屏
		if (WaterlogFamilies.isWaterloggable(block) && !WaterlogRuntime.isWaterlogged(world, x, y, z)) {
			WaterlogFlow.scheduleSpread(world, x, y, z, block);
		}
	}
}
