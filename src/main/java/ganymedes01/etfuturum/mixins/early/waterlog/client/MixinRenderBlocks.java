package ganymedes01.etfuturum.mixins.early.waterlog.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ganymedes01.etfuturum.waterlog.WaterlogFamilies;
import ganymedes01.etfuturum.waterlog.WaterlogFlow;
import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import ganymedes01.etfuturum.waterlog.client.StairWaterRenderer;
import ganymedes01.etfuturum.waterlog.client.WaterlogPaint;
import net.minecraft.block.Block;
import net.minecraft.block.BlockWall;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.world.IBlockAccess;

/**
 * 两条管线（vanilla WorldRenderer / Angelica celeritas）共用的客户端挂点：
 * <ol>
 * <li><b>画水挂点</b>：pass-1 门控对含水家族格置位（vanilla 管线 =
 * MixinWorldRenderer 的 ThreadLocal 标志；celeritas 管线 =
 * MixinAngelicaChunkBuilderMeshingTask 的 pass 捕获）后，在
 * renderBlockByRenderType HEAD 画整格静水替代 pass-1 的几何拷贝（本体已在
 * pass 0 画过）。两条管线的置位互斥（plugin 按 angelicaLoaded 二选一注册），
 * 干家族格 pass-1 一律取消——func_147805_b 会把半透明材质的重复几何画进
 * pass 1，必须拦下。</li>
 * <li><b>角高挂点</b>（桥接清单 #6）：{@code RenderBlocks#getLiquidHeight}
 * （vanilla L4343-4381）的角高平均把实心格排除，含水格（rock 材质）会被
 * 真水当成实心 → 周边真水表面对着含水格鼓包/缺口。重定向为成员感知版：
 * 涉及含水格时改走 StairWaterRenderer 的成员感知角高（含水格按 meta 0
 * 源水计权），与含水格内部水体的表面完全一致；不涉及含水格时保持 vanilla
 * 结果逐位一致（ocean plants 的 meta 0 强制不在此扩散，最小化行为面）。</li>
 * <li><b>pass-1 原住民例外</b>（染色玻璃板 getRenderBlockPass()==1）：pass-1
 * 的 renderBlockByRenderType 调用画的是本体真身而非重复拷贝——含水格先画水
 * 再放行 vanilla 本体（不取消），干格整体放行；否则板体被当副本抑制/替换 →
 * 隐身或只剩水（2026-10-07 玻璃板扩族实测）。判断标准 = 该 renderType 的
 * 本体画在哪个 pass，而非"家族格一律抑制"。</li>
 * </ol>
 */
@Mixin(RenderBlocks.class)
public abstract class MixinRenderBlocks {

	@Shadow
	public IBlockAccess blockAccess;

