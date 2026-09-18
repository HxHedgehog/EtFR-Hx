package ganymedes01.etfuturum.client.renderer.block;

import com.gtnewhorizons.angelica.api.ThreadSafeISBRH;
import ganymedes01.etfuturum.blocks.BlockSeaPickle;
import ganymedes01.etfuturum.waterlog.client.BlockWaterRenderer;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.ForgeHooksClient;

import java.util.Random;

/**
 * Renders the sea pickle cluster exactly like modern Minecraft: each pickle is
 * a small cube whose top face has a transparent "bowl" hole, a flat sheen sheet
 * hovering just below the cube top (visible through the hole), and - on wet
 * pickles only - two crossed sprout fins poking above the top. Cluster layouts
 * per pickle count are transcribed from the vanilla block models; face windings
 * and UV orientations follow the vanilla RenderBlocks face renderers.
 * Like the modern blockstate (four model entries with y = 0/90/180/270) the
 * whole cluster is rotated by a deterministic per-position rotation, so
 * neighbouring pickle clusters do not all face the same way.
 * Wet pickles additionally paint a full still-water volume underneath in pass 1.
 */
@ThreadSafeISBRH(perThread = false)
public class BlockSeaPickleRenderer extends BlockModelBase {

	// Cluster layouts from the modern block models at y-rotation 0, pixel units (1/16 block):
	// {minX, minZ, maxX, maxZ, top}
	private static final float[][] CLUSTER_1 = {{6, 6, 10, 10, 6}};
	private static final float[][] CLUSTER_2 = {{3, 3, 7, 7, 6}, {8, 8, 12, 12, 4}};
	private static final float[][] CLUSTER_3 = {{6, 9, 10, 13, 6}, {2, 2, 6, 6, 4}, {8, 4, 12, 8, 6}};
	private static final float[][] CLUSTER_4 = {{2, 2, 6, 6, 6}, {9, 10, 13, 14, 4}, {9, 2, 13, 6, 6}, {2, 8, 6, 12, 7}};
	private static final float[][][] CLUSTERS = {null, CLUSTER_1, CLUSTER_2, CLUSTER_3, CLUSTER_4};

	// Side texture u-pairs of the y=0 model, indexed by original face N,S,W,E
	private static final int[][] SIDE_U = {{4, 8}, {0, 4}, {8, 12}, {12, 16}};

	// For cluster y-rotation r: which original face (N,S,W,E = 0..3) supplies the
	// texture for each world face (indexed in world order N,S,W,E) - the texture
	// rotates rigidly with the cluster, so the world face inherits the original
	// face's UVs. Shading still follows the world-facing direction.
	private static final int[][] FACE_MAP = {
			{0, 1, 2, 3},
			{2, 3, 1, 0},
			{1, 0, 3, 2},
			{3, 2, 0, 1}
	};

	public BlockSeaPickleRenderer(int modelID) {
		super(modelID);
		set2DInventory();
	}

	@Override
	protected void renderInventoryModel(Block block, int meta, int modelId, RenderBlocks renderer, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		// The item icon is a single pickle (the first slug of the cluster)
		renderStandardInventoryCube(block, meta, modelId, renderer, 0.375F, 0.0F, 0.375F, 0.625F, 0.375F, 0.625F);
	}

	@Override
	public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
		boolean wet = block instanceof BlockSeaPickle && ((BlockSeaPickle) block).isWet();
		int pass = ForgeHooksClient.getWorldRenderPass();

		if (pass == 1) {
			if (!wet) {
				return false;
			}
			BlockWaterRenderer.renderStillWaterVolume(world, x, y, z);
		}

		int meta = world.getBlockMetadata(x, y, z);
		Tessellator tessellator = Tessellator.instance;
		tessellator.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
		IIcon icon = block.getIcon(0, meta);

		boolean skipBottom = world.getBlock(x, y - 1, z).isOpaqueCube();
		int rotation = positionRotation(x, y, z);
		float[][] cluster = CLUSTERS[BlockSeaPickle.getPickleCount(meta)];
		for (float[] pickle : cluster) {
			renderPickle(tessellator, renderer, block, icon, x, y, z, pickle, rotation, skipBottom, wet);
		}

		renderer.setRenderBounds(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
		return true;
	}

	/**
	 * Modern random-variant selection (blockstate array of 4 weighted model
	 * entries): the model is picked with a position-seeded random. The seed is
	 * the vanilla Mth#getSeed position hash, and RandomSource#create makes a
	 * LegacyRandomSource - the classic java LCG, i.e. java.util.Random - so the
	 * same position rotates the same way it does in modern Minecraft.
	 */
	private static int positionRotation(int x, int y, int z) {
		long seed = (long) x * 3129871L ^ (long) z * 116129781L ^ (long) y;
		seed = seed * seed * 42317861L + seed * 11L;
		return new Random(seed >> 16).nextInt(4);
	}

