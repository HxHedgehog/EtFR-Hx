package ganymedes01.etfuturum.blocks;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.client.sound.ModSounds;
import ganymedes01.etfuturum.core.utils.Utils;
import ganymedes01.etfuturum.waterlog.WaterlogFlow;
import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.init.Blocks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;

import java.util.ArrayList;
import java.util.Random;

/**
 * Seagrass and tall seagrass, single block ID:
 * meta 0 = short seagrass, 1 = tall seagrass lower half, 2 = tall seagrass upper half.
 * Drops nothing without shears (like tall grass); shearing yields one seagrass item
 * for the short plant and two for either half of the tall plant.
 */
public class BlockSeagrass extends BlockAquaticPlant implements IShearable, IGrowable {

	public static final int META_SHORT = 0;
	public static final int META_LOWER = 1;
	public static final int META_UPPER = 2;

	@SideOnly(Side.CLIENT)
	private IIcon lowerIcon;
	@SideOnly(Side.CLIENT)
	private IIcon upperIcon;

	public BlockSeagrass() {
		setNames("seagrass");
		Utils.setBlockSound(this, ModSounds.soundWetGrass);
		float halfWidth = 0.4F;
		setBlockBounds(0.5F - halfWidth, 0.0F, 0.5F - halfWidth, 0.5F + halfWidth, 0.8F, 0.5F + halfWidth);
	}

	@Override
	public void onBlockAdded(World world, int x, int y, int z) {
		// 26.2: the cell holds a permanent source FluidState, so placement starts
		// the spread chain the same way as the waterlogged family
		WaterlogFlow.scheduleSpread(world, x, y, z, this);
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
		int meta = world.getBlockMetadata(x, y, z);
		if (meta == META_UPPER) {
			Block below = world.getBlock(x, y - 1, z);
			if (below != this || world.getBlockMetadata(x, y - 1, z) != META_LOWER) {
				die(world, x, y, z);
				return;
			}
		} else if (meta == META_LOWER) {
			Block above = world.getBlock(x, y + 1, z);
			if (!hasSolidGround(world, x, y, z) || above != this || world.getBlockMetadata(x, y + 1, z) != META_UPPER) {
				die(world, x, y, z);
				return;
			}
		} else if (!hasSolidGround(world, x, y, z)) {
			die(world, x, y, z);
			return;
		}
		// 26.2 SeagrassBlock#updateShape: every neighbour change schedules the
		// fluid tick of the permanent source water -> one spread step
		WaterlogFlow.scheduleSpread(world, x, y, z, this);
	}

	@Override
	public void updateTick(World world, int x, int y, int z, Random rand) {
		// The scheduled fluid tick: the cell is always a source (getFluidState =
		// getSource(false)), so this is a plain spread step - no source-formation
		// recheck like the waterlogged family. Guarded on the cell still being
		// seagrass: a drain (sponge) in between must not create water from nothing.
		if (!world.isRemote && world.getBlock(x, y, z) == this) {
			WaterlogFlow.spreadFromSource(world, x, y, z);
		}
	}

	/**
	 * 26.2 SeagrassBlock extends VegetationBlock: updateShape replaces a plant
	 * that lost its support with AIR <b>silently</b> - no break particles, no
	 * sound, no drops (the loot is empty anyway) and no water restore; the
	 * surrounding water flows back through vanilla spread. (Kelp takes the
	 * GrowingPlantBlock path instead: scheduleTick -> Level.destroyBlock ->
	 * break particles + sound + drops + the cell keeps its FluidState, which is
	 * the base class {@code die()}.)
	 */
	@Override
	protected void die(World world, int x, int y, int z) {
		world.setBlock(x, y, z, Blocks.air, 0, 3);
	}

	/**
	 * Both halves keep their FluidState when the plant is dug: the dug cell via
	 * the base removedByPlayer, and - for an upper-half dig - the lower half is
	 * restored to still water with a 2001 event on it instead of vanishing
	 * through updateShape. 26.2 only does this in creative
	 * (preventDropFromBottomPart, gated on preventsBlockDrops) and lets the
	 * lower half go silent AIR on survival digs; here it is deliberately
	 * extended to survival so either half always leaves both cells watered.
	 * Digging the lower half never special-cases the upper (it just goes AIR).
	 */
	@Override
	public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z) {
		if (!world.isRemote && world.getBlockMetadata(x, y, z) == META_UPPER) {
			Block lower = world.getBlock(x, y - 1, z);
			int lowerMeta = world.getBlockMetadata(x, y - 1, z);
			if (lower == this && lowerMeta == META_LOWER) {
				world.playAuxSFX(2001, x, y - 1, z, Block.getIdFromBlock(lower) + (lowerMeta << 12));
				world.setBlock(x, y - 1, z, Blocks.water, 0, 3);
			}
		}
		return super.removedByPlayer(world, player, x, y, z);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister reg) {
		// BaseBlock uses the minecraft texture domain (empty getTextureDomain()),
		// so these live in assets/minecraft/textures/blocks/ like the other EtFR blocks
		blockIcon = reg.registerIcon(getTextureName());
		lowerIcon = reg.registerIcon("tall_seagrass_bottom");
		upperIcon = reg.registerIcon("tall_seagrass_top");
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		if (meta == META_LOWER) {
			return lowerIcon;
		}
		if (meta == META_UPPER) {
			return upperIcon;
		}
		return blockIcon;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
		return getIcon(side, world.getBlockMetadata(x, y, z));
	}

	@Override
	public boolean isShearable(ItemStack item, IBlockAccess world, int x, int y, int z) {
		return true;
	}

	@Override
	public ArrayList<ItemStack> onSheared(ItemStack item, IBlockAccess world, int x, int y, int z, int fortune) {
		ArrayList<ItemStack> ret = new ArrayList<>();
		ret.add(new ItemStack(this, 1, META_SHORT));
		if (world.getBlockMetadata(x, y, z) != META_SHORT) {
			ret.add(new ItemStack(this, 1, META_SHORT));
		}
		return ret;
	}

	@Override
	public boolean func_149851_a(World world, int x, int y, int z, boolean isClient) {
		return world.getBlockMetadata(x, y, z) == META_SHORT && isLiquidWater(world.getBlock(x, y + 1, z));
	}

	@Override
	public boolean func_149852_a(World world, Random rand, int x, int y, int z) {
		return true;
	}

	@Override
	public void func_149853_b(World world, Random rand, int x, int y, int z) {
		if (func_149851_a(world, x, y, z, false)) {
			world.setBlock(x, y, z, this, META_LOWER, 3);
			world.setBlock(x, y + 1, z, this, META_UPPER, 3);
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public String getItemIconName() {
		return getTextureName();
	}

	@Override
	public Item getItem(World worldIn, int x, int y, int z) {
		return Item.getItemFromBlock(this);
	}
}
