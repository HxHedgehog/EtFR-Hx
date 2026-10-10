package ganymedes01.etfuturum.mixins.early.waterlog.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

/**
 * Vanilla 1.7.10 draws the underwater overlay at alpha 0.5, which lets the noisy
 * {@code underwater.png} read as bright speckles. 26.2 draws the same texture at
 * alpha 0.1 and leans on the water fog for the underwater look instead
 * (ScreenEffectRenderer#submitWater: {@code ARGB.colorFromFloat(0.1f, ...)}).
 * Lowering the constant here dims the overlay for real water, matching that
 * look. Unconditional - it touches the shared overlay path, not any
 * waterlogged-block feature.
 */
@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer {

	@Shadow
	private Minecraft mc;

	@ModifyConstant(method = "renderWarpedTextureOverlay(F)V", constant = @Constant(floatValue = 0.5F))
	private float etfuturum$dimWaterOverlay(float original) {
		return 0.1F;
	}

	/** 探针节流计数（仅 WATERLOG_DEBUG）。 */
	private static int etfuturum$maskProbe;

	/**
	 * 探针（WATERLOG_DEBUG）：蒙版真实绘制——MASK-DRAW 行。本方法每帧仅在
	 * isInsideOfMaterial(water) 为真时被 renderItemInFirstPerson 调用（ItemRenderer
	 * L610）；有蒙版但日志无此行 = 蒙版由别处绘制（如 Angelica HandRenderer）。
	 */
	@Inject(method = "renderWarpedTextureOverlay(F)V", at = @At("HEAD"))
	private void etfuturum$maskDrawProbe(float partialTicks, CallbackInfo ci) {
		if (!WaterlogRuntime.WATERLOG_DEBUG || ++etfuturum$maskProbe % 60 != 0) {
			return;
		}
		EntityPlayer player = this.mc.thePlayer;
		if (player == null) {
			return;
		}
		double d0 = player.posY + (double) player.getEyeHeight();
		int i = MathHelper.floor_double(player.posX);
		int j = MathHelper.floor_double(d0);
		int k = MathHelper.floor_double(player.posZ);
		Block block = player.worldObj.getBlock(i, j, k);
		WaterlogRuntime.debug("MASK-DRAW #" + etfuturum$maskProbe + " eye=" + player.posX + "," + d0 + "," + player.posZ
				+ " cell=" + i + "," + j + "," + k + " block=" + block.getClass().getSimpleName()
				+ "/" + player.worldObj.getBlockMetadata(i, j, k)
				+ " wet=" + WaterlogRuntime.isWaterlogged(player.worldObj, i, j, k));
	}
}