	/** Rotates a pixel-space point around block centre (8,8): rot*90 clockwise viewed from above. */
	private static double[] rotatePoint(double px, double pz, int rot) {
		if (rot == 0) {
			return new double[] {px, pz};
		}
		double dx = px - 8.0D;
		double dz = pz - 8.0D;
		switch (rot) {
			case 1:
				return new double[] {8.0D - dz, 8.0D + dx};
			case 2:
				return new double[] {8.0D - dx, 8.0D - dz};
			default:
				return new double[] {8.0D + dz, 8.0D - dx};
		}
	}

	/** Rotates the pickle bounds {minX,minZ,maxX,maxZ,top} by rot*90 around block centre. */
	private static float[] rotateBounds(float[] p, int rot) {
		if (rot == 0) {
			return p;
		}
		if (rot == 1) {
			return new float[] {16.0F - p[3], p[0], 16.0F - p[1], p[2], p[4]};
		}
		if (rot == 2) {
			return new float[] {16.0F - p[2], 16.0F - p[3], 16.0F - p[0], 16.0F - p[1], p[4]};
		}
		return new float[] {p[1], 16.0F - p[2], p[3], 16.0F - p[0], p[4]};
	}

	/** Renders one pickle: body cube, sheen sheet and (wet only) sprout fins. Coordinates in pixels. */
	private void renderPickle(Tessellator tessellator, RenderBlocks renderer, Block block, IIcon icon, int x, int y, int z, float[] pickle, int rotation, boolean skipBottom, boolean wet) {
		float[] b = rotateBounds(pickle, rotation);
		double x1 = x + b[0] / 16.0D;
		double z1 = z + b[1] / 16.0D;
		double x2 = x + b[2] / 16.0D;
		double z2 = z + b[3] / 16.0D;
		double top = y + b[4] / 16.0D;
		double bottom = y;
		double sideV = 5 + b[4]; // side faces span texture rows 5..5+top

		// World-space footprint corners c0..c3, horizontal face UVs attached per corner
		double[][] corners = {{x1, z1}, {x2, z1}, {x2, z2}, {x1, z2}};
		double[] upU = {4, 8, 8, 4};
		double[] upV = {1, 1, 5, 5};

		// Body top face with the bowl hole (vertex orders keep the existing windings)
		quadHorizontal(tessellator, icon, corners, new int[] {2, 1, 0, 3}, upU, upV, top, 1.0F);

		// Bottom face culled against opaque ground like the model's cullface
		if (!skipBottom) {
			double[] downU = {8, 12, 12, 8};
			quadHorizontal(tessellator, icon, corners, new int[] {3, 0, 1, 2}, downU, upV, bottom, 0.5F);
		}

		// Side faces: texture from the original face mapped to this world direction
		for (int dir = 0; dir < 4; dir++) {
			int[] u = SIDE_U[FACE_MAP[rotation][dir]];
			switch (dir) {
				case 0: // north
					quadNorth(tessellator, icon, x1, x2, z1, bottom, top, u[0], 5, u[1], sideV, 0.8F);
					break;
				case 1: // south
					quadSouth(tessellator, icon, x1, x2, z2, bottom, top, u[0], 5, u[1], sideV, 0.8F);
					break;
				case 2: // west
					quadWest(tessellator, icon, x1, z1, z2, bottom, top, u[0], 5, u[1], sideV, 0.6F);
					break;
				default: // east
					quadEast(tessellator, icon, x2, z1, z2, bottom, top, u[0], 5, u[1], sideV, 0.6F);
					break;
			}
		}

		// Sheen sheet hovering just below the cube top, visible through the top's bowl hole
		quadHorizontal(tessellator, icon, corners, new int[] {2, 1, 0, 3}, new double[] {8, 12, 12, 8}, upV, top - 0.05D / 16.0D, 1.0F);

		if (wet) {
			// Two crossed sprout fins along the diagonals of a 1px box centred on the pickle.
			// Corner points are built from the UNROTATED layout centre and rotated exactly
			// once around block centre - using b's already-rotated centre here would rotate
			// them twice (at 90/270 that maps each diagonal back to itself, so the fin
			// texture appeared not to rotate; for off-centre pickles the fins detached).
			// They are then passed as offsets relative to (x, y, z) - renderRawDoubleSidedFace
			// adds the world position itself.
			double cxPx = (pickle[0] + pickle[2]) / 2.0D;
			double czPx = (pickle[1] + pickle[3]) / 2.0D;
			double[][] fin = {
					rotatePoint(cxPx - 0.5D, czPx - 0.5D, rotation),
					rotatePoint(cxPx + 0.5D, czPx + 0.5D, rotation),
					rotatePoint(cxPx - 0.5D, czPx + 0.5D, rotation),
					rotatePoint(cxPx + 0.5D, czPx - 0.5D, rotation)
			};
			double base = (b[4] - 0.8D) / 16.0D;
			double tip = base + 3.5D / 16.0D;
			renderRawDoubleSidedFace(renderer, block, x, y, z,
					fin[0][0] / 16.0D, fin[1][0] / 16.0D, base, tip, fin[0][1] / 16.0D, fin[1][1] / 16.0D,
					1, 0, 3, 5, icon, 0, 1, 1, 1);
			renderRawDoubleSidedFace(renderer, block, x, y, z,
					fin[2][0] / 16.0D, fin[3][0] / 16.0D, base, tip, fin[2][1] / 16.0D, fin[3][1] / 16.0D,
					13, 0, 15, 5, icon, 0, 1, 1, 1);
		}
	}

