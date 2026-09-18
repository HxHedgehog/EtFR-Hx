package ganymedes01.etfuturum.waterlog.client;

import ganymedes01.etfuturum.blocks.BlockAquaticPlant;
import ganymedes01.etfuturum.waterlog.VisualWaterlog;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

/**
 * Shared still-water painting for the pseudo-waterlogged blocks: ocean plants,
 * wet sea pickles, the stair complement and (future) waterlogged slabs. Owns
 * the liquid surface height sampling, the face cull rule and the quad
 * emission, so every painted water volume agrees on shared corners and
 * neighbours - the same corner must render at the same height no matter which
 * block painted it.
 * <p>
 * The icons stay the vanilla water references so Angelica's sprite-based
 * water tagging keeps applying the shader pack's water program to these quads.
 */
public class BlockWaterRenderer {

	private static final double EPS = 0.001D;

	/**
	 * Paints a full still water cell, from the cell floor to the liquid surface.
	 *
	 * @return whether any quad was emitted (the volume can be fully culled, e.g.
	 *         a submerged slab whose every face meets water or an opaque block)
	 */
	public static boolean renderStillWaterVolume(IBlockAccess world, int x, int y, int z) {
		return renderWaterVolume(world, x, y, z, 0.0D, false);
	}

	/**
	 * Paints a still water volume occupying the cell from {@code bottomY}
	 * (cell-local) up to the liquid surface. The surface follows the vanilla
	 * corner-averaged liquid height, clamped to the column floor so a shore
	 * dip cannot drop it below the volume's solid bottom (e.g. a slab top);
	 * with {@code surfacePinned} the surface sits flat at {@code bottomY}
	 * instead - the water under an upper-half block fills its half flush to
	 * the block bottom, like the stair complement water.
	 * <p>
	 * The vanilla liquid renderer derives both the texture (still vs flow)
	 * and the surface height from the surrounding flow state, reading the
	 * block's own metadata as a flow decay - which made painted water show
	 * the flowing texture and uneven heights around plants. This replicates
	 * the vanilla liquid geometry instead but always as a still cell: still
	 * texture on the top/bottom faces, flow texture on the sides (like
	 * vanilla static water), biome tint and per-face shading, with faces
	 * against water, opaque neighbours or visually waterlogged stairs culled
	 * (the last on the same rule the stair-water shell mixin applies to real
	 * water). The surface height uses the vanilla corner averaging
	 * (RenderBlocks#getLiquidHeight, L4343-4381) so the top face sits flush
	 * with surrounding pool water.
	 *
	 * @return whether any quad was emitted
	 */
	public static boolean renderWaterVolume(IBlockAccess world, int x, int y, int z, double bottomY, boolean surfacePinned) {
		Tessellator tessellator = Tessellator.instance;
		IIcon still = Blocks.water.getIcon(0, 0);
		IIcon flow = Blocks.water.getIcon(2, 0);
		boolean emitted = false;

		int colour = Blocks.water.colorMultiplier(world, x, y, z);
		float r = (colour >> 16 & 255) / 255.0F;
		float g = (colour >> 8 & 255) / 255.0F;
		float b = (colour & 255) / 255.0F;

		// Corner heights, vanilla renderBlockLiquid L4164-4167. Like vanilla, the
		// eps offset is subtracted in place when the top face renders, so side
		// faces share the slightly lowered top edge (L4184-4187); when the top is
		// covered the sides keep the raw height.
		double d2, d3, d4, d5;
		if (surfacePinned) {
			d2 = d3 = d4 = d5 = bottomY;
		} else {
			d2 = Math.max(getLiquidHeight(world, x, y, z), bottomY);
			d3 = Math.max(getLiquidHeight(world, x, y, z + 1), bottomY);
			d4 = Math.max(getLiquidHeight(world, x + 1, y, z + 1), bottomY);
			d5 = Math.max(getLiquidHeight(world, x + 1, y, z), bottomY);
		}

		if (!surfacePinned && !isFaceCulled(world, x, y + 1, z)
				&& Math.max(Math.max(d2, d3), Math.max(d4, d5)) > bottomY + 1e-4D) {
			d2 -= EPS;
			d3 -= EPS;
			d4 -= EPS;
			d5 -= EPS;
			emitted = true;
			tessellator.setBrightness(Blocks.water.getMixedBrightnessForBlock(world, x, y, z));
			tessellator.setColorOpaque_F(r, g, b);
			quad(tessellator, still,
					x, y + d2, z, 0, 0,
					x, y + d3, z + 1, 0, 16,
					x + 1, y + d4, z + 1, 16, 16,
					x + 1, y + d5, z, 16, 0);
		}

		// Vanilla shades the bottom face grey without biome tint; only full-cell
		// columns have one - higher columns sit on the block's own opaque faces
		if (bottomY == 0.0D && !isFaceCulled(world, x, y - 1, z)) {
			emitted = true;
			tessellator.setBrightness(Blocks.water.getMixedBrightnessForBlock(world, x, y - 1, z));
			tessellator.setColorOpaque_F(0.5F, 0.5F, 0.5F);
			quad(tessellator, still,
					x, y + EPS, z, 0, 0,
					x + 1, y + EPS, z, 16, 0,
					x + 1, y + EPS, z + 1, 16, 16,
					x, y + EPS, z + 1, 0, 16);
		}

		int[] dx = {0, 0, -1, 1};
		int[] dz = {-1, 1, 0, 0};
		for (int dir = 0; dir < 4; dir++) {
			if (isFaceCulled(world, x + dx[dir], y, z + dz[dir])) {
				continue;
			}
			double t0 = surfacePinned ? bottomY : d2;
			double t1 = surfacePinned ? bottomY : d5;
			double t2 = surfacePinned ? bottomY : d4;
			double t3 = surfacePinned ? bottomY : d3;
			if (Math.max(Math.max(t0, t1), Math.max(t2, t3)) <= bottomY + 1e-4D) {
				continue; // collapsed onto the column floor (dry shore)
			}
			emitted = true;
			tessellator.setBrightness(Blocks.water.getMixedBrightnessForBlock(world, x + dx[dir], y, z + dz[dir]));
			float shade = dir < 2 ? 0.8F : 0.6F;
			tessellator.setColorOpaque_F(shade * r, shade * g, shade * b);
			// V mapping follows vanilla L4319-4321: V_top = (1 - cornerHeight) * 8,
			// V_bottom = (1 - bottomY) * 8, so side quads meet the lowered top edge
			// and the column floor seamlessly
			switch (dir) {
			case 0: // north, corners (x,z) -> (x+1,z)
				quad(tessellator, flow,
						x, y + t0, z + EPS, 0, (1.0D - t0) * 8.0D,
						x + 1, y + t1, z + EPS, 8, (1.0D - t1) * 8.0D,
						x + 1, y + bottomY, z + EPS, 8, (1.0D - bottomY) * 8.0D,
						x, y + bottomY, z + EPS, 0, (1.0D - bottomY) * 8.0D);
				break;
			case 1: // south, corners (x+1,z+1) -> (x,z+1)
				quad(tessellator, flow,
						x + 1, y + t2, z + 1 - EPS, 0, (1.0D - t2) * 8.0D,
						x, y + t3, z + 1 - EPS, 8, (1.0D - t3) * 8.0D,
						x, y + bottomY, z + 1 - EPS, 8, (1.0D - bottomY) * 8.0D,
						x + 1, y + bottomY, z + 1 - EPS, 0, (1.0D - bottomY) * 8.0D);
				break;
			case 2: // west, corners (x,z+1) -> (x,z)
				quad(tessellator, flow,
						x + EPS, y + t3, z + 1, 0, (1.0D - t3) * 8.0D,
						x + EPS, y + t0, z, 8, (1.0D - t0) * 8.0D,
						x + EPS, y + bottomY, z, 8, (1.0D - bottomY) * 8.0D,
						x + EPS, y + bottomY, z + 1, 0, (1.0D - bottomY) * 8.0D);
				break;
			default: // east, corners (x+1,z) -> (x+1,z+1)
				quad(tessellator, flow,
						x + 1 - EPS, y + t1, z, 0, (1.0D - t1) * 8.0D,
						x + 1 - EPS, y + t2, z + 1, 8, (1.0D - t2) * 8.0D,
						x + 1 - EPS, y + bottomY, z + 1, 8, (1.0D - bottomY) * 8.0D,
						x + 1 - EPS, y + bottomY, z, 0, (1.0D - bottomY) * 8.0D);
				break;
		}
	}
	return emitted;
}

