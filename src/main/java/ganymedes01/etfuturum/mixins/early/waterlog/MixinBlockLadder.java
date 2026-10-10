package ganymedes01.etfuturum.mixins.early.waterlog;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ganymedes01.etfuturum.waterlog.WaterlogFlow;
import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

/**
 * 梯子扩族（26.2 LadderBlock：WATERLOGGED 完整支持，getStateForPlacement/
 * updateShape 蔓延调度）。与 MixinBlockPane 同构——1.7.10 BlockLadder 不声明
 * removedByPlayer/updateTick（基类空实现），按 {@code extends + @Override} 模式
 * 添加覆写（隐式重映射）；meta = 朝向 2-5，无物品侧剥离负担（damageDropped
 * 默认 0）。renderType 8/pass 0 画水走普通家族路径零改动。
 *
 * <p><b>bit16 裸比较陷阱（2026-10-10 用户实证"梯子放水里不可见"）</b>：梯子是
 * 家族中唯一不掩码 meta 的成员——renderBlockLadder（rfg L2719-2748）与
 * onNeighborBlockChange 支撑检查（L151-172）都是 {@code l == 2/3/4/5} 裸比较，
 * 含水位 meta（如 0x12）无一命中 → 什么画面片都不画 + 支撑判定失败 →
 * dropBlockAsItem + setBlockToAir（放置瞬间自毁，双症状一因）。修复 =
 * 两处 getBlockMetadata @Redirect 剥离 bit16（楼梯 &3/半砖 &7 天然掩码，
 * 只有梯子需要）。isLadder 恒 true（攀爬无恙）。
 *
 * <p>onNeighborBlockChange 是 vanilla 支撑移除逻辑（背墙消失 → 掉落 +
 * setBlockToAir，与 26.2 updateShape → AIR 丢弃水同构）——用 @Inject HEAD
 * 保留原体（不可 @Override 覆写）。
 */
@Mixin(BlockLadder.class)
public abstract class MixinBlockLadder extends Block {

	/** 含水位（bit16）从梯子的裸 meta 比较中剥离。 */
	private static int etfuturum$stripWaterlogBit(int meta) {
		return meta & ~WaterlogRuntime.WATERLOGGED_BIT;
	}

	// 注意 owner：setBlockBoundsBasedOnState 的参数是 IBlockAccess（非 World），
	// @Redirect owner 必须与调用点指令一致——曾写 World 导致静默不生效，
	// func_149797_b(0x12) 无命中 → 碰撞箱停留在构造器默认满格立方 → "整格水是固体"
	@Redirect(method = "setBlockBoundsBasedOnState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/IBlockAccess;getBlockMetadata(III)I"))
	private int etfuturum$maskBoundsMeta(net.minecraft.world.IBlockAccess world, int x, int y, int z) {
		return etfuturum$stripWaterlogBit(world.getBlockMetadata(x, y, z));
	}

	@Redirect(method = "onNeighborBlockChange", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockMetadata(III)I"))
	private int etfuturum$maskSupportMeta(World world, int x, int y, int z) {
		return etfuturum$stripWaterlogBit(world.getBlockMetadata(x, y, z));
	}

	protected MixinBlockLadder() {
		super(Material.circuits);
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
	 * 蔓延调度：vanilla 支撑移除逻辑照常执行（@Inject 不取消）。
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
