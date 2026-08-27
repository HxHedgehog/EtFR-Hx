package ganymedes01.etfuturum.recipes.crafting;

import ganymedes01.etfuturum.ModBlocks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapelessOreRecipe;

public class RecipeOldRose extends ShapelessOreRecipe {

	public RecipeOldRose() {
		super(ModBlocks.ROSE.newItemStack(6), new ItemStack(Blocks.double_plant, 1, 4), new ItemStack(Items.shears, 1, OreDictionary.WILDCARD_VALUE));
	}

	/**
	 * 合成事件回调：对剪刀扣 6 点耐久。1.7.10 没有剩余物品 API，
	 * 借助堆叠数技巧（临时设为 2，容器扣减 1 后仍剩 1 把）让剪刀留在合成格中。
	 */
	public static void damageShears(ItemStack result, IInventory craftMatrix, EntityPlayer player) {
		// 不能校验 stackSize：Shift 点击或手里已拿玫瑰时，event.crafting 是合并/扣减后的堆叠，
		// 堆叠数并非 6。仅靠物品类型 + matches() 校验输入即可（老玫瑰仅由本配方产出）。
		if (result == null || result.getItem() != Item.getItemFromBlock(ModBlocks.ROSE.get())) return;
		if (!matches(craftMatrix)) return;

		for (int i = 0; i < craftMatrix.getSizeInventory(); i++) {
			ItemStack stack = craftMatrix.getStackInSlot(i);
			if (stack != null && stack.getItem() instanceof ItemShears) {
				stack.damageItem(6, player);
				if (stack.stackSize <= 0) {
					craftMatrix.setInventorySlotContents(i, null);
				} else {
					stack.stackSize = 2;
				}
				break;
			}
		}
	}

	private static boolean matches(IInventory inv) {
		boolean bush = false;
		boolean shears = false;

		for (int i = 0; i < inv.getSizeInventory(); i++) {
			ItemStack slot = inv.getStackInSlot(i);

			if (slot != null) {
				if (slot.getItem() == Item.getItemFromBlock(Blocks.double_plant) && slot.getItemDamage() == 4) {
					if (bush) return false;
					bush = true;
				} else if (slot.getItem() instanceof ItemShears) {
					if (shears) return false;
					shears = true;
				} else {
					return false;
				}
			}
		}

		return bush && shears;
	}

}
