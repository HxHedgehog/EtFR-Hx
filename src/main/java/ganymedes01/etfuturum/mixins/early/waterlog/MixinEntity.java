package ganymedes01.etfuturum.mixins.early.waterlog;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/**
 * 呼吸/溺亡/雾色/雾密度/FOV/水中减速的单一咽喉桥接（plan §3.4 #1）：
 * {@code Entity.isInsideOfMaterial(Material.water)} 只看眼睛所在格的方块
 * 材质，含水楼梯（rock 材质）本会返回 false → 无水下效果。
 *
 * <p>桥接后按 262 的眼位分界判定：眼睛位置流体高度 = 8/9（source amount
 * 8/9），眼位 &lt; 格底 + 8/9 才算水下；在其上时不干预（vanilla 自然返回
 * false）。海带等海洋植物无需此桥接（BlockAquaticPlant 本身就是
 * Material.water，走 vanilla 链路）。
 */
@Mixin(Entity.class)
public abstract class MixinEntity {

	@Shadow
	public World worldObj;
	@Shadow
	public double posX;
	@Shadow
	public double posY;
	@Shadow
	public double posZ;

	@Shadow
	public abstract float getEyeHeight();

	/** 探针节流计数（仅 WATERLOG_DEBUG）。 */
	private static int etfuturum$insideProbe;

	@Inject(method = "isInsideOfMaterial(Lnet/minecraft/block/material/Material;)Z", at = @At("HEAD"), cancellable = true)
	private void etfuturum$waterloggedEye(Material materialIn, CallbackInfoReturnable<Boolean> cir) {
		if (materialIn != Material.water || !WaterlogRuntime.isEnabled()) {
			return;
		}
		double d0 = this.posY + (double) this.getEyeHeight();
		int i = MathHelper.floor_double(this.posX);
		int j = MathHelper.floor_double(d0);
		int k = MathHelper.floor_double(this.posZ);
		boolean wet = WaterlogRuntime.isWaterlogged(this.worldObj, i, j, k);
		// 探针（WATERLOG_DEBUG）：覆盖层/溺水/减速链路的眼位判定——INSIDE 行
		if (WaterlogRuntime.WATERLOG_DEBUG && ++etfuturum$insideProbe % 40 == 0) {
			WaterlogRuntime.debug("INSIDE #" + etfuturum$insideProbe + " eyeCell=" + i + "," + j + "," + k + " d0=" + d0
					+ " bound=" + (j + 8.0D / 9.0D) + " wet=" + wet + " -> " + wet);
		}
		// 26.2 = "眼位格 fluid state 非空即水下"整格判定（与雾链
		// getBlockAtEntityViewpoint 眼位格语义一致）——含水格内任意高度都算水下，
		// 不设 8/9 眼位门（曾与雾链分裂：雾 true / 覆盖层 false，用户实证）。
		// 真水格不拦截，vanilla 8/9 表面判线照旧。
		if (wet) {
			cir.setReturnValue(true);
		}
	}
}
