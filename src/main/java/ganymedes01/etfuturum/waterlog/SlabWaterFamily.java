package ganymedes01.etfuturum.waterlog;

import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.waterlog.client.BlockWaterRenderer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.world.IBlockAccess;

/**
 * 26.2-style waterlogging for slabs, on the same stateless scheme as the
 * stairs: nothing is stored, the open half of the slab cell renders as a
 * still water volume whenever the qualification re-derives at render time.
 * A single slab occupies one half of its cell (lower, meta bit 3 clear, or
 * upper, meta bit 3 set); the water fills the other half - above a lower
 * slab from its top (0.5) up to the sampled pool surface, below an upper
 * slab as a flush half-cell cubby. Double slabs are full opaque cubes with
 * no open half and are excluded. The paint reuses the shared still-water
 * renderer, so the surface heights, face culling and icons agree with every
 * other painted water body; the qualification is the stair rule without an
 * opening direction (a slab has no facing).
 * <p>
 * Qualification, mirroring {@link StairWaterlog}: water directly above fills
 * the open half on its own (falling streams included) - a lower slab only, an
 * upper slab's top is a full solid plane and water sitting on it cannot reach
 * the cubby below. A large platform interior stays wet through its own rule
 * instead: self an upper slab, all four horizontal sides further slab-family
 * members or full solid blocks, still water directly below - the platform
 * was flooded while submerged and 26.2's waterlogged state never drains
 * sideways. Otherwise at
 * least one horizontal side must hold still water while every other exposed
 * side is sealed by still water, a solid full cube or another slab - so the
 * painted volume always connects to a real source and never floats a wall
 * against air. Same-family members never count as a water side: adjacent
 * slabs must not wet each other, water has to reach the cell from a real
 * liquid. Flowing water never qualifies a slab horizontally (it does from
 * above), the upper-half flush cubby cannot be trimmed to a low flow level
 * and a floating wall would show through it.
 */
public class SlabWaterFamily implements WaterlogFamily {

	public static final SlabWaterFamily INSTANCE = new SlabWaterFamily();

	private SlabWaterFamily() {
	}

	@Override
	public boolean isMember(Block block) {
		// BlockSlab#isOpaqueCube returns the double-slab flag (field_150004_a);
		// a double slab fills the whole cell and has no open half
		return block instanceof BlockSlab && !block.isOpaqueCube();
	}

	@Override
	public boolean isEnabled() {
		return ConfigMixins.slabWaterlogging;
	}

	@Override
	public boolean shouldRenderWater(IBlockAccess world, int x, int y, int z) {
		if (!ConfigMixins.slabWaterlogging) {
			return false;
		}
		Block block = world.getBlock(x, y, z);
		if (!isMember(block)) {
			return false;
		}
		// Water from directly above (any form, falling streams included) fills
		// the open half on its own - a LOWER slab only: an upper slab's top is
		// a full solid plane, water sitting on it cannot reach the cubby below.
		// Upper slabs feed from their sides or, inside a platform, from below.
		if ((world.getBlockMetadata(x, y, z) & 8) == 0
				&& VisualWaterlog.isWaterBlock(world.getBlock(x, y + 1, z))) {
			return true;
		}
		// A large platform interior reads as still flooded: self an upper
		// slab, all four horizontal sides further slab-family members or full
		// solid blocks, still water directly below. The platform was flooded
		// while submerged and 26.2's waterlogged state never drains sideways,
		// so an interior cell keeps its water even with no top feed left to
		// prove it.
		if ((world.getBlockMetadata(x, y, z) & 8) != 0
				&& VisualWaterlog.isStillWaterBlock(world.getBlock(x, y - 1, z),
						world.getBlockMetadata(x, y - 1, z))) {
			boolean surrounded = true;
			for (int dir = 2; dir <= 5 && surrounded; dir++) {
				Block neighbour = world.getBlock(
						x + VisualWaterlog.offsetX(dir), y, z + VisualWaterlog.offsetZ(dir));
				surrounded = isMember(neighbour) || neighbour.isOpaqueCube();
			}
			if (surrounded) {
				return true;
			}
		}
		int waterSides = 0;
		for (int dir = 2; dir <= 5; dir++) {
			int nx = x + VisualWaterlog.offsetX(dir);
			int nz = z + VisualWaterlog.offsetZ(dir);
			Block neighbour = world.getBlock(nx, y, nz);
			if (VisualWaterlog.isStillWaterBlock(neighbour, world.getBlockMetadata(nx, y, nz))) {
				waterSides++;
			} else if (!neighbour.isOpaqueCube() && !isMember(neighbour)) {
				return false; // exposed side: the volume would float a wall against air
			}
		}
		return waterSides >= 1;
	}

	@Override
	public boolean paint(IBlockAccess world, int x, int y, int z, Tessellator tessellator) {
		if (!shouldRenderWater(world, x, y, z)) {
			return false; // stateless gate: callers only know the block is a member
		}
		if ((world.getBlockMetadata(x, y, z) & 8) != 0) {
			// Upper slab: the flush half-cell cubby below it (0 to 0.5)
			return BlockWaterRenderer.renderUnderHalfWaterVolume(world, x, y, z, tessellator);
		}
		// Lower slab: the column from the slab top (0.5) up, surface sampled
		// like the pool it sits in and clamped to the slab top so a shore dip
		// cannot drop it into the solid half
		return BlockWaterRenderer.renderWaterVolume(world, x, y, z, 0.5D, false, tessellator);
	}

	@Override
	public boolean isEyeSubmerged(IBlockAccess world, int x, int y, int z, double eyeX, double eyeY, double eyeZ) {
		if (!shouldRenderWater(world, x, y, z)) {
			return false;
		}
		// The complement spans the full cell footprint; only the vertical half
		// matters. Box-based like the stairs - a screen overlay needs no
		// surface-height precision.
		double minY = (world.getBlockMetadata(x, y, z) & 8) != 0 ? 0.0D : 0.5D;
		double ly = eyeY - y;
		return ly >= minY && ly < minY + 0.5D;
	}
}
