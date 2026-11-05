package ganymedes01.etfuturum.mixins.early.waterlog;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ganymedes01.etfuturum.waterlog.WaterlogFlow;
import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import net.minecraft.block.Block;
import net.minecraft.block.BlockPane;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * 玻璃板扩族（26.2 PaneBlock）：与 MixinBlockSlab 同构——BlockPane 不声明
 * onNeighborBlockChange/updateTick（基类空实现），按 MixinBlockSponge/
 * MixinBlockStairs 的 {@code extends + @Override} 模式添加覆写（隐式重映射，
 * 生产 SRG 环境虚分派命中）。26.2 PaneBlock.updateShape 含水时
 * {@code scheduleTick} → 源水 spread（与楼梯同构），canSurvive 恒真。
 *
 * <p>含水视觉 = 整格 8/9 静水体叠在薄板几何上（画水/celeritas/桶/射线管线
 * 均按 {@link WaterlogFamilies#isWaterloggable} 家族过滤，玻璃板登记后自动
 * 生效，无需客户端改动）。thinpanes.MixinBlockPane 同目标但注入不同方法
 * （碰撞箱/渲染边界），互不冲突。
 */
@Mixin(BlockPane.class)
public abstract class MixinBlockPane extends Block {

	protected MixinBlockPane(Material material) {
		super(material);
	}

	/**
	 * 挖掘含水方块留下水源格——与 MixinBlockStairs 同款双载全覆写（6 参 =
	 * 服务端挖掘路径；5 参 = GTNH 客户端挖掘预测，默认 setBlockToAir），共用
	 * {@link WaterlogFlow#leaveSourceWater}。
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
	public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
		if (!world.isRemote && WaterlogRuntime.isWaterlogged(world, x, y, z)) {
			WaterlogFlow.scheduleSpread(world, x, y, z, (Block) (Object) this);
		}
	}

	@Override
	public void updateTick(World world, int x, int y, int z, Random random) {
		WaterlogFlow.handleFamilyTick(world, x, y, z, (Block) (Object) this);
	}

	/**
	 * 精采掉落剥离含水位：BlockPane 覆写 createStackedBlock 直接用原 meta 构建
	 * 物品（不问 getHasSubtypes），含水格 bit16 直通精采物品（玻璃板与染色板
	 * 共用本实现）。26.2 精采 = state 的物品形态，waterlogged 天然不在物品上。
	 */
	@Inject(method = "createStackedBlock(I)Lnet/minecraft/item/ItemStack;", at = @At("HEAD"), cancellable = true)
	private void etfuturum$stripWaterlogFromSilkDrop(int meta, CallbackInfoReturnable<ItemStack> cir) {
		if (WaterlogRuntime.isEnabled() && (meta & WaterlogRuntime.WATERLOGGED_BIT) != 0) {
			cir.setReturnValue(new ItemStack(Item.getItemFromBlock((Block) (Object) this), 1, meta & ~WaterlogRuntime.WATERLOGGED_BIT));
		}
	}
}