		/**
		 * Paints the flush half-cell water volume under an upper-half block: from
	 * the cell floor up to y 0.5, with the top face hidden by the block's own
	 * underside (the solid half renders that plane). Only the four side faces
	 * are emitted, from 0 to 0.5, plus the cell floor when visible - exactly
	 * like the upper stair complement water.
	 *
	 * @return whether any quad was emitted
	 */
	public static boolean renderUnderHalfWaterVolume(IBlockAccess world, int x, int y, int z) {
		Tessellator tessellator = Tessellator.instance;
		IIcon still = Blocks.water.getIcon(0, 0);
		IIcon flow = Blocks.water.getIcon(2, 0);
		boolean emitted = false;

		int colour = Blocks.water.colorMultiplier(world, x, y, z);
		float r = (colour >> 16 & 255) / 255.0F;
		float g = (colour >> 8 & 255) / 255.0F;
		float b = (colour & 255) / 255.0F;

		int[] dx = {0, 0, -1, 1};
		int[] dz = {-1, 1, 0, 0};
		for (int dir = 0; dir < 4; dir++) {
			if (isFaceCulled(world, x + dx[dir], y, z + dz[dir])) {
				continue;
			}
			emitted = true;
			tessellator.setBrightness(Blocks.water.getMixedBrightnessForBlock(world, x + dx[dir], y, z + dz[dir]));
			float shade = dir < 2 ? 0.8F : 0.6F;
			tessellator.setColorOpaque_F(shade * r, shade * g, shade * b);
			// V mapping follows the full volume: v = (1 - height) * 8, so the
			// side meets the block underside (0.5) and the cell floor (0)
			switch (dir) {
			case 0: // north, corners (x,z) -> (x+1,z)
				quad(tessellator, flow,
						x, y + 0.5D, z + EPS, 0, 4.0D,
						x + 1, y + 0.5D, z + EPS, 8, 4.0D,
						x + 1, y + EPS, z + EPS, 8, 8.0D,
						x, y + EPS, z + EPS, 0, 8.0D);
				break;
			case 1: // south, corners (x+1,z+1) -> (x,z+1)
				quad(tessellator, flow,
						x + 1, y + 0.5D, z + 1 - EPS, 0, 4.0D,
						x, y + 0.5D, z + 1 - EPS, 8, 4.0D,
						x, y + EPS, z + 1 - EPS, 8, 8.0D,
						x + 1, y + EPS, z + 1 - EPS, 0, 8.0D);
				break;
			case 2: // west, corners (x,z+1) -> (x,z)
				quad(tessellator, flow,
						x + EPS, y + 0.5D, z + 1, 0, 4.0D,
						x + EPS, y + 0.5D, z, 8, 4.0D,
						x + EPS, y + EPS, z, 8, 8.0D,
						x + EPS, y + EPS, z + 1, 0, 8.0D);
				break;
			default: // east, corners (x+1,z) -> (x+1,z+1)
				quad(tessellator, flow,
						x + 1 - EPS, y + 0.5D, z, 0, 4.0D,
						x + 1 - EPS, y + 0.5D, z + 1, 8, 4.0D,
						x + 1 - EPS, y + EPS, z + 1, 8, 8.0D,
						x + 1 - EPS, y + EPS, z, 0, 8.0D);
				break;
			}
		}

		// Vanilla shades the bottom face grey without biome tint; only full-cell
		// columns have one - higher columns sit on the block's own opaque faces
		if (!isFaceCulled(world, x, y - 1, z)) {
			emitted = true;
			tessellator.setBrightness(Blocks.water.getMixedBrightnessForBlock(world, x, y - 1, z));
			tessellator.setColorOpaque_F(0.5F, 0.5F, 0.5F);
			quad(tessellator, still,
					x, y + EPS, z, 0, 0,
					x + 1, y + EPS, z, 16, 0,
					x + 1, y + EPS, z + 1, 16, 16,
					x, y + EPS, z + 1, 0, 16);
		}

		return emitted;
	}

