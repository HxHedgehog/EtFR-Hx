package ganymedes01.etfuturum.mixins.early.oceanplants;

import ganymedes01.etfuturum.ModBlocks;
import net.minecraft.block.BlockDynamicLiquid;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Source-level water may backfill a dry sea pickle's cell (26.2: only source
 * fluid waterlogs). Vanilla flow never even attempts the pickle - its coral
 * material blocks movement, so findFlowDirections skips it - therefore when a
 * full source cell spreads, the neighbouring dry pickle schedules its own
 * delayed re-hydration instead of converting instantly: 26.2 waterlogging
 * happens on the fluid update after the flow arrives, so the fill lands one
 * fluid tick (5 game ticks) later, and the converted cell's onBlockAdded
 * schedules the spread a further tick later. The dry pickle's scheduled tick
 * re-validates the water before converting, so water that moved on leaves it
 * dry. The subsequent flowInto on the converted cell is a no-op (canFlowInto
 * rejects same-material cells), so the pickle is never clobbered. Partial
 * flowing (meta 1-7) and falling (meta 8-15) water keep ignoring dry pickles
 * entirely.
 */
@Mixin(BlockDynamicLiquid.class)
public abstract class MixinBlockDynamicLiquidBackfill {

	/**
	 * Runs at the end of the horizontal spread-direction scan. The spreader must
	 * be a source-level cell (raw meta 0) or a wet pickle anchor - whose metadata
	 * stores the pickle count rather than a flow level, so the raw-meta check
	 * alone would miss multi-pickle stacks.
	 */
	@Inject(method = "func_149808_o", at = @At("RETURN"))
	private void etfuturum$sourcesBackfillDryPickles(World world, int x, int y, int z, CallbackInfoReturnable<boolean[]> cir) {
		if ((Object) this != Blocks.water && (Object) this != Blocks.flowing_water) {
			return; // lava never re-hydrates pickles
		}
		if (world.getBlockMetadata(x, y, z) != 0 && world.getBlock(x, y, z) != ModBlocks.SEA_PICKLE.get()) {
			return; // flowing/falling water ignores dry pickles
		}
		int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
		for (int[] dir : dirs) {
			int nx = x + dir[0];
			int nz = z + dir[1];
			if (world.getBlock(nx, y, nz) == ModBlocks.DEAD_SEA_PICKLE.get()) {
				// One-fluid-tick delayed fill (26.2: the cell waterlogs on the
				// next fluid update, not the instant the flow arrives); the dry
				// pickle's scheduled tick re-validates before converting. Repeat
				// scans just stack extra ticks - they are skipped once the cell
				// is no longer the dry pickle.
				world.scheduleBlockUpdate(nx, y, nz, ModBlocks.DEAD_SEA_PICKLE.get(), Blocks.flowing_water.tickRate(world));
			}
		}
	}
}
