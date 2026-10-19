package ganymedes01.etfuturum.mixins.early.waterlog.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.renderer.tileentity.TileEntityChestRenderer;
import net.minecraft.tileentity.TileEntityChest;

/**
 * 箱子 TESR 渲染掩码（同 renderBlockLadder 裸 meta 约定）：vanilla
 * TileEntityChestRenderer.renderTileEntityAt 用裸 getBlockMetadata() 做朝向
 * switch（2-5），含水位 meta（18-21）无一命中 → 箱子朝向回退默认值（倒水
 * 那一刻箱体"转向"，2026-10-19 活板门 Bug B 同款）。调用点 & 15 剥离。
 * 门控关闭时 meta 恒 < 16 → vanilla 行为逐位一致。
 */
@Mixin(TileEntityChestRenderer.class)
public abstract class MixinTileEntityChestRenderer {

	@Redirect(method = "renderTileEntityAt(Lnet/minecraft/tileentity/TileEntityChest;DDDF)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/tileentity/TileEntityChest;getBlockMetadata()I"))
	private int etfuturum$maskChestRenderMeta(TileEntityChest chest) {
		return chest.getBlockMetadata() & 15;
	}
}
