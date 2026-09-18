package ganymedes01.etfuturum.waterlog;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.world.IBlockAccess;

/**
 * One block family participating in the stateless visual waterlogging: a
 * membership test, the render-time waterlog qualification and the pass-1
 * water paint. Families register in {@link VisualWaterlog} and every hook
 * (pass gates, water shell suppression, neighbour invalidation, the
 * Angelica meshing path) dispatches through the registry, so a new family
 * slots in without touching the mixins again.
 * <p>
 * The paint entry takes the tessellator explicitly because celeritas hands
 * each meshing worker its own LocalTessellator; null means the static
 * instance of the vanilla pipeline. This interface stays in the common
 * package on purpose: dedicated servers never reach any of its entry
 * points (every hook sits behind the isRemote render guards), so the
 * client type in the paint signature is only ever resolved on the client.
 */
public interface WaterlogFamily {

	/** Whether the block instance belongs to this family (cheap, no world access). */
	boolean isMember(Block block);

	/** The family's config flag; gates the runtime checks when a hook runs for several families. */
	boolean isEnabled();

	/**
	 * The stateless waterlog test: a pure function of the cell's direct
	 * neighbours, evaluated at render time. Must independently re-check the
	 * family config flag - callers only know the block is a member.
	 */
	boolean shouldRenderWater(IBlockAccess world, int x, int y, int z);

	/**
	 * Paints the water volume in pass 1, re-checking the qualification
	 * itself. Returns whether any quad was emitted - the vanilla chunk
	 * builder uses this as the renderBlockByRenderType result, where
	 * reporting content for an empty pass 1 makes WorldRenderer sort an
	 * empty tessellator and crash.
	 */
	boolean paint(IBlockAccess world, int x, int y, int z, Tessellator tessellator);
}
