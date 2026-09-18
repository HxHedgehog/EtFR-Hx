package ganymedes01.etfuturum.waterlog.client;

import ganymedes01.etfuturum.waterlog.StairWaterlog;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStairs;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import java.util.ArrayList;
import java.util.List;

/**
 * Still water paint for the stateless waterlogged stair. The painted volume
 * is the exact complement of the stair as vanilla builds it: a 1.7.10 stair
 * is up to three boxes - the slab (func_150147_e), the step box
 * (func_150145_f: half footprint for a straight stair, one quarter for an
 * outside corner) and, for inside corners, a third box (func_150144_g). We
 * call those same functions, project the boxes onto a 2x2 quarter-cell
 * occupancy grid and take its complement as a disjoint rectangle cover: a
 * straight stair leaves a half column, an outside corner leaves an L (three
 * quarters) with two openings, an inside corner leaves one quarter column
 * with two half openings. Faces are derived generically from those
 * rectangles instead of being hand-enumerated per facing, so the three
 * shapes cannot drift apart. Internal edges against the stair's own step
 * boxes get no water face - the stair renders those opaque risers itself.
 * <p>
 * A side face touching another liquid cell is not painted: the shell mixin
 * suppresses the real water's matching face too, so a continuous water body
 * has zero interior quads, exactly like two adjacent vanilla water cells.
 * The icons stay the vanilla water references so Angelica's sprite-based
 * water tagging keeps applying the shader pack's water program.
 */
public class StairWaterRenderer {

	private static final double EPS = 0.001D;

	/**
	 * Paints the water complement and reports whether any face was actually
	 * emitted - the volume can be empty when every face is culled (e.g. the
	 * stair is fully surrounded by water with water above). Callers must use
	 * this as the renderBlockByRenderType result: reporting content for an
	 * empty pass1 makes WorldRenderer sort an empty tessellator and crash.
	 */
	public static boolean renderWaterVolume(IBlockAccess world, int x, int y, int z, Tessellator tessellatorIn) {
		if (!StairWaterlog.shouldRenderWater(world, x, y, z)) {
			return false; // stateless gate: callers only know the block is a stair
		}
		// Celeritas hands each worker thread its own LocalTessellator; only the
		// vanilla pipeline uses the static instance
		final Tessellator tessellator = tessellatorIn != null ? tessellatorIn : Tessellator.instance;
		IIcon still = Blocks.water.getIcon(0, 0);
		IIcon flow = Blocks.water.getIcon(2, 0);

		int colour = Blocks.water.colorMultiplier(world, x, y, z);
		float r = (colour >> 16 & 255) / 255.0F;
		float g = (colour >> 8 & 255) / 255.0F;
		float b = (colour & 255) / 255.0F;

		// Liquid surface heights at the four cell corners (vanilla corner averaging)
		double h00 = BlockWaterRenderer.getLiquidHeight(world, x, y, z);         // corner (x0, z0)
		double h01 = BlockWaterRenderer.getLiquidHeight(world, x, y, z + 1);     // corner (x0, z1)
		double h11 = BlockWaterRenderer.getLiquidHeight(world, x + 1, y, z + 1); // corner (x1, z1)
		double h10 = BlockWaterRenderer.getLiquidHeight(world, x + 1, y, z);     // corner (x1, z0)

		List<CQ> faces = buildComplementFaces(world, x, y, z, h00, h01, h11, h10);
		for (CQ face : faces) {
			if (face.dir < 0) {
				tessellator.setBrightness(Blocks.water.getMixedBrightnessForBlock(world, x, y, z));
				tessellator.setColorOpaque_F(r, g, b);
				emit(tessellator, still, face);
			} else {
				int nx = x + (face.dir == 0 ? -1 : face.dir == 1 ? 1 : 0);
				int nz = z + (face.dir == 2 ? -1 : face.dir == 3 ? 1 : 0);
				tessellator.setBrightness(Blocks.water.getMixedBrightnessForBlock(world, nx, y, nz));
				float shade = face.dir < 2 ? 0.8F : 0.6F;
				tessellator.setColorOpaque_F(shade * r, shade * g, shade * b);
				emit(tessellator, flow, face);
			}
		}
		return !faces.isEmpty();
	}

