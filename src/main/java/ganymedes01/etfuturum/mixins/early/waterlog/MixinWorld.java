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
 * re-renders the section of the changed block itself, which misses family
 * members in neighbouring sections; this marks their sections dirty on every
 * client-side setBlock - unconditionally when water appeared or disappeared
 * (the look toggles exactly then), and otherwise only when the neighbour
 * currently qualifies (so face culling follows newly placed solids).
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
		IBlockAccess access = (IBlockAccess) (Object) this;
		for (int dir = 0; dir < 6; dir++) {
			int nx = x + VisualWaterlog.offsetX(dir);
			int ny = y + VisualWaterlog.offsetY(dir);
			int nz = z + VisualWaterlog.offsetZ(dir);
			Block neighbour = this.getBlock(nx, ny, nz);
			if (VisualWaterlog.familyOf(neighbour) != null
					&& (waterInvolved || VisualWaterlog.isWaterlogged(access, nx, ny, nz))) {
				this.markBlockForUpdate(nx, ny, nz);
			}
		}
	}
}
