package ganymedes01.etfuturum.mixins.early.waterlog;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import net.minecraft.entity.EntityLivingBase;

/**
 * waterBuoyancy 水重力（26.2 parity，config 默认开）：26.2 水重力 =
 * baseGravity/16 = 0.005（LivingEntity.getFluidFallingAdjustedMovement：
 * y − baseGravity/16，带 isFalling 近平衡捕捉 −0.003 特例），1.7.10 = 0.02
 * （4 倍）——上浮平衡 26.2 = +0.175/t vs 1.7.10 = +0.1/t（1.75 倍），下沉
 * −0.025 vs −0.1（更易悬停）。实现 = @ModifyConstant 水分支重力常量
 * 0.02 → 0.005（ordinal 0 = 水分支；lava 的 ordinal 1 保持 0.02）。
 *
 * <p><b>仅在 enableSwimming 关闭时注册</b>（EtFuturumEarlyMixins）：Mixin 对同一
 * 常量只允许一个 {@code @ModifyConstant}；enableSwimming 开启时该常量由
 * swimming.MixinEntityLivingBase.etfu$swimGravity 接管——它同时实现了 sprint
 * 免重力（26.2 getFluidFallingAdjustedMovement 的 !isSprinting 条件）与
 * waterBuoyancy 浮力，两个修饰器并存时后应用者会静默失效。</p>
 *
 * <p>26.2 的 isFalling 近平衡捕捉（−0.003）省略：无捕捉时下沉平衡 −0.025 稳定无抖动。</p>
 */
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBaseBuoyancy {

	@ModifyConstant(method = "moveEntityWithHeading(FF)V", constant = @Constant(doubleValue = 0.02D, ordinal = 0))
	private double etfuturum$waterBuoyancyGravity(double original) {
		// ordinal 0 = 水分支重力（ordinal 1 = lava 分支保持 0.02 不变）。
		return ConfigFunctions.waterBuoyancy ? 0.005D : original;
	}
}
