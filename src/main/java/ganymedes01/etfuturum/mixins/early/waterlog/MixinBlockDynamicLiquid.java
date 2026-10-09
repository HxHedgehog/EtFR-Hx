package ganymedes01.etfuturum.mixins.early.waterlog;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ganymedes01.etfuturum.waterlog.WaterlogFamilies;
import ganymedes01.etfuturum.waterlog.WaterlogFlow;
import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDynamicLiquid;
import net.minecraft.block.BlockLiquid;
import net.minecraft.world.World;

/**
 * 无限水成源自动含水（3c，项目必做项）：1.7.10 的水不会流入家族方块格
 * （{@code func_149809_q} 对 rock 材质为否），家族格因此永远错过 vanilla 的
 * 成源收敛。挂点 = {@code BlockDynamicLiquid.func_149813_h}（水流写入目标格
 * 的唯一咽喉，源格下流与横流都经过；SRG 名双端一致）。
 *
 * <p><b>海泡菜式延迟语义</b>：流尝试只调度 5gt 重查，不立即含水——家族方块
 * 自己的 updateTick（MixinBlockStairs）到点重查 {@link WaterlogFlow#wouldFormSource}，
 * 水源已移走则保持干。与 26.2 的 scheduleTick(getTickDelay) 和海泡菜环境补水
 * 的"延迟一个流体 tick 后重查"时序一致（用户实测立即含水偏快）。vanilla 本体
 * 对不可流入格本就无事可做，无需 cancel。
 *
 * <p><b>宿主完整面不喂养（Bug1）</b>：{@code func_149804_e} 的全部调用点都在
 * updateTick 作用域内（自身深度 L31 + 上方喂养 L58/60 + func_149810_a 水平喂养
 * L315），asking 格坐标用 ThreadLocal 桥接进 func_149810_a。含水宿主完整面朝向
 * 的采样返回 -1（不喂养）——贴在含水楼梯背面的流动水正常消散（26.2 getNewLiquid
 * canPassThroughWall parity）。
 *
 * <p><b>下行吸附（Bug2）</b>：下行目标为可含水家族方块时 {@code func_149809_q}
 * 放行（26.2 canHoldFluid 对 LiquidBlockContainer 恒真），流体流入含水格而非像
 * 撞墙一样横向流开；func_149813_h 的内部守卫（不在重定向作用域）仍阻断真实写入，
 * 含水位由其 HEAD 调度（干格延迟成源 / 已含水无操作）。
 */
@Mixin(BlockDynamicLiquid.class)
public abstract class MixinBlockDynamicLiquid {

	@Inject(method = "func_149813_h(Lnet/minecraft/world/World;IIII)V", at = @At("HEAD"))
	private void etfuturum$scheduleFamilySourceFormation(World world, int x, int y, int z, int meta, CallbackInfo ci) {
		Block block = world.getBlock(x, y, z);
		// wet 早退：已含水格无需成源调度（蔓延重调度由 onNeighborBlockChange
		// 负责）——否则邻水的每次流动尝试都会空转调度，SCHEDULE 刷屏
		if (WaterlogFamilies.isWaterloggable(block) && !WaterlogRuntime.isWaterlogged(world, x, y, z)) {
			WaterlogFlow.scheduleSpread(world, x, y, z, block);
		}
	}

	/**
	 * updateTick 的采样宿主格（func_149810_a 内部无调用方坐标，ThreadLocal 桥接；
	 * func_149810_a 为 private 且仅被 updateTick 调用，作用域闭环）。
	 */
	private static final ThreadLocal<int[]> ASKING_CELL = new ThreadLocal<int[]>();

	@Inject(method = "updateTick", at = @At("HEAD"))
	private void etfuturum$captureAskingCell(World world, int x, int y, int z, Random random, CallbackInfo ci) {
		ASKING_CELL.set(new int[] {x, y, z});
	}

	@Inject(method = "updateTick", at = @At("RETURN"))
	private void etfuturum$releaseAskingCell(CallbackInfo ci) {
		ASKING_CELL.remove();
	}

	/** 采样格 (sx,sy,sz) 朝向 asking 格的侧面号（共享面）；非相邻返回 -1。 */
	private static int etfuturum$sideTowardAsking(int[] asking, int sx, int sy, int sz) {
		int dx = sx - asking[0];
		int dy = sy - asking[1];
		int dz = sz - asking[2];
		if (dx == 1) {
			return 4; // 采样格在 asking 东侧 → 其西面朝 asking
		}
		if (dx == -1) {
			return 5;
		}
		if (dz == 1) {
			return 2; // 南 → 北面
		}
		if (dz == -1) {
			return 3;
		}
		if (dy == 1) {
			return 0; // 上方采样 → 其底面朝 asking
		}
		return -1;
	}

	private int etfuturum$depthUnlessWalled(BlockLiquid liquid, World world, int x, int y, int z) {
		int[] asking = ASKING_CELL.get();
		if (asking != null && WaterlogRuntime.isWaterlogged(world, x, y, z)) {
			int side = etfuturum$sideTowardAsking(asking, x, y, z);
			if (side >= 0 && WaterlogFlow.isCellFaceComplete(world, x, y, z, side)) {
				return -1; // 宿主完整面朝向 asking 侧：不喂养
			}
		}
		return ((MixinBlockLiquid) (Object) liquid).etfuturum$liquidDepth(world, x, y, z);
	}

	@Redirect(method = "updateTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockDynamicLiquid;func_149804_e(Lnet/minecraft/world/World;III)I"))
	private int etfuturum$faceAwareOwnDepth(BlockDynamicLiquid liquid, World world, int x, int y, int z) {
		return etfuturum$depthUnlessWalled(liquid, world, x, y, z);
	}

	@Redirect(method = "func_149810_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockDynamicLiquid;func_149804_e(Lnet/minecraft/world/World;III)I"))
	private int etfuturum$faceAwareNeighborDepth(BlockDynamicLiquid liquid, World world, int x, int y, int z) {
		return etfuturum$depthUnlessWalled(liquid, world, x, y, z);
	}

	@Redirect(method = "updateTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockDynamicLiquid;func_149809_q(Lnet/minecraft/world/World;III)Z"))
	private boolean etfuturum$absorbIntoWaterlogged(BlockDynamicLiquid liquid, World world, int x, int y, int z) {
		// 26.2 isWaterHole：仅下方格"已是同类流体"（含水格虚拟源，L286 分支）时吸收下行；
		// 干的可含水格对下行流水是"固体"——canPlaceLiquid 只认源流体（L31 type ==
		// Fluids.WATER，spread 传的 flowing 不入）→ 照 vanilla 横向流开（用户实证 262）
		if (WaterlogRuntime.isWaterlogged(world, x, y, z)) {
			return true;
		}
		return ((ganymedes01.etfuturum.mixins.early.oceanplants.MixinBlockDynamicLiquid) (Object) liquid).etfuturum$canFlowInto(world, x, y, z);
	}
}
