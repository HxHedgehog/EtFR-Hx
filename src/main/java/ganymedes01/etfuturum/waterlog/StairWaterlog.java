package ganymedes01.etfuturum.waterlog;

import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.waterlog.client.StairWaterRenderer;
import ganymedes01.etfuturum.waterlog.client.StairWaterRenderer.Shape;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStairs;
import net.minecraft.world.IBlockAccess;

/**
 * 26.2-style waterlogging for vanilla stairs, purely visual and stateless.
 * Nothing is stored: the waterlogged look is a pure function of the stair's
 * direct neighbours, re-derived at render time (see
 * {@link #shouldRenderWater}). Section rebuilds are the invalidation
 * mechanism; {@code MixinWorld} keeps adjacent stair sections dirty across
 * client-side water changes so the look follows the water both ways. The
 * stair family of the visual-waterlogging registry; the shared water
 * predicates and the dispatch live in {@link VisualWaterlog}.
 */
public class StairWaterlog {

	/** The opening's outward face per stair orientation (meta &amp; 3). */
	private static final int[] OPEN_DIRS = {4, 5, 2, 3};

	public static boolean isEnabled() {
		return ConfigMixins.stairWaterlogging;
	}

	private static boolean isStair(Block block) {
		return block instanceof BlockStairs;
	}

