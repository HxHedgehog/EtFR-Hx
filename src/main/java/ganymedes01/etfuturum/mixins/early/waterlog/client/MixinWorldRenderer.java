package ganymedes01.etfuturum.mixins.early.waterlog.client;

import ganymedes01.etfuturum.waterlog.VisualWaterlog;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Pass gating for the stateless visual waterlogging in the vanilla chunk
 * builder. The member block's own geometry keeps rendering in pass 0 exactly
 * as vanilla does. The painted water is a translucent volume whose real
 * vanilla block (BlockLiquid) renders in pass 1, so it must join that pass
 * too; it then sorts together with the surrounding real water and receives
 * the same water treatment from shader mods. The gate only has to make that
 * pass-1 paint happen: getRenderBlockPass reports 1 for qualifying members
 * so the k3 &gt; pass check keeps the pass-1 loop alive, and canRenderInPass
 * opens the member's pass-1 slot, where renderBlockByRenderType
 * (MixinRenderBlocks) paints the water volume instead of a second geometry
 * copy - which also keeps the pass-1 "has content" flag set. The waterlog
 * test is memoized per cell (one entry): the gates evaluate it back to back
 * for the same cell against the same immutable per-rebuild ChunkCache, so
 * the judgment runs once per member per rebuild. Chunk building runs on the
 * render thread, so the tracked cell (captured per iteration from the
 * getBlock call that precedes both gate calls) needs no synchronization.
 * Membership and qualification dispatch through the VisualWaterlog registry,
 * so every family shares this gate. Only registered when Angelica is absent:
 * celeritas owns the chunk build path when Angelica is loaded, and the
 * AngelicaChunkBuilderMeshingTask mixin handles the pass there instead.
 */
@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer {

	@Unique
	private static IBlockAccess etfuturum$trackedAccess;
	@Unique
	private static int etfuturum$trackedX;
	@Unique
	private static int etfuturum$trackedY;
	@Unique
	private static int etfuturum$trackedZ;

	// One-entry memo for the waterlog test: the ChunkCache is an immutable
	// per-rebuild snapshot, so a hit is always valid within a rebuild.
	@Unique
	private static IBlockAccess etfuturum$queriedAccess;
	@Unique
	private static int etfuturum$queriedX;
	@Unique
	private static int etfuturum$queriedY;
	@Unique
	private static int etfuturum$queriedZ;
	@Unique
	private static boolean etfuturum$queriedResult;

	@Redirect(method = "updateRenderer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/ChunkCache;getBlock(III)Lnet/minecraft/block/Block;"))
	private Block etfuturum$trackCell(ChunkCache cache, int x, int y, int z) {
		Block block = cache.getBlock(x, y, z);
		etfuturum$trackedAccess = cache;
		etfuturum$trackedX = x;
		etfuturum$trackedY = y;
		etfuturum$trackedZ = z;
		return block;
	}

	/** The stateless waterlog test for the currently tracked cell, memoized for the two gate redirects below. */
	@Unique
	private static boolean etfuturum$isWaterlogged(Block block) {
		if (etfuturum$trackedAccess == null || VisualWaterlog.familyOf(block) == null) {
			return false;
		}
		if (etfuturum$trackedAccess == etfuturum$queriedAccess
				&& etfuturum$trackedX == etfuturum$queriedX
				&& etfuturum$trackedY == etfuturum$queriedY
				&& etfuturum$trackedZ == etfuturum$queriedZ) {
			return etfuturum$queriedResult;
		}
		final boolean result = VisualWaterlog.isWaterlogged(etfuturum$trackedAccess, etfuturum$trackedX, etfuturum$trackedY, etfuturum$trackedZ);
		etfuturum$queriedAccess = etfuturum$trackedAccess;
		etfuturum$queriedX = etfuturum$trackedX;
		etfuturum$queriedY = etfuturum$trackedY;
		etfuturum$queriedZ = etfuturum$trackedZ;
		etfuturum$queriedResult = result;
		return result;
	}

	@Redirect(method = "updateRenderer", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getRenderBlockPass()I"))
	private int etfuturum$waterloggedBlocksKeepPassOneLoop(Block block) {
		if (etfuturum$isWaterlogged(block)) {
			return 1; // drives the k3 > pass check so the pass-1 loop runs for the water paint
		}
		return block.getRenderBlockPass();
	}

	@Redirect(method = "updateRenderer", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;canRenderInPass(I)Z", remap = false))
	private boolean etfuturum$waterloggedBlocksFlagPassOneSlot(Block block, int pass) {
		VisualWaterlog.paintNextCall = false;
		if (pass == 1 && etfuturum$isWaterlogged(block)) {
			VisualWaterlog.paintNextCall = true;
			return true; // open the slot: renderBlockByRenderType paints the water volume instead of the geometry
		}
		return block.canRenderInPass(pass);
	}
}
