package ganymedes01.etfuturum.client.renderer.block;

import com.gtnewhorizons.angelica.api.ThreadSafeISBRH;
import ganymedes01.etfuturum.waterlog.StairWaterlog;
import ganymedes01.etfuturum.waterlog.client.StairWaterRenderer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.ForgeHooksClient;

/**
 * Pass 1 renderer for seagrass and kelp: paints a full still-water volume
 * (so biome water colour and adjacent water faces stay seamless) and then
 * draws the crossed-squares plant on top of it.
 */
@ThreadSafeISBRH(perThread = false)
public class BlockOceanPlantRenderer extends BlockModelBase {

	public BlockOceanPlantRenderer(int modelID) {
		super(modelID);
		set2DInventory();
	}

	@Override
	protected void renderInventoryModel(Block block, int meta, int modelId, RenderBlocks renderer, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		renderer.setRenderBounds(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
		Tessellator.instance.setColorOpaque_F(1.0F, 1.0F, 1.0F);
		renderer.drawCrossedSquares(block.getIcon(2, meta), 0.0D, 0.0D, 0.0D, 1.0F);
	}

	@Override
	public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
		if (ForgeHooksClient.getWorldRenderPass() != 1) {
			return false;
		}

		renderStillWaterVolume(world, x, y, z);

		Tessellator tessellator = Tessellator.instance;
		tessellator.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
		tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
		renderer.drawCrossedSquares(block.getIcon(world, x, y, z, 2), (double) x, (double) y, (double) z, 1.0F);
		return true;
	}

	/**
	 * Paints a static water volume for pseudo-waterlogged plant blocks. The
	 * vanilla liquid renderer derives both the texture (still vs flow) and the
	 * surface height from the surrounding flow state, reading the block's own
	 * metadata as a flow decay - which made the painted water show the flowing
	 * texture and uneven heights around plants. This replicates the vanilla
	 * liquid geometry instead but always as a still cell: still texture on the
	 * top/bottom faces, flow texture on the sides (like vanilla static water),
	 * biome tint and per-face shading, with faces against water, opaque
	 * neighbours or visually waterlogged stairs culled (the last on the same
	 * rule the stair-water shell mixin applies to real water). The surface
	 * height uses the vanilla corner averaging (RenderBlocks#getLiquidHeight,
	 * L4343-4381) so the top face sits flush with surrounding pool water.
	 * The icons stay the vanilla water references so Angelica's sprite-based
	 * water tagging keeps applying the shader pack's water program to these quads.
	 */
	public static void renderStillWaterVolume(IBlockAccess world, int x, int y, int z) {
		Tessellator tessellator = Tessellator.instance;
		IIcon still = Blocks.water.getIcon(0, 0);
		IIcon flow = Blocks.water.getIcon(2, 0);
		double eps = 0.001D;

		int colour = Blocks.water.colorMultiplier(world, x, y, z);
		float r = (colour >> 16 & 255) / 255.0F;
		float g = (colour >> 8 & 255) / 255.0F;
		float b = (colour & 255) / 255.0F;

		// Corner heights, vanilla renderBlockLiquid L4164-4167. Like vanilla, the
		// eps offset is subtracted in place when the top face renders, so side
		// faces share the slightly lowered top edge (L4184-4187); when the top is
		// covered the sides keep the raw full height. The height resolution is
		// the shared stair-water one, so plant water and stair complement water
		// agree on every shared corner.
		double d2 = StairWaterRenderer.getLiquidHeight(world, x, y, z);
		double d3 = StairWaterRenderer.getLiquidHeight(world, x, y, z + 1);
		double d4 = StairWaterRenderer.getLiquidHeight(world, x + 1, y, z + 1);
		double d5 = StairWaterRenderer.getLiquidHeight(world, x + 1, y, z);

		if (!isFaceCulled(world, x, y + 1, z)) {
			d2 -= eps;
			d3 -= eps;
			d4 -= eps;
			d5 -= eps;
			tessellator.setBrightness(Blocks.water.getMixedBrightnessForBlock(world, x, y, z));
			tessellator.setColorOpaque_F(r, g, b);
			quad(tessellator, still,
					x, y + d2, z, 0, 0,
					x, y + d3, z + 1, 0, 16,
					x + 1, y + d4, z + 1, 16, 16,
					x + 1, y + d5, z, 16, 0);
		}

		// Vanilla shades the bottom face grey without biome tint
		if (!isFaceCulled(world, x, y - 1, z)) {
			tessellator.setBrightness(Blocks.water.getMixedBrightnessForBlock(world, x, y - 1, z));
			tessellator.setColorOpaque_F(0.5F, 0.5F, 0.5F);
			quad(tessellator, still,
					x, y + eps, z, 0, 0,
					x + 1, y + eps, z, 16, 0,
					x + 1, y + eps, z + 1, 16, 16,
					x, y + eps, z + 1, 0, 16);
		}

		int[] dx = {0, 0, -1, 1};
		int[] dz = {-1, 1, 0, 0};
		for (int dir = 0; dir < 4; dir++) {
			if (isFaceCulled(world, x + dx[dir], y, z + dz[dir])) {
				continue;
			}
			tessellator.setBrightness(Blocks.water.getMixedBrightnessForBlock(world, x + dx[dir], y, z + dz[dir]));
			float shade = dir < 2 ? 0.8F : 0.6F;
			tessellator.setColorOpaque_F(shade * r, shade * g, shade * b);
			// V mapping follows vanilla L4319-4321: V_top = (1 - cornerHeight) * 8,
			// V_bottom = 8, so side quads meet the lowered top edge seamlessly
			switch (dir) {
			case 0: // north, corners (x,z) -> (x+1,z)
				quad(tessellator, flow,
						x, y + d2, z + eps, 0, (1.0D - d2) * 8.0D,
						x + 1, y + d5, z + eps, 8, (1.0D - d5) * 8.0D,
						x + 1, y, z + eps, 8, 8,
						x, y, z + eps, 0, 8);
				break;
			case 1: // south, corners (x+1,z+1) -> (x,z+1)
				quad(tessellator, flow,
						x + 1, y + d4, z + 1 - eps, 0, (1.0D - d4) * 8.0D,
						x, y + d3, z + 1 - eps, 8, (1.0D - d3) * 8.0D,
						x, y, z + 1 - eps, 8, 8,
						x + 1, y, z + 1 - eps, 0, 8);
				break;
			case 2: // west, corners (x,z+1) -> (x,z)
				quad(tessellator, flow,
						x + eps, y + d3, z + 1, 0, (1.0D - d3) * 8.0D,
						x + eps, y + d2, z, 8, (1.0D - d2) * 8.0D,
						x + eps, y, z, 8, 8,
						x + eps, y, z + 1, 0, 8);
				break;
			default: // east, corners (x+1,z) -> (x+1,z+1)
				quad(tessellator, flow,
						x + 1 - eps, y + d5, z, 0, (1.0D - d5) * 8.0D,
						x + 1 - eps, y + d4, z + 1, 8, (1.0D - d4) * 8.0D,
						x + 1 - eps, y, z + 1, 8, 8,
						x + 1 - eps, y, z, 0, 8);
				break;
			}
		}
	}

