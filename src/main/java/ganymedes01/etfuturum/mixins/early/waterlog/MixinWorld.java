package ganymedes01.etfuturum.mixins.early.waterlog;

import ganymedes01.etfuturum.waterlog.VisualWaterlog;
import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Invalidation for the stateless visual waterlogging: there is no stored state,
 * so the look must be re-derived whenever a nearby cell changes. Vanilla only
 * re-renders the section of the changed block itself, which misses cells in
 * neighbouring sections; this marks them dirty on every client-side setBlock -
 * family members unconditionally when water appeared or disappeared (the look
 * toggles exactly then) or when they currently qualify (so face culling follows
 * newly placed solids), and real water cells when a family member appeared or
 * vanished beside them (their faces are suppressed by the shell mixin, which
 * vanilla knows nothing about). Real water adjacent to a member is also marked
 * when water elsewhere around that member changes: the shell mixin redirects
 * the liquid corner averager, so that water's surface geometry depends on the
 * member's qualification and feed level, and vanilla would leave it stale
 * across section boundaries. Server-side changes propagate as client block
 * updates, so the client-only gate loses nothing; the isRemote check
 * short-circuits before any registry class loads on a dedicated server.
 */
@Mixin(World.class)
public abstract class MixinWorld {

	@Shadow
	public boolean isRemote;

	@Shadow
	public abstract Block getBlock(int x, int y, int z);

	@Shadow
	public abstract void markBlockForUpdate(int x, int y, int z);

	@Unique
	private Block etfuturum$replacedBlock;

	@Inject(method = "setBlock(IIILnet/minecraft/block/Block;II)Z", at = @At("HEAD"))
	private void etfuturum$captureReplacedBlock(int x, int y, int z, Block newBlock, int meta, int flag, CallbackInfoReturnable<Boolean> cir) {
		if (this.isRemote) {
			this.etfuturum$replacedBlock = this.getBlock(x, y, z);
		}
	}

	@Inject(method = "setBlock(IIILnet/minecraft/block/Block;II)Z", at = @At("TAIL"))
	private void etfuturum$refreshAdjacentWaterloggedBlocks(int x, int y, int z, Block newBlock, int meta, int flag, CallbackInfoReturnable<Boolean> cir) {
		if (!this.isRemote || !VisualWaterlog.anyEnabled()) {
			return;
		}
		boolean waterInvolved = VisualWaterlog.isWaterBlock(this.etfuturum$replacedBlock) || VisualWaterlog.isWaterBlock(newBlock);
		// A member appearing or vanishing re-evaluates the adjacent real water's
		// suppressed faces (the shell mixin); vanilla only re-renders the changed
		// cell's own section, which misses water sitting in a neighbouring one
		boolean memberChanged = VisualWaterlog.familyOf(this.etfuturum$replacedBlock) != null
				|| VisualWaterlog.familyOf(newBlock) != null;
		IBlockAccess access = (IBlockAccess) (Object) this;
		for (int dir = 0; dir < 6; dir++) {
			int nx = x + VisualWaterlog.offsetX(dir);
			int ny = y + VisualWaterlog.offsetY(dir);
			int nz = z + VisualWaterlog.offsetZ(dir);
			Block neighbour = this.getBlock(nx, ny, nz);
			if (VisualWaterlog.familyOf(neighbour) != null
					&& (waterInvolved || VisualWaterlog.isWaterlogged(access, nx, ny, nz))) {
				this.markBlockForUpdate(nx, ny, nz);
				if (waterInvolved) {
					// this water change may flip the member's qualification or
					// move its feed level, and real water beside the member
					// samples the member through the redirected corner averager
					for (int dir2 = 0; dir2 < 6; dir2++) {
						int wx = nx + VisualWaterlog.offsetX(dir2);
						int wy = ny + VisualWaterlog.offsetY(dir2);
						int wz = nz + VisualWaterlog.offsetZ(dir2);
						if (VisualWaterlog.isWaterBlock(this.getBlock(wx, wy, wz))) {
							this.markBlockForUpdate(wx, wy, wz);
						}
					}
				}
			} else if (memberChanged && VisualWaterlog.isWaterBlock(neighbour)) {
				this.markBlockForUpdate(nx, ny, nz);
			}
		}
	}
}
