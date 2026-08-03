package ganymedes01.etfuturum.creative;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;

import java.util.HashSet;
import java.util.Set;

/**
 * 自动将物品/方块归类到现代 Minecraft 风格的创造标签页中。
 * <p>
 * 策略：
 * <ol>
 *   <li>EtFuturum 物品：由 CreativeTabData 提供官方排序（见 {@link ModdedCreativeTabs.SortedCreativeTab}）</li>
 *   <li>其他 mod 物品：从对应原版 CreativeTab 接管，追加到新分类末尾</li>
 *   <li>刷怪蛋：统一归入 {@link ModdedCreativeTabs#SPAWN_EGGS}</li>
 *   <li>无法归类的（不在官方数据中、也不在原版标签页的）→ 放入"原材料"</li>
 * </ol>
 * <p>
 * 本类不再使用 Item 子类或 Block 材质判断策略，避免误伤其他 mod 的物品。
 */
public class ItemCategoryHelper {

	// ==================== 刷怪蛋集合 ====================

	private static final Set<Item> eggsSet = new HashSet<>();
	private static boolean initialized = false;

	/**
	 * 判断物品应分配到哪个自定义 CreativeTab。
	 * <p>
	 * 优先使用 Mojang 官方创造标签页数据（CreativeTabData，提取自 1.21.4 字节码），
	 * 覆盖所有原版物品及 EtFuturum 物品。
	 *
	 * @param item 待分类的物品
	 * @return 匹配的 CreativeTab，无法分类时返回 null
	 */
	public static CreativeTabs getTabForItem(Item item) {
		if (item == null) return null;

		// 1. 官方 Mojang 数据优先
		String registryName = Item.itemRegistry.getNameForObject(item);
		int officialTab = CreativeTabData.getTab(registryName);
		if (officialTab != CreativeTabData.UNKNOWN) {
			CreativeTabs tab = tabByIndex(officialTab);
			if (tab != null) return tab;
		}

		// 2. 药水类（不在 CreativeTabData 中，按 1.21.4 官方分类归入食物与饮品）
		// 1.7.10 的 minecraft:potion 通过 metadata 同时涵盖饮用和喷溅药水
		if ("minecraft:potion".equals(registryName)) {
			return ModdedCreativeTabs.FOOD_AND_DRINKS;
		}

		// 3. 刷怪蛋
		if (eggsSet.contains(item)) {
			return ModdedCreativeTabs.SPAWN_EGGS;
		}

		// 4. 无法判断 → null（保持原状，由 displayAllReleventItems Part 2 接管）
		return null;
	}

	/**
	 * 将 CreativeTabData 的官方标签页索引映射到实际 CreativeTabs 实例。
	 */
	private static CreativeTabs tabByIndex(int idx) {
		switch (idx) {
			case CreativeTabData.BUILDING_BLOCKS:     return ModdedCreativeTabs.BUILDING_BLOCKS;
			case CreativeTabData.COLORED_BLOCKS:      return ModdedCreativeTabs.COLORED_BLOCKS;
			case CreativeTabData.NATURAL_BLOCKS:      return ModdedCreativeTabs.NATURAL_BLOCKS;
			case CreativeTabData.FUNCTIONAL_BLOCKS:   return ModdedCreativeTabs.FUNCTIONAL_BLOCKS;
			case CreativeTabData.REDSTONE_BLOCKS:     return ModdedCreativeTabs.REDSTONE_BLOCKS;
			case CreativeTabData.TOOLS_AND_UTILITIES: return ModdedCreativeTabs.TOOLS_AND_UTILITIES;
			case CreativeTabData.COMBAT:              return ModdedCreativeTabs.COMBAT;
			case CreativeTabData.FOOD_AND_DRINKS:     return ModdedCreativeTabs.FOOD_AND_DRINKS;
			case CreativeTabData.INGREDIENTS:         return ModdedCreativeTabs.INGREDIENTS;
			case CreativeTabData.SPAWN_EGGS:          return ModdedCreativeTabs.SPAWN_EGGS;
			default: return null;
		}
	}

	// ==================== 初始化 ====================

	/**
	 * 在 postInit 阶段调用，扫描所有已注册的物品和方块，
	 * 将刷怪蛋和原版标签页中的物品重定向到合适的现代分类。
	 */
	@SuppressWarnings("unchecked")
	public static void reassignAllItems() {
		if (initialized) return;
		initialized = true;

		// 1. 构建刷怪蛋集合（unlocalizedName 包含 "spawn_egg" 且非基础 spawn_egg 的物品）
		for (Object obj : Item.itemRegistry) {
			if (obj instanceof Item) {
				Item item = (Item) obj;
				if (item.getUnlocalizedName() != null &&
						item.getUnlocalizedName().contains("spawn_egg") &&
						item != net.minecraft.init.Items.spawn_egg) {
					eggsSet.add(item);
				}
			}
		}

		// 2. 将原版标签页中的物品重定向到现代分类
		//    映射规则：原版标签页 → 现代分类
		//    tabFood → FOOD_AND_DRINKS, tabCombat → COMBAT, tabTools → TOOLS_AND_UTILITIES,
		//    tabMaterials/tabBrewing → INGREDIENTS, tabBlock → BUILDING_BLOCKS,
		//    tabDecorations → COLORED_BLOCKS, tabRedstone → REDSTONE_BLOCKS,
		//    tabTransport → FUNCTIONAL_BLOCKS, tabMisc → INGREDIENTS
		int redirected = 0;
		for (Object obj : Item.itemRegistry) {
			if (obj instanceof Item) {
				Item item = (Item) obj;
				CreativeTabs currentTab = item.getCreativeTab();
				if (currentTab == null) continue;

				CreativeTabs target = getTabForItem(item);
			if (target != null && target != currentTab) {
				setCreativeTab(item, target);
				redirected++;
			}
			// 未匹配到官方分类但指向原版标签页 → 回退到原材料
			else if (target == null && isVanillaTab(currentTab)) {
				setCreativeTab(item, ModdedCreativeTabs.INGREDIENTS);
				redirected++;
				}
			}
		}

		System.out.println("[EtFuturum Creative] Reassigned " + redirected + " items to modern creative tabs.");
	}

	/**
	 * 设置物品的创造标签页。
	 * ItemBlock 的 getCreativeTab() 返回 Block.getCreativeTabToDisplayOn()，
	 * 而非 Item 自身的 tabToDisplayOn 字段，因此对 ItemBlock 必须调用
	 * Block.setCreativeTab() 才能真正改变其显示标签页。
	 */
	private static void setCreativeTab(Item item, CreativeTabs tab) {
		if (item instanceof ItemBlock) {
			((ItemBlock) item).field_150939_a.setCreativeTab(tab);
		} else {
			item.setCreativeTab(tab);
		}
	}

	private static boolean isVanillaTab(CreativeTabs tab) {
		return tab == CreativeTabs.tabBlock ||
				tab == CreativeTabs.tabDecorations ||
				tab == CreativeTabs.tabRedstone ||
				tab == CreativeTabs.tabTransport ||
				tab == CreativeTabs.tabMisc ||
				tab == CreativeTabs.tabAllSearch ||
				tab == CreativeTabs.tabFood ||
				tab == CreativeTabs.tabTools ||
				tab == CreativeTabs.tabCombat ||
				tab == CreativeTabs.tabBrewing ||
				tab == CreativeTabs.tabMaterials ||
				tab == CreativeTabs.tabInventory;
	}
}