	/**
	 * Liquid faces are hidden against water, opaque neighbours or a visually
	 * waterlogged stair - the last is the same rule the shell mixin enforces
	 * for real water in renderBlockLiquid (all six faces), so the plants' own
	 * water volume does not wrap a waterlogged stair in a flowing-texture
	 * shell either; the stair's complement water owns that interface.
	 */
	private static boolean isFaceCulled(IBlockAccess world, int nx, int ny, int nz) {
		Block neighbour = world.getBlock(nx, ny, nz);
		if (neighbour.getMaterial() == Material.water || neighbour.isOpaqueCube()) {
			return true;
		}
		return neighbour instanceof BlockStairs && StairWaterlog.shouldRenderWater(world, nx, ny, nz);
	}

	/** Emits one double-sided quad (both windings, like the vanilla liquid renderer). */
	private static void quad(Tessellator tessellator, IIcon icon,
			double x1, double y1, double z1, double u1, double v1,
			double x2, double y2, double z2, double u2, double v2,
			double x3, double y3, double z3, double u3, double v3,
			double x4, double y4, double z4, double u4, double v4) {
		tessellator.addVertexWithUV(x1, y1, z1, icon.getInterpolatedU(u1), icon.getInterpolatedV(v1));
		tessellator.addVertexWithUV(x2, y2, z2, icon.getInterpolatedU(u2), icon.getInterpolatedV(v2));
		tessellator.addVertexWithUV(x3, y3, z3, icon.getInterpolatedU(u3), icon.getInterpolatedV(v3));
		tessellator.addVertexWithUV(x4, y4, z4, icon.getInterpolatedU(u4), icon.getInterpolatedV(v4));
		tessellator.addVertexWithUV(x1, y1, z1, icon.getInterpolatedU(u1), icon.getInterpolatedV(v1));
		tessellator.addVertexWithUV(x4, y4, z4, icon.getInterpolatedU(u4), icon.getInterpolatedV(v4));
		tessellator.addVertexWithUV(x3, y3, z3, icon.getInterpolatedU(u3), icon.getInterpolatedV(v3));
		tessellator.addVertexWithUV(x2, y2, z2, icon.getInterpolatedU(u2), icon.getInterpolatedV(v2));
	}
}
