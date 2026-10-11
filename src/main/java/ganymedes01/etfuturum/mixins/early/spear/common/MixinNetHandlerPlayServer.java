package ganymedes01.etfuturum.mixins.early.spear.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ganymedes01.etfuturum.core.utils.SpearChargeTicker;
import ganymedes01.etfuturum.items.ItemSpear;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.client.C02PacketUseEntity;

/**
 * 26.2 ServerGamePacketListenerImpl L1241-1253 re-tests the attack gate when the stab action
 * arrives, with tolerance 5 ticks for latency (cannotAttackWithItem(itemInHand, 5)) - the
 * optimistic strength may borrow up to 5 ticks before the server rejects. The 1.7.10 attack
 * packet is C02 with action ATTACK; the swing packet (C0A -> processAnimation -> swingItem)
 * performs the server-side reset via MixinEntityLivingBase, like 26.2's ServerPlayer.swing
 * override resetting the ticker.
 */
@Mixin(NetHandlerPlayServer.class)
public abstract class MixinNetHandlerPlayServer {

	@Shadow
	public EntityPlayerMP playerEntity;

	@Inject(method = "processUseEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayerMP;attackTargetEntityWithCurrentItem(Lnet/minecraft/entity/Entity;)V"), cancellable = true, require = 1)
	private void etfu$spearAttackGate(C02PacketUseEntity packet, CallbackInfo ci) {
		ItemStack held = this.playerEntity.getCurrentEquippedItem();
		if (held != null && held.getItem() instanceof ItemSpear) {
			ItemSpear spear = (ItemSpear) held.getItem();
			if (((SpearChargeTicker) this.playerEntity).etfu$spearAttackCharge(5, spear.getAttackDurationTicks()) < 1.0F) {
				ci.cancel(); // 26.2: optimistic latency tolerance of 5 ticks, then reject
			}
		}
	}
}
