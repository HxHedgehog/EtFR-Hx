package ganymedes01.etfuturum.mixins.early.waterlog.client;

import ganymedes01.etfuturum.waterlog.VisualWaterlog;
import ganymedes01.etfuturum.waterlog.client.BlockWaterRenderer;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * How real water renders around visually waterlogged members, vanilla
 * pipeline and Angelica alike (celeritas funnels every block through
 * RenderBlocks#renderBlockByRenderType, so this one class covers both).
 * Two things must change:
 * <p>
 * Face suppression: vanilla water paints its faces against every non-opaque
 * neighbour, so a water-embedded member block ends up wrapped in
 * flowing-texture quads that break the seamless "the cell is water" look of
 * 26.2 waterlogging. renderBlockLiquid checks each face through
 * Block#shouldSideBeRendered - redirecting those checks makes water skip its
 * faces against visually waterlogged member cells. The member's own painted
 * volume does not paint that interface either (its side faces are culled
 * against liquid neighbours), so the shared plane has zero quads, exactly
 * like two adjacent vanilla water cells, and the whole arrangement reads as
 * one continuous body of water. The waterlog check dispatches through the
 * VisualWaterlog registry, so every family shares this suppression.
 * <p>
 * Corner heights: the liquid surface height at a corner averages the cells
 * around it, and vanilla's averager would treat a member per its material -
 * solid shapes are skipped outright, MaterialLogic shapes read as air - so
 * the real water beside a member computes different corner values than the
 * member's painted surface, and the two drift apart at every shared corner
 * (a visible step), while the member's own surface, anchored by nothing,
 * interpolates freely across heterogeneous neighbours and reads as a steep
 * ramp wherever flow routes around the block. Redirecting the averager
 * through {@link BlockWaterRenderer#getLiquidHeight} makes both sides sample
 * a qualifying member as a water cell at its feed level, with the vanilla
 * weights - shared corners come out identical on both sides, and the member
 * surface stays anchored to the water feeding it, exactly like a real water
 * cell of that level. Water only; lava keeps the vanilla averager.
 */
@Mixin(RenderBlocks.class)
public abstract class MixinRenderBlocksShell {

	@Shadow
	public IBlockAccess blockAccess;

	@Redirect(method = "renderBlockLiquid",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/block/Block;shouldSideBeRendered(Lnet/minecraft/world/IBlockAccess;IIII)Z"))
	private boolean etfuturum$noWaterFaceAgainstVisualWaterlogging(Block block, IBlockAccess world, int x, int y, int z, int side) {
		if (block.getMaterial() == Material.water
				&& VisualWaterlog.isWaterlogged(world, x, y, z)) {
			return false; // the member's own painted water owns this interface
		}
		return block.shouldSideBeRendered(world, x, y, z, side);
	}

	@Inject(method = "getLiquidHeight(IIILnet/minecraft/block/material/Material;)F", at = @At("HEAD"), cancellable = true)
	private void etfuturum$memberAwareLiquidHeight(int x, int y, int z, Material material, CallbackInfoReturnable<Float> cir) {
		if (material != Material.water || !VisualWaterlog.anyEnabled()) {
			return;
		}
		cir.setReturnValue((float) BlockWaterRenderer.getLiquidHeight(this.blockAccess, x, y, z));
	}
}
