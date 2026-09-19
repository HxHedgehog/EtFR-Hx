package ganymedes01.etfuturum.client.renderer.block;

import ganymedes01.etfuturum.blocks.BlockScaffolding;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

/**
 * Faithful rebuild of the 1.21 scaffolding stable/unstable JSON models:
 * a thin top sheet (full square at y~16 plus edge bands hanging to y14) on four
 * full-height corner pillars; the unstable (bottom) variant adds a bottom plate
 * at y0..2 with its own edge bands. Culling mirrors the JSON cullface rules
 * (outward faces cull against opaque neighbours, interior faces never cull).
 */
public class BlockScaffoldingRenderer extends BlockModelBase {

	private static final double[][] PILLARS = {{0.0D, 0.0D}, {0.0D, 0.875D}, {0.875D, 0.875D}, {0.875D, 0.0D}};

	public BlockScaffoldingRenderer(int modelID) {
		super(modelID);
	}

	@Override
	public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
		int meta = world.getBlockMetadata(x, y, z);
		final boolean oldAllFaces = renderer.renderAllFaces;
		renderer.renderAllFaces = true;

		IIcon top = block.getIcon(1, meta);
		IIcon bottomIcon = block.getIcon(0, meta);
		boolean bottom = (meta & 8) != 0;

		renderTopSheet(renderer, block, x, y, z, world, top);
		renderPillars(renderer, block, x, y, z, world, bottomIcon);
		renderTopBands(renderer, block, x, y, z, world, bottomIcon);

		if (bottom) {
			renderBottomSheet(renderer, block, x, y, z, world, top, bottomIcon);
			renderBottomBands(renderer, block, x, y, z, world, bottomIcon);
		}

