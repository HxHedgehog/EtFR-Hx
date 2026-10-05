package ganymedes01.etfuturum.mixins.early.waterlog.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

/**
 * 水下视野咽喉桥（1710 实证）：水下雾的密度（EntityRenderer.setupFog L2004）
 * 与颜色（EntityRenderer.updateFogColor L1819→L1829）都由
 * {@code ActiveRenderInfo.getBlockAtEntityViewpoint} 按眼位格方块的材质判定，
 * {@code Entity.isInsideOfMaterial} 不参与——{@link ganymedes01.etfuturum.mixins.early.waterlog.MixinEntity}
 * 只覆盖溺水/第一人称覆盖层/粒子链路，没有这个桥时含水楼梯里无水下视野。
 *
 * <p>含水格替身 = 静态水 + meta 1：{@code getLiquidHeightPercent(1) = 2/9} →
 * 方法的视点表面判线恰为 {@code y + 8/9}，与 MixinEntity 的 8/9 眼位分界、
 * StairWaterRenderer 的渲染水面三点一致——眼位高于 8/9 时 vanilla 自然改查
 * 上方格（空气 → 无雾），与渲染水面无缝衔接（海带式切换）。两个注入点在方法
 * 内均唯一命中各自的调用形态（getBlock 两处都应替身：眼位格与上方回退格）；
 * 门控关闭时零改动。
 */
@Mixin(ActiveRenderInfo.class)
public abstract class MixinActiveRenderInfo {

	@Redirect(method = "getBlockAtEntityViewpoint(Lnet/minecraft/world/World;Lnet/minecraft/entity/EntityLivingBase;F)Lnet/minecraft/block/Block;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlock(III)Lnet/minecraft/block/Block;"))
	private static Block etfuturum$waterloggedEyeCell(World world, int x, int y, int z) {
		Block block = world.getBlock(x, y, z);
		return WaterlogRuntime.isWaterlogged(world, x, y, z) ? Blocks.water : block;
	}

	@Redirect(method = "getBlockAtEntityViewpoint(Lnet/minecraft/world/World;Lnet/minecraft/entity/EntityLivingBase;F)Lnet/minecraft/block/Block;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockMetadata(III)I"))
	private static int etfuturum$waterloggedSurfaceMeta(World world, int x, int y, int z) {
		return WaterlogRuntime.isWaterlogged(world, x, y, z) ? 1 : world.getBlockMetadata(x, y, z);
	}
}
