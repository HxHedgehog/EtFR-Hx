package ganymedes01.etfuturum.waterlog;

import ganymedes01.etfuturum.waterlog.client.StairWaterRenderer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStairs;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.world.IBlockAccess;

/**
 * The stair family of the visual-waterlogging registry: every vanilla and
 * modded {@link BlockStairs} qualifies through {@link StairWaterlog} and
 * paints its complement water through {@link StairWaterRenderer}.
 */
public class StairWaterFamily implements WaterlogFamily {

	public static final StairWaterFamily INSTANCE = new StairWaterFamily();

	private StairWaterFamily() {
	}

	@Override
	public boolean isMember(Block block) {
		return block instanceof BlockStairs;
	}

	@Override
	public boolean isEnabled() {
		return StairWaterlog.isEnabled();
	}

	@Override
	public boolean shouldRenderWater(IBlockAccess world, int x, int y, int z) {
		return StairWaterlog.shouldRenderWater(world, x, y, z);
	}

	@Override
	public boolean paint(IBlockAccess world, int x, int y, int z, Tessellator tessellator) {
		return StairWaterRenderer.renderWaterVolume(world, x, y, z, tessellator);
	}

	@Override
	public boolean isEyeSubmerged(IBlockAccess world, int x, int y, int z, double eyeX, double eyeY, double eyeZ) {
		return StairWaterlog.isEyeSubmerged(world, x, y, z, eyeX, eyeY, eyeZ);
	}
}
