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
 * WaterlogRuntime.markWaterlogged，26.2 BlockStatePredictionHandler 的无 ack 轻量同构）。
 * <p>
 * 20261020-P 重写为 26.2 窗口收敛语义：预测窗口内到达的 S23/S22 一律延迟应用（吞下、
 * 只更新 pending），窗口末由 MixinWorldClient.tick 应用最后到达的权威状态——回声与
 * 确认在窗口内不可区分也无须区分（内容比对版会在"服务端把格子清回预测前状态"时误吞
 * 权威写入：海绵吸水残水；不登记空气格则回声直接应用：空地倒水闪）。见
 * WaterlogRuntime.tryFilter / pollSettledPredictions 的完整语义注释。
 * <p>
 * 客户端专属：NetHandlerPlayClient 是 client 类，本 mixin 只在
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
