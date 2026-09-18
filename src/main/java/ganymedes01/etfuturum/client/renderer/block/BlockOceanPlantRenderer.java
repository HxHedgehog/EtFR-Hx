package ganymedes01.etfuturum.client.renderer.block;

import com.gtnewhorizons.angelica.api.ThreadSafeISBRH;
import ganymedes01.etfuturum.waterlog.client.BlockWaterRenderer;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
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

		BlockWaterRenderer.renderStillWaterVolume(world, x, y, z);

		Tessellator tessellator = Tessellator.instance;
		tessellator.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
		tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
		renderer.drawCrossedSquares(block.getIcon(world, x, y, z, 2), (double) x, (double) y, (double) z, 1.0F);
		return true;
	}
}
