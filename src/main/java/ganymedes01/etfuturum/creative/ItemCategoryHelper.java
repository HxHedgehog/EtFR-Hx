package ganymedes01.etfuturum.creative;

import ganymedes01.etfuturum.blocks.BaseCaveVines;
import ganymedes01.etfuturum.blocks.BaseSlab;
import ganymedes01.etfuturum.core.utils.Logger;
import net.minecraft.block.Block;
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
 *   <li>其他 mod 挂到原版标签页的物品（非 minecraft: 命名空间；原版标签页已被替换，
 *       不重定向会看不到）→ {@link ModdedCreativeTabs#UNCLASSIFIED}。
 *       原版命名空间物品由 CreativeTabData 展示，不会进未分类。</li>
 *   <li>本 mod 独有物品（不在官方 1.21.4 数据中）→ {@link ModdedCreativeTabs#UNCLASSIFIED}</li>
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
		// lingering_potion 是 mod 添加的，但 1.21.4 也有此物品，归入食物与饮品
		if ("minecraft:potion".equals(registryName) || "minecraft:lingering_potion".equals(registryName)) {
			return ModdedCreativeTabs.FOOD_AND_DRINKS;
		}

		// 2b. 1.21.4 创造栏中存在但 CreativeTabData 未收录的物品
		// （提取脚本跳过了部分 NBT 变体/技术方块，这里按 1.21.4 官方分类补全）
		if ("minecraft:tipped_arrow".equals(registryName)) {
			return ModdedCreativeTabs.COMBAT;
		}
		if ("minecraft:suspicious_stew".equals(registryName)) {
			return ModdedCreativeTabs.FOOD_AND_DRINKS;
		}
		if ("minecraft:dye_same".equals(registryName)) {
			return ModdedCreativeTabs.INGREDIENTS;
		}
		// 锁链：1.21.4 官方数据用 iron_chain，mod 注册名为 chain
		if ("minecraft:chain".equals(registryName)) {
			return ModdedCreativeTabs.FUNCTIONAL_BLOCKS;
		}
		// 西瓜块：1.21.4 官方数据用 melon（西瓜块），1.7.10 注册名是 melon_block
		if ("minecraft:melon_block".equals(registryName)) {
			return ModdedCreativeTabs.NATURAL_BLOCKS;
		}
		// 雪层：1.21.4 官方数据用 snow（雪层），1.7.10 注册名是 snow_layer
		if ("minecraft:snow_layer".equals(registryName)) {
			return ModdedCreativeTabs.NATURAL_BLOCKS;
		}
		if ("minecraft:barrier".equals(registryName) || "minecraft:light".equals(registryName)) {
			return ModdedCreativeTabs.FUNCTIONAL_BLOCKS;
		}
		// 1.21.6（Tears）/ 1.21.7（Lava Chicken）才加入的官方唱片，CreativeTabData（1.21.4）未收录
		if ("minecraft:music_disc_tears".equals(registryName) || "minecraft:music_disc_lava_chicken".equals(registryName)) {
			return ModdedCreativeTabs.TOOLS_AND_UTILITIES;
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

	/** 判断物品是否是本 mod 注册的（通过检查类所在包名） */
	private static boolean isModItem(Item item) {
		if (item instanceof ItemBlock) {
			Block block = ((ItemBlock) item).field_150939_a;
			return block.getClass().getName().startsWith("ganymedes01.etfuturum");
		}
		return item.getClass().getName().startsWith("ganymedes01.etfuturum");
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
				// 附魔书在 1.7.10 原版中未归属任何创造标签页（getCreativeTab()==null），
				// 但 1.21.4 官方将其归入原材料（INGREDIENTS）。这里手动分配，
				// 否则 displayAllReleventItems 中 enchantedBook.getCreativeTab()==this 恒不成立，附魔书不显示。
				if (currentTab == null) {
					if ((Item) Item.itemRegistry.getObject("minecraft:enchanted_book") == item) {
						setCreativeTab(item, ModdedCreativeTabs.INGREDIENTS);
						redirected++;
					}
					continue;
				}

				CreativeTabs target = getTabForItem(item);
			if (target != null && target != currentTab) {
				setCreativeTab(item, target);
				redirected++;
			}
			// 未匹配到官方分类、指向原版标签页且属于其他 mod 命名空间的物品
			// （其他 mod 挂到原版创造栏的物品）→ 归入未分类，否则原版标签页被替换后用户看不到它们。
			// 原版命名空间（minecraft:）物品不进来：它们由 CreativeTabData 的
			// displayAllReleventItems 机制展示（如刷怪蛋、唱片），与 getCreativeTab() 无关。
			else if (target == null && isVanillaTab(currentTab) && !isVanillaItem(item)) {
				setCreativeTab(item, ModdedCreativeTabs.UNCLASSIFIED);
				redirected++;
				}
			}
		}

		Logger.info("[EtFuturum Creative] Reassigned " + redirected + " items to modern creative tabs.");

		// 3. 本 mod 独有物品（不在官方 1.21.4 创造栏数据中）→ 未分类
		// 这些物品是 mod 实现了但官方 1.21.4 中没有的（如自定义唱片、升级组件等），
		// 以及 mod 联动物品（BOP/Witchery 的船、栅栏等，条件满足时才注册）。
		// 已显式设到"未分类"的物品（如屏障、光源方块）不会被重复设置。
		// 注意：需要排除被 CreativeTabData 映射系统用作 base item 的物品
		// （如 concrete_powder、planks 等），它们会被 SortedCreativeTab 通过
		// baseName 映射找到并正确显示在对应标签页中。
		Set<String> creativeTabBaseItems = ModdedCreativeTabs.getCreativeTabBaseItemIds();
		int temporary = 0;
		int classified = 0;
		for (Object obj : Item.itemRegistry) {
			if (obj instanceof Item) {
				Item item = (Item) obj;
				String regName = Item.itemRegistry.getNameForObject(item);
				if (regName == null) continue;

				// 只处理本 mod 注册的物品
				if (!isModItem(item)) continue;

				// double slab 技术方块（不显示在任何创造栏）→ 跳过
				if (item instanceof ItemBlock) {
					Block block = ((ItemBlock) item).field_150939_a;
					if (block instanceof BaseSlab && ((BaseSlab) block).getDoubleSlab() == block) {
						continue;
					}
				}

				// 洞穴藤蔓技术方块（官方创造栏不显示，发光浆果才是对应物品）→ 跳过
				if (item instanceof ItemBlock && ((ItemBlock) item).field_150939_a instanceof BaseCaveVines) {
					continue;
				}

				// 已在官方数据中 → 由 SortedCreativeTab 处理
				int officialTab = CreativeTabData.getTab(regName);
				if (officialTab != CreativeTabData.UNKNOWN) continue;

				// etfuturum: 命名空间物品（如 etfuturum:beacon）→ 检查对应 minecraft: 条目
				// 这三个方块因与 1.7.10 原版冲突而强制使用 etfuturum: 命名空间
				if (regName.startsWith("etfuturum:")) {
					String mcName = "minecraft:" + regName.substring("etfuturum:".length());
					if (CreativeTabData.getTab(mcName) != CreativeTabData.UNKNOWN) continue;
				}

				// 是 CreativeTabData 映射的 base item（如 concrete_powder、planks）→ 跳过
				if (creativeTabBaseItems.contains(regName)) continue;

				CreativeTabs currentTab = item.getCreativeTab();

				// 1.21.4 创造栏有但 CreativeTabData 未收录的物品 → 归入官方分类
				CreativeTabs target = getTabForItem(item);
				if (target != null) {
					if (currentTab != target) {
						setCreativeTab(item, target);
						classified++;
					}
					continue;
				}

				// 已显式设到"未分类"的物品不重复设置
				if (currentTab == ModdedCreativeTabs.UNCLASSIFIED) continue;

				setCreativeTab(item, ModdedCreativeTabs.UNCLASSIFIED);
				temporary++;
			}
		}
		if (temporary > 0 || classified > 0) {
			Logger.info("[EtFuturum Creative] Moved " + temporary + " mod-only items to UNCLASSIFIED tab, " + classified + " to official tabs.");
		}
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

	/** 判断物品是否属于原版命名空间（minecraft:）。原版物品由 CreativeTabData 展示，不进未分类。 */
	private static boolean isVanillaItem(Item item) {
		String regName = Item.itemRegistry.getNameForObject(item);
		return regName != null && regName.startsWith("minecraft:");
	}
}
