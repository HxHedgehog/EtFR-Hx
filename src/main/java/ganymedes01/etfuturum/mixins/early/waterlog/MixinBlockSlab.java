package ganymedes01.etfuturum.mixins.early.waterlog;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;

import ganymedes01.etfuturum.waterlog.WaterlogFlow;
import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

/**
 * 半砖的蔓延触发（与 MixinBlockStairs 同构，逻辑统一在
 * {@link WaterlogFlow#handleFamilyTick}）。写入点统一在
 * MixinItemBlock.onItemUse TAIL（半砖的 ItemSlab 合成双层分支不经过
 * ItemBlock.onItemUse，天然不含水）。
 *
 * <p>BlockSlab 不声明 onNeighborBlockChange/updateTick（基类空实现）——按
 * MixinBlockSponge/MixinBlockStairs 的 {@code extends + @Override} 模式添加
 * 覆写（隐式重映射，生产 SRG 环境虚分派命中；@Unique 是孤儿方法不可用）。
 * ItemSlab 合成双层时 {@code setBlock(double)} 重写 meta，bit16 随之丢失 =
 * 双层丢水（26.2 SlabBlock L83 合成 DOUBLE 时 WATERLOGGED=false 同款）。
 */
@Mixin(BlockSlab.class)
public abstract class MixinBlockSlab extends Block {

	protected MixinBlockSlab(Material material) {
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
}
