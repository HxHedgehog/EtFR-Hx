package ganymedes01.etfuturum.waterlog;

import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.waterlog.client.BlockWaterRenderer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAnvil;
import net.minecraft.block.BlockBasePressurePlate;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockRedstoneDiode;
import net.minecraft.block.BlockRedstoneTorch;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.BlockWall;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.world.IBlockAccess;

/**
 * AquaTweaks-style full-cell visual water for thin decorative blocks:
 * fences, fence gates, walls, glass panes and iron bars, ladders, buttons,
 * levers, pressure plates, redstone wire, redstone torches, repeaters and
 * comparators, rails and anvils. These blocks never fill their cell, so a
 * cell beside water reads as submerged - without this family, real water
 * wraps them in flowing-texture water walls, which is exactly the visual
 * noise AquaTweaks set out to remove. Membership is an instanceof over the
 * vanilla shape base classes, which covers EtFR's backported variants
 * (they subclass those bases) and modded blocks of the same shapes alike;
 * every one of them renders through RenderBlocks (no TESR), so the family
 * hooks all apply.
 * <p>
 * The stateless qualification is deliberately simpler than the stairs': a
 * thin block has no meaningful geometry to route water around, so any water
 * touching the cell from directly above or from one of the four horizontal
 * neighbours (at any flow level) fills it. Unlike the stair rule, the test
 * never consults neighbouring members - water comes from real water or not
 * at all - so it cannot recurse and stays a pure function of the actual
 * liquid. The paint is the shared full still water cell, whose corner
 * heights follow the surrounding flow, so a cell beside a draining shore
 * shows the same dipping surface as the water next to it.
 */
public class ThinBlockWaterFamily implements WaterlogFamily {

	public static final ThinBlockWaterFamily INSTANCE = new ThinBlockWaterFamily();

	private ThinBlockWaterFamily() {
	}

	@Override
	public boolean isMember(Block block) {
		return block instanceof BlockFence
				|| block instanceof BlockFenceGate
				|| block instanceof BlockWall
				|| block instanceof BlockPane
				|| block instanceof BlockLadder
				|| block instanceof BlockButton
				|| block instanceof BlockLever
				|| block instanceof BlockBasePressurePlate
				|| block instanceof BlockRedstoneWire
				|| block instanceof BlockRedstoneTorch
				|| block instanceof BlockRedstoneDiode
				|| block instanceof BlockRailBase
				|| block instanceof BlockAnvil;
	}

	@Override
	public boolean isEnabled() {
		return ConfigMixins.thinBlockWaterlogging;
	}

	@Override
	public boolean shouldRenderWater(IBlockAccess world, int x, int y, int z) {
		if (!ConfigMixins.thinBlockWaterlogging) {
			return false;
		}
		if (!isMember(world.getBlock(x, y, z))) {
			return false;
		}
		// Water touching the cell from directly above fills it, pouring
		// streams included - matching the stair rule
		if (VisualWaterlog.isWaterBlock(world.getBlock(x, y + 1, z))) {
			return true;
		}
		for (int dir = 2; dir <= 5; dir++) {
			int nx = x + VisualWaterlog.offsetX(dir);
			int nz = z + VisualWaterlog.offsetZ(dir);
			if (VisualWaterlog.isWaterBlock(world.getBlock(nx, y, nz))) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean paint(IBlockAccess world, int x, int y, int z, Tessellator tessellator) {
		if (!shouldRenderWater(world, x, y, z)) {
			return false; // stateless gate: callers only know the block is a member
		}
		return BlockWaterRenderer.renderStillWaterVolume(world, x, y, z, tessellator);
	}
}
