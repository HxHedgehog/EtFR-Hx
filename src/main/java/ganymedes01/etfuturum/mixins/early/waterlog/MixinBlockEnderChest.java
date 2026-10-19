package ganymedes01.etfuturum.mixins.early.waterlog;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import ganymedes01.etfuturum.waterlog.WaterlogFlow;
import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

/**
 * 末影箱扩族（26.2 EnderChestBlock：WATERLOGGED 完整支持，getStateForPlacement
 * L88 按 replacedFluidState 判定、updateShape 蔓延 L155/L163 scheduleTick）。
 * 1710 meta = 朝向 2-5，onBlockPlacedBy 整格重写为朝向（无双箱合并、唯一写点）
 * → @Redirect 纯保留式并回 bit16（同 MixinBlockChest 约定）；
 * onNeighborBlockChange/updateTick/removedByPlayer 均未声明（基类空实现）→
 * extends + @Override（无 super 负担，同 MixinBlockStairs 模式）；
 * damageDropped 未声明（基类默认 0）→ 物品侧干净。
 */
@Mixin(BlockEnderChest.class)
public abstract class MixinBlockEnderChest extends Block {

	protected MixinBlockEnderChest(Material material) {
		super(material);
	}

	/**
	 * 挖掘含水方块留下水源格——双载全覆写，共用
	 * {@link WaterlogFlow#leaveSourceWater}（末影箱 silk touch 走
	 * createStackedBlock → damageDropped 默认 0，物品无 bit16 负担）。
	 */
	@Override
	public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest) {
		return WaterlogFlow.leaveSourceWater(world, x, y, z) || super.removedByPlayer(world, player, x, y, z, willHarvest);
	}

	@Override
	public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z) {
		return WaterlogFlow.leaveSourceWater(world, x, y, z) || super.removedByPlayer(world, player, x, y, z);
	}

	@Override
	public void updateTick(World world, int x, int y, int z, Random random) {
		WaterlogFlow.handleFamilyTick(world, x, y, z, (Block) (Object) this);
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
		if (!world.isRemote && WaterlogRuntime.isWaterlogged(world, x, y, z)) {
			WaterlogFlow.scheduleSpread(world, x, y, z, (Block) (Object) this);
		}
	}

	@Redirect(method = "onBlockPlacedBy(Lnet/minecraft/world/World;IIILnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockMetadataWithNotify(IIIII)Z"))
	private boolean etfuturum$keepWaterlogOnPlacedBy(World world, int x, int y, int z, int meta, int flags) {
		return world.setBlockMetadataWithNotify(x, y, z, meta | (world.getBlockMetadata(x, y, z) & WaterlogRuntime.WATERLOGGED_BIT), flags);
	}
}
