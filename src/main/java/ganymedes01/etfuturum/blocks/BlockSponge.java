package ganymedes01.etfuturum.blocks;
import ganymedes01.etfuturum.creative.ModdedCreativeTabs;

import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.client.sound.ModSounds;
import ganymedes01.etfuturum.core.utils.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.util.ForgeDirection;
import org.apache.commons.lang3.ArrayUtils;

import java.util.List;
import java.util.Random;


/**
 * 湿海绵方块（{@link ModBlocks#WET_SPONGE}）。
 * <p>
 * 干海绵的吸水逻辑已移至 {@code MixinBlockSponge}（注入原版 {@link Blocks#sponge}）。
 * 本类仅处理湿海绵的蒸发逻辑：在下界放置/更新时蒸发变回原版干海绵。
 */
public class BlockSponge extends BaseBlock {

	private final boolean wet;

	public BlockSponge(boolean wet) {
		super(Material.sponge);
		this.wet = wet;
		setHardness(0.6F);
		setBlockSound(wet ? ModSounds.soundWetSponge : ModSounds.soundSponge);
		setBlockTextureName(wet ? "wet_sponge" : "sponge");
		setBlockName(Utils.getUnlocalisedName(wet ? "wet_sponge" : "sponge"));
		setCreativeTab(ModdedCreativeTabs.BUILDING_BLOCKS);
	}

	public boolean isWet() {
		return wet;
	}

	@Override
	public void onBlockAdded(World world, int x, int y, int z) {
		tryEvaporate(world, x, y, z);
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block neighborBlock) {
		tryEvaporate(world, x, y, z);
		super.onNeighborBlockChange(world, x, y, z, neighborBlock);
	}

	/**
	 * 湿海绵在下界蒸发变回干海绵。
	 */
	private void tryEvaporate(World worldIn, int x, int y, int z) {
		if (wet) {
			boolean inNether = ArrayUtils.contains(BiomeDictionary.getTypesForBiome(worldIn.getBiomeGenForCoords(x, z)), BiomeDictionary.Type.NETHER);
			if (inNether) {
				worldIn.playSoundEffect(x + .5D, y + .5D, z + .5D, "random.fizz", 1, 1);
				worldIn.setBlock(x, y, z, getDrySpongeBlock(), 0, 2);
			}
		}
	}

	/**
	 * 湿海绵蒸发后变成的方块。始终返回原版海绵 {@link Blocks#sponge}，
	 * 因为原版干海绵的吸水逻辑已通过 {@code MixinBlockSponge} 注入。
	 */
	private Block getDrySpongeBlock() {
		return Blocks.sponge;
	}

	@Override
	public void randomDisplayTick(World world, int x, int y, int z, Random rand) {
		if (wet) {
			ForgeDirection dir = getRandomDirection(rand);

			if (dir != ForgeDirection.UP && !World.doesBlockHaveSolidTopSurface(world, x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ)) {
				double d0 = x;
				double d1 = y;
				double d2 = z;

				if (dir == ForgeDirection.DOWN) {
					d1 -= 0.05D;
					d0 += rand.nextDouble();
					d2 += rand.nextDouble();
				} else {
					d1 += rand.nextDouble() * 0.8D;

					if (dir == ForgeDirection.EAST || dir == ForgeDirection.WEST) {
						d2 += rand.nextDouble();

						if (dir == ForgeDirection.EAST)
							d0++;
						else
							d0 += 0.05D;
					} else {
						d0 += rand.nextDouble();

						if (dir == ForgeDirection.SOUTH)
							d2++;
						else
							d2 += 0.05D;
					}
				}

				world.spawnParticle("dripWater", d0, d1, d2, 0.0D, 0.0D, 0.0D);
			}
		}
	}

	@Override
	public void getSubBlocks(Item item, CreativeTabs tab, List<ItemStack> list) {
		if (wet) {
			list.add(new ItemStack(item, 1, 0));
		}
	}

	private ForgeDirection getRandomDirection(Random rand) {
		return ForgeDirection.VALID_DIRECTIONS[rand.nextInt(ForgeDirection.VALID_DIRECTIONS.length)];
	}

	@Override
	public Item getItem(World world, int x, int y, int z) {
		return Item.getItemFromBlock(wet ? ModBlocks.WET_SPONGE.get() : getDrySpongeBlock());
	}

	@Override
	public Item getItemDropped(int meta, Random rand, int fortune) {
		return Item.getItemFromBlock(wet ? ModBlocks.WET_SPONGE.get() : getDrySpongeBlock());
	}
}
