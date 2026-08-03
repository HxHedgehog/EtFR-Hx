package ganymedes01.etfuturum.blocks;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.core.utils.Utils;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;

/**
 * 1.8+ 的额外石质半砖（拆分自原 STONE_SLAB）。
 * 每个变体对应一个独立方块实例，注册名为官方名称
 * (smooth_stone_slab / mossy_cobblestone_slab / mossy_stone_brick_slab / cut_sandstone_slab)。
 * <p>
 * 纹理沿用原版方块的图标，避免新增资源文件。
 */
public class BlockStoneSlab1 extends BaseSlab {

	public static final String[] VARIANTS = {"smooth_stone", "mossy_cobblestone", "mossy_stone_brick", "cut_sandstone"};

	private final int variantIndex;

	public BlockStoneSlab1(boolean isDouble, int variantIndex) {
		super(isDouble, Material.rock, VARIANTS[variantIndex]);
		this.variantIndex = variantIndex;
		setHardness(2F);
		setResistance(6F);
		setBlockName(Utils.getUnlocalisedName(VARIANTS[variantIndex] + "_slab"));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister reg) {
		IIcon[] icon = new IIcon[1];
		switch (variantIndex) {
			case 0: icon[0] = Blocks.stone.getIcon(2, 0); break;
			case 1: icon[0] = Blocks.mossy_cobblestone.getIcon(2, 0); break;
			case 2: icon[0] = Blocks.stonebrick.getIcon(2, 1); break;
			case 3: icon[0] = Blocks.sandstone.getIcon(2, 2); break;
			default: icon[0] = Blocks.stone.getIcon(2, 0); break;
		}
		setIcons(icon);
	}

	@Override
	public IIcon getIcon(int side, int meta) {
		if (variantIndex == 3) { // cut_sandstone 受 side 影响
			return Blocks.sandstone.getIcon(side, 2);
		}
		return super.getIcon(side, meta);
	}
}
