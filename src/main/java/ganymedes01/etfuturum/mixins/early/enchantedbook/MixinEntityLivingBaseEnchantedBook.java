package ganymedes01.etfuturum.mixins.early.enchantedbook;

import ganymedes01.etfuturum.enchantment.EnchantedBookTextureHandler;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.util.IIcon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Safety net for the entity-held item render path. Even though
 * {@link EnchantedBookTextureHandler} already swaps the sprite via
 * {@link net.minecraft.item.Item#getIconIndex(ItemStack)}, this hooks the exact
 * entry point that {@link net.minecraft.client.renderer.ItemRenderer} and entity
 * renderers use to fetch the held item's icon, guaranteeing that a held enchanted
 * book always renders with its Bibliophilia-style sprite regardless of any other
 * override. Enchanted books are never multi-pass, so replacing the icon here is
 * always safe.
 * <p>
 * Load only on the client and only when {@code betterBookTextures} is enabled.
 */
@Mixin(EntityLivingBase.class)
public class MixinEntityLivingBaseEnchantedBook {

	@Inject(method = "getItemIcon(Lnet/minecraft/item/ItemStack;I)Lnet/minecraft/util/IIcon;", at = @At("RETURN"), cancellable = true)
	private void etfuturum$swapHeldIcon(ItemStack stack, int renderPass, CallbackInfoReturnable<IIcon> cir) {
		if (stack != null && stack.getItem() instanceof ItemEnchantedBook) {
			IIcon custom = EnchantedBookTextureHandler.INSTANCE.getIcon(stack);
			if (custom != null) {
				cir.setReturnValue(custom);
			}
		}
	}
}