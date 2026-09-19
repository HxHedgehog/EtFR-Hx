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
		// The cell qualifies when at least two of the four horizontal sides
		// touch water. A single side is not enough: a thin block sitting in a
		// one-deep shore line would otherwise read as fully submerged, which
		// looks wrong for a block that only touches water on one face.
		// Same-family members never count as a water side - two adjacent
		// fences in a row must still need water in front of/behind them (or
		// above), not just next to each other. "Same family" is resolved
		// through the registry, so a fence next to a slab still does not
		// count either side as water.
		int waterSides = 0;
		for (int dir = 2; dir <= 5; dir++) {
			int nx = x + VisualWaterlog.offsetX(dir);
			int nz = z + VisualWaterlog.offsetZ(dir);
			Block neighbour = world.getBlock(nx, y, nz);
			if (VisualWaterlog.isWaterBlock(neighbour)) {
				waterSides++;
			} else if (VisualWaterlog.familyOf(neighbour) != null) {
				// Same-family member on this side: not a water side.
				continue;
			}
		}
		return waterSides >= 2;
	}

	@Override
	public boolean paint(IBlockAccess world, int x, int y, int z, Tessellator tessellator) {
		if (!shouldRenderWater(world, x, y, z)) {
			return false; // stateless gate: callers only know the block is a member
		}
		return BlockWaterRenderer.renderStillWaterVolume(world, x, y, z, tessellator);
	}

	@Override
	public boolean isEyeSubmerged(IBlockAccess world, int x, int y, int z, double eyeX, double eyeY, double eyeZ) {
		// Full-cell water: the entire column is painted, so any eye point in
		// the cell is submerged
		return shouldRenderWater(world, x, y, z);
	}
}
