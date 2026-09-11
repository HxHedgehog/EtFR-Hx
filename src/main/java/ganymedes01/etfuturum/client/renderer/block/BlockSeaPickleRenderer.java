package ganymedes01.etfuturum.client.renderer.block;

import com.gtnewhorizons.angelica.api.ThreadSafeISBRH;
import ganymedes01.etfuturum.blocks.BlockSeaPickle;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.ForgeHooksClient;

/**
 * Renders the sea pickle cluster exactly like modern Minecraft: each pickle is
 * a small cube whose top face has a transparent "bowl" hole, a flat sheen sheet
 * hovering just below the cube top (visible through the hole), and - on wet
 * pickles only - two crossed sprout fins poking above the top. Cluster layouts
 * per pickle count are transcribed from the vanilla block models; face windings
 * and UV orientations follow the vanilla RenderBlocks face renderers.
 * Wet pickles additionally paint a full still-water volume underneath in pass 1.
 */
@ThreadSafeISBRH(perThread = false)
public class BlockSeaPickleRenderer extends BlockModelBase {

	// Cluster layouts from the modern block models, pixel units (1/16 block):
	// {minX, minZ, maxX, maxZ, top}
	private static final float[][] CLUSTER_1 = {{6, 6, 10, 10, 6}};
	private static final float[][] CLUSTER_2 = {{3, 3, 7, 7, 6}, {8, 8, 12, 12, 4}};
	private static final float[][] CLUSTER_3 = {{6, 9, 10, 13, 6}, {2, 2, 6, 6, 4}, {8, 4, 12, 8, 6}};
	private static final float[][] CLUSTER_4 = {{2, 2, 6, 6, 6}, {9, 10, 13, 14, 4}, {9, 2, 13, 6, 6}, {2, 8, 6, 12, 7}};
	private static final float[][][] CLUSTERS = {null, CLUSTER_1, CLUSTER_2, CLUSTER_3, CLUSTER_4};

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
			BlockOceanPlantRenderer.renderStillWaterVolume(world, x, y, z);
		}

		int meta = world.getBlockMetadata(x, y, z);
		Tessellator tessellator = Tessellator.instance;
		tessellator.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
		IIcon icon = block.getIcon(0, meta);

		boolean skipBottom = world.getBlock(x, y - 1, z).isOpaqueCube();
		float[][] cluster = CLUSTERS[BlockSeaPickle.getPickleCount(meta)];
		for (float[] pickle : cluster) {
			renderPickle(tessellator, renderer, block, icon, x, y, z, pickle, skipBottom, wet);
		}

		renderer.setRenderBounds(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
		return true;
	}

	/** Renders one pickle: body cube, sheen sheet and (wet only) sprout fins. Coordinates in pixels. */
	private void renderPickle(Tessellator tessellator, RenderBlocks renderer, Block block, IIcon icon, int x, int y, int z, float[] pickle, boolean skipBottom, boolean wet) {
		double x1 = x + pickle[0] / 16.0D;
		double z1 = z + pickle[1] / 16.0D;
		double x2 = x + pickle[2] / 16.0D;
		double z2 = z + pickle[3] / 16.0D;
		double top = y + pickle[4] / 16.0D;
		double bottom = y;
		double sideV = 5 + pickle[4]; // side faces span texture rows 5..5+top

		// Body cube (bottom face culled against opaque ground like the model's cullface)
		if (!skipBottom) {
			quadDown(tessellator, icon, x1, z1, x2, z2, bottom, 8, 1, 12, 5, 0.5F);
		}
		quadUp(tessellator, icon, x1, z1, x2, z2, top, 4, 1, 8, 5, 1.0F);
		quadNorth(tessellator, icon, x1, x2, z1, bottom, top, 4, 5, 8, sideV, 0.8F);
		quadSouth(tessellator, icon, x1, x2, z2, bottom, top, 0, 5, 4, sideV, 0.8F);
		quadWest(tessellator, icon, x1, z1, z2, bottom, top, 8, 5, 12, sideV, 0.6F);
		quadEast(tessellator, icon, x2, z1, z2, bottom, top, 12, 5, 16, sideV, 0.6F);

		// Sheen sheet hovering just below the cube top, visible through the top's bowl hole
		quadUp(tessellator, icon, x1, z1, x2, z2, top - 0.05D / 16.0D, 8, 1, 12, 5, 1.0F);

		if (wet) {
			// Two crossed sprout fins along the diagonals of a 1px box centred on the pickle.
			// renderRawFace expects offsets relative to (x, y, z) - it adds the world position itself.
			double cx = (x1 + x2) / 2.0D - x;
			double cz = (z1 + z2) / 2.0D - z;
			double half = 0.5D / 16.0D;
			double base = (pickle[4] - 0.8F) / 16.0D;
			double tip = base + 3.5D / 16.0D;
			renderRawDoubleSidedFace(renderer, block, x, y, z, cx - half, cx + half, base, tip, cz - half, cz + half, 1, 0, 3, 5, icon, 1, 1, 1);
			renderRawDoubleSidedFace(renderer, block, x, y, z, cx - half, cx + half, base, tip, cz + half, cz - half, 13, 0, 15, 5, icon, 1, 1, 1);
		}
	}

	private static void quadUp(Tessellator tessellator, IIcon icon, double x1, double z1, double x2, double z2, double y, double u1, double v1, double u2, double v2, float shade) {
		tessellator.setColorOpaque_F(shade, shade, shade);
		tessellator.addVertexWithUV(x2, y, z2, icon.getInterpolatedU(u2), icon.getInterpolatedV(v2));
		tessellator.addVertexWithUV(x2, y, z1, icon.getInterpolatedU(u2), icon.getInterpolatedV(v1));
		tessellator.addVertexWithUV(x1, y, z1, icon.getInterpolatedU(u1), icon.getInterpolatedV(v1));
		tessellator.addVertexWithUV(x1, y, z2, icon.getInterpolatedU(u1), icon.getInterpolatedV(v2));
	}

	private static void quadDown(Tessellator tessellator, IIcon icon, double x1, double z1, double x2, double z2, double y, double u1, double v1, double u2, double v2, float shade) {
		tessellator.setColorOpaque_F(shade, shade, shade);
		tessellator.addVertexWithUV(x1, y, z2, icon.getInterpolatedU(u1), icon.getInterpolatedV(v2));
		tessellator.addVertexWithUV(x1, y, z1, icon.getInterpolatedU(u1), icon.getInterpolatedV(v1));
		tessellator.addVertexWithUV(x2, y, z1, icon.getInterpolatedU(u2), icon.getInterpolatedV(v1));
		tessellator.addVertexWithUV(x2, y, z2, icon.getInterpolatedU(u2), icon.getInterpolatedV(v2));
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