	/** Emits one horizontal quad with explicit per-corner UVs (corners indexed c0..c3, order gives winding). */
	private static void quadHorizontal(Tessellator tessellator, IIcon icon, double[][] corners, int[] order,
			double[] us, double[] vs, double y, float shade) {
		tessellator.setColorOpaque_F(shade, shade, shade);
		for (int q : order) {
			tessellator.addVertexWithUV(corners[q][0], y, corners[q][1], icon.getInterpolatedU(us[q]), icon.getInterpolatedV(vs[q]));
		}
	}

	private static void quadNorth(Tessellator tessellator, IIcon icon, double x1, double x2, double z, double bottom, double top, double u1, double v1, double u2, double v2, float shade) {
		tessellator.setColorOpaque_F(shade, shade, shade);
		tessellator.addVertexWithUV(x1, top, z, icon.getInterpolatedU(u2), icon.getInterpolatedV(v1));
		tessellator.addVertexWithUV(x2, top, z, icon.getInterpolatedU(u1), icon.getInterpolatedV(v1));
		tessellator.addVertexWithUV(x2, bottom, z, icon.getInterpolatedU(u1), icon.getInterpolatedV(v2));
		tessellator.addVertexWithUV(x1, bottom, z, icon.getInterpolatedU(u2), icon.getInterpolatedV(v2));
	}

	private static void quadSouth(Tessellator tessellator, IIcon icon, double x1, double x2, double z, double bottom, double top, double u1, double v1, double u2, double v2, float shade) {
		tessellator.setColorOpaque_F(shade, shade, shade);
		tessellator.addVertexWithUV(x1, top, z, icon.getInterpolatedU(u1), icon.getInterpolatedV(v1));
		tessellator.addVertexWithUV(x1, bottom, z, icon.getInterpolatedU(u1), icon.getInterpolatedV(v2));
		tessellator.addVertexWithUV(x2, bottom, z, icon.getInterpolatedU(u2), icon.getInterpolatedV(v2));
		tessellator.addVertexWithUV(x2, top, z, icon.getInterpolatedU(u2), icon.getInterpolatedV(v1));
	}

	private static void quadWest(Tessellator tessellator, IIcon icon, double x, double z1, double z2, double bottom, double top, double u1, double v1, double u2, double v2, float shade) {
		tessellator.setColorOpaque_F(shade, shade, shade);
		tessellator.addVertexWithUV(x, top, z2, icon.getInterpolatedU(u2), icon.getInterpolatedV(v1));
		tessellator.addVertexWithUV(x, top, z1, icon.getInterpolatedU(u1), icon.getInterpolatedV(v1));
		tessellator.addVertexWithUV(x, bottom, z1, icon.getInterpolatedU(u1), icon.getInterpolatedV(v2));
		tessellator.addVertexWithUV(x, bottom, z2, icon.getInterpolatedU(u2), icon.getInterpolatedV(v2));
	}

	private static void quadEast(Tessellator tessellator, IIcon icon, double x, double z1, double z2, double bottom, double top, double u1, double v1, double u2, double v2, float shade) {
		tessellator.setColorOpaque_F(shade, shade, shade);
		tessellator.addVertexWithUV(x, top, z2, icon.getInterpolatedU(u1), icon.getInterpolatedV(v1));
		tessellator.addVertexWithUV(x, bottom, z2, icon.getInterpolatedU(u1), icon.getInterpolatedV(v2));
		tessellator.addVertexWithUV(x, bottom, z1, icon.getInterpolatedU(u2), icon.getInterpolatedV(v2));
		tessellator.addVertexWithUV(x, top, z1, icon.getInterpolatedU(u2), icon.getInterpolatedV(v1));
	}
}
