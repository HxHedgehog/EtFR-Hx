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
 * 那一刻箱体"转向"，2026-10-19 活板门 Bug B 同款）。调用点 & 7 剥离——
 * 位 4 起全部是扩展位：含水 bit16（waterlog 包）与双箱 TYPE/MARKER
 * 32/64/128（chestpair 包），switch 只需要朝向 2-5。注册于 waterlog 与
 * chestpair 两个特性的并集（见 EtFuturumEarlyMixins）；仅 waterlog 开启时
 * meta 恒 < 32，行为与 & 15 等价。
 */
@Mixin(TileEntityChestRenderer.class)
public abstract class MixinTileEntityChestRenderer {

	@Redirect(method = "renderTileEntityAt(Lnet/minecraft/tileentity/TileEntityChest;DDDF)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/tileentity/TileEntityChest;getBlockMetadata()I"))
	private int etfuturum$maskChestRenderMeta(TileEntityChest chest) {
		return chest.getBlockMetadata() & 7;
	}
}
