package ganymedes01.etfuturum.mixins.early.goldenapple;

import net.minecraft.item.Item;
import net.minecraft.item.ItemAppleGold;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives the vanilla 1.7.10 enchanted golden apple (meta 1 of the golden apple
 * item) its own display name, instead of sharing "Golden Apple" with the
 * regular one like vanilla 1.7.10 does. Modern versions (1.11+) call it
 * "Enchanted Golden Apple"; the translation key
 * {@code item.appleGold.enchanted.name} is supplied by this mod's lang files
 * (vanilla 1.7.10 has no such key).
 * <p>
 * {@code ItemAppleGold} does not override
 * {@code getUnlocalizedName(ItemStack)}, so the mixin targets the base
 * {@link Item} and narrows by instanceof. Always applied; only the
 * client-visible display name changes.
 */
@Mixin(Item.class)
public class MixinItemAppleGold {

	@Inject(method = "getUnlocalizedName(Lnet/minecraft/item/ItemStack;)Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
	private void etfuturum$enchantedGoldenAppleName(ItemStack stack, CallbackInfoReturnable<String> cir) {
		// === 附魔金苹果（golden_apple meta 1）：使用独立名称键，其余物品原样放行 ===
		if ((Object) this instanceof ItemAppleGold && stack.getItemDamage() > 0) {
			cir.setReturnValue("item.appleGold.enchanted.name");
		}
	}
}
