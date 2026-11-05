package ganymedes01.etfuturum.mixins.early.waterlog;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

/**
 * 游泳/浮力/inWater 字段的咽喉桥接（plan §3.4 #2）：vanilla 的
 * {@code Entity.inWater} 与游泳推力全走
 * {@code World.handleMaterialAcceleration(AABB, Material.water, entity)}，
 * 遍历 AABB 内格子按材质判定。含水楼梯（rock 材质）本会被跳过。
 *
 * <p>两处重定向让含水格表现为 262 的虚拟流体（meta 0 静水源）：
 * <ul>
 * <li>{@code getBlock}：含水格返回原版静水方块替身——Material.water 且
 * BlockStaticLiquid 不覆写 {@code velocityToAddToEntity}（含水格是静水，
 * 262 语义无流向推力）；{@code getMaterial} 判定因此放行；</li>
 * <li>{@code getBlockMetadata}：含水格按 meta 0 计算水面
 * {@code (y+1) - 1/9 = y + 8/9}（262 的 source amount 8/9），与渲染水面
 * 与眼位分界一致。</li>
 * </ul>
 * 两个注入点在方法内均唯一；门控关闭时零改动。
 */
@Mixin(World.class)
public abstract class MixinWorld {

	@Redirect(method = "handleMaterialAcceleration", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlock(III)Lnet/minecraft/block/Block;"))
	private Block etfuturum$waterloggedAsWater(World world, int x, int y, int z) {
		Block block = world.getBlock(x, y, z);
		return WaterlogRuntime.isWaterlogged(world, x, y, z) ? Blocks.water : block;
	}

	@Redirect(method = "handleMaterialAcceleration", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockMetadata(III)I"))
	private int etfuturum$waterloggedMeta(World world, int x, int y, int z) {
		return WaterlogRuntime.isWaterlogged(world, x, y, z) ? 0 : world.getBlockMetadata(x, y, z);
	}
}