	/** One planned water quad: 4 world corners plus UVs; dir -1 is the top. */
	private static final class CQ {
		final double[][] p = new double[4][3];
		final double[][] uv = new double[4][2];
		final int dir;

		CQ(int dir) {
			this.dir = dir;
		}
	}

	/** Footprint rectangle in cell-local fractions, e.g. [0,0]-[0.5,1]. */
	private static final class Rect {
		final double x0, z0, x1, z1;

		Rect(double x0, double z0, double x1, double z1) {
			this.x0 = x0;
			this.z0 = z0;
			this.x1 = x1;
			this.z1 = z1;
		}
	}

	/**
	 * The resolved stair shape: the 2x2 occupancy of the boxes vanilla renders
	 * above the slab plus the vertical range of the water complement. Shared by
	 * the waterlog qualification and the renderer.
	 */
	public static final class Shape {
		/** Occupancy of the upper boxes: [qz][qx]. */
		public final boolean[][] solid = new boolean[2][2];
		public final boolean upper;
		/** Lower end of the water column (0.5 lower-half stairs, 0 upper). */
		public final double yBottom;

		Shape(boolean upper) {
			this.upper = upper;
			this.yBottom = upper ? 0.0D : 0.5D;
		}
	}

	/**
	 * Resolves the actual rendered stair shape by calling vanilla's own stair
	 * box functions (the exact sequence renderBlockStairs and
	 * addCollisionBoxesToList use), so the water complement can never disagree
	 * with the wood. Block bounds are global Block state mutated by those
	 * functions, so the bounds are reset afterwards, exactly as
	 * BlockStairs#addCollisionBoxesToList does.
	 */
	public static Shape resolveShape(IBlockAccess world, int x, int y, int z) {
		BlockStairs stair = (BlockStairs) world.getBlock(x, y, z);
		int meta = world.getBlockMetadata(x, y, z);
		Shape shape = new Shape((meta & 4) != 0);
		try {
			boolean straight = stair.func_150145_f(world, x, y, z);
			markFootprint(shape.solid, stair);
			if (straight && stair.func_150144_g(world, x, y, z)) {
				markFootprint(shape.solid, stair);
			}
		} finally {
			stair.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
		}
		return shape;
	}

	/** Marks the 2x2 quarter cells covered by the block's current bounds. */
	private static void markFootprint(boolean[][] solid, Block block) {
		double minX = block.getBlockBoundsMinX();
		double minZ = block.getBlockBoundsMinZ();
		double maxX = block.getBlockBoundsMaxX();
		double maxZ = block.getBlockBoundsMaxZ();
		for (int qz = 0; qz < 2; qz++) {
			for (int qx = 0; qx < 2; qx++) {
				double cx = (qx + 0.5D) * 0.5D;
				double cz = (qz + 0.5D) * 0.5D;
				if (cx >= minX - 1e-9 && cx <= maxX + 1e-9
						&& cz >= minZ - 1e-9 && cz <= maxZ + 1e-9) {
					solid[qz][qx] = true;
				}
			}
		}
	}

	/**
	 * Disjoint rectangle cover of the non-solid quarter cells. Greedily grows
	 * each unclaimed cell across its row then across rows, which on a 2x2 grid
	 * yields one rect for half/quarter shapes and two for the L shape.
	 */
	private static List<Rect> waterRects(boolean[][] solid) {
		final List<Rect> rects = new ArrayList<Rect>();
		final boolean[][] used = new boolean[2][2];
		for (int qz = 0; qz < 2; qz++) {
			for (int qx = 0; qx < 2; qx++) {
				if (solid[qz][qx] || used[qz][qx]) {
					continue;
				}
				int ex = qx;
				while (ex + 1 < 2 && !solid[qz][ex + 1] && !used[qz][ex + 1]) {
					ex++;
				}
				int ez = qz;
				while (ez + 1 < 2) {
					boolean allFree = true;
					for (int ix = qx; ix <= ex; ix++) {
						if (solid[ez + 1][ix] || used[ez + 1][ix]) {
							allFree = false;
							break;
						}
					}
					if (!allFree) {
						break;
					}
					ez++;
				}
				for (int iz = qz; iz <= ez; iz++) {
					for (int ix = qx; ix <= ex; ix++) {
						used[iz][ix] = true;
					}
				}
				rects.add(new Rect(qx * 0.5D, qz * 0.5D, (ex + 1) * 0.5D, (ez + 1) * 0.5D));
			}
		}
		return rects;
	}

