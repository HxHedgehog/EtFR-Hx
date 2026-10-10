package ganymedes01.etfuturum.mixins.early.waterlog;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ganymedes01.etfuturum.waterlog.WaterlogFlow;
import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import net.minecraft.block.Block;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

/**
 * 活板门扩族（26.2 TrapDoorBlock：WATERLOGGED 完整支持，getStateForPlacement/
 * updateShape/neighborChanged 蔓延调度）。与 MixinBlockPane 同构——1.7.10
 * BlockTrapDoor 不声明 removedByPlayer/updateTick（基类空实现），按
 * {@code extends + @Override} 模式添加覆写（隐式重映射）；meta = 朝向&3 +
 * bit2 开 + bit3 上/下半，开关为 XOR 位操作不清 bit16 → 无物品/开关侧剥离
 * 负担（damageDropped 默认 0）；renderType 0/pass 0 画水走普通家族路径零改动。
 *
 * <p>onNeighborBlockChange 是 vanilla 支撑移除 + 红石开关逻辑——用 @Inject HEAD
 * 保留原体（不可 @Override 覆写）。
 */
@Mixin(BlockTrapDoor.class)
public abstract class MixinBlockTrapDoor extends Block {

	protected MixinBlockTrapDoor(Material material) {
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

	/**
	 * 蔓延调度：vanilla 支撑移除 + 红石逻辑照常执行（@Inject 不取消）。
	 */
	@Inject(method = "onNeighborBlockChange(Lnet/minecraft/world/World;IIILnet/minecraft/block/Block;)V", at = @At("HEAD"))
	private void etfuturum$scheduleSpreadOnNeighbor(World world, int x, int y, int z, Block neighbor, CallbackInfo ci) {
		if (!world.isRemote && WaterlogRuntime.isWaterlogged(world, x, y, z)) {
			WaterlogFlow.scheduleSpread(world, x, y, z, (Block) (Object) this);
		}
	}

	@Override
	public void updateTick(World world, int x, int y, int z, Random random) {
		WaterlogFlow.handleFamilyTick(world, x, y, z, (Block) (Object) this);
	}
}
