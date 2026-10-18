package ganymedes01.etfuturum.mixins.early.waterlog.client;

import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import net.minecraft.block.Block;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.S23PacketBlockChange;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 20261018-A 水闪烁修复的包侧半边（预测登记在 MixinItemBucket 桶路径 +
 * WaterlogRuntime.markWaterlogged，26.2 BlockStatePredictionHandler 的无 ack 轻量同构）：
 * 服务端旧写入 flush 的 S23/S22（回声，恰好等于客户端预测前状态）会在预测后 1-2 tick
 * 到达并无条件覆写预测，造成 DRY→WET→DRY 闪 2 tick。TCP 有序 ⇒ 回声必先于确认包，
 * 故对每个格做"未过期预测 且 到来状态==预测前状态 → 丢弃"的过滤；确认包与无关权威
 * 写入照常应用。S21（≥64 格全量权威）不过滤。
 *
 * <p>客户端专属：NetHandlerPlayClient 是 client 类，本 mixin 只在
 * {@code side == CLIENT && waterlogOn} 时注册（EtFuturumEarlyMixins）。
 */
@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerPlayClient {

	@Shadow
	private WorldClient clientWorldController;

	/**
	 * 单格方块更新（S23）：HEAD cancellable，整包只有一格，命中回声直接取消。
	 */
	@Inject(method = "handleBlockChange", at = @At("HEAD"), cancellable = true)
	private void etfuturum$filterStaleBlockChange(S23PacketBlockChange packet, CallbackInfo ci) {
		if (WaterlogRuntime.tryFilter(packet.func_148879_d(), packet.func_148878_e(), packet.func_148877_f(),
				Block.getIdFromBlock(packet.func_148880_c()), packet.func_148881_g(), this.clientWorldController.getTotalWorldTime())) {
			ci.cancel();
		}
	}

	/**
	 * 1-64 格批量更新（S22）：@Redirect 挂循环内唯一的应用调用
	 * {@code WorldClient.func_147492_c(IIILnet/minecraft/block/Block;I)Z}，
	 * 命中回声的格返回 true 伪装成功跳过该格，其余格照常。
	 */
	@Redirect(method = "handleMultiBlockChange",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/WorldClient;func_147492_c(IIILnet/minecraft/block/Block;I)Z"))
	private boolean etfuturum$filterStaleMultiBlockChange(WorldClient world, int x, int y, int z, Block block, int meta) {
		if (WaterlogRuntime.tryFilter(x, y, z, Block.getIdFromBlock(block), meta, world.getTotalWorldTime())) {
			return true;
		}
		return world.func_147492_c(x, y, z, block, meta);
	}
}
