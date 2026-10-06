package ganymedes01.etfuturum.mixins.early.waterlog;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ganymedes01.etfuturum.waterlog.WaterlogFlow;
import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

/**
 * 蔓延触发（26.2 语义：含水格内部是源水，updateShape → scheduleTick →
 * spread）。写入点已统一迁移到 MixinItemBlock.onItemUse TAIL（楼梯/半砖
 * 共用），本类只保留调度与执行：
 * <ul>
 * <li>{@code onNeighborBlockChange}：BlockStairs 未声明（基类空实现），按
 * MixinBlockSponge 的 {@code extends Block + @Override} 模式添加覆写——
 * 隐式重映射在生产环境（SRG 名）命中 vanilla 的虚分派（onBlockAdded L485 的
 * {@code this.onNeighborBlockChange(air)} 调用经合并后的覆写覆盖 setblock 直接
 * 带 bit16 的放置路径）；<b>不可用 @Unique</b>：@Unique 方法不做重映射，生产
 * 环境下是孤儿方法，虚分派不命中（2026-10-05 蔓延失效实证）。</li>
 * <li>{@code updateTick} HEAD：到点执行 {@link WaterlogFlow#handleFamilyTick}
 * （含水 → 蔓延；未含水 → 3c 延迟成源重查），并取消向基座方块的委托。</li>
 * </ul>
 */
@Mixin(BlockStairs.class)
public abstract class MixinBlockStairs extends Block {

	protected MixinBlockStairs(Material materialIn) {
		super(materialIn);
	}

	/**
	 * 挖掘含水方块留下水源格（26.2 {@code Level.removeBlock(pos, movedByPiston)} =
	 * {@code setBlock(pos, fluidState.createLegacyBlock(), 3)}——含水方块的
	 * FluidState 是源水，破坏后留下静态源水而非空气）。
	 *
	 * <p>GTNH forge 的挖掘路径 {@code ItemInWorldManager.removeBlock(IIIZ)Z}
	 * 调用的正是这个 6 参重载（@Deprecated 的 5 参默认实现只是向下委托，生产
	 * 环境虚分派经合并后的覆写命中）。返回 true 让 tryHarvestBlock 的
	 * harvestBlock（掉落）照常进行——它用挖掘前捕获的 meta，与本返回值无关。
	 * 海洋植物不走这里：它们的格不带 bit16，且 BlockAquaticPlant 已覆写恢复水源。
	 */
	@Override
	public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest) {
		if (WaterlogRuntime.isWaterlogged(world, x, y, z)) {
			world.setBlock(x, y, z, Blocks.water, 0, 3);
			WaterlogRuntime.debug("DIG-SOURCE " + x + "," + y + "," + z);
			return true;
		}
		return super.removedByPlayer(world, player, x, y, z, willHarvest);
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
		if (!world.isRemote && WaterlogRuntime.isWaterlogged(world, x, y, z)) {
			WaterlogFlow.scheduleSpread(world, x, y, z, (Block) (Object) this);
		}
	}

	@Inject(method = "updateTick(Lnet/minecraft/world/World;IIILjava/util/Random;)V", at = @At("HEAD"), cancellable = true)
	private void etfuturum$familyTick(World world, int x, int y, int z, Random random, CallbackInfo ci) {
		if (WaterlogRuntime.isEnabled()) {
			ci.cancel(); // 家族 tick 代替向基座方块 updateTick 的委托
			WaterlogFlow.handleFamilyTick(world, x, y, z, (Block) (Object) this);
		}
	}
}