	/**
	 * Builds the complement face list: top quads (lower-half stairs, unless
	 * the neighbour above hides them) and one side quad per covered
	 * cell-border interval. Faces against the stair's own step boxes are
	 * never emitted - the stair renders those opaque risers itself. Quads
	 * whose whole top edge collapses onto the bottom edge (dry shore
	 * corners) are dropped.
	 */
	private static List<CQ> buildComplementFaces(IBlockAccess world, int x, int y, int z,
			double h00, double h01, double h11, double h10) {
		final List<CQ> faces = new ArrayList<CQ>();
		final Shape shape = resolveShape(world, x, y, z);
		final List<Rect> rects = waterRects(shape.solid);
		if (rects.isEmpty()) {
			return faces;
		}
		final double botY = shape.yBottom;
		final double vBot = v(botY);

		// Surface heights: upper-half columns are flat-capped by the slab at
		// y 0.5; lower-half ones follow the corner heights, eps-lowered when
		// the top is painted and clamped to the slab top so a shore dip cannot
		// invert a face.
		final boolean topVisible = !shape.upper && !BlockWaterRenderer.isFaceCulled(world, x, y + 1, z);
		final double a00 = h00 - EPS, a01 = h01 - EPS, a11 = h11 - EPS, a10 = h10 - EPS;

		// Top surface quads over the water footprint.
		if (topVisible) {
			for (Rect rect : rects) {
				double y0 = topY(shape, rect.x0, rect.z0, a00, a01, a11, a10);
				double y1 = topY(shape, rect.x0, rect.z1, a00, a01, a11, a10);
				double y2 = topY(shape, rect.x1, rect.z1, a00, a01, a11, a10);
				double y3 = topY(shape, rect.x1, rect.z0, a00, a01, a11, a10);
				if (Math.max(Math.max(y0, y1), Math.max(y2, y3)) <= botY + 1e-4D) {
					continue;
				}
				CQ f = new CQ(-1);
				set(f, 0, x + rect.x0, y + y0, z + rect.z0, 16 * rect.x0, 16 * rect.z0);
				set(f, 1, x + rect.x0, y + y1, z + rect.z1, 16 * rect.x0, 16 * rect.z1);
				set(f, 2, x + rect.x1, y + y2, z + rect.z1, 16 * rect.x1, 16 * rect.z1);
				set(f, 3, x + rect.x1, y + y3, z + rect.z0, 16 * rect.x1, 16 * rect.z0);
				faces.add(f);
			}
		}

		// Side faces: collect the covered intervals on each cell border.
		for (int dir = 0; dir < 4; dir++) {
			int nx = x + (dir == 0 ? -1 : dir == 1 ? 1 : 0);
			int nz = z + (dir == 2 ? -1 : dir == 3 ? 1 : 0);
			if (BlockWaterRenderer.isFaceCulled(world, nx, y, nz)) {
				continue;
			}
			List<double[]> intervals = borderIntervals(rects, dir);
			for (double[] interval : intervals) {
				double a0 = interval[0], a1 = interval[1];
				double t0, t1;
				if (shape.upper) {
					t0 = t1 = 0.5D;
				} else {
					// axis coordinate along the border; the fixed coordinate is
					// the border itself (xf 0/1 or zf 0/1)
					t0 = topY(shape, dir < 2 ? (dir == 0 ? 0.0D : 1.0D) : a0,
							dir < 2 ? a0 : (dir == 2 ? 0.0D : 1.0D), a00, a01, a11, a10);
					t1 = topY(shape, dir < 2 ? (dir == 0 ? 0.0D : 1.0D) : a1,
							dir < 2 ? a1 : (dir == 2 ? 0.0D : 1.0D), a00, a01, a11, a10);
				}
				if (Math.max(t0, t1) <= botY + 1e-4D) {
					continue;
				}
				CQ f = new CQ(dir);
				double fixed = dir == 0 ? EPS : dir == 1 ? 1.0D - EPS : dir == 2 ? EPS : 1.0D - EPS;
				double uSpan = 8.0D * (a1 - a0);
				if (dir < 2) { // X face: interval runs along Z
					double wx = x + fixed;
					set(f, 0, wx, y + t0, z + a0, 0, v(t0));
					set(f, 1, wx, y + t1, z + a1, uSpan, v(t1));
					set(f, 2, wx, y + botY, z + a1, uSpan, vBot);
					set(f, 3, wx, y + botY, z + a0, 0, vBot);
				} else { // Z face: interval runs along X
					double wz = z + fixed;
					set(f, 0, x + a0, y + t0, wz, 0, v(t0));
					set(f, 1, x + a1, y + t1, wz, uSpan, v(t1));
					set(f, 2, x + a1, y + botY, wz, uSpan, vBot);
					set(f, 3, x + a0, y + botY, wz, 0, vBot);
				}
				faces.add(f);
			}
		}
		return faces;
	}

