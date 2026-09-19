package ganymedes01.etfuturum.mixins.early.waterlog;

import ganymedes01.etfuturum.waterlog.VisualWaterlog;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Invalidation for the stateless visual waterlogging: there is no stored state,
 * so the look must be re-derived whenever a nearby cell changes. Vanilla only
 * re-renders the section of the changed block itself, which misses cells in
 * neighbouring sections; this marks them dirty on every client-side setBlock
 * whose old/new cell can flip a member's verdict. A member's qualification is
 * a function of each of its direct neighbours' role - water (source), a member
 * or an opaque cube (both seal), anything else (exposed side, no
 * qualification) - so any role change re-evaluates every member neighbour,
 * whether it turns them wet or dries them out: digging out the block that
 * sealed a stair's side must clear its painted water shell just as placing it
 * must summon one. Real water cells are also marked when a member appeared or
 * vanished beside them (their faces are suppressed by the shell mixin, which
 * vanilla knows nothing about) and when the seal changed beside them: the
 * shell mixin redirects the liquid corner averager, so that water's surface
 * geometry depends on the solid/air state of every cell around it, and
 * vanilla would leave it stale across section boundaries. Real water is also
 * always marked around a re-marked member: a member flipping its verdict
 * (wet to dry or dry to wet) toggles the shell mixin's face suppression on
 * those water cells too, and they sit one cell past the changed block, out of
 * reach of the outer loop - without it, water one cell beyond a member kept
 * its suppressed face geometry and the interface read as a see-through gap.
 * <p>
 * The hook lives on {@code markAndNotifyBlock} - Forge's split-off "block
 * actually changed" tail of {@code setBlock} - and NOT on {@code setBlock}
 * itself: that method compiles to four RETURN opcodes (two y-bounds checks,
 * the success path and the trailing x/z-bounds {@code return false}), and
 * {@code @At("TAIL")} targets the LAST of them, so a TAIL hook there only
 * ever fired for out-of-bounds coordinates and never ran once in practice.
 * {@code markAndNotifyBlock} is a plain void method with a single exit, so
 * its TAIL is reliable; its {@code oldBlock} parameter supplies the replaced
 * block directly (null when the caller passed flags without bit 1, which the
 * water/member checks treat as no water and no member), and the
 * {@code flag & 2} guard mirrors the vanilla render-notification condition.
 * Server-side changes propagate as client block updates, so the client-only
 * gate loses nothing; the isRemote check short-circuits before any registry
 * class loads on a dedicated server.
 */
@Mixin(World.class)
public abstract class MixinWorld {

	@Shadow
	public boolean isRemote;

	@Shadow
	public abstract Block getBlock(int x, int y, int z);

	@Shadow
	public abstract void markBlockForUpdate(int x, int y, int z);

	@Inject(
			method = "markAndNotifyBlock(IIILnet/minecraft/world/chunk/Chunk;Lnet/minecraft/block/Block;Lnet/minecraft/block/Block;I)V",
			at = @At("TAIL"),
			// Forge-added method, absent from the SRG table: it keeps its MCP
			// name at runtime in both dev and production, so no remap applies.
			remap = false)
	private void etfuturum$refreshAdjacentWaterloggedBlocks(int x, int y, int z, Chunk chunk, Block replaced, Block newBlock, int flag, CallbackInfo ci) {
		// The flag guard matches vanilla: flag & 2 is exactly the "send the
		// change to clients / re-render" bit, so a setBlock that asked not to
		// re-render (flag & 4) or carries no render notification keeps the
		// look untouched, same as the vanilla markBlockForUpdate right above.
		if (!this.isRemote || !VisualWaterlog.anyEnabled() || (flag & 2) == 0) {
			return;
		}
		// A changed cell's role for a neighbouring member: water (source), a
		// member or an opaque cube (both seal), anything else (exposed). Any
		// role change can flip the member's verdict - including wet to dry:
		// digging out the block that sealed a stair's side leaves its painted
		// water shell stale unless the member itself is re-rendered, and a
		// gate on the member's own current verdict could not see that flip.
		boolean waterInvolved = VisualWaterlog.isWaterBlock(replaced) || VisualWaterlog.isWaterBlock(newBlock);
		boolean memberChanged = VisualWaterlog.familyOf(replaced) != null || VisualWaterlog.familyOf(newBlock) != null;
		boolean sealChanged = (replaced != null && replaced.isOpaqueCube())
				!= (newBlock != null && newBlock.isOpaqueCube());
		if (!waterInvolved && !memberChanged && !sealChanged) {
			return;
		}
		for (int dir = 0; dir < 6; dir++) {
			int nx = x + VisualWaterlog.offsetX(dir);
			int ny = y + VisualWaterlog.offsetY(dir);
			int nz = z + VisualWaterlog.offsetZ(dir);
			Block neighbour = this.getBlock(nx, ny, nz);
			if (VisualWaterlog.familyOf(neighbour) != null) {
				this.markBlockForUpdate(nx, ny, nz);
				// The member's verdict may have flipped either way - wet to dry
				// or dry to wet - which toggles the shell mixin's face
				// suppression on every adjacent real water cell, moves the
				// member's painted surface and the feed level real water
				// samples from it through the redirected corner averager.
				// Those water cells are not neighbours of the changed block
				// (the member sits between them), so the outer loop never marks
				// them and they must be picked up here.
				for (int dir2 = 0; dir2 < 6; dir2++) {
					int wx = nx + VisualWaterlog.offsetX(dir2);
					int wy = ny + VisualWaterlog.offsetY(dir2);
					int wz = nz + VisualWaterlog.offsetZ(dir2);
					if (VisualWaterlog.isWaterBlock(this.getBlock(wx, wy, wz))) {
						this.markBlockForUpdate(wx, wy, wz);
					}
				}
			} else if ((memberChanged || sealChanged) && VisualWaterlog.isWaterBlock(neighbour)) {
				// a member appearing or vanishing toggles the shell mixin's face
				// suppression; an opaque/seal change moves the corner-averaged
				// liquid surface (solid cells are skipped, air counts) - either
				// way the adjacent real water needs its geometry re-derived
				this.markBlockForUpdate(nx, ny, nz);
			}
		}
	}
}