		renderer.renderAllFaces = oldAllFaces;
		return true;
	}

	@Override
	protected void renderInventoryModel(Block block, int meta, int modelId, RenderBlocks renderer, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		final Tessellator tessellator = Tessellator.instance;
		IIcon top = block.getIcon(1, meta);
		IIcon side = block.getIcon(2, meta);
		IIcon bottomIcon = block.getIcon(0, meta);

		// Top sheet
		renderer.setRenderBounds(0.0D, 0.999375D, 0.0D, 1.0D, 1.0D, 1.0D);
		tessellator.setNormal(0.0F, 1.0F, 0.0F);
		renderer.renderFaceYPos(block, 0.0D, 0.0D, 0.0D, top);
		tessellator.setNormal(0.0F, -1.0F, 0.0F);
		renderer.renderFaceYNeg(block, 0.0D, 0.0D, 0.0D, top);

		// Corner pillars
		for (double[] p : PILLARS) {
			renderer.setRenderBounds(p[0], 0.0D, p[1], p[0] + 0.125D, 1.0D, p[1] + 0.125D);
			inventoryBox(tessellator, renderer, block, side, bottomIcon);
		}

		// Top edge bands
		renderer.setRenderBounds(0.125D, 0.875D, 0.0D, 0.875D, 1.0D, 0.125D);
		inventoryBox(tessellator, renderer, block, side, bottomIcon);
		renderer.setRenderBounds(0.125D, 0.875D, 0.875D, 0.875D, 1.0D, 1.0D);
		inventoryBox(tessellator, renderer, block, side, bottomIcon);
		renderer.setRenderBounds(0.0D, 0.875D, 0.125D, 0.125D, 1.0D, 0.875D);
		inventoryBox(tessellator, renderer, block, side, bottomIcon);
		renderer.setRenderBounds(0.875D, 0.875D, 0.125D, 1.0D, 1.0D, 0.875D);
		inventoryBox(tessellator, renderer, block, side, bottomIcon);
	}

	/** Renders the 6 faces of a small box with side texture on the sides and the given bottom texture below. */
	private void inventoryBox(Tessellator tessellator, RenderBlocks renderer, Block block, IIcon side, IIcon bottomIcon) {
		tessellator.setNormal(0.0F, -1.0F, 0.0F);
		renderer.renderFaceYNeg(block, 0.0D, 0.0D, 0.0D, bottomIcon);
		tessellator.setNormal(0.0F, 0.0F, -1.0F);
		renderer.renderFaceZNeg(block, 0.0D, 0.0D, 0.0D, side);
		tessellator.setNormal(0.0F, 0.0F, 1.0F);
		renderer.renderFaceZPos(block, 0.0D, 0.0D, 0.0D, side);
		tessellator.setNormal(-1.0F, 0.0F, 0.0F);
		renderer.renderFaceXNeg(block, 0.0D, 0.0D, 0.0D, side);
		tessellator.setNormal(1.0F, 0.0F, 0.0F);
		renderer.renderFaceXPos(block, 0.0D, 0.0D, 0.0D, side);
	}

	private void renderTopSheet(RenderBlocks renderer, Block block, int x, int y, int z, IBlockAccess world, IIcon top) {
		renderer.setRenderBounds(0.0D, 0.999375D, 0.0D, 1.0D, 1.0D, 1.0D);
		if (!world.getBlock(x, y + 1, z).isOpaqueCube()) {
			renderFaceYPos(renderer, block, x, y, z); // cullface:up
		}
		renderFaceYNeg(renderer, block, x, y, z, 0.0D, 0.0D, 0.0D, top); // sheet underside, never culled
	}

	private void renderPillars(RenderBlocks renderer, Block block, int x, int y, int z, IBlockAccess world, IIcon bottomIcon) {
		for (double[] p : PILLARS) {
			renderer.setRenderBounds(p[0], 0.0D, p[1], p[0] + 0.125D, 1.0D, p[1] + 0.125D);
			// outward faces cull against opaque neighbours (JSON cullface), interior faces never
			if (p[0] > 0.0D || !world.getBlock(x - 1, y, z).isOpaqueCube()) {
				renderFaceXNeg(renderer, block, x, y, z);
			}
			if (p[0] < 0.875D || !world.getBlock(x + 1, y, z).isOpaqueCube()) {
				renderFaceXPos(renderer, block, x, y, z);
			}
			if (p[1] > 0.0D || !world.getBlock(x, y, z - 1).isOpaqueCube()) {
				renderFaceZNeg(renderer, block, x, y, z);
			}
			if (p[1] < 0.875D || !world.getBlock(x, y, z + 1).isOpaqueCube()) {
				renderFaceZPos(renderer, block, x, y, z);
			}
			// down face uses the bottom icon (cullface:down)
			if (!world.getBlock(x, y - 1, z).isOpaqueCube()) {
				renderFaceYNeg(renderer, block, x, y, z, 0.0D, 0.0D, 0.0D, bottomIcon);
			}
		}
	}

	private void renderTopBands(RenderBlocks renderer, Block block, int x, int y, int z, IBlockAccess world, IIcon bottomIcon) {
		// north band
		renderer.setRenderBounds(0.125D, 0.875D, 0.0D, 0.875D, 1.0D, 0.125D);
		if (!world.getBlock(x, y, z - 1).isOpaqueCube()) {
			renderFaceZNeg(renderer, block, x, y, z);
		}
		renderFaceZPos(renderer, block, x, y, z);
		renderFaceYNeg(renderer, block, x, y, z, 0.0D, 0.0D, 0.0D, bottomIcon);

		// south band
		renderer.setRenderBounds(0.125D, 0.875D, 0.875D, 0.875D, 1.0D, 1.0D);
		if (!world.getBlock(x, y, z + 1).isOpaqueCube()) {
			renderFaceZPos(renderer, block, x, y, z);
		}
		renderFaceZNeg(renderer, block, x, y, z);
		renderFaceYNeg(renderer, block, x, y, z, 0.0D, 0.0D, 0.0D, bottomIcon);

		// west band
		renderer.setRenderBounds(0.0D, 0.875D, 0.125D, 0.125D, 1.0D, 0.875D);
		if (!world.getBlock(x - 1, y, z).isOpaqueCube()) {
			renderFaceXNeg(renderer, block, x, y, z);
		}
		renderFaceXPos(renderer, block, x, y, z);
		renderFaceYNeg(renderer, block, x, y, z, 0.0D, 0.0D, 0.0D, bottomIcon);

		// east band
		renderer.setRenderBounds(0.875D, 0.875D, 0.125D, 1.0D, 1.0D, 0.875D);
		if (!world.getBlock(x + 1, y, z).isOpaqueCube()) {
			renderFaceXPos(renderer, block, x, y, z);
		}
		renderFaceXNeg(renderer, block, x, y, z);
		renderFaceYNeg(renderer, block, x, y, z, 0.0D, 0.0D, 0.0D, bottomIcon);
	}

	private void renderBottomSheet(RenderBlocks renderer, Block block, int x, int y, int z, IBlockAccess world, IIcon top, IIcon bottomIcon) {
		// JSON: the bottom plate is the THIN layer at y 1.99..2 px (mirroring the top
		// sheet at 15.99..16), NOT a full 0..2 slab - a full slab would put its down
		// face coplanar with the edge bands' down faces at y=0 (z-fighting flicker).
		renderer.setRenderBounds(0.0D, 0.124375D, 0.0D, 1.0D, 0.125D, 1.0D);
		renderFaceYPos(renderer, block, x, y, z, 0.0D, 0.0D, 0.0D, top);
		if (!world.getBlock(x, y - 1, z).isOpaqueCube()) {
			renderFaceYNeg(renderer, block, x, y, z, 0.0D, 0.0D, 0.0D, top); // cullface:down
		}
	}

	private void renderBottomBands(RenderBlocks renderer, Block block, int x, int y, int z, IBlockAccess world, IIcon bottomIcon) {
		// north band
		renderer.setRenderBounds(0.125D, 0.0D, 0.0D, 0.875D, 0.125D, 0.125D);
		if (!world.getBlock(x, y, z - 1).isOpaqueCube()) {
			renderFaceZNeg(renderer, block, x, y, z);
		}
		renderFaceZPos(renderer, block, x, y, z);
		if (!world.getBlock(x, y - 1, z).isOpaqueCube()) {
			renderFaceYNeg(renderer, block, x, y, z, 0.0D, 0.0D, 0.0D, bottomIcon);
		}

		// south band
		renderer.setRenderBounds(0.125D, 0.0D, 0.875D, 0.875D, 0.125D, 1.0D);
		if (!world.getBlock(x, y, z + 1).isOpaqueCube()) {
			renderFaceZPos(renderer, block, x, y, z);
		}
		renderFaceZNeg(renderer, block, x, y, z);
		if (!world.getBlock(x, y - 1, z).isOpaqueCube()) {
			renderFaceYNeg(renderer, block, x, y, z, 0.0D, 0.0D, 0.0D, bottomIcon);
		}

		// west band
		renderer.setRenderBounds(0.0D, 0.0D, 0.125D, 0.125D, 0.125D, 0.875D);
		if (!world.getBlock(x - 1, y, z).isOpaqueCube()) {
			renderFaceXNeg(renderer, block, x, y, z);
		}
		renderFaceXPos(renderer, block, x, y, z);
		if (!world.getBlock(x, y - 1, z).isOpaqueCube()) {
			renderFaceYNeg(renderer, block, x, y, z, 0.0D, 0.0D, 0.0D, bottomIcon);
		}

		// east band
		renderer.setRenderBounds(0.875D, 0.0D, 0.125D, 1.0D, 0.125D, 0.875D);
		if (!world.getBlock(x + 1, y, z).isOpaqueCube()) {
			renderFaceXPos(renderer, block, x, y, z);
		}
		renderFaceXNeg(renderer, block, x, y, z);
		if (!world.getBlock(x, y - 1, z).isOpaqueCube()) {
			renderFaceYNeg(renderer, block, x, y, z, 0.0D, 0.0D, 0.0D, bottomIcon);
		}
	}
}
