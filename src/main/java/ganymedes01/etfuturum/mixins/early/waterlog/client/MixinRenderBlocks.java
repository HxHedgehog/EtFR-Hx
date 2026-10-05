package ganymedes01.etfuturum.mixins.early.waterlog.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import ganymedes01.etfuturum.waterlog.client.StairWaterRenderer;
import ganymedes01.etfuturum.waterlog.client.WaterlogPaint;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.world.IBlockAccess;

/**
 * vanilla 管线的两个客户端挂点：
 * <ol>
 * <li><b>画水挂点</b>：pass-1 门控（MixinWorldRenderer）对含水家族格发出
 * paintNextCall 后，在 renderBlockByRenderType HEAD 画整格静水替代 pass-1
 * 的几何拷贝（本体已在 pass 0 画过）。画水器自判含水位并回报真实内容状态。</li>
 * <li><b>角高挂点</b>（桥接清单 #6）：{@code RenderBlocks#getLiquidHeight}
 * （vanilla L4343-4381）的角高平均把实心格排除，含水格（rock 材质）会被
 * 真水当成实心 → 周边真水表面对着含水格鼓包/缺口。重定向为成员感知版：
 * 涉及含水格时改走 StairWaterRenderer 的成员感知角高（含水格按 meta 0
 * 源水计权），与含水格内部水体的表面完全一致；不涉及含水格时保持 vanilla
 * 结果逐位一致（ocean plants 的 meta 0 强制不在此扩散，最小化行为面）。</li>
 * </ol>
 */
@Mixin(RenderBlocks.class)
public abstract class MixinRenderBlocks {

	@Shadow
	public IBlockAccess blockAccess;

	@Inject(method = "renderBlockByRenderType", at = @At("HEAD"), cancellable = true)
	private void etfuturum$paintWaterInsteadOfBlockCopy(Block block, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
		if (WaterlogPaint.paintNextCall) {
			WaterlogPaint.paintNextCall = false;
			boolean emitted = WaterlogPaint.paintIfWaterlogged(this.blockAccess, block, x, y, z);
			WaterlogRuntime.debug("PAINT " + x + "," + y + "," + z + " block=" + block + " -> emitted=" + emitted);
			cir.setReturnValue(emitted);
		}
	}

	@Inject(method = "getLiquidHeight(IIILnet/minecraft/block/material/Material;)F", at = @At("HEAD"), cancellable = true)
	private void etfuturum$memberAwareLiquidHeight(int x, int y, int z, Material material, CallbackInfoReturnable<Float> cir) {
		if (material != Material.water || !WaterlogRuntime.isEnabled()) {
			return; // 岩浆不与含水交互；门控关闭零改动
		}
		for (int i1 = 0; i1 < 4; ++i1) {
			int j1 = x - (i1 & 1);
			int k1 = z - (i1 >> 1 & 1);
			if (WaterlogRuntime.isWaterlogged(this.blockAccess, j1, y, k1) || WaterlogRuntime.isWaterlogged(this.blockAccess, j1, y + 1, k1)) {
				cir.setReturnValue((float) StairWaterRenderer.getLiquidHeight(this.blockAccess, x, y, z));
				return;
			}
		}
	}
}
