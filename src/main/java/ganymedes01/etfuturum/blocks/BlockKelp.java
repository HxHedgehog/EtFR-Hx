package ganymedes01.etfuturum.blocks;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.client.sound.ModSounds;
import ganymedes01.etfuturum.core.utils.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.item.Item;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Random;

/**
 * Kelp tip (growing head), aligned with the modern {@code GrowingPlantHeadBlock}:
 * <ul>
 * <li>Metadata stores the age (0-{@link #MAX_AGE}); on growth the head moves one
 * block up and the old block turns into {@link BlockKelpPlant}. The modern cap is
 * age 25, but 1.7.10 metadata only holds 4 bits, so 15 is the hard limit here.</li>
 * <li>Bone meal ignores the age and always grows one block, even at max age
 * (modern {@code performBonemeal} caps the new tip age instead of failing).</li>
 * <li>A head placed underneath an existing stalk becomes a body segment
 * immediately, matching modern {@code getStateForPlacement}.</li>
 * <li>A player-placed head gets a random age (0-14), like the modern
 * {@code getStateForPlacement(random)} (nextInt(25)).</li>
 * </ul>
 * The head always drops a kelp item when destroyed, by any means.
 */
public class BlockKelp extends BlockAquaticPlant implements IGrowable {

	public static final int MAX_AGE = 15;

	/** Modern KelpBlock GROW_PER_TICK_PROBABILITY: 14% per random tick. */
	private static final float GROW_PER_TICK_PROBABILITY = 0.14F;

	public BlockKelp() {
		setNames("kelp");
		Utils.setBlockSound(this, ModSounds.soundWetGrass);
		setTickRandomly(true);
		setBlockBounds(0.1F, 0.0F, 0.1F, 0.9F, 1.0F, 0.9F);
	}

	@Override
	public void onBlockAdded(World world, int x, int y, int z) {
		// Modern GrowingPlantBlock#getStateForPlacement: a kelp item placed beneath
		// an existing stalk becomes a body segment right away. setBlock only notifies
		// the six neighbours, so the placed block itself never receives
		// onNeighborBlockChange and would otherwise stay a head forever.
		Block above = world.getBlock(x, y + 1, z);
		if (!world.isRemote && (above == ModBlocks.KELP.get() || above == ModBlocks.KELP_PLANT.get())) {
			world.setBlock(x, y, z, ModBlocks.KELP_PLANT.get(), 0, 3);
		}
	}

	/**
	 * Modern GrowingPlantHeadBlock#getStateForPlacement(random): a placed head
	 * gets a random age so player-built stalks have varied remaining growth.
	 * onPostBlockPlaced is only called from ItemBlock#placeBlockAt, never by
	 * worldgen or natural growth, so this can't clobber the age set by growOne();
	 * the getBlock check skips the stalk-underneath case already converted to a
	 * body segment by {@link #onBlockAdded}.
	 */
	@Override
	public void onPostBlockPlaced(World world, int x, int y, int z, int meta) {
		if (!world.isRemote && world.getBlock(x, y, z) == this) {
			world.setBlockMetadataWithNotify(x, y, z, world.rand.nextInt(MAX_AGE), 3);
		}
	}

	@Override
	public boolean canPlaceBlockAt(World world, int x, int y, int z) {
		if (!isLiquidWater(world.getBlock(x, y, z))) {
			return false;
		}
		Block below = world.getBlock(x, y - 1, z);
		return below == ModBlocks.KELP.get() || below == ModBlocks.KELP_PLANT.get()
				|| below.isSideSolid(world, x, y - 1, z, ForgeDirection.UP);
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
		Block below = world.getBlock(x, y - 1, z);
		if (below != ModBlocks.KELP.get() && below != ModBlocks.KELP_PLANT.get()
				&& !below.isSideSolid(world, x, y - 1, z, ForgeDirection.UP)) {
			die(world, x, y, z);
			return;
		}
		// Matches modern GrowingPlantHeadBlock#updateShape: a head that has kelp
		// growing on top of it (another placed head or a plant segment) turns into
		// a plant stem itself. This is what makes a column of player-placed kelp
		// visually connect instead of leaving the head texture's empty top gap.
		Block above = world.getBlock(x, y + 1, z);
		if (!world.isRemote && (above == ModBlocks.KELP.get() || above == ModBlocks.KELP_PLANT.get())) {
			world.setBlock(x, y, z, ModBlocks.KELP_PLANT.get(), 0, 3);
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public String getItemIconName() {
		return "kelp";
	}

	@Override
	public Item getItemDropped(int meta, Random random, int fortune) {
		return Item.getItemFromBlock(this);
	}

	@Override
	public int quantityDropped(Random random) {
		return 1;
	}

	@Override
	public void updateTick(World world, int x, int y, int z, Random rand) {
		// Modern GrowingPlantHeadBlock#randomTick: only below max age, 14% chance.
		if (!world.isRemote && (world.getBlockMetadata(x, y, z) & 15) < MAX_AGE && rand.nextFloat() < GROW_PER_TICK_PROBABILITY) {
			growOne(world, x, y, z);
		}
	}

	/**
	 * Modern GrowingPlantHeadBlock#performBonemeal for kelp: grows exactly one
	 * block and caps the new tip age, without checking the current age, so bone
	 * meal keeps working at max age. Returns false when the space above isn't water.
	 */
	public boolean growOne(World world, int x, int y, int z) {
		if (!isLiquidWater(world.getBlock(x, y + 1, z))) {
			return false;
		}
		int age = Math.min((world.getBlockMetadata(x, y, z) & 15) + 1, MAX_AGE);
		world.setBlock(x, y, z, ModBlocks.KELP_PLANT.get(), 0, 3);
		world.setBlock(x, y + 1, z, this, age, 3);
		return true;
	}

	@Override
	public boolean func_149851_a(World world, int x, int y, int z, boolean isClient) {
		// Modern isValidBonemealTarget: no age check, only the growth space above.
		return isLiquidWater(world.getBlock(x, y + 1, z));
	}

	@Override
	public boolean func_149852_a(World world, Random rand, int x, int y, int z) {
		return true;
	}

	@Override
	public void func_149853_b(World world, Random rand, int x, int y, int z) {
		growOne(world, x, y, z);
	}
}
