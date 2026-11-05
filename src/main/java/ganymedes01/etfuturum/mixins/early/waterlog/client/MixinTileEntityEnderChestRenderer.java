package ganymedes01.etfuturum.mixins.early.waterlog.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.renderer.tileentity.TileEntityEnderChestRenderer;
import net.minecraft.tileentity.TileEntityEnderChest;

/**
 * 末影箱 TESR 渲染掩码（同 MixinTileEntityChestRenderer 约定）：vanilla
 * TileEntityEnderChestRenderer.renderTileEntityAt 用裸 getBlockMetadata() 做
 * 朝向 switch（2-5），含水位 meta（18-21）无一命中 → 朝向回退默认值。
 * 调用点 & 15 剥离；门控关闭时 meta 恒 < 16 → vanilla 行为逐位一致。
 */
@Mixin(TileEntityEnderChestRenderer.class)
public abstract class MixinTileEntityEnderChestRenderer {

	@Redirect(method = "renderTileEntityAt(Lnet/minecraft/tileentity/TileEntityEnderChest;DDDF)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/tileentity/TileEntityEnderChest;getBlockMetadata()I"))
	private int etfuturum$maskEnderChestRenderMeta(TileEntityEnderChest chest) {
		return chest.getBlockMetadata() & 15;
	}
}
