package ganymedes01.etfuturum.items;

import java.util.function.Supplier;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.core.utils.Utils;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

/**
 * Spear from modern Minecraft (registered like Items.java L1198-1204:
 * Properties.spear(ToolMaterial, ...) -> durability comes from the tool material).
 * Phase 1: item, icons and the official hand rendering only, attack/use behaviour comes later.
 */
public class ItemSpear extends Item {

	private final String name;
	private final Supplier<ItemStack> repairMaterial;

	@SideOnly(Side.CLIENT)
	private IIcon inHandIcon;

	public ItemSpear(String name, int uses, Supplier<ItemStack> repairMaterial) {
		this.name = name;
		this.repairMaterial = repairMaterial;
		setUnlocalizedName(Utils.getUnlocalisedName(name));
		setTextureName(name);
		setMaxDamage(uses); // also caps max stack size at 1, like official spears
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister register) {
		super.registerIcons(register); // flat {tier}_spear icon: gui/ground/fixed/on_shelf contexts
		this.inHandIcon = register.registerIcon(name + "_in_hand"); // fallback context: held in hand
	}

	/**
	 * The wide {tier}_spear_in_hand sprite, consumed by ItemSpearRenderer.
	 */
	@SideOnly(Side.CLIENT)
	public IIcon getInHandIcon() {
		return inHandIcon;
	}

	/**
	 * Long-shaft weapon, rendered like swords in third person (RenderBiped isFull3D branch:
	 * S(0.625,-0.625,0.625) R(-100X) R(45Y) in front of the IItemRenderer call).
	 */
	@Override
	public boolean isFull3D() {
		return true;
	}

	@Override
	public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
		return repair.getItem() == repairMaterial.get().getItem() || super.getIsRepairable(toRepair, repair);
	}
}
