package ganymedes01.etfuturum.mixins.early.waterlog;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;

import ganymedes01.etfuturum.waterlog.WaterlogFlow;
import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

/**
 * 栅栏扩族（26.2 FenceBlock extends CrossCollisionBlock——与玻璃板/铁栏杆同基类，
 * WATERLOGGED 完整支持：getStateForPlacement 按 replacedFluidState 判定、updateShape
 * 含水 scheduleTick 蔓延）。与 MixinBlockPane 同构——1.7.10 BlockFence 不声明
 * onNeighborBlockChange/updateTick/removedByPlayer（基类空实现），按
 * {@code extends + @Override} 模式添加覆写（隐式重映射，生产 SRG 环境虚分派命中）。
 *
 * <p>1.7.10 BlockFence 无 meta（连接状态靠 canConnectFenceTo 邻居查询，碰撞箱由
 * setBlockBoundsBasedOnState 动态计算）→ 无 meta 重写/物品侧剥离负担
 * （damageDropped 默认 0 天然干净）；柱足碰撞箱窄条，完整面判定四象限自动不完整。
 * EtFR 木栅栏/新木栅栏（BlockWoodFence/BlockModernWoodFence）经 instanceof 自动继承。
 */
@Mixin(BlockFence.class)
public abstract class MixinBlockFence extends Block {

	protected MixinBlockFence(String texture, Material material) {
		super(material);
	}

	/**
	 * 挖掘含水方块留下水源格——双载全覆写（6 参 = 服务端挖掘路径；5 参 = GTNH
	 * 客户端挖掘预测，默认 setBlockToAir），共用 {@link WaterlogFlow#leaveSourceWater}。
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