	/**
	 * Stateless waterlog test evaluated at render time, purely local: a stair
	 * renders its complement water when either water reaches the cell from
	 * above (any form, incl. falling water and ocean plants), or its opening
	 * faces a still water cell while the three remaining exposed sides are
	 * sealed by still water, solid full cubes or further stairs. For an
	 * inside corner (two full backs, two half openings) up to three cells
	 * are checked: the diagonal cell beyond the open corner and the two
	 * orthogonally adjacent cells along the half-opening sides - still
	 * water in any one of them fills the quarter column, while the two
	 * backs are entirely owned by stair geometry and never checked. The face
	 * behind the step is sealed by the stair's own full-height half, so its
	 * neighbour is ignored; the top and bottom faces are deliberately not
	 * checked - a stair at the surface of a one-deep pool (air above) or with
	 * a scooped-out cell below is a valid waterlogged spot, and the renderer's
	 * corner-averaged surface sits flush with the pool's water line either
	 * way. The verdict is a pure function of the cell's direct neighbours: no
	 * flood fill, no component coupling, so one stair never depends on
	 * another's verdict and one cell's change only re-renders its direct
	 * neighbours.
	 * <p>
	 * The straight-stair half-cell model used here ({@link #getWaterBox}) is
	 * the conservative qualification approximation only; the painted geometry
	 * itself comes from the stair's real vanilla boxes in
	 * {@link client.StairWaterRenderer}, so corner stairs get their exact L /
	 * quarter shape. Inside corners bypass the approximation entirely (the
	 * up-to-three-cells still-water rule above). For the rest it never paints water
	 * for a dry stair - at worst it refuses an arrangement whose water only
	 * enters through a side the straight model treats as sealed.
	 * <p>
	 * Known limitation (accepted for now): a straight or outside-corner
	 * stair whose opening faces another stair never qualifies, even when
	 * real water arrives through that stair (same-level L-shaped and winding
	 * stair arrangements, mutually facing pairs). Accepting stair neighbours
	 * as an opening's water source needs a qualification lookahead that
	 * turns mutually facing pairs circular - revisit only if this matters in
	 * practice. Sealing a side with a stair is unconditional, so a qualifying
	 * stair directly beside a dry one shows a water boundary wall against
	 * the dry stair's open half.
	 */
	public static boolean shouldRenderWater(IBlockAccess world, int x, int y, int z) {
		if (!ConfigMixins.stairWaterlogging) {
			return false;
		}
		Block block = world.getBlock(x, y, z);
		if (!isStair(block)) {
			return false;
		}
		// A stream pouring in from above (any water, including falling) wets the
		// stair on its own
		if (VisualWaterlog.isWaterBlock(world.getBlock(x, y + 1, z))) {
			return true;
		}

		// Inside corner (two full backs, two half openings): up to three
		// water-contact cells - the diagonal beyond the open corner and the
		// two half-opening side neighbours. Still water in any one fills the
		// quarter column.
		int[][] waterCells = insideCornerWaterCells(world, x, y, z);
		if (waterCells != null) {
			for (int[] offset : waterCells) {
				int wx = x + offset[0];
				int wz = z + offset[1];
				if (VisualWaterlog.isStillWaterBlock(world.getBlock(wx, y, wz), world.getBlockMetadata(wx, y, wz))) {
					return true;
				}
			}
			return false;
		}

		final int meta = world.getBlockMetadata(x, y, z);
		final double[] box = getWaterBox(meta);

		for (int dir = 2; dir <= 5; dir++) {
			if (isInnerFace(box, dir)) {
				continue; // face behind the step: sealed by the stair's own full-height half
			}
			int nx = x + VisualWaterlog.offsetX(dir);
			int nz = z + VisualWaterlog.offsetZ(dir);
			Block neighbour = world.getBlock(nx, y, nz);
			int nmeta = world.getBlockMetadata(nx, y, nz);
			if (dir == OPEN_DIRS[meta & 3]) {
				// the opening: must hold real still water - it is this cell's water source
				if (!VisualWaterlog.isStillWaterBlock(neighbour, nmeta)) {
					return false;
				}
			} else {
				// half side: sealed by still water, a solid full cube or another stair
				if (!VisualWaterlog.isStillWaterBlock(neighbour, nmeta) && !neighbour.isOpaqueCube()
						&& !(neighbour instanceof BlockStairs)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Straight-stair complement approximation used by the qualification test:
	 * the open half-cell on the facing side, flipped to the lower half for
	 * upper-half stairs. BlockStairs orient 0-3 put the step half at
	 * +X/-X/+Z/-Z (verified against func_150145_f), so the opening - and the
	 * water - sits on the opposite side. Returns
	 * {minX, minY, minZ, maxX, maxY, maxZ}.
	 */
	private static double[] getWaterBox(int meta) {
		double minX = 0.0D, maxX = 1.0D, minZ = 0.0D, maxZ = 1.0D;
		switch (meta & 3) {
		case 0:
			maxX = 0.5D;
			break;
		case 1:
			minX = 0.5D;
			break;
		case 2:
			maxZ = 0.5D;
			break;
		default:
			minZ = 0.5D;
			break;
		}
		double minY = (meta & 4) != 0 ? 0.0D : 0.5D;
		double maxY = (meta & 4) != 0 ? 0.5D : 1.0D;
		return new double[] {minX, minY, minZ, maxX, maxY, maxZ};
	}

	/**
	 * Whether a horizontal face (dir 2 -Z, 3 +Z, 4 -X, 5 +X) is an interface
	 * with the stair body: its outward normal points into the cell interior
	 * occupied by the step half, so that side needs no world contact.
	 */
	private static boolean isInnerFace(double[] box, int dir) {
		switch (dir) {
		case 2:
			return box[2] == 0.5D; // -Z covered by the step half
		case 3:
			return box[5] == 0.5D; // +Z covered by the step half
		case 4:
			return box[0] == 0.5D; // -X covered by the step half
		default:
			return box[3] == 0.5D; // +X covered by the step half
		}
	}

	/**
	 * For an inside corner resolves the up to three cells that can supply
	 * water to its open quarter column: the diagonal cell beyond the open
	 * corner, plus the two orthogonally adjacent cells along the
	 * half-opening sides. Returns null for straight and outside-corner
	 * stairs. The shape comes from the same vanilla-box resolution the
	 * renderer uses ({@link client.StairWaterRenderer#resolveShape}); an
	 * inside corner is identified by exactly three covered quarters.
	 */
	private static int[][] insideCornerWaterCells(IBlockAccess world, int x, int y, int z) {
		Shape shape = StairWaterRenderer.resolveShape(world, x, y, z);
		int qx = -1, qz = -1, free = 0;
		for (int iz = 0; iz < 2; iz++) {
			for (int ix = 0; ix < 2; ix++) {
				if (!shape.solid[iz][ix]) {
					qx = ix;
					qz = iz;
					free++;
				}
			}
		}
		if (free != 1) {
			return null;
		}
		int ox = qx == 1 ? 1 : -1;
		int oz = qz == 1 ? 1 : -1;
		return new int[][] {{ox, 0}, {0, oz}, {ox, oz}};
	}
}
