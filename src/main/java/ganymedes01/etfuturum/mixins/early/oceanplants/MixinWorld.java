package ganymedes01.etfuturum.mixins.early.oceanplants;

import ganymedes01.etfuturum.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The outline ray trace box of a wet sea pickle is the pickle column, so the
 * empty bucket's liquid ray trace (stopOnLiquid) sails right past it through
 * the surrounding water and lands on a block behind - the scoop never sees the
 * pickle. 26.2 instead clips the waterlogged cell's fluid, which covers the
 * whole cell. Bucket rays run through the same World#func_147447_a as the
 * selection ray, distinguished only by the stopOnLiquid flag, so the
 * collisionRayTrace calls inside it are redirected: only when the flag is set
 * does a missed pickle column fall back to a full-cell intercept. Selection
 * outlines, arrows and every stopOnLiquid=false ray keep seeing the plain
 * column, and the collision box is untouched.
 */
@Mixin(World.class)
public abstract class MixinWorld {

	@Redirect(method = "func_147447_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;collisionRayTrace(Lnet/minecraft/world/World;IIILnet/minecraft/util/Vec3;Lnet/minecraft/util/Vec3;)Lnet/minecraft/util/MovingObjectPosition;"))
	private MovingObjectPosition etfuturum$liquidRayHitsWholePickleCell(Block block, World world, int x, int y, int z, Vec3 start, Vec3 end, Vec3 outerStart, Vec3 outerEnd, boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock) {
		MovingObjectPosition mop = block.collisionRayTrace(world, x, y, z, start, end);
		if (mop == null && stopOnLiquid && block == ModBlocks.SEA_PICKLE.get()) {
			// 26.2 waterlogged fluid clip: the cell counts as full water for
			// bucket rays even where the column leaves it open
			AxisAlignedBB cell = AxisAlignedBB.getBoundingBox(x, y, z, x + 1.0D, y + 1.0D, z + 1.0D);
			MovingObjectPosition intercept = cell.calculateIntercept(start, end);
			if (intercept != null) {
				double hx = intercept.hitVec.xCoord - x;
				double hy = intercept.hitVec.yCoord - y;
				double hz = intercept.hitVec.zCoord - z;
				int side;
				if (hy <= 0.0D) {
					side = 0;
				} else if (hy >= 1.0D) {
					side = 1;
				} else if (hz <= 0.0D) {
					side = 2;
				} else if (hz >= 1.0D) {
					side = 3;
				} else if (hx <= 0.0D) {
					side = 4;
				} else {
					side = 5;
				}
				return new MovingObjectPosition(x, y, z, side, intercept.hitVec);
			}
		}
		return mop;
	}
}
