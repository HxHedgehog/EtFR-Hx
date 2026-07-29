package ganymedes01.etfuturum.blocks;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public abstract class BaseLeaves extends BlockLeaves implements ISubBlocksBlock {

	private final String[] types;

	public BaseLeaves(String... types) {
		this.types = new String[types.length];
		for (int i = 0; i < types.length; i++) {
			this.types[i] = types[i] + "_leaves";
		}
	}

	public int getRange(int meta) {
		return 4;
	}

	@Override
	public void getSubBlocks(Item item, CreativeTabs tab, List<ItemStack> list) {
		for (int i = 0; i < getTypes().length; i++) {
			list.add(new ItemStack(item, 1, i));
		}
	}

	@Override
	public IIcon getIcon(int side, int meta) {
		return field_150129_M[isOpaqueCube() /*OptiFine compat*/ ? 1 : 0][(meta % 4) % types.length];
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister reg) {
		this.field_150129_M[0] = new IIcon[types.length];
		this.field_150129_M[1] = new IIcon[types.length];
		for (int i = 0; i < types.length; ++i) {
			this.field_150129_M[0][i] = reg.registerIcon(types[i]);
			this.field_150129_M[1][i] = reg.registerIcon(types[i] + "_opaque");
		}
	}

	@Override
	public String[] func_150125_e() {
		return getTypes();
	}

	@Override
	public abstract Item getItemDropped(int meta, Random random, int fortune);

	@Override
	public boolean isOpaqueCube() { //OptiFine compat
		return Blocks.leaves.isOpaqueCube();
	}

	@Override
	public boolean shouldSideBeRendered(IBlockAccess worldIn, int x, int y, int z, int side) { //OptiFine compat
		return Blocks.leaves.shouldSideBeRendered(worldIn, x, y, z, side);
	}

	@Override
	public int getFlammability(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
		return 30;
	}

	@Override
	public int getFireSpreadSpeed(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
		return 60;
	}

	@Override
	public IIcon[] getIcons() {
		return field_150129_M[0];
	}

	@Override
	public String[] getTypes() {
		return types;
	}

	@Override
	public String getNameFor(ItemStack stack) {
		return types[stack.getItemDamage() % types.length];
	}

	@Override
	public void updateTick(World worldIn, int x, int y, int z, Random random) {
		if (!worldIn.isRemote) {
			final int meta = worldIn.getBlockMetadata(x, y, z);
			if ((meta & 8) != 0 && (meta & 4) == 0) {
				final int decayRange = getRange(meta % 4);
				handleLeafDecay(this, worldIn, x, y, z, meta, decayRange);
			}
		}
	}

	private static final int MAX_RANGE = 7;
	private static final int MAX_SIDE = 2 * MAX_RANGE + 1;
	private static final int MAX_VOLUME = MAX_SIDE * MAX_SIDE * MAX_SIDE;
	private static final boolean[] visited = new boolean[MAX_VOLUME];
	private static final int[] queue = new int[MAX_VOLUME];
	private static final int[] DX = { -1, 1, 0, 0, 0, 0 };
	private static final int[] DY = { 0, 0, -1, 1, 0, 0 };
	private static final int[] DZ = { 0, 0, 0, 0, -1, 1 };

	private static void handleLeafDecay(Block block, World world, int x, int y, int z, int meta, int range) {
		if (range > 7) {
			throw new IllegalArgumentException();
		}
		final int r = range + 1;
		if (world.checkChunksExist(x - r, y - r, z - r, x + r, y + r, z + r)) {
			if (isConnectedToLog(world, x, y, z, range)) {
				world.setBlockMetadataWithNotify(x, y, z, meta & -9, 4);
			} else {
				block.dropBlockAsItem(world, x, y, z, meta, 0);
				world.setBlockToAir(x, y, z);
			}
		} else {
			world.setBlockMetadataWithNotify(x, y, z, meta & -9, 4);
		}
	}

	private static boolean isConnectedToLog(World world, int x, int y, int z, int range) {
		final int side = 2 * range + 1;
		final int sideSquared = side * side;
		final int volume = sideSquared * side;
		final int maxDepth = range - 1;
		Arrays.fill(visited, 0, volume, false);
		int head = 0;
		int tail = 0;
		final int startIdx = range * sideSquared + range * side + range;
		visited[startIdx] = true;
		queue[tail++] = startIdx;
		while (head < tail) {
			final int entry = queue[head++];
			final int dist = entry >>> 16;
			final int idx = entry & 0xFFFF;
			final int dx = (idx / sideSquared) - range;
			final int dy = ((idx % sideSquared) / side) - range;
			final int dz = (idx % side) - range;
			for (int face = 0; face < 6; face++) {
				final int nx = dx + DX[face];
				final int ny = dy + DY[face];
				final int nz = dz + DZ[face];
				if (nx < -range || nx > range || ny < -range || ny > range || nz < -range || nz > range) continue;
				final int nIdx = (nx + range) * sideSquared + (ny + range) * side + (nz + range);
				if (visited[nIdx]) continue;
				visited[nIdx] = true;
				final Block block = world.getBlock(x + nx, y + ny, z + nz);
				if (block.canSustainLeaves(world, x + nx, y + ny, z + nz)) {
					return true;
				}
				if (dist < maxDepth && block.isLeaves(world, x + nx, y + ny, z + nz)) {
					queue[tail++] = ((dist + 1) << 16) | nIdx;
				}
			}
		}
		return false;
	}
}
