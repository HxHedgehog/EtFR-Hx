package ganymedes01.etfuturum.enchantment;

import ganymedes01.etfuturum.configuration.configs.ConfigEnchantsPotions;
import ganymedes01.etfuturum.items.equipment.ItemArmorElytra;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.item.ItemStack;

/**
 * Backport of the modern Depth Strider enchantment (26.2 minecraft:depth_strider).
 * Works on boots as in vanilla; the elytra additionally accepts it so that,
 * with the {@code elytraOceanEnchant} config enabled, it can glide underwater
 * (level 1+; level 3 also removes the underwater swimming penalty on boots).
 * The enchantment is always registered, regardless of that config.
 */
public class WaterWalker extends Enchantment {

	public WaterWalker() {
		super(ConfigEnchantsPotions.waterWalkerID, 2, EnumEnchantmentType.armor_feet);
		Enchantment.addToBookList(this);
		// Reuses the pre-existing four-language key "enchantment.waterWalker" = "Depth Strider".
		setName("waterWalker");
	}

	@Override
	public int getMinEnchantability(int enchantmentLevel) {
		// 26.2: dynamicMinCost(10, 10) per level
		return enchantmentLevel * 10;
	}

	@Override
	public int getMaxEnchantability(int enchantmentLevel) {
		// 26.2: dynamicMaxCost(25, 10) per level
		return this.getMinEnchantability(enchantmentLevel) + 15;
	}

	@Override
	public int getMaxLevel() {
		return 3;
	}

	@Override
	public boolean canApply(ItemStack stack) {
		// Also accepts the elytra: covers the enchanting table (canApplyAtEnchantingTable
		// delegates here by default) and the anvil book-merge path (ContainerRepair uses
		// canApply on the target item).
		return super.canApply(stack) || stack != null && stack.getItem() instanceof ItemArmorElytra;
	}

	@Override
	public boolean canApplyTogether(Enchantment ench) {
		// 26.2: mutually exclusive with Frost Walker (BOOTS_EXCLUSIVE set).
		return !(ench instanceof FrostWalker) && super.canApplyTogether(ench);
	}
}
