package ganymedes01.etfuturum.mixins.early.waterlog.client;

import ganymedes01.etfuturum.waterlog.VisualWaterlog;
import ganymedes01.etfuturum.waterlog.WaterlogFamily;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Pass-1 water paint for the stateless visual waterlogging in the vanilla
 * chunk builder: the pass gate (MixinWorldRenderer) opens a qualifying
 * member's pass-1 slot so the "pass has content" flag is set, and this hook
 * swaps the otherwise duplicated block geometry copy for the water volume -
 * the member's real geometry already rendered in pass 0, so a second copy
 * here would only add redundant opaque geometry. Hooked at the head of
 * renderBlockByRenderType so the entry is type-agnostic: whatever family
 * the block belongs to, the paint dispatches through the VisualWaterlog
 * registry. The painter re-checks the stateless waterlog condition itself
 * and reports the real content state - an all-culled volume returns false
 * so pass 1 must not claim content (empty pass-1 sort crash).
 */
@Mixin(RenderBlocks.class)
public abstract class MixinRenderBlocks {

	@Shadow
	public IBlockAccess blockAccess;

	@Inject(method = "renderBlockByRenderType", at = @At("HEAD"), cancellable = true)
	private void etfuturum$paintWaterInsteadOfBlockCopy(Block block, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
		if (VisualWaterlog.paintNextCall) {
			VisualWaterlog.paintNextCall = false;
			WaterlogFamily family = VisualWaterlog.familyOf(block);
			if (family != null) {
				cir.setReturnValue(family.paint(this.blockAccess, x, y, z, null));
			}
		}
	}
}
