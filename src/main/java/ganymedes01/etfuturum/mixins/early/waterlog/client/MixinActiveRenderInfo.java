package ganymedes01.etfuturum.mixins.early.waterlog.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.util.Vec3;
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
 * StairWaterRenderer 的渲染水面三点一致——眼位低于 8/9 时返回 water（雾 ✓）。
 *
 * <p><b>上部 1/9 无雾修复（2026-10-09 用户实证）</b>：眼位高于替身水面 8/9 时
 * vanilla 改查上方格（空气 → 无雾）——紧贴含水栅栏/玻璃板时眼位格会进入栅栏格
 * （玩家中心越界 ~0.075m），其上部 1/9 窄窗内无雾且随高度上下变化。26.2 的
 * 判定是"眼位格 fluid state 非空即水下"的<b>整格判定，无表面判线</b>——修复 =
 * 上方回退时若<b>眼位格</b>（y-1）含水则强制返回 water（26.2 parity：含水格内
 * 任意高度都有雾，雾先于视觉水面 1/9 格）。真水格（非含水）不拦截，vanilla
 * 表面判线照旧；两个注入点（主采样 + 上方回退）都在本 redirect 内。
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
	 * getBlockAtEntityViewpoint（rfg 全树实证），无外溢。
	 *
	 * <p>探针（WATERLOG_DEBUG）：FOG-EYE 行 = 眼位采样坐标；若日志中无 FOG 行
	 * 说明该链路未被执行（如 Angelica 绕过 EntityRenderer 雾管线）。
	 */
	private static int etfuturum$eyeProbe;

	@Redirect(method = "getBlockAtEntityViewpoint(Lnet/minecraft/world/World;Lnet/minecraft/entity/EntityLivingBase;F)Lnet/minecraft/block/Block;", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ActiveRenderInfo;projectViewFromEntity(Lnet/minecraft/entity/EntityLivingBase;D)Lnet/minecraft/util/Vec3;"))
	private static Vec3 etfuturum$sampleEyeCell(EntityLivingBase entity, double partialTicks) {
		double x = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
		double y = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks + entity.getEyeHeight();
		double z = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks;
		if (WaterlogRuntime.WATERLOG_DEBUG && ++etfuturum$eyeProbe % 40 == 0) {
			WaterlogRuntime.debug("FOG-EYE #" + etfuturum$eyeProbe + " eye=" + x + "," + y + "," + z + " view=" + entity.rotationPitch + "/" + entity.rotationYaw);
		}
		return Vec3.createVectorHelper(x, y, z);
	}

	/** 探针节流计数（仅 WATERLOG_DEBUG）。 */
	private static int etfuturum$cellProbe;

	@Redirect(method = "getBlockAtEntityViewpoint(Lnet/minecraft/world/World;Lnet/minecraft/entity/EntityLivingBase;F)Lnet/minecraft/block/Block;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlock(III)Lnet/minecraft/block/Block;"))
	private static Block etfuturum$waterloggedEyeCell(World world, int x, int y, int z) {
		Block block = world.getBlock(x, y, z);
		boolean wet = WaterlogRuntime.isWaterlogged(world, x, y, z);
		boolean wetBelow = WaterlogRuntime.isWaterlogged(world, x, y - 1, z);
		if (WaterlogRuntime.WATERLOG_DEBUG && ++etfuturum$cellProbe % 40 == 0) {
			WaterlogRuntime.debug("FOG-CELL #" + etfuturum$cellProbe + " cell=" + x + "," + y + "," + z + " block=" + block
					+ " wet=" + wet + " wetBelow=" + wetBelow + " -> " + (wet || wetBelow ? "WATER" : block.getMaterial()));
			etfuturum$dumpNeighborhood(world, x, y, z);
		}
		if (wet) {
			return Blocks.water;
		}
		// 上方回退（vanilla L96-100：眼位高于液体表面判线时改查上方格）：
		// 眼位格（y-1）含水 → 强制 water，保住 26.2 的整格判定
		if (wetBelow) {
			return Blocks.water;
		}
		return block;
	}

	/** 探针：眼位 3x3x3 邻域快照。W=水材质 L=含水家族 .=空气 B=其他。中格 = [1,1]。 */
	private static void etfuturum$dumpNeighborhood(World world, int cx, int cy, int cz) {
		for (int dy = 1; dy >= -1; --dy) {
			StringBuilder sb = new StringBuilder("FOG-3x3 y=" + (cy + dy) + ":");
			for (int dx = -1; dx <= 1; ++dx) {
				for (int dz = -1; dz <= 1; ++dz) {
					Block b = world.getBlock(cx + dx, cy + dy, cz + dz);
					char mark;
					if (dx == 0 && dz == 0 && dy == 0) {
						mark = '*';
					} else if (b.getMaterial() == Material.water) {
						mark = 'W';
					} else if (WaterlogRuntime.isWaterlogged(world, cx + dx, cy + dy, cz + dz)) {
						mark = 'L';
					} else if (b.getMaterial() == Material.air) {
						mark = '.';
					} else {
						mark = 'B';
					}
					sb.append(' ').append(mark);
				}
			}
			WaterlogRuntime.debug(sb.toString());
		}
	}

	@Redirect(method = "getBlockAtEntityViewpoint(Lnet/minecraft/world/World;Lnet/minecraft/entity/EntityLivingBase;F)Lnet/minecraft/block/Block;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockMetadata(III)I"))
	private static int etfuturum$waterloggedSurfaceMeta(World world, int x, int y, int z) {
		return WaterlogRuntime.isWaterlogged(world, x, y, z) ? 1 : world.getBlockMetadata(x, y, z);
	}

	/** 探针节流计数（仅 WATERLOG_DEBUG）。 */
	private static int etfuturum$fogEventProbe;

	/**
	 * 探针（WATERLOG_DEBUG）：setupFog 的 Forge FogDensity 事件——被取消时
	 * vanilla 水雾分支（L2004）整体跳过（Angelica shader 组件挂此事件）。
	 * FOG-EVENT cancelled=true 即雾被外部接管。
	 */
	@Redirect(method = "func_78468_a(IF)V", remap = false, at = @At(value = "INVOKE", target = "Lcpw/mods/fml/common/eventhandler/EventBus;post(Lcpw/mods/fml/common/eventhandler/Event;)Z", remap = false))
	private static boolean etfuturum$probeFogDensityEvent(cpw.mods.fml.common.eventhandler.EventBus bus, cpw.mods.fml.common.eventhandler.Event event) {
		boolean cancelled = bus.post(event);
		if (WaterlogRuntime.WATERLOG_DEBUG && event instanceof net.minecraftforge.client.event.EntityViewRenderEvent.FogDensity
				&& ++etfuturum$fogEventProbe % 40 == 0) {
			WaterlogRuntime.debug("FOG-EVENT #" + etfuturum$fogEventProbe + " cancelled=" + cancelled
					+ " density=" + ((net.minecraftforge.client.event.EntityViewRenderEvent.FogDensity) event).density);
		}
		return cancelled;
	}
}
