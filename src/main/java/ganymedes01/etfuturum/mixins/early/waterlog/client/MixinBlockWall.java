package ganymedes01.etfuturum.mixins.early.waterlog.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.BlockWall;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;

/**
 * 墙的客户端贴图含水位剥离：vanilla {@link BlockWall#getIcon(int, int)} 用裸
 * {@code meta == 1} 区分 normal/mossy（含水苔石墙 meta 0x11 不命中 → 贴成
 * 普通圆石）。该方法带 {@code @SideOnly(CLIENT)}——专用服务器上被
 * SideTransformer 剥离成员，common mixin 注入会 apply 失败 → 放 client 包
 * 仅客户端注册。条件命中（bit16 置位）时按剥位 meta 等价复刻 vanilla 体，
 * 干墙原体放行；EtFR BaseWall 子类的取模剥位在源码内完成，不走本注入。
 */
@Mixin(BlockWall.class)
public abstract class MixinBlockWall {

	@Inject(method = "getIcon(II)Lnet/minecraft/util/IIcon;", at = @At("HEAD"), cancellable = true)
	public void etfuturum$waterlogStripIconMeta(int side, int meta, CallbackInfoReturnable<IIcon> cir) {
		if ((meta & 0x10) != 0) {
			cir.setReturnValue((meta & 15) == 1 ? Blocks.mossy_cobblestone.getBlockTextureFromSide(side)
					: Blocks.cobblestone.getBlockTextureFromSide(side));
		}
	}
}
