package ganymedes01.etfuturum.mixins.early.waterlog;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import ganymedes01.etfuturum.waterlog.WaterlogFlow;
import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.world.World;

/**
 * 26.2 FallingBlockEntity 首 tick 清原格 =
 * {@code level.setBlock(pos, state.getFluidState().createLegacyBlock(), 3)}
 * （L104）——含水方块的 FluidState 是源水，坠落后原格留水源。1.7.10 对应点 =
 * {@code onUpdate} 的 {@code setBlockToAir}（{@code field_145812_b == 1} 分支，
 * 全类唯一调用点），直通清 bit16 丢水。
 * <p>
 * 家族通用行为（26.2 对所有含水坠落方块一致）：按
 * {@link WaterlogRuntime#isWaterlogged}（内建 enabled + WaterlogFamilies 注册表
 * + bit16）判定——落沙/沙砾等非家族方块零行为变化。目前唯一家族坠落方块 =
 * 脚手架（distance-7 坠落，fallMeta 已并回 bit16 → 落地含水格原样放置）。
 */
@Mixin(EntityFallingBlock.class)
public abstract class MixinEntityFallingBlock {

	@Redirect(method = "onUpdate",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockToAir(III)Z"))
	private boolean etfuturum$leaveSourceWaterOnOriginClear(World world, int x, int y, int z) {
		if (WaterlogRuntime.isWaterlogged(world, x, y, z)) {
			// placeSourceWater = setBlock 源水 + vanilla 唤醒（BlockStaticLiquid
			// 只能经邻居变化 setNotStationary 才会蔓延，裸 setBlock 悬空静止）
			WaterlogFlow.placeSourceWater(world, x, y, z);
			return true;
		}
		return world.setBlockToAir(x, y, z);
	}
}
