package ganymedes01.etfuturum.client.renderer.block;

import com.gtnewhorizons.angelica.api.ThreadSafeISBRH;
import ganymedes01.etfuturum.blocks.BlockAquaticPlant;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
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
	 * biome tint and per-face shading, with faces against water or opaque
	 * neighbours culled like BlockLiquid#shouldSideBeRendered. The surface
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
		// covered the sides keep the raw full height.
		double d2 = getLiquidHeight(world, x, y, z);
		double d3 = getLiquidHeight(world, x, y, z + 1);
		double d4 = getLiquidHeight(world, x + 1, y, z + 1);
		double d5 = getLiquidHeight(world, x + 1, y, z);

		if (!isFaceCulled(world.getBlock(x, y + 1, z))) {
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
		if (!isFaceCulled(world.getBlock(x, y - 1, z))) {
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
			if (isFaceCulled(world.getBlock(x + dx[dir], y, z + dz[dir]))) {
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
	 * Port of RenderBlocks#getLiquidHeight (1.7.10 L4343-4381): averages the
	 * liquid air-fraction over the 4 cells around corner (x,z) - including the
	 * sample's above cell for full-height columns. Ocean plant samples force
	 * meta 0 since their metadata stores count/age, not a flow decay.
	 */
	private static double getLiquidHeight(IBlockAccess world, int x, int y, int z) {
		int l = 0;
		float f = 0.0F;

		for (int i1 = 0; i1 < 4; ++i1) {
			int j1 = x - (i1 & 1);
			int k1 = z - (i1 >> 1 & 1);

			if (world.getBlock(j1, y + 1, k1).getMaterial() == Material.water) {
				return 1.0D;
			}

			Block block = world.getBlock(j1, y, k1);
			Material material = block.getMaterial();

			if (material == Material.water) {
				int meta = world.getBlockMetadata(j1, y, k1);
				if (BlockAquaticPlant.isOceanPlant(block)) {
					meta = 0;
				}
				if (meta >= 8 || meta == 0) {
					f += BlockLiquid.getLiquidHeightPercent(meta) * 10.0F;
					l += 10;
				}

				f += BlockLiquid.getLiquidHeightPercent(meta);
				++l;
			} else if (!material.isSolid()) {
				++f;
				++l;
			}
		}

		return 1.0D - (double) (f / (float) l);
	}

	/** Liquid faces are hidden against same-liquid (water) or opaque neighbours, like BlockLiquid#shouldSideBeRendered. */
	private static boolean isFaceCulled(Block neighbour) {
		return neighbour.getMaterial() == Material.water || neighbour.isOpaqueCube();
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
