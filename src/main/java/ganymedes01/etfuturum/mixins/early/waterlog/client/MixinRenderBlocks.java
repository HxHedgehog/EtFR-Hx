package ganymedes01.etfuturum.mixins.early.waterlog.client;

import ganymedes01.etfuturum.waterlog.client.StairWaterRenderer;
import net.minecraft.block.BlockStairs;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Pass-1 water paint for the stateless waterlogged stairs in the vanilla
 * chunk builder: the pass gate (MixinWorldRenderer) opens a qualifying
 * stair's pass-1 slot so the "pass has content" flag is set, and this
 * redirect swaps the otherwise duplicated stair geometry copy for the water
 * volume. The stair's real geometry already rendered in pass 0, so a second
 * copy here would only add redundant opaque geometry. The painter re-checks
 * the stateless waterlog condition itself.
 */
@Mixin(RenderBlocks.class)
public abstract class MixinRenderBlocks {

	@Shadow
	public IBlockAccess blockAccess;

	@Redirect(method = "renderBlockByRenderType",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/RenderBlocks;renderBlockStairs(Lnet/minecraft/block/BlockStairs;III)Z"))
	private boolean etfuturum$paintWaterInsteadOfStairCopy(RenderBlocks renderBlocks, BlockStairs stair, int x, int y, int z) {
		if (StairWaterRenderer.paintWaterNextCall) {
			StairWaterRenderer.paintWaterNextCall = false;
			// Report the real content state: an all-culled complement paints
			// nothing and pass1 must not claim content (empty pass1 sort crash)
			return StairWaterRenderer.renderWaterVolume(this.blockAccess, x, y, z, null);
		}
		return renderBlocks.renderBlockStairs(stair, x, y, z);
	}
}
