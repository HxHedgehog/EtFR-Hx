package ganymedes01.etfuturum.mixins.early.waterlog;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import net.minecraft.entity.EntityLivingBase;

/**
 * 水下梯子攀爬（26.2 LivingEntity.travelInWater parity）：1.7.10 的水物理分支
 * （moveEntityWithHeading 的 isInWater 段）没有梯子上爬逻辑——梯子爬行
 * （isOnLadder → motionY = 0.2）只在 else 陆地分支里，isInWater 时整段跳过
 * → 含水/水下梯子无法攀爬。26.2 travelInWater（LivingEntity L2396-2404）的
 * 顺序 = move → {@code if (horizontalCollision && onClimbable()) motion.y = 0.2}
 * → 拖拽 y×0.8 → 重力 → jumpOutOfFluid 0.3（最后，共存时 0.3 覆写 0.2）。
 *
 * <p>注入点 = 水分支的 {@code moveEntity(DDD)} 调用之后（ordinal 0 = 水分支；
 * lava L1601 / 陆地 L1680 不干预）——0.2 必须设在拖拽**之前**再吃 y×0.8 和
 * 水重力，稳态爬速 = 0.2×0.8−0.005 = 0.155/t（26.2 逐位一致）。旧版注入在
 * isOffsetPositionInLiquid 之后（拖拽已过），0.2 绕过阻尼 → 0.2/t，比官方
 * 快 29%（2026-11-05 源码核实后修正）。靠墙+上目标为液体时 vanilla 水面
 * 跳出（motionY = 0.3）在拖拽之后赋值 → 0.3 胜出，与 26.2 优先序一致。
 *
 * <p>**waterBuoyancy 水重力（26.2 parity，config 默认开）**：26.2 水重力 =
 * baseGravity/16 = 0.005（LivingEntity.getFluidFallingAdjustedMovement：
 * y − baseGravity/16，带 isFalling 近平衡捕捉 −0.003 特例），1.7.10 = 0.02
 * （4 倍）——上浮平衡 26.2 = +0.175/t vs 1.7.10 = +0.1/t（1.75 倍），下沉
 * −0.025 vs −0.1（更易悬停）。实现 = @ModifyConstant 水分支重力常量
 * 0.02 → 0.005（ordinal 0 = 水分支；lava 的 ordinal 1 保持 0.02）。
 */
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase {

	@Shadow
	public abstract boolean isOnLadder();

	@Inject(method = "moveEntityWithHeading(FF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;moveEntity(DDD)V", ordinal = 0, shift = At.Shift.AFTER))
	private void etfuturum$ladderClimbInWater(float strafe, float forward, CallbackInfo ci) {
		if (this.isOnLadder()) {
			// motionY/isCollidedHorizontally 是 Entity 声明的字段（@Shadow 继承陷阱），
			// 经 waterlog.MixinEntity 的 shadow cast 访问；isCollidedHorizontally 由
			// 26.2 的 horizontalCollision 条件保证，此处显式判一次防御。
			MixinEntity self = (MixinEntity) (Object) this;
			if (self.isCollidedHorizontally) {
				// 0.2 先设、再吃本 tick 的 y×0.8 拖拽与水重力（26.2 travelInWater
				// L2399-2402 顺序），稳态 = 0.155/t；vanilla 水面跳出 0.3 在拖拽后
				// 赋值，共存时 0.3 胜出（26.2 jumpOutOfFluid 最后执行的优先序）。
				self.motionY = 0.20000000298023224D;
			}
		}
	}

	@ModifyConstant(method = "moveEntityWithHeading(FF)V", constant = @Constant(doubleValue = 0.02D, ordinal = 0))
	private double etfuturum$waterBuoyancyGravity(double original) {
		// ordinal 0 = 水分支重力（ordinal 1 = lava 分支保持 0.02 不变）。
		// 26.2 水重力 = getEffectiveGravity()/16 = 0.005；拖拽（y×0.8）与游动加速
		//（+0.04/tick）两侧同值，仅此常量即完全对齐：上浮平衡 +0.175/t（26.2）
		// vs +0.1/t（1.7.10），下沉 −0.025/t vs −0.1/t（更易悬停）。
		// 26.2 的 isFalling 近平衡捕捉（−0.003）省略：无捕捉时下沉平衡 −0.025 稳定无抖动。
		return ConfigFunctions.waterBuoyancy ? 0.005D : original;
	}
}
