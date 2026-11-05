package ganymedes01.etfuturum.client.renderer.block;

import com.gtnewhorizons.angelica.api.ThreadSafeISBRH;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

@ThreadSafeISBRH(perThread = false)
public class BlockChainRenderer extends BlockModelBase {
	public BlockChainRenderer(int modelID) {
		super(modelID);
		set2DInventory();
	}

	@Override
	public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
		IIcon iicon = renderer.hasOverrideBlockTexture() ? renderer.overrideBlockTexture : renderer.getBlockIcon(block);
		// 裸 meta 剥位（bit16）：default 分支 = Y 轴，含水横链 meta 17/18 裸判会
		// 渲染成竖直（含水横链"竖起来"病灶）。轴向只用低 4 位，& 15 对干链等价。
		switch (world.getBlockMetadata(x, y, z) & 15) {
			case 1:
				renderRawDoubleSidedFace(renderer, block, x, y, z, 0, 1, 0.4375D, 0.5625D, 0.5625D, 0.4375D, 0, 0, 3, 16, iicon, 1);
				renderRawDoubleSidedFace(renderer, block, x, y, z, 0, 1, 0.4375D, 0.5625D, 0.4375D, 0.5625D, 3, 0, 6, 16, iicon, 1);
				break;
			case 2:
				renderRawDoubleSidedFace(renderer, block, x, y, z, 0.4375D, 0.5625D, 0.5625D, 0.4375D, 0, 1, 0, 0, 3, 16, iicon, 2);
				renderRawDoubleSidedFace(renderer, block, x, y, z, 0.4375D, 0.5625D, 0.4375D, 0.5625D, 0, 1, 3, 0, 6, 16, iicon, 2);
				break;
			case 0:
			default:
				renderRawDoubleSidedFace(renderer, block, x, y, z, 0.4375D, 0.5625D, 0, 1, 0.5625D, 0.4375D, 0, 0, 3, 16, iicon);
				renderRawDoubleSidedFace(renderer, block, x, y, z, 0.4375D, 0.5625D, 0, 1, 0.4375D, 0.5625D, 3, 0, 6, 16, iicon);
				break;
		}
		return true;
	}
}
