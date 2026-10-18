package ganymedes01.etfuturum.mixins.early.waterlog.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**
 * 水下视野咽喉桥（1710 实证）：水下雾的密度（EntityRenderer.setupFog L2004）
 * 与颜色（EntityRenderer.updateFogColor L1819→L1829）都由
 * {@code ActiveRenderInfo.getBlockAtEntityViewpoint} 按眼位格方块的材质判定，
 * {@code Entity.isInsideOfMaterial} 不参与——{@link ganymedes01.etfuturum.mixins.early.waterlog.MixinEntity}
 * 只覆盖溺水/第一人称覆盖层/粒子链路，没有这个桥时含水楼梯里无水下视野。
 *
 * <p>含水格替身 = 无条件返回静态水，表面的高低取舍交还 vanilla 自带的上方
 * 回退——meta 替身（下方 redirect 返回 1）使 vanilla 的视点判线恰为
 * {@code y + 8/9}，与 StairWaterRenderer 的渲染水面一致：眼位低于 8/9 →
 * 返回替身水（雾 ✓）；高于 8/9 → vanilla 改查上方格，上方是空气则无雾、
 * 是真水（淹没含水块）则雾连续——<b>干缝自动闭合</b>。
 *
 * <p><b>干缝修复（2026-10-10 探针实证）</b>：早先"眼位高于 8/9 不替身"短路了
 * vanilla 回退（直接返回栅栏/玻璃板本体 → 非水材质 → 无雾），淹没含水块的
 * 8/9 判线与上方真水格底之间出现 1/9 格干缝，海中贴栅栏上下移动时 FOV/蒙版
 * 连续闪烁。修复 = 含水格无条件替身返回水，让 vanilla 判线/回退自己做取舍：
 * 海上回退命中真水（雾连续），干地上回退命中空气（正确无雾）。<b>含水块柱
 * 补丁（BUILD-D）</b>：回退命中"上方含水块"（柱叠）时同样返回水——柱内逐格
 * 8/9 判线之间的 1/9 干缝闭合，与蒙版链 wetAbove 一致（BUILD-C 探针实证：
 * 3 层含水玻璃板柱，眼位驻留 60.91 干缝 ret=Pane 无雾而蒙版开 → FOV 闪烁）。
 * 真水流水格（meta 1-7）用真水面判线修正 vanilla +1/9 泄漏，源水/跌水整格
 * 放行 vanilla（海面浮游不闪烁，见方法注释）。两个注入点（主采样 + 上方
 * 回退）都在本 redirect 内。
 *
 * <p><b>视线采样点修复（2026-10-10 用户实证"最新 jar 无效果"）</b>：真正的根因
 * 是 {@code projectViewFromEntity}——采样点 = <b>眼位 + 视线方向 × 1.0</b>
 * （rfg L79-81，objectX/Y/Z 为相机朝向向量），vanilla 判定的是"视线前方 1 格"
 * 的方块：贴含水栅栏看水时视线微调使采样点在含水格/后方真水/水面上方空气间
 * 跳动，雾随视角开关。26.2 = {@code camera.getBlockAtCamera()} 流体判定——
 * <b>相机格，与视线方向无关</b>。修复 = 重定向返回纯眼位（去掉视线偏移），
 * 采样格 = 眼位格；真水的 8/9 表面判线在眼位格上照 vanilla 保留。
 */
@Mixin(ActiveRenderInfo.class)
public abstract class MixinActiveRenderInfo {

	/**
	 * 采样点改为纯眼位（26.2 camera 格语义）：去掉沿视线 +1.0 的偏移，让
	 * getBlockAtEntityViewpoint 判定眼位格而非视线前方 1 格。唯一调用方即
	 * getBlockAtEntityViewpoint（rfg 全树实证），无外溢。眼位格坐标进
	 * ThreadLocal，供 getBlock 重定向精确区分主采样与上方回退两个调用点。
	 */
	private static final ThreadLocal<double[]> EYE_POS = new ThreadLocal<double[]>();