	@Inject(method = "renderBlockByRenderType", at = @At("HEAD"), cancellable = true)
	private void etfuturum$paintWaterInsteadOfBlockCopy(Block block, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
		// 探针（20261012-W）：墙的客户端渲染取样——rawMeta 是判定核心：
		// 服务端 MARK 了 16+ 而这里 rawMeta < 16 = S21 网络层丢 bit16（NEID）；
		// rawMeta 有 bit16 而 flag/cel 全 false = pass-1 门控没给墙开槽
		if (block instanceof BlockWall && WaterlogFlow.probeBudget("RENDER-WALL", 40)) {
			WaterlogRuntime.debug("RENDER-WALL " + x + "," + y + "," + z
					+ " cls=" + block.getClass().getSimpleName()
					+ " rawMeta=" + this.blockAccess.getBlockMetadata(x, y, z)
					+ " family=" + WaterlogFamilies.isWaterloggable(block)
					+ " wet=" + WaterlogRuntime.isWaterlogged(this.blockAccess, x, y, z)
					+ " flag=" + WaterlogPaint.isFlagged() + " cel=" + WaterlogPaint.isCeleritasPassOne());
		}
		boolean flagged = WaterlogPaint.isFlagged(); // vanilla 管线：MixinWorldRenderer 门控置位
		boolean celeritasPassOne = WaterlogPaint.isCeleritasPassOne(); // celeritas 管线：pass 捕获
		if (!flagged && !celeritasPassOne) {
			return;
		}
		boolean waterlogged = WaterlogRuntime.isWaterlogged(this.blockAccess, x, y, z);
		if (flagged) {
			WaterlogPaint.clearFlag();
		}
		// pass-1 原住民（染色玻璃板，BlockStainedGlassPane.getRenderBlockPass()==1）：
		// pass-1 的本次调用画的是本体真身，不是 pass-0 本体的重复拷贝——含水 =
		// 先画水再放行 vanilla 画本体（水与板同进 pass-1 缓冲，26.2 里即 translucent
		// 流体层与板模型分层合成）；干格 = 整体放行照 vanilla 渲染。此前把真身当
		// 副本替换/抑制：含水染色板只剩水没有板（"自带水+隐身"）、干染色板在
		// celeritas 下完全隐身（2026-10-07 玻璃板扩族实测）。
		if (block.getRenderBlockPass() == 1) {
			if (waterlogged) {
				StairWaterRenderer.renderStillWaterVolume(this.blockAccess, x, y, z);
				WaterlogRuntime.debug("PAINT+BODY " + x + "," + y + "," + z + " block=" + block);
			}
			return;
		}
		if (flagged) {
			if (waterlogged) {
				boolean emitted = StairWaterRenderer.renderStillWaterVolume(this.blockAccess, x, y, z);
				WaterlogRuntime.debug("PAINT " + x + "," + y + "," + z + " block=" + block + " -> emitted=" + emitted);
				cir.setReturnValue(emitted);
			} else {
				WaterlogRuntime.debug("SUPPRESS-DRY(vanilla) " + x + "," + y + "," + z + " block=" + block);
				cir.setReturnValue(false); // 防御：置位但格已变干 → 仍取消第二份几何
			}
			return;
		}
		// celeritas pass 1：只处理家族方块——真水/冰/染色玻璃等 pass-1 常客
		// 必须照 vanilla 放行。含水格画水替代重复几何；干家族格抑制（否则
		// func_147805_b 把半透明材质的楼梯几何重复画进 pass 1）
		if (!WaterlogFamilies.isWaterloggable(block)) {
			return;
		}
		if (waterlogged) {
			boolean emitted = StairWaterRenderer.renderStillWaterVolume(this.blockAccess, x, y, z);
			WaterlogRuntime.debug("PAINT " + x + "," + y + "," + z + " block=" + block + " -> emitted=" + emitted);
			cir.setReturnValue(emitted);
		} else {
			// 干格抑制探针：连放"干块闪"若来自渲染层（构建快照过期/竞态），
			// 此行会在实际含水的格上出现——与同格 PAINT 行的时序对照定性
			WaterlogRuntime.debug("SUPPRESS-DRY " + x + "," + y + "," + z + " block=" + block);
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "getLiquidHeight(IIILnet/minecraft/block/material/Material;)F", at = @At("HEAD"), cancellable = true)
	private void etfuturum$memberAwareLiquidHeight(int x, int y, int z, Material material, CallbackInfoReturnable<Float> cir) {
		if (material != Material.water || !WaterlogRuntime.isEnabled()) {
			return; // 岩浆不与含水交互；门控关闭零改动
		}
		for (int i1 = 0; i1 < 4; ++i1) {
			int j1 = x - (i1 & 1);
			int k1 = z - (i1 >> 1 & 1);
			if (WaterlogRuntime.isWaterlogged(this.blockAccess, j1, y, k1) || WaterlogRuntime.isWaterlogged(this.blockAccess, j1, y + 1, k1)) {
				cir.setReturnValue((float) StairWaterRenderer.getLiquidHeight(this.blockAccess, x, y, z));
				return;
			}
		}
	}

	/**
	 * 梯子渲染的裸 meta 开关剥离含水位：renderBlockLadder（rfg L2719-2748）按
	 * {@code l == 2/3/4/5} 画面片，含水位 meta（0x12+）无一命中 → 水中梯子
	 * 完全不画（2026-10-10 用户实证）。楼梯/半砖的渲染 meta 消费天然掩码，
	 * 只有梯子需要。
	 */
	@Redirect(method = "renderBlockLadder", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/IBlockAccess;getBlockMetadata(III)I"))
	private int etfuturum$maskLadderRenderMeta(net.minecraft.world.IBlockAccess world, int x, int y, int z) {
		return world.getBlockMetadata(x, y, z) & ~WaterlogRuntime.WATERLOGGED_BIT;
	}

	/**
	 * 墙的"上方开放"判定含水对齐（26.2 WallBlock.shouldRaisePost → isCovered）：
	 * vanilla renderBlockWall（rfg L6595）的低直墙分支要求上方格是空气
	 * （{@code flag6 = isAirBlock}），含水墙上方是水体 → 直墙线整体走"柱+臂"
	 * 分支，水下墙线视觉上全是竖桩（2026-10-12 用户实证）。26.2 的升柱判定 =
	 * 上方块碰撞形状覆盖柱测试形状（TEST_SHAPE_POST）——水体无碰撞形状 →
	 * 不覆盖 → 不升柱（与空气同权，低直墙）；上方是实心方块/含水墙（自身碰撞
	 * 覆盖柱列）/含水栅栏 → 维持升柱（1.7.10 现行为已一致）。拐角升柱由
	 * hasCorner 驱动，两侧一致不受影响。修复 = renderBlockWall 的 isAirBlock
	 * 调用点对水体格（静态/流水任意 meta）返回 true；enableWaterlogging 关闭时
	 * 保留原版行为（ vanilla 瀑布下的墙维持柱剖面）。
	 */
	@Redirect(method = "renderBlockWall", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/IBlockAccess;isAirBlock(III)Z"))
	private boolean etfuturum$waterAboveKeepsLowProfile(net.minecraft.world.IBlockAccess world, int x, int y, int z) {
		if (WaterlogRuntime.isEnabled() && WaterlogRuntime.isWaterCell(world, x, y, z)) {
			return true;
		}
		return world.isAirBlock(x, y, z);
	}
}
