package ganymedes01.etfuturum.mixins.early.waterlog.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import ganymedes01.etfuturum.waterlog.WaterlogFamilies;
import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import ganymedes01.etfuturum.waterlog.client.WaterlogPaint;
import net.minecraft.block.Block;

/**
 * celeritas（Angelica 接管 embeddium 区块构建）管线的 pass-1 门控与 pass
 * 捕获。部署 26.09.06 jar 实证：{@code execute} 对每个非空气方块逐 pass 调
 * {@code renderBlock}，renderBlock 内部 {@code startDrawingQuads →
 * renderBlockByRenderType（func_147805_b）→ copyRawBuffer} 全量走通（无
 * vanilla renderType 拦截），EtFR 的 MixinRenderBlocks 画水挂点两条管线通用。
 *
 * <p>本类两个注入都只用 EtFR classpath 内类型——unimixins 0.3.1 运行时对
 * @Inject handler 的 @Coerce Object 不放宽（Invalid descriptor → 目标类定义
 * 失败 → NoClassDefFoundError，2026-10-05 实证），跨 mod 目标的 handler 签名
 * 严禁出现编不进来的具体类型：
 * <ul>
 * <li>{@code execute} 里 {@code canRenderInPass}（Forge 添加方法，运行时保持
 * MCP 名）为家族方块打开 pass-1 槽位——否则楼梯 pass 1 根本不进 renderBlock；
 * 不需要格坐标（含水判定在画水挂点做）。</li>
 * <li>{@code renderBlock} 里紧随 {@code setRenderPass} 的 side-effect 调用
 * {@code canRenderInPass}（同样 Forge 方法）被纯捕获重定向：把当前 pass 记入
 * ThreadLocal，行为不变。画水挂点（MixinRenderBlocks，自有 blockAccess 与
 * x/y/z）据 pass==1 分支画水或抑制干家族格的半透明重复几何。</li>
 * </ul>
 * 子类（主线程/线程化任务）都委托 super.execute 且不覆写 renderBlock，基类
 * 注入全覆盖；Threaded 的 canRenderOffThread 下楼梯直连、延迟路径同线程相邻
 * 捕获/消费，ThreadLocal 天然隔离并发构建线程。shader pack 对家族方块的
 * per-block 层覆写走 override 分支绕过 canRenderInPass，该路径暂不含水（罕见
 * 配置，接受）。
 */
@Mixin(targets = "com.gtnewhorizons.angelica.rendering.celeritas.AngelicaChunkBuilderMeshingTask", remap = false)
public abstract class MixinAngelicaChunkBuilderMeshingTask {

	@Redirect(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;canRenderInPass(I)Z", remap = false))
	private boolean etfuturum$waterloggedBlocksGetPassOneSlot(Block block, int pass) {
		if (pass == 1 && WaterlogRuntime.isEnabled() && WaterlogFamilies.isWaterloggable(block)) {
			return true; // 打开槽位；含水判定在画水挂点做（画水或抑制空几何）
		}
		return block.canRenderInPass(pass);
	}

	@Redirect(method = "renderBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;canRenderInPass(I)Z", remap = false))
	private boolean etfuturum$captureRenderBlockPass(Block block, int pass) {
		WaterlogPaint.captureCeleritasPass(pass);
		return block.canRenderInPass(pass); // 行为不变，纯捕获
	}
}