	@Redirect(method = "getBlockAtEntityViewpoint(Lnet/minecraft/world/World;Lnet/minecraft/entity/EntityLivingBase;F)Lnet/minecraft/block/Block;", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ActiveRenderInfo;projectViewFromEntity(Lnet/minecraft/entity/EntityLivingBase;D)Lnet/minecraft/util/Vec3;"))
	private static Vec3 etfuturum$sampleEyeCell(EntityLivingBase entity, double partialTicks) {
		double x = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
		double y = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks + entity.getEyeHeight();
		double z = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks;
		EYE_POS.set(new double[] {x, y, z});
		return Vec3.createVectorHelper(x, y, z);
	}

	/**
	 * 主采样按 26.2 相机格语义处理：含水眼位格 → 无条件替身返回水，高低取舍
	 * 交还 vanilla 表面门（meta 替身 = 8/9 判线）：眼位低于 8/9 → 返回替身水；
	 * 高于 8/9 → vanilla 改查上方格（空气 → 无雾；真水 → 雾连续，干缝闭合）。
	 * 流水眼位格（meta 1-7）→ 真水面判线修正 vanilla (9−meta)/9 判线的 +1/9
	 * 泄漏（"流动水末端蓝雾泄漏到水面上方"）。
	 *
	 * <p><b>源水（meta 0）/跌水（meta≥8）不干预</b>（2026-10-10 用户实证"海中
	 * 贴栅栏上下移动 FOV 连续闪烁"）：本方法同时喂 fog 色/密度与 FOV×60/70
	 * （EntityRenderer.getFOVModifier L496-501）——若以视觉水面 8/9 为界，海面
	 * 浮游的眼位围绕 8/9 波动，FOV/雾逐帧翻转；vanilla/26.2 整格判定的边界在
	 * 格顶（meta 0 判线 f2 = y+1），只在离水时快速跨越一次、无驻留 → 不闪烁，
	 * 且源水整格观感与原版/26.2 一致。
	 *
	 * <p><b>禁止无条件 y-1/y+1 检查</b>（2026-10-10 两轮用户实证）：y-1 在主
	 * 采样上命中下方含水格 → 水上出雾；y+1 在眼位处于上方空气格时命中下方
	 * 含水格 → FOV/雾闪。效果边界必须锁在 8/9 水面线/流水真水面上。
	 */
	@Redirect(method = "getBlockAtEntityViewpoint(Lnet/minecraft/world/World;Lnet/minecraft/entity/EntityLivingBase;F)Lnet/minecraft/block/Block;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlock(III)Lnet/minecraft/block/Block;"))
	private static Block etfuturum$waterloggedEyeCell(World world, int x, int y, int z) {
		Block block = world.getBlock(x, y, z);
		double[] eye = EYE_POS.get();
		if (eye != null) {
			int cx = MathHelper.floor_double(eye[0]);
			int cy = MathHelper.floor_double(eye[1]);
			int cz = MathHelper.floor_double(eye[2]);
			if (x == cx && z == cz && y == cy) {
				if (WaterlogRuntime.isWaterlogged(world, x, y, z)) {
					// 含水眼位格：无条件替身返回水，高低取舍交还 vanilla 表面门
					// （meta 替身 = 8/9 判线）——眼位高于判线时 vanilla 自己回退
					// 上方格：真水 → 雾连续（干缝闭合），空气 → 无雾（水面之上）
					return Blocks.water;
				} else if (block.getMaterial().isLiquid()) {
					// 流水 meta 1-7：真水面判线修正 vanilla (9−meta)/9 判线的 +1/9
					// 泄漏。源水/跌水放行 vanilla（整格边界=格顶，海面浮游不闪烁）。
					int meta = world.getBlockMetadata(x, y, z);
					if (meta >= 1 && meta < 8 && eye[1] >= y + (8 - meta) / 9.0D) {
						Block above = world.getBlock(x, y + 1, z);
						// 上方是含水块 → 返回水（静态部分水之上的含水干缝同理闭合，
						// 与蒙版链 wetAbove 一致）
						return WaterlogRuntime.isWaterlogged(world, x, y + 1, z) ? Blocks.water : above;
					}
				}
			} else if (x == cx && z == cz && y == cy + 1 && WaterlogRuntime.isWaterlogged(world, x, y, z)) {
				// 上方回退点（vanilla 表面门通过后改查眼位格 y+1）：命中含水块 →
				// 返回水——含水块柱逐格 8/9 判线之间的 1/9 干缝闭合（BUILD-C 探针
				// 实证：3 层含水玻璃板柱，眼位驻留 60.91 干缝 ret=Pane 无雾，而蒙版
				// 链 wetAbove 判水下 → 两链不一致 + FOV 随眼位跨缝闪烁）。空气/干块
				// → 原样返回（栅栏露出水面部分正确无雾）。
				return Blocks.water;
			}
		}
		return block;
	}

	@Redirect(method = "getBlockAtEntityViewpoint(Lnet/minecraft/world/World;Lnet/minecraft/entity/EntityLivingBase;F)Lnet/minecraft/block/Block;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockMetadata(III)I"))
	private static int etfuturum$waterloggedSurfaceMeta(World world, int x, int y, int z) {
		return WaterlogRuntime.isWaterlogged(world, x, y, z) ? 1 : world.getBlockMetadata(x, y, z);
	}
}
