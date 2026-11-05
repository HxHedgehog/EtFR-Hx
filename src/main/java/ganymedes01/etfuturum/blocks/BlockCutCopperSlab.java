package ganymedes01.etfuturum.blocks;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.client.sound.ModSounds;
import ganymedes01.etfuturum.core.utils.Utils;
import ganymedes01.etfuturum.waterlog.WaterlogFlow;
import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

public class BlockCutCopperSlab extends BaseSlab implements IDegradable {

	public BlockCutCopperSlab(boolean isDouble) {
		this(isDouble, "cut_copper", "exposed_cut_copper", "weathered_cut_copper", "oxidized_cut_copper", "waxed_cut_copper", "waxed_exposed_cut_copper",
				"waxed_weathered_cut_copper", "waxed_oxidized_cut_copper");
	}

	public BlockCutCopperSlab(boolean isDouble, String... types) {
		super(isDouble, Material.iron, types);
		setHardness(3);
		setResistance(6);
		setHarvestLevel("pickaxe", 1);
		setBlockName(Utils.getUnlocalisedName("cut_copper_slab"));
		setBlockTextureName("cut_copper");
		setBlockSound(ModSounds.soundCopper);
		setTickRandomly(true);
	}

	@Override
	public void updateTick(World world, int x, int y, int z, Random rand) {
		// 家族含水 tick：MixinBlockSlab 在 BlockSlab.updateTick 注入的蔓延调度被本
		// 覆写遮蔽（虚分派不经过基类方法体），须显式补上（26.2 updateShape →
		// scheduleTick 语义）。本 tick 也会被风化调度触发，用 isEnabled 门控，
		// 关闭时零行为（handleFamilyTick 的 wouldFormSource 不查开关）。
		if (WaterlogRuntime.isEnabled()) {
			WaterlogFlow.handleFamilyTick(world, x, y, z, this);
		}
		tickDegradation(world, x, y, z, rand);
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer entityPlayer, int side, float subX, float subY, float subZ) {
		return tryWaxOnWaxOff(world, x, y, z, entityPlayer);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister reg) {
		setIcons(new IIcon[4]);
		for (int i = 0; i < getIcons().length; i++) {
			getIcons()[i] = reg.registerIcon((getTextureDomain().isEmpty() ? "" : getTextureDomain() + ":") + getTypes()[i]);
		}
	}

	@Override
	public int getFinalCopperMeta(IBlockAccess world, int x, int y, int z, int copperMeta, int worldMeta) {
		return (copperMeta - (copperMeta > 11 ? 8 : 4)) + (worldMeta > 7 ? 8 : 0);
	}

	public int getCopperMeta(int meta) {
		return meta % 8 + (meta % 8 < 4 ? 4 : 8);
	}

	public Block getCopperBlockFromMeta(int meta) {
		return this;
	}

}
