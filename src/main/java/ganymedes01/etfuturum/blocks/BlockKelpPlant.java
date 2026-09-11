package ganymedes01.etfuturum.blocks;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.client.sound.ModSounds;
import ganymedes01.etfuturum.core.utils.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Random;

/**
 * Kelp body segment, aligned with the modern {@code GrowingPlantBodyBlock}.
 * No random ticks: only the head ({@link BlockKelp}) grows.
 * If the block above is no longer kelp (e.g. the tip was harvested) the exposed
 * body turns into a fresh head with a random age, matching the modern
 * growing-plant behaviour; if it loses all support it breaks and drops a kelp item.
 * Bone meal on a segment climbs the stalk and fertilises the top head, exactly
 * like modern {@code GrowingPlantBodyBlock#performBonemeal}.
 */
public class BlockKelpPlant extends BlockAquaticPlant implements IGrowable {

	public BlockKelpPlant() {
		setNames("kelp_plant");
		Utils.setBlockSound(this, ModSounds.soundWetGrass);
		// Modern KelpPlantBlock uses a full cube shape.
		setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
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
		boolean supported = below == ModBlocks.KELP.get() || below == ModBlocks.KELP_PLANT.get()
				|| below.isSideSolid(world, x, y - 1, z, ForgeDirection.UP);
		if (!supported) {
			die(world, x, y, z);
			return;
		}

		Block above = world.getBlock(x, y + 1, z);
		if (above != ModBlocks.KELP.get() && above != ModBlocks.KELP_PLANT.get()) {
			// Matches modern GrowingPlantBodyBlock#updateShape: an exposed stem tip
			// (the head was harvested, or it is the surface touching air) turns into
			// a fresh head with a random age (getStateForPlacement); only the loss
			// of support below kills the plant.
			if (!world.isRemote) {
				world.setBlock(x, y, z, ModBlocks.KELP.get(), world.rand.nextInt(BlockKelp.MAX_AGE), 3);
			}
		}
	}

	@Override
	public Item getItemDropped(int meta, Random random, int fortune) {
		return Item.getItemFromBlock(ModBlocks.KELP.get());
	}

	@Override
	public int quantityDropped(Random random) {
		return 1;
	}

	@Override
	public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z, EntityPlayer player) {
		// Modern GrowingPlantBodyBlock#getCloneItemStack: pick block yields the kelp item.
		return new ItemStack(Item.getItemFromBlock(ModBlocks.KELP.get()));
	}

	/**
	 * Modern GrowingPlantBodyBlock#getHeadPos (BlockUtil.getTopConnectedBlock):
	 * walks up the stalk over body segments; returns the tip head's Y, or -1 if
	 * the stalk doesn't end in a head.
	 */
	private int findHeadY(World world, int x, int y, int z) {
		int yy = y;
		while (yy < 255 && world.getBlock(x, yy + 1, z) == ModBlocks.KELP_PLANT.get()) {
			yy++;
		}
		return world.getBlock(x, yy + 1, z) == ModBlocks.KELP.get() ? yy + 1 : -1;
	}

	@Override
	public boolean func_149851_a(World world, int x, int y, int z, boolean isClient) {
		// Modern GrowingPlantBodyBlock#isValidBonemealTarget: the stalk must end in
		// a head whose growth position above is water.
		int headY = findHeadY(world, x, y, z);
		return headY >= 0 && BlockAquaticPlant.isLiquidWater(world.getBlock(x, headY + 1, z));
	}

	@Override
	public boolean func_149852_a(World world, Random rand, int x, int y, int z) {
		return true;
	}

	@Override
	public void func_149853_b(World world, Random rand, int x, int y, int z) {
		int headY = findHeadY(world, x, y, z);
		if (headY >= 0) {
			((BlockKelp) ModBlocks.KELP.get()).growOne(world, x, headY, z);
		}
	}
}
