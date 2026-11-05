package ganymedes01.etfuturum.mixins.early.waterlog;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/**
 * 呼吸/溺亡/水下波纹蒙版/水中减速的单一咽喉桥接（plan §3.4 #1）——
 * {@code Entity.isInsideOfMaterial(Material.water)} 的消费方：蒙版
 * （ItemRenderer L610 → renderWarpedTextureOverlay）、溺水（EntityLivingBase.
 * onEntityUpdate）、氧气条（GuiIngame L811）、三人称相机收拢（EntityRenderer
 * L1309/L1325）。FOV×60/70 与雾色/雾密度走的是 ActiveRenderInfo 链，见
 * client.MixinActiveRenderInfo，两条链由本类与彼类分别对齐。
 *
 * <p>含水眼位格：8/9 眼位门（26.2 观感，学海洋植物液面语义——效果边界与
 * 渲染水面 8/9 对齐，头部越过水面线才有效果，消除格边界的 FOV 闪烁）。
 *
 * <p>vanilla 流水整格蒙版泄天修正（2026-10-10 用户实证"流动水格未浸入蒙版
 * 仍在"）：Forge 补丁后的 isInsideOfMaterial 不再调用 getLiquidHeightPercent，
 * 而是 IFluidBlock.getFilledPercentage（原版 BlockLiquid 非 IFluidBlock →
 * filled 恒 1.0 = 整格判定）→ 流水 meta 1-7 的格子上部（真水面上方）蒙版/
 * 溺水泄天。方法内无 getLiquidHeightPercent 调用点，Redirect 方案不存在
 * （required 配置下打不中 = 启动崩），只能在本 HEAD 注入对流水 meta 1-7
 * 显式关门：眼位高于真水面 (8−meta)/9 直接 false，低于则 vanilla 自然 true。
 * 源水/跌水/沉降窗视觉满格不干预——海面浮游时眼位围绕视觉水面 8/9 波动，
 * 整格边界（格顶）只在离水时快速跨越一次，无驻留 → 不闪烁。
 *
 * <p>海带等海洋植物无需此桥接（BlockAquaticPlant 本身就是
 * Material.water，走 vanilla 链路）。
 */
@Mixin(Entity.class)
public abstract class MixinEntity {

	@Shadow
	public World worldObj;
	@Shadow
	public double posX;
	@Shadow
	public double posY;
	@Shadow
	public double posZ;
	@Shadow
	public double motionY; // 供 MixinEntityLivingBase 水下攀爬使用（Entity 声明，跨 mixin cast 访问）
	@Shadow
	public boolean isCollidedHorizontally; // 同上

	@Shadow
	public abstract float getEyeHeight();

	@Inject(method = "isInsideOfMaterial(Lnet/minecraft/block/material/Material;)Z", at = @At("HEAD"), cancellable = true)
	private void etfuturum$waterloggedEye(Material materialIn, CallbackInfoReturnable<Boolean> cir) {
		if (materialIn != Material.water || !WaterlogRuntime.isEnabled()) {
			return;
		}
		double d0 = this.posY + (double) this.getEyeHeight();
		int i = MathHelper.floor_double(this.posX);
		int j = MathHelper.floor_double(d0);
		int k = MathHelper.floor_double(this.posZ);
		// 含水眼位格：8/9 眼位门 + 浸没旁路——眼位低于水面线，或上方格仍处于
		// 水体（液体/含水块，块体没入水中），整格算水下。旁路消除"淹没含水块
		// 顶 1/9 干缝"（2026-10-10 探针实证：海面下含水玻璃板格 y=61 的 61.888~
		// 62.0 干缝夹在上方源水整格之间，眼位上下移动即连续翻转蒙版/FOV）。
		// 露头块（上方为空气）保持 8/9 门——效果边界=视觉水面。
		if (WaterlogRuntime.isWaterlogged(this.worldObj, i, j, k)) {
			if (d0 < (double) j + 8.0D / 9.0D || etfuturum$isWetAbove(this.worldObj, i, j, k)) {
				cir.setReturnValue(Boolean.TRUE);
			}
			return;
		}
		// 部分水格（静置 BlockStaticLiquid 与流水 BlockDynamicLiquid，meta 1-7）
		// 整格蒙版泄天修正：Forge 补丁后本方法用 IFluidBlock.getFilledPercentage
		// 判液体高度，原版 BlockLiquid 非 IFluidBlock → filled 恒 1.0（整格判定）
		// → 真水面 (8−meta)/9 之上蒙版/溺水泄天（2026-10-10 探针实证：
		// BlockStaticLiquid/4 格眼位高于 4/9 仍 ret=true——水传播沉降留下的是
		// 静置部分水，只查 flowing_water 类接不住）。眼位高于真水面且上方无水
		// → false；否则 vanilla 自然判定。源水/跌水/沉降窗视觉满格，不干预。
		Block block = this.worldObj.getBlock(i, j, k);
		if (block.getMaterial() == Material.water) {
			int meta = this.worldObj.getBlockMetadata(i, j, k);
			if (meta >= 1 && meta < 8 && d0 >= (double) j + (8 - meta) / 9.0D
					&& !etfuturum$isWetAbove(this.worldObj, i, j, k)) {
				cir.setReturnValue(Boolean.FALSE);
			}
		}
	}

	/** 上方格是否仍处水体（液体或含水块）——淹没含水块/水柱内不设表面门。 */
	private static boolean etfuturum$isWetAbove(World world, int x, int y, int z) {
		Block above = world.getBlock(x, y + 1, z);
		return above.getMaterial().isLiquid() || WaterlogRuntime.isWaterlogged(world, x, y + 1, z);
	}
}
