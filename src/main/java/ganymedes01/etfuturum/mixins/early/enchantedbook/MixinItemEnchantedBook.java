package ganymedes01.etfuturum.mixins.early.enchantedbook;

import ganymedes01.etfuturum.enchantment.EnchantedBookTextureHandler;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.IIcon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Replaces the default enchanted-book sprite with the Bibliophilia-style sprite
 * of the book's first stored enchantment (see
 * {@link EnchantedBookTextureHandler}) and makes the item display the stored
 * enchantment name (e.g. "锋利 II") instead of the generic "附魔书".
 * <p>
 * Both {@code getIconIndex} and {@code getItemStackDisplayName} are declared on
 * {@link Item} and merely inherited by {@link ItemEnchantedBook}, so the mixin
 * targets {@link Item} and only acts when the stack is actually an enchanted
 * book. When no bundled sprite matches the stack's enchantment - or the texture
 * data has not been initialised - the original/vanilla sprite is kept untouched.
 */
@Mixin(Item.class)
public class MixinItemEnchantedBook {

	@Inject(method = "getIconIndex(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/util/IIcon;", at = @At("RETURN"), cancellable = true)
	private void etfuturum$swapIcon(ItemStack stack, CallbackInfoReturnable<IIcon> cir) {
		if (stack != null && stack.getItem() instanceof ItemEnchantedBook) {
			IIcon custom = EnchantedBookTextureHandler.INSTANCE.getIcon(stack);
			if (custom != null) {
				cir.setReturnValue(custom);
			}
		}
	}

	@Inject(method = "getItemStackDisplayName(Lnet/minecraft/item/ItemStack;)Ljava/lang/String;", at = @At("RETURN"), cancellable = true)
	private void etfuturum$enchantmentName(ItemStack stack, CallbackInfoReturnable<String> cir) {
		if (stack == null || !(stack.getItem() instanceof ItemEnchantedBook)) {
			return;
		}
		String name = firstStoredEnchantmentName(stack);
		if (name != null) {
			cir.setReturnValue(name);
		}
	}

	/**
	 * Returns the translated name (with level, e.g. "锋利 II") of the first
	 * stored enchantment, or {@code null} when the book has none, mirroring the
	 * logic already used by {@link ItemEnchantedBook#addInformation}.
	 */
	private static String firstStoredEnchantmentName(ItemStack stack) {
		NBTTagCompound tag = stack.getTagCompound();
		if (tag == null || !tag.hasKey("StoredEnchantments", 9)) {
			return null;
		}
		NBTTagList list = tag.getTagList("StoredEnchantments", 10);
		for (int i = 0; i < list.tagCount(); i++) {
			NBTTagCompound enchTag = list.getCompoundTagAt(i);
			short id = enchTag.getShort("id");
			short lvl = enchTag.getShort("lvl");
			if (id >= 0 && id < Enchantment.enchantmentsList.length && Enchantment.enchantmentsList[id] != null) {
				return Enchantment.enchantmentsList[id].getTranslatedName(lvl);
			}
		}
		return null;
	}
}