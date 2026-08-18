package ganymedes01.etfuturum.blocks;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

/**
 * 蘑菇柄（Mushroom Stem），1.13+ 从蘑菇方块中分离出的独立方块。
 * <p>
 * 在 1.7.10 中，蘑菇茎是 BlockHugeMushroom 的 meta 10 和 15。
 * 本方块分离出蘑菇柄作为一种独立方块，所有面都显示茎纹理。
 */
public class BlockMushroomStem extends Block {

	@SideOnly(Side.CLIENT)
	private IIcon stemIcon;

	public BlockMushroomStem() {
		super(Material.wood);
		this.setHardness(0.2F);
		this.setStepSound(Block.soundTypeWood);
		this.setBlockName("mushroom_stem");
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister reg) {
		this.stemIcon = reg.registerIcon("mushroom_block_skin_stem");
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		return stemIcon;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
		return stemIcon;
	}

	@Override
	protected boolean canSilkHarvest() {
		return true;
	}
}