	/** Surface height of a lower-half column at a cell-local point. */
	private static double topY(Shape shape, double xf, double zf, double a00, double a01, double a11, double a10) {
		if (shape.upper) {
			return 0.5D;
		}
		double h = (1.0D - zf) * ((1.0D - xf) * a00 + xf * a10)
				+ zf * ((1.0D - xf) * a01 + xf * a11);
		return Math.max(h, shape.yBottom);
	}

	private static void set(CQ f, int i, double x, double y, double z, double u, double vv) {
		f.p[i][0] = x;
		f.p[i][1] = y;
		f.p[i][2] = z;
		f.uv[i][0] = u;
		f.uv[i][1] = vv;
	}

	/**
	 * Covered intervals [start,end] (cell-local, along the border axis) of the
	 * water rectangles touching a cell border; adjacent intervals are merged.
	 * dir: 0 -X (axis z), 1 +X (axis z), 2 -Z (axis x), 3 +Z (axis x).
	 */
	private static List<double[]> borderIntervals(List<Rect> rects, int dir) {
		List<double[]> raw = new ArrayList<double[]>();
		for (Rect rect : rects) {
			switch (dir) {
			case 0:
				if (rect.x0 == 0.0D) {
					raw.add(new double[] {rect.z0, rect.z1});
				}
				break;
			case 1:
				if (rect.x1 == 1.0D) {
					raw.add(new double[] {rect.z0, rect.z1});
				}
				break;
			case 2:
				if (rect.z0 == 0.0D) {
					raw.add(new double[] {rect.x0, rect.x1});
				}
				break;
			default:
				if (rect.z1 == 1.0D) {
					raw.add(new double[] {rect.x0, rect.x1});
				}
				break;
			}
		}
		raw.sort((p, q) -> Double.compare(p[0], q[0]));
		List<double[]> merged = new ArrayList<double[]>();
		for (double[] interval : raw) {
			if (merged.isEmpty() || interval[0] > merged.get(merged.size() - 1)[1] + 1e-9) {
				merged.add(new double[] {interval[0], interval[1]});
			} else {
				merged.get(merged.size() - 1)[1] = Math.max(merged.get(merged.size() - 1)[1], interval[1]);
			}
		}
		return merged;
	}

	private static void emit(Tessellator tessellator, IIcon icon, CQ face) {
		BlockWaterRenderer.quad(tessellator, icon,
				face.p[0][0], face.p[0][1], face.p[0][2], face.uv[0][0], face.uv[0][1],
				face.p[1][0], face.p[1][1], face.p[1][2], face.uv[1][0], face.uv[1][1],
				face.p[2][0], face.p[2][1], face.p[2][2], face.uv[2][0], face.uv[2][1],
				face.p[3][0], face.p[3][1], face.p[3][2], face.uv[3][0], face.uv[3][1]);
	}

	/** Vanilla liquid side-face V mapping: v = (1 - height) * 8. */
	private static double v(double height) {
		return (1.0D - height) * 8.0D;
	}
}
