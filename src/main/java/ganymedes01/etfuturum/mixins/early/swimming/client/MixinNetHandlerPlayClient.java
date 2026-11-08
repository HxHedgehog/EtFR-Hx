package ganymedes01.etfuturum.mixins.early.swimming.client;

import java.util.Iterator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.DataWatcher;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerPlayClient {

	@Shadow
	private net.minecraft.client.multiplayer.WorldClient clientWorldController;

	/**
	 * 26.2 never sends the local player their own entity metadata (the server only tracks
	 * a player for OTHER clients), so the vanilla sprint bit stays client-authoritative.
	 * 1.7.10 single player DOES echo the integrated server's whole flags byte 0 back onto
	 * the local player with a one to two tick C0B lag, and the two channels feed each
	 * other: the T5 sprint branch re-fires on the stomped bit while the lagged echo keeps
	 * clearing it (F9 captures: dw0 flip-flops 0x08/0x00 every tick with zero local
	 * setSprinting calls, motionY alternating -0.0139/-0.0111 in lockstep, sprint FOV
	 * flickering - a stutter that predates the swimming port). Every bit of byte 0 is
	 * client-computed for the local player in 1.7.10 (fire, sneaking, sprinting, the
	 * elytra flag), so dropping the self flags entry from self metadata matches the 26.2
	 * behaviour. Health and all other watchable objects still apply normally.
	 */
	@Inject(method = "handleEntityMetadata", at = @At("HEAD"), cancellable = true)
	private void etfu$filterSelfFlagsMetadata(S1CPacketEntityMetadata packetIn, CallbackInfo ci) {
		Entity entity = this.clientWorldController.getEntityByID(packetIn.func_149375_d());
		if (entity != Minecraft.getMinecraft().thePlayer) {
			return;
		}
		ci.cancel();

		List<DataWatcher.WatchableObject> list = packetIn.func_149376_c();
		if (list == null) {
			return;
		}
		Iterator<DataWatcher.WatchableObject> it = list.iterator();
		while (it.hasNext()) {
			if (it.next().getDataValueId() == 0) {
				it.remove();
			}
		}
		if (!list.isEmpty()) {
			entity.getDataWatcher().updateWatchedObjectsFromList(list);
		}
	}
}