	/**
	 * Port of {@code RenderBlocks#getLiquidHeight} (vanilla corner averaging).
	 * Samples force meta 0 when they are one of our ocean plants or wet sea
	 * pickles. Any visually waterlogged member cell counts as a full still
	 * water cell in the average - the corner heights then match the
	 * surrounding real water exactly (the air above the shore pulls the
	 * surface down the same way on both sides of the seam), which is what
	 * keeps the painted plane flush with the pool. Non-qualifying members
	 * take the solid or non-solid path instead - the judgment is purely
	 * local, so this stays an O(1) check.
	 */
	public static double getLiquidHeight(IBlockAccess world, int x, int y, int z) {
		int l = 0;
		float f = 0.0F;

		for (int i1 = 0; i1 < 4; ++i1) {
			int j1 = x - (i1 & 1);
			int k1 = z - (i1 >> 1 & 1);

			if (world.getBlock(j1, y + 1, k1).getMaterial() == Material.water) {
				return 1.0D; // the water body continues above: full height
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
			} else if (VisualWaterlog.isWaterlogged(world, j1, y, k1)) {
				// visually waterlogged member cell: weight the sample exactly
				// like a full still water cell
				f += BlockLiquid.getLiquidHeightPercent(0) * 10.0F;
				l += 10;
				f += BlockLiquid.getLiquidHeightPercent(0);
				++l;
			} else if (!material.isSolid()) {
				f += 1;
				++l;
			}
		}

		return 1.0D - (double) (f / (float) l);
	}

	/**
	 * Shared face-cull rule for painted water volumes (all six faces): hidden
	 * against opaque blocks, liquid cells and visually waterlogged member
	 * neighbours. Against real water the shell mixin suppresses that water's
	 * matching face as well, so neither side of a continuous water body paints
	 * the shared plane - vanilla's own rule for two adjacent water cells;
	 * against another qualified member the neighbour's own geometry or its
	 * painted water forms the shared plane either way, and painting here
	 * would only add an interior shell visible through the water.
	 */
	public static boolean isFaceCulled(IBlockAccess world, int x, int y, int z) {
		Block neighbour = world.getBlock(x, y, z);
		if (neighbour.isOpaqueCube() || neighbour.getMaterial() == Material.water) {
			return true;
		}
		return VisualWaterlog.isWaterlogged(world, x, y, z);
	}

	/** Emits one double-sided quad (both windings, like the vanilla liquid renderer). */
	static void quad(Tessellator tessellator, IIcon icon,
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
