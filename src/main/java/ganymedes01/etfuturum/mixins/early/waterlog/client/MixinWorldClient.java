package ganymedes01.etfuturum.mixins.early.waterlog.client;

import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import net.minecraft.block.Block;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 20261020-P 预测窗口收敛驱动（26.2 endPredictionsUpTo 同构的定时近似）。
 * <p>
 * 客户端预测登记后，窗口内的 S23/S22 由 {@code MixinNetHandlerPlayClient} 吞下并
 * 只更新 pending（见 {@link WaterlogRuntime#tryFilter}）；本 mixin 在每个客户端 tick
 * 取出收敛期已到的格，用与 vanilla S22/S23 相同的底层写入
 * {@code WorldClient.func_147492_c}（invalidateRegionAndSetBlock：setBlock + 区域
 * 重绘）应用最后到达的权威状态。无 ack 包的 1.7.10 用"首包后 2 tick"近似 ack 时机，
 * 与 ping 无关；TTL 过期的登记由 pollSettledPredictions 静默清除，回归原版行为。
 * <p>
 * 客户端专属：WorldClient 是 client 类，本 mixin 只在 {@code side == CLIENT &&
 * waterlogOn} 时注册（EtFuturumEarlyMixins）。功能关闭时 pollSettledPredictions
 * 返回空数组，tick 注入零开销短路。
 */
@Mixin(WorldClient.class)
public abstract class MixinWorldClient {

	@Inject(method = "tick", at = @At("HEAD"))
	private void etfuturum$settlePredictions(CallbackInfo ci) {
		final int[][] settled = WaterlogRuntime.pollSettledPredictions(((World) (Object) this).getTotalWorldTime());
		for (final int[] cell : settled) {
			((WorldClient) (Object) this).func_147492_c(cell[0], cell[1], cell[2], Block.getBlockById(cell[3]), cell[4]);
		}
	}
}
