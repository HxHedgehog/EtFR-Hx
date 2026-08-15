package ganymedes01.etfuturum.creative;

import cpw.mods.fml.common.registry.GameData;
import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.core.utils.Logger;
import ganymedes01.etfuturum.items.ItemSuspiciousStew;
import ganymedes01.etfuturum.recipes.ModRecipes;
import net.minecraft.client.Minecraft;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionHelper;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 仿造现代 Minecraft (1.19+) 的创造模式物品栏标签页排序。
 * 共 10 个标签页：建筑方块、染色方块、自然方块、功能方块、红石方块、
 * 工具与实用物品、战斗用品、食物与饮品、原材料、刷怪蛋。
 * <p>
 * 物品分类由 {@link ItemCategoryHelper#reassignAllItems()} 在 postInit 阶段
 * 统一处理，将原版和其他 Mod 的物品重定向到合适的标签页。
 * 标签页内排序由 {@link CreativeTabData} 提供官方 1.21.4 顺序。
 */
public class ModdedCreativeTabs {

	/** 注意：CreativeTabs.getTranslatedTabLabel() 会自动加 "itemGroup." 前缀，所以这里不需要加 */
	private static final String LANG_PREFIX = "etfuturum.";

	/** 所有自定义创造标签页（按 GUI 显示顺序） */
	public static final CreativeTabs[] ALL_TABS;

	// ==================== 标签页定义 ====================

	/** 1. 建筑方块 */
	public static final CreativeTabs BUILDING_BLOCKS = new SortedCreativeTab(0, LANG_PREFIX + "buildingBlocks", CreativeTabs.tabBlock) {
		@Override
		public Item getTabIconItem() {
			return Item.getItemFromBlock(Blocks.brick_block);
		}
	};

	/** 2. 染色方块 */
	public static final CreativeTabs COLORED_BLOCKS = new SortedCreativeTab(1, LANG_PREFIX + "coloredBlocks", CreativeTabs.tabDecorations) {
		@Override
		public Item getTabIconItem() {
			return Item.getItemFromBlock(Blocks.lapis_block);
		}
	};

	/** 3. 自然方块 */
	public static final CreativeTabs NATURAL_BLOCKS = new SortedCreativeTab(2, LANG_PREFIX + "naturalBlocks") {
		@Override
		public Item getTabIconItem() {
			return Item.getItemFromBlock(Blocks.grass);
		}
	};

	/** 4. 功能方块 */
	public static final CreativeTabs FUNCTIONAL_BLOCKS = new SortedCreativeTab(3, LANG_PREFIX + "functionalBlocks", CreativeTabs.tabTransport) {
		@Override
		public Item getTabIconItem() {
			return Item.getItemFromBlock(Blocks.crafting_table);
		}
	};

	/** 5. 红石方块 */
	public static final CreativeTabs REDSTONE_BLOCKS = new SortedCreativeTab(4, LANG_PREFIX + "redstoneBlocks", CreativeTabs.tabRedstone) {
		@Override
		public Item getTabIconItem() {
			return Items.redstone;
		}
	};

	/** 6. 工具与实用物品（索引 6，跳过 5 以保留给 tabAllSearch） */
	public static final CreativeTabs TOOLS_AND_UTILITIES = new SortedCreativeTab(6, LANG_PREFIX + "toolsAndUtilities", CreativeTabs.tabTools) {
		@Override
		public Item getTabIconItem() {
			return Items.diamond_pickaxe;
		}
	};

	/** 7. 战斗用品 */
	public static final CreativeTabs COMBAT = new SortedCreativeTab(7, LANG_PREFIX + "combat", CreativeTabs.tabCombat) {
		@Override
		public Item getTabIconItem() {
			return Items.diamond_sword;
		}
	};

	/** 8. 食物与饮品 */
	public static final CreativeTabs FOOD_AND_DRINKS = new SortedCreativeTab(8, LANG_PREFIX + "foodAndDrinks", CreativeTabs.tabFood) {
		@Override
		public Item getTabIconItem() {
			return Items.golden_apple;
		}
	};

	/** 9. 原材料 */
	public static final CreativeTabs INGREDIENTS = new SortedCreativeTab(9, LANG_PREFIX + "ingredients", CreativeTabs.tabMaterials) {
		@Override
		public Item getTabIconItem() {
			return Items.iron_ingot;
		}
	};

	/** 10. 刷怪蛋 */
	public static final CreativeTabs SPAWN_EGGS = new SortedCreativeTab(10, LANG_PREFIX + "spawnEggs", CreativeTabs.tabMisc) {
		@Override
		public Item getTabIconItem() {
			return Items.spawn_egg;
		}
	};

	/** 11. 分类之外 - 本 mod 添加的高版本也不存在的物品（如下界合金楼梯），不参与排序面板 */
	public static final CreativeTabs TEMPORARY = new CreativeTabs(12, LANG_PREFIX + "unclassified") {
		@Override
		public Item getTabIconItem() {
			return Item.getItemFromBlock(ModBlocks.OBSERVER.get());
		}
	};

	static {
		ALL_TABS = new CreativeTabs[]{
				BUILDING_BLOCKS, COLORED_BLOCKS, NATURAL_BLOCKS, FUNCTIONAL_BLOCKS,
				REDSTONE_BLOCKS, TOOLS_AND_UTILITIES, COMBAT, FOOD_AND_DRINKS,
				INGREDIENTS, SPAWN_EGGS, TEMPORARY
		};
	}

	// ==================== 初始化 ====================

	/**
	 * 在 postInit 阶段调用，完成标签页注册。
	 * 将原版 CreativeTabs.creativeTabArray 替换为我们的自定义标签页数组，
	 * 同时保留搜索 (Search)、生存物品栏 (Inventory) 和其他 Mod 的自定义标签页。
	 */
	public static void init() {
		replaceCreativeTabArray();
	}

	/**
	 * 构建新数组，按 tabIndex 顺序排列：
	 * [建筑(0), 染色(1), 自然(2), 功能(3), 红石(4), 搜索(5),
	 *  工具(6), 战斗(7), 食物(8), 原材料(9), 刷怪蛋(10), 生存栏(11), 其他Mod...]
	 * <p>
	 * 索引 5 保留给 tabAllSearch（搜索标签），不创建自定义标签页占用此索引。
	 * 这样 hasSearchBar()（判断 tabIndex == tabAllSearch.tabIndex）只对搜索标签生效。
	 */
	private static void replaceCreativeTabArray() {
		try {
			CreativeTabs[] oldArray = CreativeTabs.creativeTabArray;

			List<CreativeTabs> newTabs = new ArrayList<>();

			// 1. 前 5 个自定义标签页（索引 0-4）
			newTabs.add(BUILDING_BLOCKS);
			newTabs.add(COLORED_BLOCKS);
			newTabs.add(NATURAL_BLOCKS);
			newTabs.add(FUNCTIONAL_BLOCKS);
			newTabs.add(REDSTONE_BLOCKS);

			// 2. 搜索标签页（索引 5，保持原样不被覆盖）
			newTabs.add(CreativeTabs.tabAllSearch);

			// 3. 后 5 个自定义标签页（索引 6-10）
			newTabs.add(TOOLS_AND_UTILITIES);
			newTabs.add(COMBAT);
			newTabs.add(FOOD_AND_DRINKS);
			newTabs.add(INGREDIENTS);
			newTabs.add(SPAWN_EGGS);

			// 4. 生存物品栏标签页（索引 11）
			newTabs.add(CreativeTabs.tabInventory);

			// 5. 分类之外标签页（本 mod 独有的、高版本也不存在的物品）
			newTabs.add(TEMPORARY);

			// 6. 添加其他 Mod 的自定义标签页（非原版、非我们自己的、非搜索/生存栏、非本 mod 的）
			for (CreativeTabs tab : oldArray) {
				if (tab == null) continue;
				if (isOurTab(tab)) continue;
				if (tab == CreativeTabs.tabAllSearch || tab == CreativeTabs.tabInventory) continue;
				if (isVanillaTab(tab)) continue;
			newTabs.add(tab);
		}

			CreativeTabs[] newArray = newTabs.toArray(new CreativeTabs[0]);
			Field field = CreativeTabs.class.getDeclaredField("creativeTabArray");
			field.setAccessible(true);
			field.set(null, newArray);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static boolean isOurTab(CreativeTabs tab) {
		for (CreativeTabs t : ALL_TABS) {
			if (t == tab) return true;
		}
		return false;
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

	// ==================== 1.7.10 metadata 兼容 ====================

	/** Minecraft 16 色前缀，用于将 1.21.4 独立注册名映射到 1.7.10 metadata 物品 */
	private static final String[] COLOR_PREFIXES = {
			"white_", "orange_", "magenta_", "light_blue_", "yellow_", "lime_", "pink_",
			"gray_", "light_gray_", "cyan_", "purple_", "blue_", "brown_", "green_", "red_", "black_"
	};

	/**
	 * 必须走 baseName 映射的物品（不能直接 lookup）。
	 * 这些物品在 1.7.10 原版有同名但含义不同的注册名，直接 lookup 会命中错误物品。
	 * 例如：minecraft:grass 在 1.7.10 是草方块，但 1.21.4 是草丛，需映射到 tallgrass。
	 */
	private static final Set<String> FORCE_BASENAME_LOOKUP = new HashSet<>(Arrays.asList(
			"minecraft:grass",  // 1.7.10 grass = grass block, 1.21.4 grass = 草丛
			"minecraft:nether_brick"  // 1.7.10 nether_brick = 方块(ItemBlock), 1.21.4 nether_brick = 物品(需映射到 netherbrick)
	));

	/**
	 * 查找物品：先直接查找官方注册名，若失败则尝试 1.21.4→1.7.10 名称别名
	 * （如 shovel→spade，用于原版 1.7.10 工具的命名差异）。
	 * <p>
	 * Mod 物品现已全部注册到 minecraft: 命名空间且名称与官方一致，
	 * 不再需要 etfuturum: 回退查询。
	 */
	private static Object lookupItem(String itemId) {
		Object obj = Item.itemRegistry.getObject(itemId);
		if (obj == null) {
			String alias = getNameAlias(itemId);
			if (alias != null) {
				obj = Item.itemRegistry.getObject(alias);
			}
		}
		return obj;
	}

	/**
	 * 原版 1.7.10 → 1.21.4 命名差异映射。
	 * 这些别名仅用于原版 1.7.10 物品（非 mod 物品），因为 1.7.10 的注册名
	 * 与 1.21.4 的官方数据不同。
	 * 返回 1.7.10 中的等价注册名，或 null 表示无已知映射。
	 */
	private static String getNameAlias(String itemId) {
		int colon = itemId.indexOf(':');
		String domain = colon >= 0 ? itemId.substring(0, colon + 1) : "";
		String path = colon >= 0 ? itemId.substring(colon + 1) : itemId;

		// shovel → spade (1.21.4 用 "shovel"，1.7.10 用 "spade")
		if (path.contains("shovel")) {
			return domain + path.replace("shovel", "spade");
		}
		// grass_block → grass
		if (path.equals("grass_block")) {
			return domain + "grass";
		}
		// cobweb → web
		if (path.equals("cobweb")) {
			return domain + "web";
		}
		// music_disc_relic → etfuturum:record_relic（mod 注册域名为 etfuturum:）
		if (path.equals("music_disc_relic")) {
			return "etfuturum:record_relic";
		}
		// armor_stand → wooden_armorstand（1.21.4 名 → mod 物品）
		if (path.equals("armor_stand")) {
			return domain + "wooden_armorstand";
		}
		// nether_quartz_ore → quartz_ore（1.21.4 名 → 1.7.10 原版名）
	if (path.equals("nether_quartz_ore")) {
		return domain + "quartz_ore";
	}
	// enchanting_table → enchantment_table（1.21.4 名 → 1.7.10 原版名）
	if (path.equals("enchanting_table")) {
		return domain + "enchantment_table";
	}
	return null;
}

	/**
	 * 对于 1.7.10 的 metadata 物品，将 1.21.4 独立注册名映射到 1.7.10 基础物品名。
	 * 例如：minecraft:white_stained_glass → minecraft:stained_glass
	 *      minecraft:zombie_spawn_egg → minecraft:spawn_egg
	 * 返回 null 表示无需映射。
	 */
	public static String getBaseItemName(String itemId) {
		int colon = itemId.indexOf(':');
		String domain = colon >= 0 ? itemId.substring(0, colon + 1) : "";
		String path = colon >= 0 ? itemId.substring(colon + 1) : itemId;

		// 刷怪蛋：minecraft:zombie_spawn_egg → minecraft:spawn_egg
		if (path.endsWith("_spawn_egg") && !path.equals("spawn_egg")) {
			return domain + "spawn_egg";
		}
		// 陶瓦：1.21.4 "terracotta" → 1.7.10 "stained_hardened_clay"(染色) / "hardened_clay"(纯色)
		if (path.endsWith("_terracotta")) {
			return domain + "stained_hardened_clay";
		}
		if (path.equals("terracotta")) {
			return domain + "hardened_clay";
		}
		// 染色床：mod 注册了独立物品（white_bed 等，每个颜色一个 Block），
		// 不做颜色前缀剥离，让 lookup 直接找 mod 的独立物品。
		// red_bed 例外：mod 没注册，映射到原版 bed（meta=14 由 getMetaForOfficialName 处理）。
		if (path.endsWith("_bed")) {
			if (path.equals("red_bed")) {
				return domain + "bed";
			}
			return null;
		}
		// 新式染料（1.14+ 独立物品）：white/blue/brown/black 映射到 mod 的 dye_same
		if (path.equals("white_dye") || path.equals("blue_dye") ||
			path.equals("brown_dye") || path.equals("black_dye")) {
			return domain + "dye_same";
		}
		// 青金石/骨粉/墨囊（1.21.4 独立物品，1.7.10 是 minecraft:dye 的 meta 4/15/0）
		if (path.equals("lapis_lazuli") || path.equals("bone_meal") || path.equals("ink_sac")) {
			return domain + "dye";
		}
		// 染色变体：minecraft:white_stained_glass → minecraft:stained_glass
		// 同时支持花等需要二次映射的物品：blue_orchid → orchid → red_flower
		for (String prefix : COLOR_PREFIXES) {
			if (path.startsWith(prefix)) {
				String stripped = path.substring(prefix.length());
				String vanillaBase = getVanillaMetadataBase(stripped);
				return domain + (vanillaBase != null ? vanillaBase : stripped);
			}
		}
		// 涂蜡铜变体：waxed_exposed_copper → exposed_copper → getVanillaMetadataBase 处理
		if (path.startsWith("waxed_")) {
			String base = getVanillaMetadataBase(path.substring(6));
			if (base != null) return domain + base;
			// 对于 mod 直接注册的块（如 copper_bulb, copper_grate 等），直接查原名
			return domain + path.substring(6);
		}

		// === 1.7.10 原版 metadata 物品映射 ===
		// 这些物品在 1.21.4 是独立注册名，但在 1.7.10 是一个 Item 的不同 metadata 变体
		String vanillaBase = getVanillaMetadataBase(path);
		if (vanillaBase != null) {
			return domain + vanillaBase;
		}
		return null;
	}

	/**
	 * 1.7.10 原版 metadata 物品映射。
	 * 返回 1.7.10 的实际注册名（不含 domain），或 null 如果不匹配。
	 */
	private static String getVanillaMetadataBase(String path) {
		// 音乐唱片：music_disc_cat → record_cat
		if (path.startsWith("music_disc_")) {
			return path.replace("music_disc_", "record_");
		}
		// 原木：oak/spruce/birch/jungle → log, acacia/dark_oak → log2
		if (path.endsWith("_log")) {
			String wood = path.substring(0, path.lastIndexOf('_'));
			if (wood.equals("oak") || wood.equals("spruce") || wood.equals("birch") || wood.equals("jungle")) {
				return "log";
			}
			if (wood.equals("acacia") || wood.equals("dark_oak")) {
				return "log2";
			}
		}
		// 木头（树皮块）：oak/spruce/birch/jungle → bark, acacia/dark_oak → bark2
		if (path.endsWith("_wood")) {
			String wood = path.substring(0, path.lastIndexOf('_'));
			if (wood.equals("oak") || wood.equals("spruce") || wood.equals("birch") || wood.equals("jungle")) {
				return "bark";
			}
			if (wood.equals("acacia") || wood.equals("dark_oak")) {
				return "bark2";
			}
		}
		// 去皮原木：oak/spruce/birch/jungle → log_stripped, acacia/dark_oak → log2_stripped
		if (path.startsWith("stripped_")) {
			String wood = path.substring("stripped_".length());
			if (wood.equals("oak_log") || wood.equals("spruce_log") || wood.equals("birch_log") || wood.equals("jungle_log")) {
				return "log_stripped";
			}
			if (wood.equals("acacia_log") || wood.equals("dark_oak_log")) {
				return "log2_stripped";
			}
			if (wood.equals("oak_wood") || wood.equals("spruce_wood") || wood.equals("birch_wood") || wood.equals("jungle_wood")) {
				return "wood_stripped";
			}
			if (wood.equals("acacia_wood") || wood.equals("dark_oak_wood")) {
				return "wood2_stripped";
			}
			if (wood.equals("cherry_log") || wood.equals("cherry_wood")) return "cherry_log";
			if (wood.equals("bamboo_block")) return "bamboo_block";
		}
		// 木板
		if (path.equals("cherry_planks") || path.equals("bamboo_planks")) return "wood_planks";
		if (path.endsWith("_planks")) {
			return "planks";
		}
		// 树叶：oak/spruce/birch/jungle → leaves, acacia/dark_oak → leaves2
		if (path.endsWith("_leaves")) {
			String wood = path.substring(0, path.lastIndexOf('_'));
			if (wood.equals("oak") || wood.equals("spruce") || wood.equals("birch") || wood.equals("jungle")) {
				return "leaves";
			}
			if (wood.equals("acacia") || wood.equals("dark_oak")) {
				return "leaves2";
			}
		}
		// 树苗
		if (path.endsWith("_sapling") && !path.equals("bamboo_sapling") && !path.equals("cherry_sapling")) {
			return "sapling";
		}
		// 石头变体
		if (path.equals("granite") || path.equals("polished_granite") ||
			path.equals("diorite") || path.equals("polished_diorite") ||
			path.equals("andesite") || path.equals("polished_andesite")) {
			return "stone";
		}
		// 花（getBaseItemName 已剥离颜色前缀，故用简短名匹配）
		if (path.equals("dandelion")) return "yellow_flower";
		if (path.equals("poppy") || path.equals("orchid") || path.equals("allium") ||
			path.equals("azure_bluet") || path.equals("tulip") || path.equals("oxeye_daisy")) {
			return "red_flower";
		}
		// 高花
		if (path.equals("sunflower") || path.equals("lilac") || path.equals("rose_bush") ||
			path.equals("peony") || path.equals("tall_grass") || path.equals("large_fern")) {
			return "double_plant";
		}
		// 草丛/蕨
		if (path.equals("fern")) return "tallgrass";
		// 草丛（1.21.4 "grass" = 草丛，区别于 grass_block 草方块）
		if (path.equals("grass")) return "tallgrass";
		// 草丛（1.21.4 "short_grass" = 草丛）
		if (path.equals("short_grass")) return "tallgrass";
		// 鱼
		if (path.equals("cod") || path.equals("salmon") || path.equals("pufferfish") || path.equals("tropical_fish")) {
			return "fish";
		}
		if (path.equals("cooked_cod") || path.equals("cooked_salmon")) {
			return "cooked_fished";
		}
		// 煤炭/木炭
		if (path.equals("charcoal")) return "coal";
		// 砖块
		if (path.equals("bricks")) return "brick_block";
		// 圆石楼梯（1.7.10 叫 stone_stairs）
		if (path.equals("cobblestone_stairs")) return "stone_stairs";
		// 枯萎的灌木
		if (path.equals("dead_bush")) return "deadbush";
		// 沙子/红沙
		if (path.equals("red_sand")) return "sand";
		// 命名差异：1.21.4 用带 _block 后缀，1.7.10 用简短名
		if (path.equals("slime_block")) return "slime";
		if (path.equals("note_block")) return "noteblock";
		// 石台阶（1.7.10 用 stone_slab 统一）
		if (path.equals("cobblestone_slab") || path.equals("brick_slab") || path.equals("stone_brick_slab") ||
			path.equals("nether_brick_slab") || path.equals("quartz_slab") || path.equals("sandstone_slab") ||
			path.equals("red_sandstone_slab") || path.equals("smooth_quartz_slab") ||
			path.equals("smooth_red_sandstone_slab") || path.equals("smooth_sandstone_slab")) {
			return "stone_slab";
		}
		// 石台阶_2（花岗岩/闪长岩/安山岩系列的台阶，包括磨制版本）
		if (path.equals("andesite_slab") || path.equals("granite_slab") || path.equals("diorite_slab") ||
			path.equals("polished_andesite_slab") || path.equals("polished_granite_slab") || path.equals("polished_diorite_slab")) {
			return "stone_slab_2";
		}
		// 木台阶（1.7.10 原版木台阶用 wooden_slab，mod 新木材用 wood_slab）
		if (path.equals("oak_slab") || path.equals("spruce_slab") || path.equals("birch_slab") ||
			path.equals("jungle_slab") || path.equals("acacia_slab") || path.equals("dark_oak_slab") ||
			path.equals("mangrove_slab") || path.equals("pale_oak_slab")) {
			return "wooden_slab";
		}
		if (path.equals("cherry_slab") || path.equals("bamboo_slab") || path.equals("crimson_slab") ||
			path.equals("warped_slab")) {
			return "wood_slab";
		}
		// 石砖变体
		if (path.equals("mossy_stone_bricks") || path.equals("cracked_stone_bricks") ||
			path.equals("chiseled_stone_bricks")) {
			return "stonebrick";
		}
		// mossy cobblestone wall → vanilla 1.7.10 cobblestone_wall meta 1
		if (path.equals("mossy_cobblestone_wall")) {
			return "cobblestone_wall";
		}
		// 墙（1.7.10 / mod 用 stone_wall / stone_wall_2 统配 metadata）
		if (path.equals("brick_wall") || path.equals("stone_brick_wall") ||
			path.equals("mossy_stone_brick_wall") ||
			path.equals("sandstone_wall")) {
			return "stone_wall";
		}
		if (path.equals("granite_wall") || path.equals("diorite_wall") || path.equals("andesite_wall")) {
			return "stone_wall_2";
		}
		// 石英块变体
		if (path.equals("chiseled_quartz_block") || path.equals("quartz_pillar")) {
			return "quartz_block";
		}
		// 砂岩变体
		if (path.equals("chiseled_sandstone") || path.equals("cut_sandstone") ||
			path.equals("smooth_sandstone")) {
			return "sandstone";
		}
		// 粗矿（mod 用 raw_ore 统一注册，meta 0=铜、1=铁、2=金）
	if (path.equals("raw_copper") || path.equals("raw_iron") || path.equals("raw_gold")) {
		return "raw_ore";
	}
	// 粗矿块（mod 用 raw_ore_block 统一注册，meta 0=铜、1=铁、2=金）
	if (path.equals("raw_copper_block") || path.equals("raw_iron_block") || path.equals("raw_gold_block")) {
		return "raw_ore_block";
	}
		if (path.equals("chiseled_red_sandstone") || path.equals("cut_red_sandstone") ||
			path.equals("smooth_red_sandstone")) {
			return "red_sandstone";
		}
		if (path.equals("cut_red_sandstone_slab")) return "red_sandstone_slab";
		// 深板岩变体（mod 用 deepslate_bricks 带 meta 统配）
		if (path.equals("chiseled_deepslate") || path.equals("cracked_deepslate_bricks") ||
			path.equals("cracked_deepslate_tiles") || path.equals("deepslate_tiles")) {
			return "deepslate_bricks";
		}
		if (path.equals("deepslate_tile_slab")) return "deepslate_brick_slab";
	if (path.equals("deepslate_tile_wall")) return "deepslate_brick_wall";
	// 深板岩台阶/墙（mod 用 deepslate_slab / deepslate_wall 带 meta 0=圆石、1=磨制）
	if (path.equals("cobbled_deepslate_slab") || path.equals("polished_deepslate_slab")) return "deepslate_slab";
	if (path.equals("cobbled_deepslate_wall") || path.equals("polished_deepslate_wall")) return "deepslate_wall";
		// 下界砖变体（mod 用 red_nether_bricks 带 meta 统配）
		if (path.equals("cracked_nether_bricks") || path.equals("chiseled_nether_bricks")) {
			return "red_nether_bricks";
		}
		// 黑石变体（mod 用 blackstone 带 meta 统配）
		if (path.equals("cracked_polished_blackstone_bricks") || path.equals("chiseled_polished_blackstone") ||
			path.equals("polished_blackstone")) {
			return "blackstone";
		}
		// 磨制黑石砖墙（mod 用 blackstone_wall meta 2）
		if (path.equals("polished_blackstone_brick_wall")) return "blackstone_wall";
		// 凝灰岩变体（mod 用 tuff / tuff_slab / tuff_wall 带 meta 统配）
		if (path.equals("chiseled_tuff") || path.equals("chiseled_tuff_bricks") ||
			path.equals("polished_tuff")) {
			return "tuff";
		}
		if (path.equals("polished_tuff_slab") || path.equals("tuff_brick_slab")) return "tuff_slab";
		if (path.equals("polished_tuff_wall") || path.equals("tuff_brick_wall")) return "tuff_wall";
		// 磨制玄武岩
		if (path.equals("polished_basalt")) return "basalt";
		// 黑石台阶/墙（mod 用 blackstone_slab / blackstone_wall 带 meta 统配）
		if (path.equals("polished_blackstone_slab") || path.equals("polished_blackstone_brick_slab")) return "blackstone_slab";
		if (path.equals("polished_blackstone_wall")) return "blackstone_wall";
		// 海晶石台阶（mod 用 prismarine_slab 带 meta 统配）
		if (path.equals("prismarine_brick_slab") || path.equals("dark_prismarine_slab")) return "prismarine_slab";
		// 泥砖（mod 用 packed_mud 带 meta 统配）
		if (path.equals("mud_bricks")) return "packed_mud";
		// 铜块及其所有变体（mod 用 copper_block / chiseled_copper / copper_bulb / copper_grate 带 meta 区分）
		boolean copperWaxed = path.startsWith("waxed_");
		String copperPath = copperWaxed ? path.substring(6) : path;
		if (copperPath.startsWith("exposed_") || copperPath.startsWith("weathered_") || copperPath.startsWith("oxidized_")) {
			// 提取基块类型
			String base = copperPath;
			if (base.startsWith("exposed_")) base = base.substring(8);
			else if (base.startsWith("weathered_")) base = base.substring(10);
			else if (base.startsWith("oxidized_")) base = base.substring(9);
			if (base.equals("copper") || base.equals("copper_block")) return "copper_block";
			if (base.equals("cut_copper")) return "copper_block";
			if (base.equals("chiseled_copper")) return "chiseled_copper";
			if (base.equals("copper_bulb")) return "copper_bulb";
			if (base.equals("copper_grate")) return "copper_grate";
			if (base.equals("cut_copper_slab")) return "cut_copper_slab";
		}
		if (copperPath.equals("cut_copper") || copperPath.equals("copper_block") || copperPath.equals("copper")) return "copper_block";
		if (copperPath.equals("chiseled_copper")) return "chiseled_copper";
		if (copperPath.equals("copper_bulb")) return "copper_bulb";
		if (copperPath.equals("copper_grate")) return "copper_grate";
		if (copperPath.equals("cut_copper_slab")) return "cut_copper_slab";
		// snow_block → snow (1.7.10 使用同一个 snow block)
		if (path.equals("snow_block")) return "snow";
		// 铁砧变体（1.7.10 用 anvil meta 0/1/2 统配）
		if (path.equals("chipped_anvil") || path.equals("damaged_anvil")) {
			return "anvil";
		}
		// 雕刻南瓜 / 南瓜灯 / 岩浆块 / 地狱砖
		if (path.equals("carved_pumpkin")) return "pumpkin";
		if (path.equals("jack_o_lantern")) return "lit_pumpkin";
		if (path.equals("magma_block")) return "magma";
		if (path.equals("nether_bricks")) return "nether_brick";
		// nether_brick (1.21.4 物品) → netherbrick (1.7.10 物品注册名，无下划线)
		if (path.equals("nether_brick")) return "netherbrick";
		// 樱花木（mod 用 cherry_log meta 1 统配）
		if (path.equals("cherry_wood")) return "cherry_log";
		// 泥砖（mod 用 packed_mud meta 1）
		if (path.equals("mud_bricks")) return "packed_mud";
		// 磨制黑石变体（mod 用 blackstone/blackstone_slab/blackstone_wall 带 meta 统配）
		if (path.equals("polished_blackstone_bricks")) return "blackstone";
		if (path.equals("polished_blackstone_brick_slab") || path.equals("polished_blackstone_slab")) return "blackstone_slab";
		if (path.equals("polished_blackstone_wall")) return "blackstone_wall";
		// 蠹虫方块（1.7.10 用 monster_egg 统配）
		if (path.equals("infested_stone") || path.equals("infested_cobblestone") ||
			path.equals("infested_stone_bricks") || path.equals("infested_cracked_stone_bricks") ||
			path.equals("infested_mossy_stone_bricks") || path.equals("infested_chiseled_stone_bricks")) {
			return "monster_egg";
		}
		// 紫水晶芽/簇（mod 用 amethyst_cluster_1 / amethyst_cluster_2）
		if (path.equals("small_amethyst_bud") || path.equals("medium_amethyst_bud")) return "amethyst_cluster_1";
		if (path.equals("large_amethyst_bud") || path.equals("amethyst_cluster")) return "amethyst_cluster_2";
		// 1.7.10 命名差异
		if (path.equals("firework_star")) return "firework_charge";
		if (path.equals("firework_rocket")) return "fireworks";
		if (path.equals("powered_rail")) return "golden_rail";
		if (path.equals("stone_bricks")) return "stonebrick";
		if (path.equals("lily_pad")) return "waterlily";
		if (path.equals("sugar_cane")) return "reeds";
		if (path.equals("oak_boat")) return "boat";
		if (path.equals("spawner")) return "mob_spawner";
		if (path.equals("melon_slice")) return "melon";
		if (path.equals("glistering_melon_slice")) return "speckled_melon";
		if (path.equals("popped_chorus_fruit")) return "chorus_fruit_popped";
		if (path.equals("enchanted_golden_apple")) return "golden_apple";
		// 去皮樱花原木/木（mod 用 cherry_log meta 2/3 统配）
		if (path.equals("stripped_cherry_log") || path.equals("stripped_cherry_wood")) return "cherry_log";
		// 凝灰岩砖变体
		if (path.equals("tuff_brick_slab")) return "tuff_slab";
		if (path.equals("tuff_bricks")) return "tuff";
		// 海晶石变体
		if (path.equals("prismarine")) return "prismarine_block";
		if (path.equals("prismarine_bricks") || path.equals("dark_prismarine")) {
			return "prismarine_block";
		}
		if (path.equals("prismarine_brick_slab") || path.equals("dark_prismarine_slab")) {
			return "prismarine_slab";
		}
		// 海晶石楼梯（mod 用 prismarine_stairs_brick / prismarine_stairs_dark 命名）
		if (path.equals("prismarine_brick_stairs")) return "prismarine_stairs_brick";
		if (path.equals("dark_prismarine_stairs")) return "prismarine_stairs_dark";
		// 泥土变体
		if (path.equals("coarse_dirt") || path.equals("podzol")) {
			return "dirt";
		}
		// 头颅
		if (path.equals("skeleton_skull") || path.equals("wither_skeleton_skull") ||
			path.equals("zombie_head") || path.equals("player_head") ||
			path.equals("creeper_head") || path.equals("dragon_head")) {
			return "skull";
		}
		// 橡木物品 → 1.7.10 原版注册名（其他木材由 mod 直接注册为独立名）
		// 1.7.10 只有橡木变体是原版物品，其他木材（云杉/白桦/丛林/金合欢/深色橡）
		// 的 fence/door 等由 mod 注册为独立名称（如 spruce_fence），无需映射
		if (path.equals("oak_fence")) return "fence";
		if (path.equals("oak_fence_gate")) return "fence_gate";
		if (path.equals("oak_door")) return "wooden_door";
		if (path.equals("oak_trapdoor")) return "trapdoor";
		if (path.equals("oak_pressure_plate")) return "wooden_pressure_plate";
		if (path.equals("oak_button")) return "wooden_button";
		if (path.equals("oak_sign")) return "sign";
		// mod 新木材栅栏（cherry/bamboo 使用 WOOD_FENCE 统一块，带 metadata）
		if (path.equals("cherry_fence") || path.equals("bamboo_fence")) {
			return "wood_fence";
		}
		return null;
	}

	// ==================== Metadata 值提取 ====================

	/** 刷怪蛋：1.21.4 名称 → 1.7.10 entity ID（无对应实体的物品将跳过） */
	private static final java.util.Map<String, Integer> SPAWN_EGG_META = new java.util.HashMap<>();
	static {
		// === 原版 1.7.10 实体 ===
		SPAWN_EGG_META.put("creeper_spawn_egg", 50);
		SPAWN_EGG_META.put("skeleton_spawn_egg", 51);
		SPAWN_EGG_META.put("spider_spawn_egg", 52);
		SPAWN_EGG_META.put("zombie_spawn_egg", 54);
		SPAWN_EGG_META.put("slime_spawn_egg", 55);
		SPAWN_EGG_META.put("ghast_spawn_egg", 56);
		SPAWN_EGG_META.put("zombified_piglin_spawn_egg", 57); // 1.7.10: PigZombie
		SPAWN_EGG_META.put("enderman_spawn_egg", 58);
		SPAWN_EGG_META.put("cave_spider_spawn_egg", 59);
		SPAWN_EGG_META.put("silverfish_spawn_egg", 60);
		SPAWN_EGG_META.put("blaze_spawn_egg", 61);
		SPAWN_EGG_META.put("magma_cube_spawn_egg", 62);       // 1.7.10: LavaSlime
		SPAWN_EGG_META.put("bat_spawn_egg", 65);
		SPAWN_EGG_META.put("witch_spawn_egg", 66);
		SPAWN_EGG_META.put("pig_spawn_egg", 90);
		SPAWN_EGG_META.put("sheep_spawn_egg", 91);
		SPAWN_EGG_META.put("cow_spawn_egg", 92);
		SPAWN_EGG_META.put("chicken_spawn_egg", 93);
		SPAWN_EGG_META.put("squid_spawn_egg", 94);
		SPAWN_EGG_META.put("wolf_spawn_egg", 95);
		SPAWN_EGG_META.put("mooshroom_spawn_egg", 96);
		SPAWN_EGG_META.put("snow_golem_spawn_egg", 97);
		SPAWN_EGG_META.put("ocelot_spawn_egg", 98);
		SPAWN_EGG_META.put("iron_golem_spawn_egg", 99);
		SPAWN_EGG_META.put("horse_spawn_egg", 100);
		SPAWN_EGG_META.put("villager_spawn_egg", 120);
		// === Mod 新增实体（egg ID 从 500 开始）===
		SPAWN_EGG_META.put("rabbit_spawn_egg", 500);
		SPAWN_EGG_META.put("endermite_spawn_egg", 501);
		SPAWN_EGG_META.put("husk_spawn_egg", 502);
		SPAWN_EGG_META.put("stray_spawn_egg", 503);
		SPAWN_EGG_META.put("zombie_villager_spawn_egg", 504);
		SPAWN_EGG_META.put("shulker_spawn_egg", 505);
		SPAWN_EGG_META.put("bee_spawn_egg", 506);
		SPAWN_EGG_META.put("fox_spawn_egg", 507);
	}

	/** 铜块：1.21.4 名称 → copper_block meta (0-15) */
	private static final java.util.Map<String, Integer> COPPER_META = new java.util.HashMap<>();
	static {
		// 无蜡普通变体
		COPPER_META.put("copper_block", 0);
		COPPER_META.put("exposed_copper", 1);
		COPPER_META.put("weathered_copper", 2);
		COPPER_META.put("oxidized_copper", 3);
		// 无蜡切制变体
		COPPER_META.put("cut_copper", 4);
		COPPER_META.put("exposed_cut_copper", 5);
		COPPER_META.put("weathered_cut_copper", 6);
		COPPER_META.put("oxidized_cut_copper", 7);
		// 涂蜡普通变体
		COPPER_META.put("waxed_copper_block", 8);
		COPPER_META.put("waxed_exposed_copper", 9);
		COPPER_META.put("waxed_weathered_copper", 10);
		COPPER_META.put("waxed_oxidized_copper", 11);
		// 涂蜡切制变体
		COPPER_META.put("waxed_cut_copper", 12);
		COPPER_META.put("waxed_exposed_cut_copper", 13);
		COPPER_META.put("waxed_weathered_cut_copper", 14);
		COPPER_META.put("waxed_oxidized_cut_copper", 15);
	}

	/** 雕纹铜块：1.21.4 名称 → chiseled_copper meta (0-7) */
	private static final java.util.Map<String, Integer> CHISELED_COPPER_META = new java.util.HashMap<>();
	static {
		CHISELED_COPPER_META.put("chiseled_copper", 0);
		CHISELED_COPPER_META.put("exposed_chiseled_copper", 1);
		CHISELED_COPPER_META.put("weathered_chiseled_copper", 2);
		CHISELED_COPPER_META.put("oxidized_chiseled_copper", 3);
		CHISELED_COPPER_META.put("waxed_chiseled_copper", 4);
		CHISELED_COPPER_META.put("waxed_exposed_chiseled_copper", 5);
		CHISELED_COPPER_META.put("waxed_weathered_chiseled_copper", 6);
		CHISELED_COPPER_META.put("waxed_oxidized_chiseled_copper", 7);
	}

	/** 铜格栅：1.21.4 名称 → copper_grate meta (0-7) */
	private static final java.util.Map<String, Integer> COPPER_GRATE_META = new java.util.HashMap<>();
	static {
		COPPER_GRATE_META.put("copper_grate", 0);
		COPPER_GRATE_META.put("exposed_copper_grate", 1);
		COPPER_GRATE_META.put("weathered_copper_grate", 2);
		COPPER_GRATE_META.put("oxidized_copper_grate", 3);
		COPPER_GRATE_META.put("waxed_copper_grate", 4);
		COPPER_GRATE_META.put("waxed_exposed_copper_grate", 5);
		COPPER_GRATE_META.put("waxed_weathered_copper_grate", 6);
		COPPER_GRATE_META.put("waxed_oxidized_copper_grate", 7);
	}

	/** 铜灯：1.21.4 名称 → copper_bulb meta (0/1/2/3/8/9/10/11，无 lit 状态) */
	private static final java.util.Map<String, Integer> COPPER_BULB_META = new java.util.HashMap<>();
	static {
		COPPER_BULB_META.put("copper_bulb", 0);
		COPPER_BULB_META.put("exposed_copper_bulb", 1);
		COPPER_BULB_META.put("weathered_copper_bulb", 2);
		COPPER_BULB_META.put("oxidized_copper_bulb", 3);
		COPPER_BULB_META.put("waxed_copper_bulb", 8);
		COPPER_BULB_META.put("waxed_exposed_copper_bulb", 9);
		COPPER_BULB_META.put("waxed_weathered_copper_bulb", 10);
		COPPER_BULB_META.put("waxed_oxidized_copper_bulb", 11);
	}

	/** 切制铜台阶：1.21.4 名称 → cut_copper_slab meta (0-7) */
	private static final java.util.Map<String, Integer> CUT_COPPER_SLAB_META = new java.util.HashMap<>();
	static {
		CUT_COPPER_SLAB_META.put("cut_copper_slab", 0);
		CUT_COPPER_SLAB_META.put("exposed_cut_copper_slab", 1);
		CUT_COPPER_SLAB_META.put("weathered_cut_copper_slab", 2);
		CUT_COPPER_SLAB_META.put("oxidized_cut_copper_slab", 3);
		CUT_COPPER_SLAB_META.put("waxed_cut_copper_slab", 4);
		CUT_COPPER_SLAB_META.put("waxed_exposed_cut_copper_slab", 5);
		CUT_COPPER_SLAB_META.put("waxed_weathered_cut_copper_slab", 6);
		CUT_COPPER_SLAB_META.put("waxed_oxidized_cut_copper_slab", 7);
	}

	/** 木系方块（log/wood 组）：oak/spruce/birch/jungle → meta 0/1/2/3 */
	private static final String[] WOOD_LOG_GROUP_A = {"oak", "spruce", "birch", "jungle"};
	/** 木系方块（log2/bark2 组）：acacia/dark_oak → meta 0/1 */
	private static final String[] WOOD_LOG_GROUP_B = {"acacia", "dark_oak"};
	/** 木系方块（mod 新木材组）：cherry/bamboo → planks/slab meta 3/4, log meta 0 */
	private static final String[] WOOD_LOG_GROUP_C = {"cherry", "bamboo"};
	/** 木板/台阶 组（全部 6 种木头共用一个 base item）：oak～dark_oak → meta 0-5 */
	private static final String[] WOOD_PLANKS_GROUP = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak"};

	/**
	 * 根据 1.21.4 官方注册名提取对应的 1.7.10 metadata 值。
	 * 返回 -1 表示无法确定（应回退到 getSubItems() 全量添加）。
	 */
	private static int getMetaForOfficialName(String officialId) {
		int colon = officialId.indexOf(':');
		String path = colon >= 0 ? officialId.substring(colon + 1) : officialId;

		// === 鸡蛋变体（1.7.10 无独立 brown_egg/blue_egg，避免颜色前缀误匹配成 egg:11/12） ===
		if (path.equals("brown_egg") || path.equals("blue_egg")) return -2;

		// === 南瓜（1.7.10 原版 pumpkin 即 26.2 的 carved_pumpkin；26.2 未雕刻 pumpkin 未实现） ===
		if (path.equals("pumpkin")) return -2;
		if (path.equals("carved_pumpkin")) return 0;

		// === 新式染料（mod 用 dye_same 的 meta 0/1/2/3 区分） ===
		if (path.equals("white_dye")) return 0;
		if (path.equals("blue_dye")) return 1;
		if (path.equals("brown_dye")) return 2;
		if (path.equals("black_dye")) return 3;
		// === 青金石/骨粉/墨囊（1.7.10 是 minecraft:dye 的 meta 4/15/0） ===
		if (path.equals("lapis_lazuli")) return 4;
		if (path.equals("bone_meal")) return 15;
		if (path.equals("ink_sac")) return 0;
		// 其余 12 色染料 → 1.7.10 原版 minecraft:dye 的正确 meta
		// （1.7.10 dye meta：0=墨囊 1=红 2=绿 3=棕 4=蓝 5=紫 6=青 7=浅灰 8=灰
		//   9=粉 10=黄绿 11=黄 12=浅蓝 13=品红 14=橙 15=骨粉）
		if (path.equals("light_gray_dye")) return 7;
		if (path.equals("gray_dye")) return 8;
		if (path.equals("red_dye")) return 1;
		if (path.equals("orange_dye")) return 14;
		if (path.equals("yellow_dye")) return 11;
		if (path.equals("lime_dye")) return 10;
		if (path.equals("green_dye")) return 2;
		if (path.equals("cyan_dye")) return 6;
		if (path.equals("light_blue_dye")) return 12;
		if (path.equals("purple_dye")) return 5;
		if (path.equals("magenta_dye")) return 13;
		if (path.equals("pink_dye")) return 9;

		// === 烟花火箭（1.7.10 原版 fireworks，只显示飞行时间 3） ===
		if (path.equals("firework_rocket")) return 0;

		// === 紫水晶芽/簇（mod 用 amethyst_cluster_1/2 的 meta 0/6 区分） ===
		if (path.equals("small_amethyst_bud")) return 0;   // amethyst_cluster_1:0
		if (path.equals("medium_amethyst_bud")) return 6;   // amethyst_cluster_1:6
		if (path.equals("large_amethyst_bud")) return 0;   // amethyst_cluster_2:0
		if (path.equals("amethyst_cluster")) return 6;     // amethyst_cluster_2:6

		// === 刷怪蛋（优先匹配，返回 -2 表示 1.7.10 无此实体） ===
		if (path.endsWith("_spawn_egg") && !path.equals("spawn_egg")) {
			Integer id = SPAWN_EGG_META.get(path);
			return id != null ? id : -2;
		}

		// === 1.7.10 不存在的木材种类 → 跳过 ===
		if (isUnsupportedWood(path)) return -2;

		// === 铜块变体 ===
		Integer copper = COPPER_META.get(path);
		if (copper != null) return copper;
		copper = CHISELED_COPPER_META.get(path);
		if (copper != null) return copper;
		copper = COPPER_GRATE_META.get(path);
		if (copper != null) return copper;
		copper = COPPER_BULB_META.get(path);
		if (copper != null) return copper;
		copper = CUT_COPPER_SLAB_META.get(path);
		if (copper != null) return copper;

		// === 木系方块 meta ===
		// cherry_wood / bamboo_block 及去皮变体（不遵循 vanilla meta 规格，必须优先判断）
		if (path.equals("cherry_wood")) return 1;
		if (path.equals("stripped_cherry_log")) return 2;
		if (path.equals("stripped_cherry_wood")) return 3;
		if (path.equals("bamboo_block")) return 0;
		if (path.equals("stripped_bamboo_block")) return 1;
		int woodMeta = getWoodMeta(path);
		if (woodMeta >= 0) return woodMeta;
		if (path.startsWith("stripped_")) {
			int sMeta = getWoodMeta(path.substring("stripped_".length()));
			if (sMeta >= 0) return sMeta;
		}

		// === 树叶 ===
		if (path.endsWith("_leaves")) {
			return getLogGroupMeta(path.substring(0, path.length() - "_leaves".length()));
		}

		// === 树苗 ===
		if (path.endsWith("_sapling") && !path.equals("bamboo_sapling") && !path.equals("cherry_sapling")) {
			return getPlanksGroupMeta(path.substring(0, path.length() - "_sapling".length()));
		}

		// === 花 ===
		if (path.equals("poppy")) return 0;
		if (path.equals("blue_orchid")) return 1;
		if (path.equals("allium")) return 2;
		if (path.equals("azure_bluet")) return 3;
		if (path.equals("red_tulip")) return 4;
		if (path.equals("orange_tulip")) return 5;
		if (path.equals("white_tulip")) return 6;
		if (path.equals("pink_tulip")) return 7;
		if (path.equals("oxeye_daisy")) return 8;

		// === 低草丛/蕨（1.7.10 tallgrass meta 1=草丛, 2=蕨；meta 0=死灌木已废弃，避免与 dead_bush 重复显示） ===
		if (path.equals("short_grass")) return 1;
		if (path.equals("fern")) return 2;

		// === 高花 (double_plant) ===
		if (path.equals("sunflower")) return 0;
		if (path.equals("lilac")) return 1;
		if (path.equals("tall_grass")) return 2;
		if (path.equals("large_fern")) return 3;
		if (path.equals("rose_bush")) return 4;
		if (path.equals("peony")) return 5;

		// === 鱼 ===
		if (path.equals("cod")) return 0;
		if (path.equals("salmon")) return 1;
		if (path.equals("pufferfish")) return 2;
		if (path.equals("tropical_fish") || path.equals("clownfish")) return 3;

		// === 物品 meta ===
		if (path.equals("charcoal")) return 1;
		if (path.equals("red_sand")) return 1;
		if (path.equals("cooked_salmon")) return 1;
		if (path.equals("enchanted_golden_apple")) return 1;
		if (path.equals("wet_sponge")) return 1;
		if (path.equals("coarse_dirt")) return 1;
		if (path.equals("podzol")) return 2;

		// === 石头变体 (stone meta 0-6) ===
		if (path.equals("granite")) return 1;
		if (path.equals("polished_granite")) return 2;
		if (path.equals("diorite")) return 3;
		if (path.equals("polished_diorite")) return 4;
		if (path.equals("andesite")) return 5;
		if (path.equals("polished_andesite")) return 6;

		// === 石砖变体 (stonebrick meta 0-3) ===
		if (path.equals("mossy_stone_bricks")) return 1;
		if (path.equals("cracked_stone_bricks")) return 2;
		if (path.equals("chiseled_stone_bricks")) return 3;

		// === 粗矿（mod 用 raw_ore 统一注册，meta 0=铜、1=铁、2=金） ===
	if (path.equals("raw_copper")) return 0;
	if (path.equals("raw_iron")) return 1;
	if (path.equals("raw_gold")) return 2;

	// === 粗矿块（mod 用 raw_ore_block 统一注册，meta 0=铜、1=铁、2=金） ===
	if (path.equals("raw_copper_block")) return 0;
	if (path.equals("raw_iron_block")) return 1;
	if (path.equals("raw_gold_block")) return 2;

		// === 砂岩变体 ===
		if (path.equals("chiseled_sandstone")) return 1;
		if (path.equals("cut_sandstone") || path.equals("smooth_sandstone")) return 2;

		// === 红砂岩变体 ===
		if (path.equals("chiseled_red_sandstone")) return 1;
		if (path.equals("cut_red_sandstone") || path.equals("smooth_red_sandstone")) return 2;

		// === 石台阶 (stone_slab meta) ===
		if (path.equals("sandstone_slab")) return 1;
		if (path.equals("cobblestone_slab")) return 3;
		if (path.equals("brick_slab")) return 4;
		if (path.equals("stone_brick_slab")) return 5;
		if (path.equals("nether_brick_slab")) return 6;
		if (path.equals("quartz_slab")) return 7;

		// === 石台阶_2 (stone_slab_2 meta 0-5) ===
		if (path.equals("granite_slab")) return 0;
		if (path.equals("polished_granite_slab")) return 1;
		if (path.equals("diorite_slab")) return 2;
		if (path.equals("polished_diorite_slab")) return 3;
		if (path.equals("andesite_slab")) return 4;
		if (path.equals("polished_andesite_slab")) return 5;

		// === 墙 (stone_wall meta 0-3) ===
		if (path.equals("stone_brick_wall")) return 0;
		if (path.equals("mossy_stone_brick_wall")) return 1;
		if (path.equals("sandstone_wall")) return 2;
		if (path.equals("brick_wall")) return 3;

		// === 圆石墙（vanilla 1.7.10 cobblestone_wall meta 0-1） ===
		if (path.equals("mossy_cobblestone_wall")) return 1;
		if (path.equals("cobblestone_wall")) return 0;

		// === 墙_2 (stone_wall_2 meta 0-2) ===
		if (path.equals("granite_wall")) return 0;
		if (path.equals("diorite_wall")) return 1;
		if (path.equals("andesite_wall")) return 2;

		// === 深板岩变体 (deepslate_bricks meta 0-4) ===
		if (path.equals("cracked_deepslate_bricks")) return 1;
		if (path.equals("deepslate_tiles")) return 2;
		if (path.equals("cracked_deepslate_tiles")) return 3;
		if (path.equals("chiseled_deepslate")) return 4;

		// === 深板岩台阶 / 墙 ===
	if (path.equals("cobbled_deepslate_slab")) return 0;   // deepslate_slab meta 0
	if (path.equals("polished_deepslate_slab")) return 1;   // deepslate_slab meta 1
	if (path.equals("deepslate_tile_slab")) return 1;       // deepslate_brick_slab meta 1
	if (path.equals("cobbled_deepslate_wall")) return 0;    // deepslate_wall meta 0
	if (path.equals("polished_deepslate_wall")) return 1;    // deepslate_wall meta 1
	if (path.equals("deepslate_tile_wall")) return 1;        // deepslate_brick_wall meta 1

		// === 黑石变体 (blackstone meta 0-4) ===
		if (path.equals("polished_blackstone")) return 1;
		if (path.equals("polished_blackstone_bricks")) return 2;
		if (path.equals("cracked_polished_blackstone_bricks")) return 3;
		if (path.equals("chiseled_polished_blackstone")) return 4;
		// 黑石台阶 / 墙
		if (path.equals("polished_blackstone_slab")) return 1;           // blackstone_slab meta 1
		if (path.equals("polished_blackstone_brick_slab")) return 2;     // blackstone_slab meta 2
		if (path.equals("polished_blackstone_wall")) return 1;           // blackstone_wall meta 1
		if (path.equals("polished_blackstone_brick_wall")) return 2;     // blackstone_wall meta 2

		// === 凝灰岩变体 (tuff meta 0-4) ===
		if (path.equals("polished_tuff")) return 1;
		if (path.equals("tuff_bricks")) return 2;
		if (path.equals("chiseled_tuff")) return 3;
		if (path.equals("chiseled_tuff_bricks")) return 4;
		// 凝灰岩台阶 / 墙
		if (path.equals("polished_tuff_slab")) return 1;    // tuff_slab meta 1
		if (path.equals("tuff_brick_slab")) return 2;       // tuff_slab meta 2
		if (path.equals("polished_tuff_wall")) return 1;    // tuff_wall meta 1
		if (path.equals("tuff_brick_wall")) return 2;       // tuff_wall meta 2

		// === 海晶石变体 (prismarine_block meta 0-2) ===
		if (path.equals("prismarine_bricks")) return 1;
		if (path.equals("dark_prismarine")) return 2;
		// 海晶石台阶
		if (path.equals("prismarine_brick_slab")) return 1;     // prismarine_slab meta 1
		if (path.equals("dark_prismarine_slab")) return 2;      // prismarine_slab meta 2

		// === 下界砖变体 (red_nether_bricks meta) ===
		if (path.equals("cracked_nether_bricks")) return 1;
		if (path.equals("chiseled_nether_bricks")) return 2;

		// === 石英块变体 (quartz_block meta) ===
		if (path.equals("chiseled_quartz_block")) return 1;
		if (path.equals("quartz_pillar")) return 2;

		// === 红砂岩台阶 (red_sandstone_slab meta) ===
		if (path.equals("cut_red_sandstone_slab")) return 1;

		// === 头颅 ===
	if (path.equals("skeleton_skull")) return 0;
	if (path.equals("wither_skeleton_skull")) return 1;
	if (path.equals("zombie_head")) return 2;
	if (path.equals("player_head")) return 3;
	if (path.equals("creeper_head")) return 4;
	if (path.equals("dragon_head")) return 5;

	// === 铁砧变体 (anvil meta 0=正常, 1=轻微损坏, 2=严重损坏) ===
	if (path.equals("chipped_anvil")) return 1;
	if (path.equals("damaged_anvil")) return 2;

		// === 怪物蛋 (monster_egg meta) ===
		if (path.equals("infested_stone")) return 0;
		if (path.equals("infested_cobblestone")) return 1;
		if (path.equals("infested_stone_bricks")) return 2;
		if (path.equals("infested_mossy_stone_bricks")) return 3;
		if (path.equals("infested_cracked_stone_bricks")) return 4;
		if (path.equals("infested_chiseled_stone_bricks")) return 5;

		// === 橡木物品（1.7.10 只有橡木是原版 item，meta=0） ===
		if (path.equals("oak_fence")) return 0;
		if (path.equals("oak_fence_gate")) return 0;
		if (path.equals("oak_door")) return 0;
		if (path.equals("oak_trapdoor")) return 0;
		if (path.equals("oak_pressure_plate")) return 0;
		if (path.equals("oak_button")) return 0;
		if (path.equals("oak_sign")) return 0;

		// === 直接命中的 metadata 基础物品（默认变体 meta=0） ===
		if (path.equals("stone")) return 0;
		if (path.equals("stone_bricks") || path.equals("stonebrick")) return 0;
		if (path.equals("sandstone")) return 0;
		if (path.equals("red_sandstone")) return 0;
		if (path.equals("sponge")) return 0;
		if (path.equals("dirt")) return 0;
		if (path.equals("sand")) return 0;
		if (path.equals("coal")) return 0;
		if (path.equals("golden_apple")) return 0;
		if (path.equals("cooked_cod")) return 0;
		if (path.equals("planks")) return 0;
		if (path.equals("log")) return 0;
		if (path.equals("sapling")) return 0;
		if (path.equals("leaves")) return 0;
		if (path.equals("red_flower")) return 0;
		if (path.equals("fish")) return 0;
		if (path.equals("cooked_fished")) return 0;
		if (path.equals("skull")) return 0;
		if (path.equals("monster_egg")) return 0;
		if (path.equals("stained_glass")) return 0;
		if (path.equals("stained_glass_pane")) return 0;
		if (path.equals("stained_hardened_clay")) return 0;
		if (path.equals("carpet")) return 0;
		if (path.equals("wool")) return 0;
		if (path.equals("concrete")) return 0;
		if (path.equals("concrete_powder")) return 0;
		if (path.equals("glazed_terracotta")) return 0;
		if (path.equals("bed")) return 0;
		if (path.equals("shulker_box")) return 0;
		if (path.equals("banner")) return 0;
		if (path.equals("wooden_slab")) return 0;
		if (path.equals("double_plant")) return 0;
		if (path.equals("tallgrass")) return 0;
		if (path.equals("deepslate_bricks")) return 0;
		if (path.equals("blackstone")) return 0;
		if (path.equals("tuff")) return 0;
		if (path.equals("prismarine_block") || path.equals("prismarine")) return 0;
		if (path.equals("red_nether_bricks")) return 0;
		if (path.equals("quartz_block")) return 0;
		if (path.equals("smooth_stone")) return 0;
		if (path.equals("stone_slab")) return 0;
		if (path.equals("stone_slab_2")) return 0;
		if (path.equals("stone_wall")) return 0;
		if (path.equals("stone_wall_2")) return 0;
		if (path.equals("cobblestone_wall")) return 0;

		// === 颜色前缀 → meta 0-15（放在最后，避免误匹配 red_sandstone 等） ===
		for (int i = 0; i < COLOR_PREFIXES.length; i++) {
			if (path.startsWith(COLOR_PREFIXES[i])) return i;
		}

		return -1;
	}

	/**
	 * 从路径中提取木系 meta（log/wood/planks/slab 系列）。
	 */
	private static int getWoodMeta(String path) {
		for (int i = 0; i < WOOD_LOG_GROUP_A.length; i++) {
			String prefix = WOOD_LOG_GROUP_A[i] + "_";
			if (path.startsWith(prefix)) {
				String blockType = path.substring(prefix.length());
				if (blockType.equals("planks") || blockType.equals("slab")) return i;
				if (blockType.equals("log") || blockType.equals("wood")) return i;
				return -1;
			}
		}
		for (int i = 0; i < WOOD_LOG_GROUP_B.length; i++) {
			String prefix = WOOD_LOG_GROUP_B[i] + "_";
			if (path.startsWith(prefix)) {
				String blockType = path.substring(prefix.length());
				if (blockType.equals("planks") || blockType.equals("slab")) return 4 + i;
				if (blockType.equals("log") || blockType.equals("wood")) return i;
				return -1;
			}
		}
		// Mod 新木材（cherry/bamboo）：planks/slab → meta 3/4, log → meta 0
		for (int i = 0; i < WOOD_LOG_GROUP_C.length; i++) {
			String prefix = WOOD_LOG_GROUP_C[i] + "_";
			if (path.startsWith(prefix)) {
				String blockType = path.substring(prefix.length());
				if (blockType.equals("planks") || blockType.equals("slab") || blockType.equals("fence")) return 3 + i;
				if (blockType.equals("log")) return 0;
				if (blockType.equals("wood")) return 1; // cherry_wood → meta 1
				return -1;
			}
		}
		return -1;
	}

	/** 1.7.10 不存在的木材种类（应跳过，不映射到任何现有物品） */
	private static boolean isUnsupportedWood(String path) {
		// 剥离 stripped_ 前缀，让 stripped_mangrove_log 等也能被识别为不支持的木材
		if (path.startsWith("stripped_")) {
			path = path.substring("stripped_".length());
		}
		for (String prefix : new String[] {"mangrove_", "pale_oak_", "crimson_", "warped_"}) {
			if (path.startsWith(prefix)) return true;
		}
		return false;
	}

	/**
	 * log/wood 分组 meta（oak/spruce/birch/jungle=0-3, acacia/dark_oak=0-1）。
	 */
	private static int getLogGroupMeta(String wood) {
		for (int i = 0; i < WOOD_LOG_GROUP_A.length; i++) {
			if (WOOD_LOG_GROUP_A[i].equals(wood)) return i;
		}
		for (int i = 0; i < WOOD_LOG_GROUP_B.length; i++) {
			if (WOOD_LOG_GROUP_B[i].equals(wood)) return i;
		}
		return -1;
	}

	/**
	 * planks/slab 分组 meta（全部 6 种木头用同一个 base item，meta 0-5）。
	 */
	private static int getPlanksGroupMeta(String wood) {
		for (int i = 0; i < WOOD_PLANKS_GROUP.length; i++) {
			if (WOOD_PLANKS_GROUP[i].equals(wood)) return i;
		}
		return -1;
	}

	// ==================== 调试：F7 导出创造栏差异对比 ====================

	/**
	 * 对比官方 CreativeTabData 排序与游戏实际显示，输出所有差异到 creative_tab_dump.txt。
	 * 只输出不一致的部分：位置偏差、多余物品、缺失物品。
	 */
	public static void dumpAllTabs() {
		PrintWriter pw = null;
		try {
			File file = new File(Minecraft.getMinecraft().mcDataDir, "mods/creative_tab_dump.txt");
			pw = new PrintWriter(file, "UTF-8");

			pw.println("========================================================");
			pw.println("  Creative Tab Diff Report");
			pw.println("  对比官方 26.2 排序 vs 游戏实际显示");
			pw.println("  只列出不一致的部分");
			pw.println("========================================================");
			pw.println();

			CreativeTabs[] tabs = CreativeTabs.creativeTabArray;
			int totalMismatches = 0;
			int totalMissing = 0;
			int totalExtra = 0;

			for (int tabIdx = 0; tabIdx < tabs.length; tabIdx++) {
				CreativeTabs tab = tabs[tabIdx];
				if (tab == null) continue;
				String tabLabel = tab.getTabLabel();

				// 跳过搜索、生存物品栏等非 SortedCreativeTab 标签页
				List<String> officialItems = CreativeTabData.getItemsForTab(tabIdx);
				if (officialItems == null || officialItems.isEmpty()) continue;

				// --- 构建"期望列表"：官方排序 → 1.7.10 (regName, meta) ---
				List<String> expectedKeys = new ArrayList<>();
				List<String> expectedNames = new ArrayList<>(); // for display
				for (String itemId : officialItems) {
					int meta = getMetaForOfficialName(itemId);
					if (meta == -2) continue; // 1.7.10 不存在

					// 与 Part 1 一致的 lookup 顺序：先直接名，找到则 meta=0；否则走 baseName
				String regName = null;
				String baseName = null;
				Object obj = null;
				if (!FORCE_BASENAME_LOOKUP.contains(itemId)) {
					obj = lookupItem(itemId);
				}
				if (obj != null) {
					// 直接命中 mod 独立物品，meta=0
					meta = 0;
				} else {
					baseName = getBaseItemName(itemId);
					if (baseName != null) {
						obj = lookupItem(baseName);
						if (meta < 0) meta = 0;
						regName = baseName;
					}
				}
				if (obj == null) {
					obj = lookupItem(itemId);
				}
					if (obj instanceof Item) {
				if (regName == null) {
					regName = Item.itemRegistry.getNameForObject((Item) obj);
				}
				if (meta < 0) meta = 0;
				// 用 display name 作为 key，绕过 meta/NBT/独立物品等数据格式差异
				ItemStack stack = new ItemStack((Item) obj, 1, meta);
				// 特殊处理：shulker_box 的颜色变体通过 NBT Color 区分
				if (regName.equals("minecraft:shulker_box")) {
					int color;
					if (itemId.equals("minecraft:shulker_box")) {
						color = 0;
					} else {
						color = meta + 1;  // COLOR_PREFIXES index + 1 = NBT Color
					}
					if (color > 0) {
						NBTTagCompound tag = new NBTTagCompound();
						tag.setByte("Color", (byte) color);
						stack.setTagCompound(tag);
					}
				}
				String displayName = stack.getDisplayName();
				expectedKeys.add(displayName);
				expectedNames.add(itemId + " [" + displayName + "]");
			}
			}

			// --- 构建"实际列表"：游戏实际显示 ---
				List actualItems = new ArrayList();
				tab.displayAllReleventItems(actualItems);

				List<String> actualKeys = new ArrayList<>();   // display name
				List<String> actualNames = new ArrayList<>();  // for display
				for (Object obj : actualItems) {
					ItemStack stack = (ItemStack) obj;
					String displayName = stack.getDisplayName();
				actualKeys.add(displayName);
				actualNames.add(String.format("[%d] %s", actualNames.size(), displayName));
				}

				// --- 对比（用队列按顺序匹配重复的 display name，如多个"音乐唱片"） ---
			Map<String, LinkedList<Integer>> actualPosMap = new LinkedHashMap<>();
			for (int i = 0; i < actualKeys.size(); i++) {
				actualPosMap.computeIfAbsent(actualKeys.get(i), k -> new LinkedList<>()).add(i);
			}

			List<String> missing = new ArrayList<>();
			List<String> mismatches = new ArrayList<>();
			Set<Integer> matchedActualPos = new HashSet<>();

			for (int i = 0; i < expectedKeys.size(); i++) {
				String key = expectedKeys.get(i);
				LinkedList<Integer> positions = actualPosMap.get(key);
				if (positions != null && !positions.isEmpty()) {
					int actualPos = positions.removeFirst();
					matchedActualPos.add(actualPos);
					if (actualPos != i) {
						int diff = actualPos - i;
						mismatches.add(String.format("  expected[%d] → actual[%d] (偏移%+d)  %s  vs  %s",
								i, actualPos, diff, expectedNames.get(i), actualNames.get(actualPos)));
					}
				} else {
					missing.add(expectedNames.get(i));
				}
			}

			// 多余：actual 中未被匹配的位置
			List<String> extra = new ArrayList<>();
			for (int i = 0; i < actualKeys.size(); i++) {
				if (!matchedActualPos.contains(i)) {
					extra.add(actualNames.get(i));
			}
		}

			// 仅当有差异时才输出
			if (mismatches.isEmpty() && missing.isEmpty() && extra.isEmpty()) continue;

				pw.println("========================================");
				pw.println("Tab #" + tabIdx + ": " + tabLabel);
				pw.println("========================================");
				pw.println("Official (resolved): " + expectedKeys.size() + "  Actual: " + actualKeys.size());
				pw.println();

				if (!mismatches.isEmpty()) {
					pw.println("--- POSITION MISMATCHES (" + mismatches.size() + ") ---");
					for (String s : mismatches) {
						pw.println(s);
					}
					pw.println();
					totalMismatches += mismatches.size();
				}

				if (!missing.isEmpty()) {
					pw.println("--- MISSING (in official but not in game) (" + missing.size() + ") ---");
					for (String s : missing) {
						pw.println("  " + s);
					}
					pw.println();
					totalMissing += missing.size();
				}

				if (!extra.isEmpty()) {
					pw.println("--- EXTRA (in game but not in official) (" + extra.size() + ") ---");
					for (String s : extra) {
						pw.println("  " + s);
					}
					pw.println();
					totalExtra += extra.size();
				}
			}

			// ==================== 分类之外（TEMPORARY）内容 ====================
			CreativeTabs tempTab = ModdedCreativeTabs.TEMPORARY;
			if (tempTab != null) {
				pw.println("========================================================");
				pw.println("Tab #" + tempTab.getTabIndex() + ": " + tempTab.getTabLabel() + " (分类之外)");
				pw.println("========================================================");
				List<ItemStack> tempItems = new ArrayList<>();
				for (Object obj : Item.itemRegistry) {
					if (obj instanceof Item) {
						Item item = (Item) obj;
						if (item.getCreativeTab() == tempTab) {
							item.getSubItems(item, tempTab, tempItems);
						}
					}
				}
				pw.println("Total items: " + tempItems.size());
				for (ItemStack stack : tempItems) {
					String regName = Item.itemRegistry.getNameForObject(stack.getItem());
					String displayName = stack.getDisplayName();
					pw.println("  " + regName + " (meta=" + stack.getItemDamage() + ") [" + displayName + "]");
				}
				pw.println();
			}

			// 汇总
			pw.println("========================================================");
			pw.println("  SUMMARY");
			pw.println("========================================================");
			pw.println("Total position mismatches: " + totalMismatches);
			pw.println("Total missing items:       " + totalMissing);
			pw.println("Total extra items:         " + totalExtra);
			pw.println("Total issues:              " + (totalMismatches + totalMissing + totalExtra));

			pw.close();
			Logger.info("Creative tab diff written to: " + file.getAbsolutePath());
		} catch (Exception e) {
			e.printStackTrace();
			if (pw != null) pw.close();
		}
	}

	// ==================== 调试：F7 输出未显示在创造栏的物品 ====================

	/**
	 * 从"功能代码角度"检测缺失物品：
	 * 遍历每个 SortedCreativeTab 的 CreativeTabData 条目，
	 * 用与 displayAllReleventItems() Part 1 相同的 lookup 路径尝试解析，
	 * 输出"条目存在但无法解析到实际物品"的差距。
	 * <p>
	 * 相比遍历 ItemRegistry 的方式，这种方式：
	 * - 不会被双台阶/技术方块等干扰
	 * - 不会被 CreativeTabData 中大量预留条目干扰
	 * - 直接从代码功能层面暴露映射缺失
	 */
	public static void dumpNotInCreative() {
		PrintWriter pw = null;
		try {
			File file = new File(Minecraft.getMinecraft().mcDataDir, "mods/creative_tab_missing.txt");
			pw = new PrintWriter(file, "UTF-8");

			pw.println("========================================================");
			pw.println("  Missing Report (仅关注已实现但未显示的内容)");
			pw.println("  (未实现的官方条目只统计总数，不逐条列出)");
			pw.println("========================================================");
			pw.println();

			int totalGaps = 0;

			CreativeTabs[] tabs = CreativeTabs.creativeTabArray;

			// 遍历每个 creative tab index，跳过没有 CreativeTabData 的标签页
			for (int tabIdx = 0; tabIdx < tabs.length; tabIdx++) {
				List<String> officialItems = CreativeTabData.getItemsForTab(tabIdx);
				if (officialItems == null || officialItems.isEmpty()) continue;

				for (String itemId : officialItems) {
					// 用与 displayAllReleventItems() Part 1 完全一致的路径检测
					int meta = getMetaForOfficialName(itemId);
					if (meta == -2) continue; // isUnsupportedWood / 不存在于 1.7.10

					Item obj = null;

					// Part 1 路径：先直接 lookup（除非 FORCE_BASENAME_LOOKUP），
					// 失败则走 baseName 映射，最后回退直接 lookup。
					// lookupItem 内部会处理 getNameAlias。
					if (!FORCE_BASENAME_LOOKUP.contains(itemId)) {
						obj = (Item) lookupItem(itemId);
					}
					if (obj == null) {
						String baseName = getBaseItemName(itemId);
						if (baseName != null) {
							obj = (Item) lookupItem(baseName);
						}
					}
					if (obj == null) {
						obj = (Item) lookupItem(itemId);
					}

					if (obj == null) {
						totalGaps++;
					}
				}
			}

			if (totalGaps == 0) {
				pw.println("All CreativeTabData entries resolve to items successfully.");
				pw.println();
			}

			// Step 2: 检查"已解析但实际不显示"的物品
			// (物品能被 lookupItem 找到，但 displayAllReleventItems 没把它加进去)
			// 这种通常是因为 getSubItems 没返回正确 meta 或者物品被 skip
			pw.println("========================================================");
			pw.println("  Resolved Items NOT Actually Displayed In Their Tab");
			pw.println("  (Item found by lookupItem but not in displayAllReleventItems output)");
			pw.println("========================================================");
			pw.println();

			int notDisplayed = 0;
			for (int tabIdx = 0; tabIdx < tabs.length; tabIdx++) {
				List<String> officialItems = CreativeTabData.getItemsForTab(tabIdx);
				if (officialItems == null || officialItems.isEmpty()) continue;
				CreativeTabs tab = tabs[tabIdx];
				if (tab == null) continue;

				// 收集此 tab 实际显示的物品列表
			List displayList = new ArrayList();
			tab.displayAllReleventItems(displayList);
			Set<String> displayedKeys = new HashSet<>();
			for (Object o : displayList) {
				ItemStack stack = (ItemStack) o;
				if (stack == null || stack.getItem() == null) continue;
				String regName = Item.itemRegistry.getNameForObject(stack.getItem());
				displayedKeys.add(regName + "@" + stack.getItemDamage());
				// Shulker box 颜色变体通过 NBT Color 区分，不是 meta
				if (regName.equals("minecraft:shulker_box") && stack.getTagCompound() != null) {
					int color = stack.getTagCompound().getByte("Color");
					displayedKeys.add(regName + "#color=" + color);
				}
			}

				for (String itemId : officialItems) {
				int meta = getMetaForOfficialName(itemId);
				if (meta == -2) continue;

				// 与 Part 1 一致的 lookup 路径（含 FORCE_BASENAME_LOOKUP）
				Item obj = null;
				String baseName = null;

				if (!FORCE_BASENAME_LOOKUP.contains(itemId)) {
					obj = (Item) lookupItem(itemId);
				}
				if (obj != null) {
					meta = 0;
				} else {
					baseName = getBaseItemName(itemId);
					if (baseName != null) {
						obj = (Item) lookupItem(baseName);
					}
				}
				if (obj == null) {
					obj = (Item) lookupItem(itemId);
				}

				if (obj != null) {
					String finalRegName = Item.itemRegistry.getNameForObject(obj);
					int finalMeta = (meta < 0) ? 0 : meta;
					String checkKey = finalRegName + "@" + finalMeta;
					// Shulker box 颜色变体用 NBT Color 检查，不是 meta；
					// 无色变体 (color=0) 无 NBT tag，仍用 @0 匹配
					if (finalRegName.equals("minecraft:shulker_box")) {
						int color = itemId.equals("minecraft:shulker_box") ? 0 : finalMeta + 1;
						checkKey = color == 0 ? finalRegName + "@0" : finalRegName + "#color=" + color;
					}
					if (!displayedKeys.contains(checkKey)) {
						notDisplayed++;
						pw.println(String.format("  [%s] %s → %s (meta=%d) — resolved but not displayed",
							tab.getTabLabel(), itemId, finalRegName, finalMeta));
					}
				}
			}
			}

			if (notDisplayed == 0) {
				pw.println("All resolved items are displayed correctly.");
				pw.println();
			}

			pw.println("========================================================");
			pw.println("  TOTAL NOT IMPLEMENTED (官方有但 mod 未实现): " + totalGaps);
			pw.println("  TOTAL IMPLEMENTED BUT NOT DISPLAYED (已实现但未显示): " + notDisplayed);
			pw.println("========================================================");

			pw.close();
			Logger.info("Missing items written to: " + file.getAbsolutePath());
		} catch (Exception e) {
			e.printStackTrace();
			if (pw != null) pw.close();
		}
	}

	// ==================== 药水 1.21.4 官方排序 ====================

	/**
	 * 1.21.4 官方药水注册顺序（仅效果药水，基础药水 water/mundane/thick/awkward 单独处理）。
	 * 每个 Entry: {Potion.id, variant}  variant: 0=普通, 1=延长(long_), 2=强化(strong_/II)
	 * <p>
	 * 顺序来源: BuiltInRegistries.POTION 注册顺序 (Potions.bootstrap)。
	 * 注意: 1.7.10 的 ItemPotion.getSubItems() 按 metadata 顺序返回，与 1.21.4 不同。
	 */
	private static List<int[]> getPotionOrder121() {
		List<int[]> order = new ArrayList<>();
		// night_vision
		order.add(new int[]{Potion.nightVision.id, 0});
		order.add(new int[]{Potion.nightVision.id, 1});
		// invisibility
		order.add(new int[]{Potion.invisibility.id, 0});
		order.add(new int[]{Potion.invisibility.id, 1});
		// leaping
		order.add(new int[]{Potion.jump.id, 0});
		order.add(new int[]{Potion.jump.id, 1});
		order.add(new int[]{Potion.jump.id, 2});
		// fire_resistance
		order.add(new int[]{Potion.fireResistance.id, 0});
		order.add(new int[]{Potion.fireResistance.id, 1});
		// swiftness
		order.add(new int[]{Potion.moveSpeed.id, 0});
		order.add(new int[]{Potion.moveSpeed.id, 1});
		order.add(new int[]{Potion.moveSpeed.id, 2});
		// slowness
		order.add(new int[]{Potion.moveSlowdown.id, 0});
		order.add(new int[]{Potion.moveSlowdown.id, 1});
		order.add(new int[]{Potion.moveSlowdown.id, 2});
		// water_breathing
		order.add(new int[]{Potion.waterBreathing.id, 0});
		order.add(new int[]{Potion.waterBreathing.id, 1});
		// healing (instant, no long)
		order.add(new int[]{Potion.heal.id, 0});
		order.add(new int[]{Potion.heal.id, 2});
		// harming (instant, no long)
		order.add(new int[]{Potion.harm.id, 0});
		order.add(new int[]{Potion.harm.id, 2});
		// poison
		order.add(new int[]{Potion.poison.id, 0});
		order.add(new int[]{Potion.poison.id, 1});
		order.add(new int[]{Potion.poison.id, 2});
		// regeneration
		order.add(new int[]{Potion.regeneration.id, 0});
		order.add(new int[]{Potion.regeneration.id, 1});
		order.add(new int[]{Potion.regeneration.id, 2});
		// strength
		order.add(new int[]{Potion.damageBoost.id, 0});
		order.add(new int[]{Potion.damageBoost.id, 1});
		order.add(new int[]{Potion.damageBoost.id, 2});
		// weakness
		order.add(new int[]{Potion.weakness.id, 0});
		order.add(new int[]{Potion.weakness.id, 1});
		return order;
	}

	/**
	 * 1.7.10 原版药水的基础持续时间（ticks），用于区分延长版 (long_)。
	 * 即时型药水 (healing, harming) 返回 -1。
	 */
	private static int getBasePotionDuration(int potionId) {
		switch (potionId) {
			case 1:  // moveSpeed (swiftness) 3:00
			case 5:  // damageBoost (strength) 3:00
			case 8:  // jump (leaping) 3:00
			case 12: // fireResistance 3:00
			case 13: // waterBreathing 3:00
			case 14: // invisibility 3:00
			case 16: // nightVision 3:00
				return 3600;
			case 2:  // moveSlowdown (slowness) 1:30
			case 18: // weakness 1:30
				return 1800;
			case 10: // regeneration 0:45
			case 19: // poison 0:45
				return 900;
			default:
				return -1; // 即时或未知
		}
	}

	/**
	 * 从药水 ItemStack 生成排序 key: "potionId:variant"
	 * variant: "normal", "long", "strong"
	 * 基础药水（无效果）返回 "base:meta"
	 */
	private static String getPotionSortKey(ItemStack stack) {
		int meta = stack.getItemDamage();
		boolean isSplash = (meta & 16384) != 0; // bit 14 = splash
		@SuppressWarnings("unchecked")
		List<PotionEffect> effects = PotionHelper.getPotionEffects(meta, isSplash);
		if (effects == null || effects.isEmpty()) {
			return "base:" + (meta & 0x3FFF); // 基础药水（去掉 splash bit）
		}
		PotionEffect first = effects.get(0);
		int potionId = first.getPotionID();
		int amplifier = first.getAmplifier();
		int duration = first.getDuration();
		int baseDur = getBasePotionDuration(potionId);
		String variant;
		if (amplifier > 0) {
			variant = "strong";
		} else if (baseDur > 0 && duration > baseDur) {
			variant = "long";
		} else {
			variant = "normal";
		}
		return potionId + ":" + variant;
	}

	/**
	 * 按官方 1.21.4 顺序生成药水 ItemStack 列表。
	 * <p>
	 * 1.21.4 Food & Drinks 标签页药水排列：
	 * 1) 饮用药水（water, mundane, thick, awkward, 然后效果药水按官方顺序）
	 * 2) 喷溅药水（同顺序，无基础药水）
	 * 3) 滞留药水（同顺序，无基础药水）
	 * <p>
	 * 对于 tipped_arrow（COMBAT 标签页），只有效果药水，顺序同上。
	 *
	 * @param potionItem  药水 Item（Items.potion / ItemLingeringPotion / ItemArrowTipped）
	 * @param tab         当前 CreativeTab
	 * @param isVanillaPotion  true=minecraft:potion（getSubItems 返回饮用+喷溅混合），
	 *                         false=其他（lingering/tipped_arrow，只返回非喷溅）
	 */
	private static List<ItemStack> generatePotionsIn121Order(Item potionItem, CreativeTabs tab, boolean isVanillaPotion) {
		List<ItemStack> all = new ArrayList<>();
		potionItem.getSubItems(potionItem, tab, all);

		List<ItemStack> basePotions = new ArrayList<>();
		Map<String, ItemStack> drinkableMap = new LinkedHashMap<>();
		Map<String, ItemStack> splashMap = new LinkedHashMap<>();

		for (ItemStack stack : all) {
			String key = getPotionSortKey(stack);
			boolean isSplash = (stack.getItemDamage() & 16384) != 0;
			if (key.startsWith("base:")) {
				if (!isSplash) basePotions.add(stack);
			} else {
				Map<String, ItemStack> target = isSplash ? splashMap : drinkableMap;
				if (!target.containsKey(key)) target.put(key, stack);
			}
		}

		List<ItemStack> result = new ArrayList<>();
		String[] variants = {"normal", "long", "strong"};
		List<int[]> order = getPotionOrder121();

		if (isVanillaPotion) {
			// 1. 饮用药水：基础药水 + 效果药水（官方顺序）
			// 基础药水按 metadata 排序 (water=0 先)
			List<ItemStack> sortedBase = new ArrayList<>(basePotions);
			sortedBase.sort((a, b) -> Integer.compare(a.getItemDamage() & 0x3FFF, b.getItemDamage() & 0x3FFF));
			result.addAll(sortedBase);
			for (int[] entry : order) {
				String key = entry[0] + ":" + variants[entry[1]];
				ItemStack s = drinkableMap.remove(key);
				if (s != null) result.add(s);
			}
			result.addAll(drinkableMap.values()); // 剩余（如 levitation）

			// 2. 喷溅药水：效果药水（官方顺序，无基础药水）
			for (int[] entry : order) {
				String key = entry[0] + ":" + variants[entry[1]];
				ItemStack s = splashMap.remove(key);
				if (s != null) result.add(s);
			}
			result.addAll(splashMap.values());
		} else {
			// lingering / tipped_arrow：只有效果药水，无基础药水，无喷溅
			for (int[] entry : order) {
				String key = entry[0] + ":" + variants[entry[1]];
				ItemStack s = drinkableMap.remove(key);
				if (s != null) result.add(s);
			}
			result.addAll(drinkableMap.values());
		}

		return result;
	}

	/**
	 * 返回所有被 CreativeTabData 映射系统用作 base item 的注册名。
	 * 这些物品不被直接包含在 CreativeTabData 中，但会被 SortedCreativeTab 的
	 * displayAllReleventItems 通过 baseName 映射找到并正确显示在对应标签页中。
	 * 例如：minecraft:concrete_powder（是 white_concrete_powder 的 base）
	 *      minecraft:planks（是 oak_planks 的 base）
	 */
	public static Set<String> getCreativeTabBaseItemIds() {
		Set<String> bases = new HashSet<>();
		for (String itemId : CreativeTabData.getAllItemIds()) {
			String base = getBaseItemName(itemId);
			if (base != null) {
				bases.add(base);
			}
			// 也检查 getNameAlias 映射（如 armor_stand → wooden_armorstand）
			String alias = getNameAlias(itemId);
			if (alias != null) {
				bases.add(alias);
			}
		}
		return bases;
	}

	// ==================== 内部类：可排序的 CreativeTab ====================

	/**
	 * 自定义 CreativeTab 基类。
	 * displayAllReleventItems 直接遍历 CreativeTabData 中的官方物品列表，
	 * 按官方顺序逐个添加已注册的物品，未实现的物品跳过。
	 * 同一物品可在多个标签页出现（如门同时在建筑和红石分类）。
	 */
	private abstract static class SortedCreativeTab extends CreativeTabs {

		private final int creativeTabIndex;
		/** 对应的原版 CreativeTab（用于接管其他 mod 物品），可为 null */
		private final CreativeTabs vanillaSourceTab;

		public SortedCreativeTab(int index, String label) {
			this(index, label, null);
		}

		public SortedCreativeTab(int index, String label, CreativeTabs vanillaSourceTab) {
			super(index, label);
			this.creativeTabIndex = index;
			this.vanillaSourceTab = vanillaSourceTab;
		}

		@Override
		@SuppressWarnings("unchecked")
		public void displayAllReleventItems(List itemStacks) {
			List<String> officialItems = CreativeTabData.getItemsForTab(creativeTabIndex);
			Set<Item> addedItems = new HashSet<>();

		// Part 1: 按官方顺序遍历官方数据，添加已注册的物品
		Set<String> addedMetaKeys = new HashSet<>();

		for (String itemId : officialItems) {
			int meta = getMetaForOfficialName(itemId);
			if (meta == -2) continue; // 1.7.10 中不存在（如 axolotl_spawn_egg）

			// 优先 lookup 直接名（mod 注册的独立物品，如 granite, red_sandstone, white_bed），
		// 找到则用 meta=0（独立物品无 meta 变体）。
		// 只有 mod 没注册独立物品时，才走 baseName 映射（如 red_bed → bed:14）。
		// 例外：grass 等物品必须走 baseName（避免命中 1.7.10 同名但不同的物品）。
		String baseName = null;
		Object obj = null;
		if (!FORCE_BASENAME_LOOKUP.contains(itemId)) {
			obj = lookupItem(itemId);
		}
		if (obj != null) {
			// 直接命中 mod 独立物品，meta=0
			meta = 0;
		} else {
			// 走 baseName 映射（1.7.10 metadata 物品，如 red_bed → bed:14）
			baseName = getBaseItemName(itemId);
			if (baseName != null) {
				obj = lookupItem(baseName);
			}
		}
		if (obj == null) {
			// baseName 也失败，最后尝试直接 lookup（原版同名 item）
			obj = lookupItem(itemId);
		}

			if (obj instanceof Item) {
			Item item = (Item) obj;
			// 跳过已直接设到"分类之外"标签页的创意专用物品（如下界合金楼梯）
			if (item.getCreativeTab() == TEMPORARY) continue;
			// 特殊处理：shulker_box 的颜色变体通过 NBT tag "Color" 区分（不是 meta）。
			// Part 1 按 official 列表逐条添加，每条用 NBT Color 创建 ItemStack。
			// official 列表：shulker_box (无色 color=0), white_shulker_box (color=1), ..., black_shulker_box (color=16)
			// 注意 getMetaForOfficialName 对颜色前缀返回 0-15（COLOR_PREFIXES index），需 +1 转为 NBT Color。
			String effectiveBase = baseName != null ? baseName : itemId;
			if (effectiveBase.equals("minecraft:shulker_box")) {
				int color;
				if (itemId.equals("minecraft:shulker_box")) {
					color = 0;  // 无色（默认）
				} else {
					color = meta + 1;  // 染色变体：COLOR_PREFIXES index + 1 = NBT Color
				}
				String colorKey = "minecraft:shulker_box#color=" + color;
				if (addedMetaKeys.contains(colorKey)) continue;
				addedMetaKeys.add(colorKey);
				ItemStack stack = new ItemStack(item, 1, 0);
				if (color > 0) {
					NBTTagCompound tag = new NBTTagCompound();
					tag.setByte("Color", (byte) color);
					stack.setTagCompound(tag);
				}
				itemStacks.add(stack);
				continue;
			}
			if (effectiveBase.equals("minecraft:fireworks")) {
				// 烟花火箭：只显示飞行时间 3 的（与现代版本一致，NBT 构造等同 3 火药合成）
				String fwKey = "minecraft:fireworks#flight=3";
				if (addedMetaKeys.contains(fwKey)) continue;
				addedMetaKeys.add(fwKey);
				ItemStack stack = new ItemStack(item, 1, 0);
				NBTTagCompound tag = new NBTTagCompound();
				NBTTagCompound fw = new NBTTagCompound();
				fw.setByte("Flight", (byte) 3);
				fw.setTag("Explosions", new NBTTagList());
				tag.setTag("Fireworks", fw);
				stack.setTagCompound(tag);
				itemStacks.add(stack);
				continue;
			}
			if (meta >= 0) {
					// 精确 meta：只添加该变体（per-meta 去重）
					String metaKey = (baseName != null ? baseName : itemId) + "@" + meta;
					if (addedMetaKeys.contains(metaKey)) continue;
					addedMetaKeys.add(metaKey);
					itemStacks.add(new ItemStack(item, 1, meta));
				} else if (!addedItems.contains(item)) {
					addedItems.add(item);
					if ("spawn_egg".equals(baseName)) {
						// 刷怪蛋：official 只列 1 个条目，需展开全部生物变体
						item.getSubItems(item, this, itemStacks);
					} else {
						// 其他：只添加 meta=0，避免 getSubItems dump 全部变体造成连锁错位
						// （变种靠 official 列表各自的 meta 映射单独添加；遗漏只导致单个 missing）
						itemStacks.add(new ItemStack(item, 1, 0));
					}
				}
			}
			// 谜之炖菜：官方数据跳过 NBT 变体，需在 rabbit_stew(39) 后、milk_bucket(40) 前手动展开
			if (itemId.equals("minecraft:rabbit_stew")) {
				Item suspiciousStew = (Item) Item.itemRegistry.getObject("minecraft:suspicious_stew");
				if (suspiciousStew != null) {
					for (ItemStack flower : ModRecipes.getStewFlowers()) {
						PotionEffect effect = EtFuturum.getSuspiciousStewEffect(flower);
						if (effect == null) continue;
						String key = "minecraft:suspicious_stew@" + effect.getPotionID() + ":" + effect.getDuration();
						if (addedMetaKeys.contains(key)) continue;
						addedMetaKeys.add(key);
						ItemStack stew = new ItemStack(suspiciousStew, 1, 0);
						NBTTagCompound tag = new NBTTagCompound();
						NBTTagList effects = new NBTTagList();
						NBTTagCompound eff = new NBTTagCompound();
						eff.setByte(ItemSuspiciousStew.stewEffect, (byte) effect.getPotionID());
						eff.setInteger(ItemSuspiciousStew.stewEffectDuration, effect.getDuration());
						effects.appendTag(eff);
						tag.setTag(ItemSuspiciousStew.effectsList, effects);
						stew.setTagCompound(tag);
						itemStacks.add(stew);
					}
				}
			}
			// If not registered, skip
		}

			// Part 2: [临时禁用] 接管其他 mod 物品
			Set<String> existingKeys = new HashSet<>();
			for (Object o : itemStacks) {
				ItemStack s = (ItemStack) o;
				existingKeys.add(Item.itemRegistry.getNameForObject(s.getItem()) + "@" + s.getItemDamage());
			}
			// if (vanillaSourceTab != null) {
			// 	List vanillaItems = new ArrayList();
			// 	vanillaSourceTab.displayAllReleventItems(vanillaItems);
			// 	for (Object o : vanillaItems) {
			// 		ItemStack stack = (ItemStack) o;
			// 		String name = Item.itemRegistry.getNameForObject(stack.getItem());
			// 		if (name == null || name.startsWith("etfuturum:")) continue;
			// 		String key = name + "@" + stack.getItemDamage();
			// 		if (!existingKeys.contains(key)) {
			// 			existingKeys.add(key);
			// 			itemStacks.add(stack);
			// 		}
			// 	}
			// }

			// Part 3: 特殊物品（不在 official 数据中，需要手动展开变体）
			// 官方 1.21.4 分类：附魔书 → INGREDIENTS，药水 → FOOD_AND_DRINKS，药水箭 → COMBAT
			// 各物品的 getCreativeTab() 已在 reassignAllItems() 或构造函数中设置到对应 tab，
			// 因此以下逻辑只在 this == 对应 tab 时触发。

			// 附魔书变体（ItemEnchantedBook 没有重写 getSubItems，需要手动 NBT 添加）
			// 官方 Ingredients 标签页只显示最高等级（搜索栏可见所有等级）
			Item enchantedBook = (Item) Item.itemRegistry.getObject("minecraft:enchanted_book");
			if (enchantedBook != null && enchantedBook.getCreativeTab() == this) {
				for (Enchantment ench : Enchantment.enchantmentsBookList) {
					if (ench != null) {
						for (int lvl = ench.getMinLevel(); lvl <= ench.getMaxLevel(); lvl++) {
							ItemStack bookStack = new ItemStack(enchantedBook, 1, 0);
							NBTTagCompound nbt = new NBTTagCompound();
							NBTTagList list = new NBTTagList();
							NBTTagCompound enchTag = new NBTTagCompound();
							enchTag.setShort("id", (short) ench.effectId);
							enchTag.setShort("lvl", (short) lvl);
							list.appendTag(enchTag);
							nbt.setTag("StoredEnchantments", list);
							bookStack.setTagCompound(nbt);
							// 用附魔 ID + 等级作为 key 去重
							String key = "enchanted_book@" + ench.effectId + ":" + lvl;
							if (!existingKeys.contains(key)) {
								existingKeys.add(key);
								itemStacks.add(bookStack);
							}
						}
					}
				}
			}

			// 药水类（potion, splash_potion, lingering_potion）- 按 1.21.4 官方顺序排列
			// minecraft:potion 的 getSubItems 返回饮用+喷溅混合，需分离排序
			// minecraft:lingering_potion 只返回非喷溅变体
			Item vanillaPotion = (Item) Item.itemRegistry.getObject("minecraft:potion");
			if (vanillaPotion != null && vanillaPotion.getCreativeTab() == this) {
				for (ItemStack s : generatePotionsIn121Order(vanillaPotion, this, true)) {
					String key = "minecraft:potion@" + s.getItemDamage();
					if (!existingKeys.contains(key)) {
						existingKeys.add(key);
						itemStacks.add(s);
					}
				}
			}
			Item lingeringPotion = (Item) Item.itemRegistry.getObject("minecraft:lingering_potion");
			if (lingeringPotion != null && lingeringPotion.getCreativeTab() == this) {
				for (ItemStack s : generatePotionsIn121Order(lingeringPotion, this, false)) {
					String key = "minecraft:lingering_potion@" + s.getItemDamage();
					if (!existingKeys.contains(key)) {
						existingKeys.add(key);
						itemStacks.add(s);
					}
				}
			}

			// 药水箭（tipped_arrow）- 按 1.21.4 官方顺序排列（只有效果变体）
			Item tippedArrow = (Item) Item.itemRegistry.getObject("minecraft:tipped_arrow");
			if (tippedArrow != null && tippedArrow.getCreativeTab() == this) {
				for (ItemStack s : generatePotionsIn121Order(tippedArrow, this, false)) {
					String key = "minecraft:tipped_arrow@" + s.getItemDamage();
					if (!existingKeys.contains(key)) {
						existingKeys.add(key);
						itemStacks.add(s);
					}
				}
			}
		}
	}
}
