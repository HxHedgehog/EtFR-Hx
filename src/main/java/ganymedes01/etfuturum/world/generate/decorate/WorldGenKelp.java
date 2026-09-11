package ganymedes01.etfuturum.world.generate.decorate;

import ganymedes01.etfuturum.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Random;

/**
 * Grows a kelp stalk from the ocean floor upwards: body segments are
 * KELP_PLANT and the tip is a KELP head with a near-max age so the forest
 * stops growing shortly after worldgen, matching the modern KelpFeature.
 * The y argument is ignored; the stalk is planted on the first water block
 * resting on solid ground below sea level.
 */
public class WorldGenKelp extends WorldGenerator {

	@Override
	public boolean generate(World world, Random rand, int x, int y, int z) {
		int floorY = findWaterFloor(world, x, z);
		if (floorY < 0) {
			return false;
		}

		int height = 2 + rand.nextInt(8);
		for (int i = 0; i < height; i++) {
			int yy = floorY + i;
			Block at = world.getBlock(x, yy, z);
			if (at != Blocks.water && at != Blocks.flowing_water) {
				if (i == 0) {
					return false;
				}
				// Growth blocked mid-stalk: the last placed segment becomes a fresh head
				world.setBlock(x, yy - 1, z, ModBlocks.KELP.get(), randomHeadAge(rand), 2);
				return true;
			}
			if (i == height - 1) {
				world.setBlock(x, yy, z, ModBlocks.KELP.get(), randomHeadAge(rand), 2);
			} else {
				world.setBlock(x, yy, z, ModBlocks.KELP_PLANT.get(), 0, 2);
			}
		}
		return true;
	}

	private static int randomHeadAge(Random rand) {
		return 12 + rand.nextInt(4);
	}

	/**
	 * Scans down from sea level for the first water block resting on solid ground.
	 * Returns -1 for land columns or water deeper than the cutoff.
	 */
	static int findWaterFloor(World world, int x, int z) {
		for (int y = 63; y > 40; y--) {
			Block at = world.getBlock(x, y, z);
			if (at == Blocks.water || at == Blocks.flowing_water) {
				Block below = world.getBlock(x, y - 1, z);
				if (below.isSideSolid(world, x, y - 1, z, ForgeDirection.UP)) {
					return y;
				}
			} else if (!at.isAir(world, x, y, z)) {
				return -1;
			}
		}
		return -1;
	}
}
