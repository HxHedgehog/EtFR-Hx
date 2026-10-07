package ganymedes01.etfuturum.client.renderer.block;

import com.gtnewhorizons.angelica.api.ThreadSafeISBRH;
import ganymedes01.etfuturum.waterlog.client.StairWaterRenderer;
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

		// 植物交叉面片先画、水体后画（26.2 顺序：cutout 植物在半透明流体之前）。
		// 反过来先画水时，顶面/侧面按角高倾斜（流水邻居拉低共享角高）先写入深度，
		// 交叉面片的水下部分被深度裁剪、贴近水面处又以无水色全亮露出——静水格紧邻
		// 流水时水面倾斜穿过 X 面片的高度各不相同，从侧面看就是顶部纹理上的
		// "X 状裂口"，且随摄像机视角变化（2026-10-07 用户实证；静水池全源水面平坦
		// 故不明显）。植物先画后，水下部分被半透明水体罩染（26.2 观感），露出水面
		// 的部分不受水体覆盖。
		Tessellator tessellator = Tessellator.instance;
		tessellator.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
		tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
		renderer.drawCrossedSquares(block.getIcon(world, x, y, z, 2), (double) x, (double) y, (double) z, 1.0F);

		StairWaterRenderer.renderStillWaterVolume(world, x, y, z);
		return true;
	}
}
