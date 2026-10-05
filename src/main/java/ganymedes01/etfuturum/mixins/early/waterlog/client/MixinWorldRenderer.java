package ganymedes01.etfuturum.mixins.early.waterlog.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import ganymedes01.etfuturum.waterlog.client.WaterlogPaint;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.IBlockAccess;

/**
 * vanilla 区块构建器的 pass-1 门控（镜像上代伪含水架构）：家族方块本体
 * 照 vanilla 在 pass 0 渲染；内部水体是半透明体积，vanilla 的真水
 * （BlockLiquid）渲染在 pass 1，所以画水必须也进 pass 1，与周边真水一起
 * 排序、接受光影的水处理。
 *
 * <p>门控要做的是"让 pass-1 画水发生"：{@code getRenderBlockPass} 对含水格
 * 报 1，使 {@code k3 > pass} 检查在 pass-1 循环里保住该格；{@code canRenderInPass}
 * 打开该格的 pass-1 槽位并置 {@link WaterlogPaint#paintNextCall}，随后
 * renderBlockByRenderType（MixinRenderBlocks）画水替代第二份几何拷贝。
 * 每格门控判定 O(1)（bit16 测试），无需 memo。追踪格由同迭代里先于两次
 * 门控调用的 getBlock 捕获；区块构建在渲染线程，无同步问题。
 *
 * <p>仅在 Angelica 缺席时注册：celeritas 接管区块构建路径，Angelica 管线的
 * pass 处理在后续阶段实现。
 */
@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer {

	@Unique
	private static IBlockAccess etfuturum$trackedAccess;
	@Unique
	private static int etfuturum$trackedX;
	@Unique
	private static int etfuturum$trackedY;
	@Unique
	private static int etfuturum$trackedZ;

	@Redirect(method = "updateRenderer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/ChunkCache;getBlock(III)Lnet/minecraft/block/Block;"))
	private Block etfuturum$trackCell(ChunkCache cache, int x, int y, int z) {
		Block block = cache.getBlock(x, y, z);
		etfuturum$trackedAccess = cache;
		etfuturum$trackedX = x;
		etfuturum$trackedY = y;
		etfuturum$trackedZ = z;
		return block;
	}

	@Unique
	private static boolean etfuturum$isWaterlogged() {
		return etfuturum$trackedAccess != null && WaterlogRuntime.isWaterlogged(etfuturum$trackedAccess, etfuturum$trackedX, etfuturum$trackedY, etfuturum$trackedZ);
	}

	@Redirect(method = "updateRenderer", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getRenderBlockPass()I"))
	private int etfuturum$waterloggedBlocksKeepPassOneLoop(Block block) {
		if (etfuturum$isWaterlogged()) {
			return 1; // 驱动 k3 > pass 检查，让 pass-1 循环为画水保住该格
		}
		return block.getRenderBlockPass();
	}

	@Redirect(method = "updateRenderer", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;canRenderInPass(I)Z", remap = false))
	private boolean etfuturum$waterloggedBlocksFlagPassOneSlot(Block block, int pass) {
		WaterlogPaint.paintNextCall = false;
		if (pass == 1 && etfuturum$isWaterlogged()) {
			WaterlogPaint.paintNextCall = true;
			WaterlogRuntime.debug("GATE " + etfuturum$trackedX + "," + etfuturum$trackedY + "," + etfuturum$trackedZ + " pass-1 slot opened");
			return true; // 打开槽位：renderBlockByRenderType 将画水替代几何拷贝
		}
		return block.canRenderInPass(pass);
	}
}
