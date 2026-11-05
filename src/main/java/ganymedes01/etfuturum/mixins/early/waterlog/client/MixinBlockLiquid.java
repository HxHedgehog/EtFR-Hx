package ganymedes01.etfuturum.mixins.early.waterlog.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import net.minecraft.block.BlockLiquid;
import net.minecraft.world.IBlockAccess;

/**
 * 真水对含水格的面剔除桥接（plan 桥接清单 #5）：vanilla 规则是邻居
 * material 相同才剔除（BlockLiquid.shouldSideBeRendered L167-171），含水
 * 楼梯是 rock 材质 → 真水本会对它画面，水体内部露一面。桥接后含水邻居
 * 按"同流体"剔除（含 top 面：含水格内部水体与下方真水相接，无空气缝隙）。
 * 门控关闭/非含水格时零改动。
 */
@Mixin(BlockLiquid.class)
public abstract class MixinBlockLiquid {

	@Inject(method = "shouldSideBeRendered(Lnet/minecraft/world/IBlockAccess;IIII)Z", at = @At("HEAD"), cancellable = true)
	private void etfuturum$cullFacesAgainstWaterlogged(IBlockAccess world, int x, int y, int z, int side, CallbackInfoReturnable<Boolean> cir) {
		if (WaterlogRuntime.isWaterlogged(world, x, y, z)) {
			cir.setReturnValue(false);
		}
	}
}
