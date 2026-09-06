package ganymedes01.etfuturum.core.utils.helpers;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class WorldHelper {

	/**
	 * Port of the modern (1.21+) {@code EndPlatformFeature#createEndPlatform}.
	 * Builds a 5x5 obsidian platform at y - 1 and clears a 5x5x3 air space above it.
	 * Blocks that already match the target block are skipped; mismatched blocks are broken with drops first.
	 */
	public static void createEndPlatform(WorldServer world, int x, int y, int z, boolean dropResources) {
		for (int dy = -1; dy < 3; dy++) {
			for (int dx = -2; dx <= 2; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					Block block = dy == -1 ? Blocks.obsidian : Blocks.air;
					int bx = x + dx;
					int by = y + dy;
					int bz = z + dz;
					if (world.getBlock(bx, by, bz) == block) {
						continue;
					}
					if (dropResources) {
						world.func_147480_a(bx, by, bz, true); // breakBlock
					}
					world.setBlock(bx, by, bz, block, 0, 3);
				}
			}
		}
	}

	public static boolean isAreaLoaded(World world, int xStart, int yStart, int zStart, int xEnd, int yEnd, int zEnd) {
		if (yEnd >= 0 && yStart < 256) {
			xStart = xStart >> 4;
			zStart = zStart >> 4;
			xEnd = xEnd >> 4;
			zEnd = zEnd >> 4;

			for (int i = xStart; i <= xEnd; ++i) {
				for (int j = zStart; j <= zEnd; ++j) {
					if (!isChunkLoaded(world, i, j)) {
						return false;
					}
				}
			}

			return true;
		}
		return false;
	}

	private static boolean isChunkLoaded(World world, int x, int z) {
		return world.getChunkProvider().chunkExists(x, z);
	}

}
