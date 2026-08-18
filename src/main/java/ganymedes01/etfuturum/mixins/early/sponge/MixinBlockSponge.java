package ganymedes01.etfuturum.mixins.early.sponge;

import com.google.common.collect.Lists;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.configuration.configs.ConfigSounds;
import ganymedes01.etfuturum.lib.Reference;
import ganymedes01.etfuturum.world.WorldCoord;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSponge;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.util.Tuple;
import net.minecraft.world.World;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.util.ForgeDirection;
import org.apache.commons.lang3.ArrayUtils;
import org.spongepowered.asm.mixin.Mixin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * 给原版 1.7.10 {@link BlockSponge}（纯装饰方块）注入 1.8+ 吸水/湿海绵逻辑。
 * <p>
 * 参考 {@code MixinBlockBed} 模式：{@code extends Block} + {@code @Override}，
 * 使 Mixin 框架对方法进行 implicit remap，在生产环境（混淆环境）中也能正确重写。
 * 吸水后替换为 {@link ModBlocks#WET_SPONGE}。
 */
@Mixin(BlockSponge.class)
public abstract class MixinBlockSponge extends Block {

	protected MixinBlockSponge(Material materialIn) {
		super(materialIn);
	}

	@Override
	public void onBlockAdded(World world, int x, int y, int z) {
		tryAbsorb(world, x, y, z);
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block neighborBlock) {
		tryAbsorb(world, x, y, z);
	}

	private void tryAbsorb(World worldIn, int x, int y, int z) {
		boolean inNether = ArrayUtils.contains(BiomeDictionary.getTypesForBiome(worldIn.getBiomeGenForCoords(x, z)), BiomeDictionary.Type.NETHER);
		if (!inNether && absorb(worldIn, x, y, z)) {
			worldIn.setBlock(x, y, z, ModBlocks.WET_SPONGE.get(), 0, 2);
			if (ConfigSounds.newBlockSounds) {
				worldIn.playSoundEffect(x + .5D, y + .5D, z + .5D, Reference.MCAssetVer + ":block.sponge.absorb", 1, 1);
			}
		}
	}

	private boolean absorb(World world, int x, int y, int z) {
		LinkedList<Tuple> linkedlist = Lists.newLinkedList();
		ArrayList<WorldCoord> arraylist = Lists.newArrayList();
		linkedlist.add(new Tuple(new WorldCoord(x, y, z), 0));
		int i = 0;
		WorldCoord blockpos1;

		while (!linkedlist.isEmpty()) {
			Tuple tuple = linkedlist.poll();
			blockpos1 = (WorldCoord) tuple.getFirst();
			int j = (Integer) tuple.getSecond();

			for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
				WorldCoord blockpos2 = blockpos1.add(dir);

				if (world.getBlock(blockpos2.x, blockpos2.y, blockpos2.z).getMaterial() == Material.water) {
					world.setBlockToAir(blockpos2.x, blockpos2.y, blockpos2.z);
					arraylist.add(blockpos2);
					i++;
					if (j < 6)
						linkedlist.add(new Tuple(blockpos2, j + 1));
				}
			}

			if (i > 64)
				break;
		}

		Iterator<WorldCoord> iterator = arraylist.iterator();

		while (iterator.hasNext()) {
			blockpos1 = iterator.next();
			world.notifyBlockOfNeighborChange(blockpos1.x, blockpos1.y, blockpos1.z, Blocks.air);
		}

		return i > 0;
	}
}
