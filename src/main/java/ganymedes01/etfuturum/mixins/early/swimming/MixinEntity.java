package ganymedes01.etfuturum.mixins.early.swimming;

import ganymedes01.etfuturum.swimming.SwimmingRuntime;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class MixinEntity {

	/**
	 * 26.2 Entity#baseTick calls updateSwimming() after the fluid interaction update.
	 * The server is authoritative for all players. Remote players on the client never run
	 * entity physics in 1.7.10, so they simply keep the synced data watcher flag.
	 */
	@Inject(method = "onEntityUpdate", at = @At("TAIL"))
	private void etfu$updateSwimming(CallbackInfo ci) {
		Entity entity = (Entity) (Object) this;
		if (entity.worldObj != null && !entity.worldObj.isRemote && entity instanceof EntityPlayer) {
			SwimmingRuntime.updateSwimming((EntityPlayer) entity);
		}
	}

	// NOTE: the swimming water probe (26.2 whole-deflated-box isInWater semantics, replacing
	// 1.7.10's expand(0,-0.4,0) band that inverts for the 0.6-high swimming box) lives in
	// oceanplants.MixinEntity - a single handleWaterMovement call site only supports one
	// @Redirect, so the two handlers are merged there.
}
