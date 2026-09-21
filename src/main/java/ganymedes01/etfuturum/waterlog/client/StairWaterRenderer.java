package ganymedes01.etfuturum.waterlog.client;

import ganymedes01.etfuturum.blocks.BlockAquaticPlant;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.world.IBlockAccess;

/**
 * Shared water-surface helpers for the ocean plants' painted water volumes.
 * The stateless stair complement painter that previously lived here was
 * removed together with the fake waterlogging system; the real waterlogging
 * renderer will be modelled on 26.2's fluid renderer in a later phase.
 */
public class StairWaterRenderer {

	/**
	 * Port of {@code RenderBlocks#getLiquidHeight} (vanilla corner averaging),
	 * shared with the ocean plant water volumes. Ocean-plant flavour: samples
	 * force meta 0 when they are one of our ocean plants. Stairs take the
	 * solid path - the judgment is purely local, so this stays an O(1) check.
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
			} else if (!material.isSolid()) {
				f += 1;
				++l;
			}
		}

		return 1.0D - (double) (f / (float) l);
	}

	/**
	 * Shared face-cull rule for the ocean plants' own water volumes (all six
	 * faces): hidden against opaque blocks and liquid cells. Against real
	 * water the neighbour suppresses its matching face as well (vanilla's own
	 * rule for two adjacent water cells), so neither side of a continuous
	 * water body paints the shared plane.
	 */
	public static boolean isFaceCulled(IBlockAccess world, int x, int y, int z) {
		Block neighbour = world.getBlock(x, y, z);
		return neighbour.isOpaqueCube() || neighbour.getMaterial() == Material.water;
	}
}
