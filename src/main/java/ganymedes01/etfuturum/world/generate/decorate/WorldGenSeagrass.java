package ganymedes01.etfuturum.world.generate.decorate;

import ganymedes01.etfuturum.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

import java.util.Random;

/**
 * Places a single seagrass tuft on the floor of a water body. Roughly 30% of
 * tufts are tall (two blocks), matching the modern SeagrassFeature; a tall
 * tuft falls back to short when there is no water block above the floor.
 * The y argument is ignored; the tuft is planted on the first water block
 * resting on solid ground below sea level.
 */
public class WorldGenSeagrass extends WorldGenerator {

	@Override
	public boolean generate(World world, Random rand, int x, int y, int z) {
		int floorY = WorldGenKelp.findWaterFloor(world, x, z);
		if (floorY < 0) {
			return false;
		}

		boolean tall = rand.nextFloat() < 0.3F;
		if (tall) {
			Block above = world.getBlock(x, floorY + 1, z);
			if (above != Blocks.water && above != Blocks.flowing_water) {
				tall = false;
			}
		}

		if (tall) {
			world.setBlock(x, floorY, z, ModBlocks.SEAGRASS.get(), 1, 2);
			world.setBlock(x, floorY + 1, z, ModBlocks.SEAGRASS.get(), 2, 2);
		} else {
			world.setBlock(x, floorY, z, ModBlocks.SEAGRASS.get(), 0, 2);
		}
		return true;
	}
}
