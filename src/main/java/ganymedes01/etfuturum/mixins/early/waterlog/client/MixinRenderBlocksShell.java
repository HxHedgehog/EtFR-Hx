package ganymedes01.etfuturum.mixins.early.waterlog.client;

import ganymedes01.etfuturum.waterlog.VisualWaterlog;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Water shell suppression for the stateless visual waterlogging: vanilla water
 * paints its faces against every non-opaque neighbour, so a water-embedded
 * member block ends up wrapped in flowing-texture quads that break the seamless
 * "the cell is water" look of 26.2 waterlogging. renderBlockLiquid checks
 * each face through Block#shouldSideBeRendered - redirecting those checks
 * makes water skip its faces against visually waterlogged member cells. The
 * member's own painted volume does not paint that interface either (its
 * side faces are culled against liquid neighbours), so the shared plane has
 * zero quads, exactly like two adjacent vanilla water cells, and the whole
 * arrangement reads as one continuous body of water. The waterlog check
 * dispatches through the VisualWaterlog registry, so every family shares
 * this suppression.
 */
@Mixin(RenderBlocks.class)
public abstract class MixinRenderBlocksShell {

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
}
