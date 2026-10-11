package ganymedes01.etfuturum.mixins.early.spear.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import ganymedes01.etfuturum.items.ItemSpear;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;

/**
 * 26.2 UseEffects(canSprint = true, speedMultiplier = 1.0) for spears: using a spear never slows
 * the player down and never blocks sprinting. The three isUsingItem gates in EntityPlayerSP
 * (strafe/forward x0.2, both double-tap-sprint gates) are redirected for spears only.
 */
@Mixin(EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP {

	@Redirect(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;isUsingItem()Z"), require = 3)
	private boolean etfu$spearNoSlow(EntityPlayerSP player) {
		ItemStack stack = player.getItemInUse();
		if (stack != null && stack.getItem() instanceof ItemSpear)
			return false;
		return player.isUsingItem();
	}
}
