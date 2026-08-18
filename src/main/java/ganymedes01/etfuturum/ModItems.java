package ganymedes01.etfuturum;
import ganymedes01.etfuturum.creative.ModdedCreativeTabs;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.registry.GameRegistry;
import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.blocks.BlockWoodSign;
import ganymedes01.etfuturum.compat.ModsList;

import ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems;
import ganymedes01.etfuturum.configuration.configs.ConfigEntities;
import ganymedes01.etfuturum.configuration.configs.ConfigExperiments;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.configuration.configs.ConfigModCompat;
import ganymedes01.etfuturum.core.utils.Utils;

import ganymedes01.etfuturum.items.BaseFood;
import ganymedes01.etfuturum.items.BaseItem;
import ganymedes01.etfuturum.items.BaseSubtypesItem;
import ganymedes01.etfuturum.items.DebugTestItem;
import ganymedes01.etfuturum.items.ItemArmorStand;
import ganymedes01.etfuturum.items.ItemArrowTipped;
import ganymedes01.etfuturum.items.ItemBamboo;
import ganymedes01.etfuturum.items.ItemBarrelUpgrade;
import ganymedes01.etfuturum.items.ItemBeetrootSeeds;
import ganymedes01.etfuturum.items.ItemBeetrootSoup;
import ganymedes01.etfuturum.items.ItemChorusFruit;
import ganymedes01.etfuturum.items.ItemEndCrystal;
import ganymedes01.etfuturum.items.ItemEtFuturumRecord;
import ganymedes01.etfuturum.items.ItemGlowBerries;
import ganymedes01.etfuturum.items.ItemHoneyBottle;
import ganymedes01.etfuturum.items.ItemLingeringPotion;
import ganymedes01.etfuturum.items.ItemNetheriteIngot;
import ganymedes01.etfuturum.items.ItemNewBoat;
import ganymedes01.etfuturum.items.ItemRabbitStew;
import ganymedes01.etfuturum.items.ItemShulkerBoxUpgrade;
import ganymedes01.etfuturum.items.ItemSuspiciousStew;
import ganymedes01.etfuturum.items.ItemSweetBerries;
import ganymedes01.etfuturum.items.ItemWoodSign;
import ganymedes01.etfuturum.items.equipment.ItemArmorElytra;
import ganymedes01.etfuturum.items.equipment.ItemEFRArmour;
import ganymedes01.etfuturum.items.equipment.ItemEFRAxe;
import ganymedes01.etfuturum.items.equipment.ItemEFRHoe;
import ganymedes01.etfuturum.items.equipment.ItemEFRPickaxe;
import ganymedes01.etfuturum.items.equipment.ItemEFRSpade;
import ganymedes01.etfuturum.items.equipment.ItemEFRSword;
import ganymedes01.etfuturum.items.rawore.modded.BaseRawOre;
import ganymedes01.etfuturum.items.rawore.modded.ItemGeneralModdedRawOre;
import ganymedes01.etfuturum.lib.Reference;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public enum ModItems {
	MUTTON(ConfigBlocksItems.enableMutton, new BaseFood(2, 0.3F, true).setNames("mutton")),
	COOKED_MUTTON(ConfigBlocksItems.enableMutton, new BaseFood(6, 0.8F, true).setNames("cooked_mutton")),
	PRISMARINE_SHARD(ConfigBlocksItems.enablePrismarine, new BaseItem("prismarine_shard")),
	PRISMARINE_CRYSTALS(ConfigBlocksItems.enablePrismarine, new BaseItem("prismarine_crystals")),
	WOODEN_ARMORSTAND(ConfigBlocksItems.enableArmourStand, new ItemArmorStand()),
	RABBIT(ConfigEntities.enableRabbit, new BaseFood(3, 0.3F, true).setNames("rabbit")),
	COOKED_RABBIT(ConfigEntities.enableRabbit, new BaseFood(5, 0.6F, true).setNames("cooked_rabbit")),
	RABBIT_FOOT(ConfigEntities.enableRabbit, new BaseItem("rabbit_foot").setPotionEffect("+0+1-2+3&4-4+13")),
	RABBIT_HIDE(ConfigEntities.enableRabbit, new BaseItem("rabbit_hide")),
	RABBIT_STEW(ConfigEntities.enableRabbit, new ItemRabbitStew()),
	BEETROOT(ConfigBlocksItems.enableBeetroot, new BaseFood(1, 0.6F, false).setNames("beetroot")),
	BEETROOT_SEEDS(ConfigBlocksItems.enableBeetroot, new ItemBeetrootSeeds()),
	BEETROOT_SOUP(ConfigBlocksItems.enableBeetroot, new ItemBeetrootSoup()),
	CHORUS_FRUIT(ConfigBlocksItems.enableChorusFruit, new ItemChorusFruit()),
	CHORUS_FRUIT_POPPED(ConfigBlocksItems.enableChorusFruit, new BaseItem("popped_chorus_fruit")),
	TIPPED_ARROW(ConfigBlocksItems.enableTippedArrows, new ItemArrowTipped()),
	LINGERING_POTION(ConfigBlocksItems.enableLingeringPotions, new ItemLingeringPotion()),
	DRAGON_BREATH(ConfigBlocksItems.enableLingeringPotions, new BaseItem("dragon_breath").setContainerItem(Items.glass_bottle).setPotionEffect("-14+13")),
	ELYTRA(ConfigMixins.enableElytra, new ItemArmorElytra()),
	END_CRYSTAL(ConfigEntities.enableDragonRespawn, new ItemEndCrystal()),
	IRON_NUGGET(ConfigBlocksItems.enableIronNugget, new BaseItem("iron_nugget")),
	RAW_ORE(ConfigBlocksItems.enableRawOres, new BaseSubtypesItem("raw_copper", "raw_iron", "raw_gold").setNames("raw_ore")),
	//modded_raw_ore(true, new ItemRawOre(true)),
	NETHERITE_SCRAP(ConfigBlocksItems.enableNetherite, new BaseItem("netherite_scrap")),
	NETHERITE_INGOT(ConfigBlocksItems.enableNetherite, new ItemNetheriteIngot()),
	NETHERITE_HELMET(ConfigBlocksItems.enableNetherite, new ItemEFRArmour(ModMaterials.NETHERITE_ARMOUR, 0, ConfigBlocksItems.netheriteHelmetDurability)),
	NETHERITE_CHESTPLATE(ConfigBlocksItems.enableNetherite, new ItemEFRArmour(ModMaterials.NETHERITE_ARMOUR, 1, ConfigBlocksItems.netheriteChestplateDurability)),
	NETHERITE_LEGGINGS(ConfigBlocksItems.enableNetherite, new ItemEFRArmour(ModMaterials.NETHERITE_ARMOUR, 2, ConfigBlocksItems.netheriteLeggingsDurability)),
	NETHERITE_BOOTS(ConfigBlocksItems.enableNetherite, new ItemEFRArmour(ModMaterials.NETHERITE_ARMOUR, 3, ConfigBlocksItems.netheriteBootsDurability)),
	NETHERITE_PICKAXE(ConfigBlocksItems.enableNetherite, new ItemEFRPickaxe(ModMaterials.NETHERITE_TOOL, ConfigBlocksItems.netheritePickaxeDurability)),
	NETHERITE_SHOVEL(ConfigBlocksItems.enableNetherite, new ItemEFRSpade(ModMaterials.NETHERITE_TOOL, ConfigBlocksItems.netheriteSpadeDurability)),
	NETHERITE_AXE(ConfigBlocksItems.enableNetherite, new ItemEFRAxe(ModMaterials.NETHERITE_TOOL, ConfigBlocksItems.netheriteAxeDurability)),
	NETHERITE_HOE(ConfigBlocksItems.enableNetherite, new ItemEFRHoe(ModMaterials.NETHERITE_TOOL, ConfigBlocksItems.netheriteHoeDurability)),
	NETHERITE_SWORD(ConfigBlocksItems.enableNetherite, new ItemEFRSword(ModMaterials.NETHERITE_TOOL, ConfigBlocksItems.netheriteSwordDurability)),
	TOTEM_OF_UNDYING(ConfigBlocksItems.enableTotemUndying, new BaseItem("totem_of_undying").setMaxStackSize(1)),
	DYE(ConfigBlocksItems.enableNewDyes, new BaseSubtypesItem("white_dye", "blue_dye", "brown_dye", "black_dye").setNames("dye")),
	COPPER_INGOT(ConfigBlocksItems.enableCopper && !ConfigModCompat.disableCopperOreAndIngotOnly, new BaseItem("copper_ingot")),
	SUSPICIOUS_STEW(ConfigBlocksItems.enableSuspiciousStew, new ItemSuspiciousStew()),
	SWEET_BERRIES(ConfigBlocksItems.enableSweetBerryBushes, new ItemSweetBerries()),
	GLOW_BERRIES(ConfigBlocksItems.enableGlowBerries, new ItemGlowBerries(ModBlocks.CAVE_VINE.get())),
	SHULKER_SHELL(ConfigBlocksItems.enableShulkerBoxes, new BaseItem("shulker_shell")),
	MUSIC_DISC_PIGSTEP(ConfigBlocksItems.enablePigstep, new ItemEtFuturumRecord("pigstep")),
	MUSIC_DISC_OTHERSIDE(ConfigBlocksItems.enableOtherside, new ItemEtFuturumRecord("otherside")),
	MUSIC_DISC_PRECIPICE(ConfigBlocksItems.enablePrecipice, new ItemEtFuturumRecord("precipice")),
	MUSIC_DISC_CREATOR_MUSIC_BOX(ConfigBlocksItems.enableCreatorMusicBox, new ItemEtFuturumRecord("creator_music_box")),
	MUSIC_DISC_CREATOR(ConfigBlocksItems.enableCreator, new ItemEtFuturumRecord("creator")),
	MUSIC_DISC_TEARS(ConfigBlocksItems.enableTears, new ItemEtFuturumRecord("tears")),
	MUSIC_DISC_LAVA_CHICKEN(ConfigBlocksItems.enableLavaChicken, new ItemEtFuturumRecord("lava_chicken")),
	MUSIC_DISC_5(ConfigBlocksItems.enable5, new ItemEtFuturumRecord("5")),
	DISC_FRAGMENT_5(ConfigBlocksItems.enable5, new BaseItem("disc_fragment_5", true)),
	AMETHYST_SHARD(ConfigBlocksItems.enableAmethyst, new BaseItem("amethyst_shard")),
	SHULKER_BOX_UPGRADE(ModsList.IRON_CHEST.isLoaded() && ConfigModCompat.shulkerBoxesIronChest, new ItemShulkerBoxUpgrade()),
	BARREL_UPGRADE(ModsList.IRON_CHEST.isLoaded() && ConfigModCompat.barrelIronChest, new ItemBarrelUpgrade()),
	HONEYCOMB(ConfigBlocksItems.enableHoney, new BaseItem("honeycomb")),
	HONEY_BOTTLE(ConfigBlocksItems.enableHoney, new ItemHoneyBottle()),
	BAMBOO(ConfigBlocksItems.enableBambooBlocks, new ItemBamboo()),

	OAK_BOAT(ConfigBlocksItems.enableNewBoats && !ConfigBlocksItems.replaceOldBoats, new ItemNewBoat("minecraft", "oak", () -> Item.getItemFromBlock(Blocks.planks), 0, false, false)),
	OAK_CHEST_BOAT(ConfigBlocksItems.enableNewBoats, new ItemNewBoat("minecraft", "oak", () -> Item.getItemFromBlock(Blocks.planks), 0, true, false)),
	SPRUCE_BOAT(ConfigBlocksItems.enableNewBoats, new ItemNewBoat("minecraft", "spruce", () -> Item.getItemFromBlock(Blocks.planks), 1, false, false)),
	SPRUCE_CHEST_BOAT(ConfigBlocksItems.enableNewBoats, new ItemNewBoat("minecraft", "spruce", () -> Item.getItemFromBlock(Blocks.planks), 1, true, false)),
	BIRCH_BOAT(ConfigBlocksItems.enableNewBoats, new ItemNewBoat("minecraft", "birch", () -> Item.getItemFromBlock(Blocks.planks), 2, false, false)),
	BIRCH_CHEST_BOAT(ConfigBlocksItems.enableNewBoats, new ItemNewBoat("minecraft", "birch", () -> Item.getItemFromBlock(Blocks.planks), 2, true, false)),
	JUNGLE_BOAT(ConfigBlocksItems.enableNewBoats, new ItemNewBoat("minecraft", "jungle", () -> Item.getItemFromBlock(Blocks.planks), 3, false, false)),
	JUNGLE_CHEST_BOAT(ConfigBlocksItems.enableNewBoats, new ItemNewBoat("minecraft", "jungle", () -> Item.getItemFromBlock(Blocks.planks), 3, true, false)),
	ACACIA_BOAT(ConfigBlocksItems.enableNewBoats, new ItemNewBoat("minecraft", "acacia", () -> Item.getItemFromBlock(Blocks.planks), 4, false, false)),
	ACACIA_CHEST_BOAT(ConfigBlocksItems.enableNewBoats, new ItemNewBoat("minecraft", "acacia", () -> Item.getItemFromBlock(Blocks.planks), 4, true, false)),
	DARK_OAK_BOAT(ConfigBlocksItems.enableNewBoats, new ItemNewBoat("minecraft", "dark_oak", () -> Item.getItemFromBlock(Blocks.planks), 5, false, false)),
	DARK_OAK_CHEST_BOAT(ConfigBlocksItems.enableNewBoats, new ItemNewBoat("minecraft", "dark_oak", () -> Item.getItemFromBlock(Blocks.planks), 5, true, false)),
	MANGROVE_OAK_BOAT(ConfigBlocksItems.enableNewBoats && ConfigExperiments.enableMangroveBlocks, new ItemNewBoat("minecraft", "mangrove", ModBlocks.WOOD_PLANKS::getItem, 2, false, false)),
	MANGROVE_CHEST_BOAT(ConfigBlocksItems.enableNewBoats && ConfigExperiments.enableMangroveBlocks, new ItemNewBoat("minecraft", "mangrove", ModBlocks.WOOD_PLANKS::getItem, 2, true, false)),
	CHERRY_BOAT(ConfigBlocksItems.enableNewBoats && ConfigBlocksItems.enableCherryBlocks, new ItemNewBoat("minecraft", "cherry", ModBlocks.WOOD_PLANKS::getItem, 3, false, false)),
	CHERRY_CHEST_BOAT(ConfigBlocksItems.enableNewBoats && ConfigBlocksItems.enableCherryBlocks, new ItemNewBoat("minecraft", "cherry", ModBlocks.WOOD_PLANKS::getItem, 3, true, false)),
	BAMBOO_RAFT(ConfigBlocksItems.enableNewBoats, new ItemNewBoat("minecraft", "bamboo", ModBlocks.WOOD_PLANKS::getItem, 4, false, true)),
	BAMBOO_CHEST_RAFT(ConfigBlocksItems.enableNewBoats, new ItemNewBoat("minecraft", "bamboo", ModBlocks.WOOD_PLANKS::getItem, 4, true, true)),

	BOP_SACREDOAK_BOAT(ConfigBlocksItems.enableNewBoats && ModsList.BIOMES_O_PLENTY.isLoaded(), new ItemNewBoat("biomesoplenty", "sacredoak", () -> GameRegistry.findItem("BiomesOPlenty", "planks"), 0, false, false)),
	BOP_SACREDOAK_CHEST_BOAT(ConfigBlocksItems.enableNewBoats && ModsList.BIOMES_O_PLENTY.isLoaded(), new ItemNewBoat("biomesoplenty", "sacredoak", () -> GameRegistry.findItem("BiomesOPlenty", "planks"), 0, true, false)),
	BOP_CHERRY_BOAT(ConfigBlocksItems.enableNewBoats && ModsList.BIOMES_O_PLENTY.isLoaded(), new ItemNewBoat("biomesoplenty", "cherry", () -> GameRegistry.findItem("BiomesOPlenty", "planks"), 1, false, false)),
	BOP_CHERRY_CHEST_BOAT(ConfigBlocksItems.enableNewBoats && ModsList.BIOMES_O_PLENTY.isLoaded(), new ItemNewBoat("biomesoplenty", "cherry", () -> GameRegistry.findItem("BiomesOPlenty", "planks"), 1, true, false)),
	BOP_DARK_BOAT(ConfigBlocksItems.enableNewBoats && ModsList.BIOMES_O_PLENTY.isLoaded(), new ItemNewBoat("biomesoplenty", "dark", () -> GameRegistry.findItem("BiomesOPlenty", "planks"), 2, false, false)),
	BOP_DARK_CHEST_BOAT(ConfigBlocksItems.enableNewBoats && ModsList.BIOMES_O_PLENTY.isLoaded(), new ItemNewBoat("biomesoplenty", "dark", () -> GameRegistry.findItem("BiomesOPlenty", "planks"), 2, true, false)),
	BOP_FIR_BOAT(ConfigBlocksItems.enableNewBoats && ModsList.BIOMES_O_PLENTY.isLoaded(), new ItemNewBoat("biomesoplenty", "fir", () -> GameRegistry.findItem("BiomesOPlenty", "planks"), 3, false, false)),
	BOP_FIR_CHEST_BOAT(ConfigBlocksItems.enableNewBoats && ModsList.BIOMES_O_PLENTY.isLoaded(), new ItemNewBoat("biomesoplenty", "fir", () -> GameRegistry.findItem("BiomesOPlenty", "planks"), 3, true, false)),
	BOP_ETHEREAL_BOAT(ConfigBlocksItems.enableNewBoats && ModsList.BIOMES_O_PLENTY.isLoaded(), new ItemNewBoat("biomesoplenty", "ethereal", () -> GameRegistry.findItem("BiomesOPlenty", "planks"), 4, false, false)),
	BOP_ETHEREAL_CHEST_BOAT(ConfigBlocksItems.enableNewBoats && ModsList.BIOMES_O_PLENTY.isLoaded(), new ItemNewBoat("biomesoplenty", "ethereal", () -> GameRegistry.findItem("BiomesOPlenty", "planks"), 4, true, false)),
	BOP_MAGIC_BOAT(ConfigBlocksItems.enableNewBoats && ModsList.BIOMES_O_PLENTY.isLoaded(), new ItemNewBoat("biomesoplenty", "magic", () -> GameRegistry.findItem("BiomesOPlenty", "planks"), 5, false, false)),
	BOP_MAGIC_CHEST_BOAT(ConfigBlocksItems.enableNewBoats && ModsList.BIOMES_O_PLENTY.isLoaded(), new ItemNewBoat("biomesoplenty", "magic", () -> GameRegistry.findItem("BiomesOPlenty", "planks"), 5, true, false)),
	BOP_MANGROVE_BOAT(ConfigBlocksItems.enableNewBoats && ModsList.BIOMES_O_PLENTY.isLoaded(), new ItemNewBoat("biomesoplenty", "mangrove", () -> GameRegistry.findItem("BiomesOPlenty", "planks"), 6, false, false)),
	BOP_MANGROVE_CHEST_BOAT(ConfigBlocksItems.enableNewBoats && ModsList.BIOMES_O_PLENTY.isLoaded(), new ItemNewBoat("biomesoplenty", "mangrove", () -> GameRegistry.findItem("BiomesOPlenty", "planks"), 6, true, false)),
	BOP_PALM_BOAT(ConfigBlocksItems.enableNewBoats && ModsList.BIOMES_O_PLENTY.isLoaded(), new ItemNewBoat("biomesoplenty", "palm", () -> GameRegistry.findItem("BiomesOPlenty", "planks"), 7, false, false)),
	BOP_PALM_CHEST_BOAT(ConfigBlocksItems.enableNewBoats && ModsList.BIOMES_O_PLENTY.isLoaded(), new ItemNewBoat("biomesoplenty", "palm", () -> GameRegistry.findItem("BiomesOPlenty", "planks"), 7, true, false)),
	BOP_REDWOOD_BOAT(ConfigBlocksItems.enableNewBoats && ModsList.BIOMES_O_PLENTY.isLoaded(), new ItemNewBoat("biomesoplenty", "redwood", () -> GameRegistry.findItem("BiomesOPlenty", "planks"), 8, false, false)),
	BOP_REDWOOD_CHEST_BOAT(ConfigBlocksItems.enableNewBoats && ModsList.BIOMES_O_PLENTY.isLoaded(), new ItemNewBoat("biomesoplenty", "redwood", () -> GameRegistry.findItem("BiomesOPlenty", "planks"), 8, true, false)),
	BOP_WILLOW_BOAT(ConfigBlocksItems.enableNewBoats && ModsList.BIOMES_O_PLENTY.isLoaded(), new ItemNewBoat("biomesoplenty", "willow", () -> GameRegistry.findItem("BiomesOPlenty", "planks"), 9, false, false)),
	BOP_WILLOW_CHEST_BOAT(ConfigBlocksItems.enableNewBoats && ModsList.BIOMES_O_PLENTY.isLoaded(), new ItemNewBoat("biomesoplenty", "willow", () -> GameRegistry.findItem("BiomesOPlenty", "planks"), 9, true, false)),
	BOP_BAMBOO_BOAT(ConfigBlocksItems.enableNewBoats && ModsList.BIOMES_O_PLENTY.isLoaded(), new ItemNewBoat("biomesoplenty", "bamboo", () -> GameRegistry.findItem("BiomesOPlenty", "planks"), 10, false, true)),
	BOP_BAMBOO_CHEST_BOAT(ConfigBlocksItems.enableNewBoats && ModsList.BIOMES_O_PLENTY.isLoaded(), new ItemNewBoat("biomesoplenty", "bamboo", () -> GameRegistry.findItem("BiomesOPlenty", "planks"), 10, true, true)),
	BOP_PINE_BOAT(ConfigBlocksItems.enableNewBoats && ModsList.BIOMES_O_PLENTY.isLoaded(), new ItemNewBoat("biomesoplenty", "pine", () -> GameRegistry.findItem("BiomesOPlenty", "planks"), 11, false, false)),
	BOP_PINE_CHEST_BOAT(ConfigBlocksItems.enableNewBoats && ModsList.BIOMES_O_PLENTY.isLoaded(), new ItemNewBoat("biomesoplenty", "pine", () -> GameRegistry.findItem("BiomesOPlenty", "planks"), 11, true, false)),
	BOP_HELLBARK_BOAT(ConfigBlocksItems.enableNewBoats && ModsList.BIOMES_O_PLENTY.isLoaded(), new ItemNewBoat("biomesoplenty", "hellbark", () -> GameRegistry.findItem("BiomesOPlenty", "planks"), 12, false, false)),
	BOP_HELLBARK_CHEST_BOAT(ConfigBlocksItems.enableNewBoats && ModsList.BIOMES_O_PLENTY.isLoaded(), new ItemNewBoat("biomesoplenty", "hellbark", () -> GameRegistry.findItem("BiomesOPlenty", "planks"), 12, true, false)),
	BOP_JACARANDA_BOAT(ConfigBlocksItems.enableNewBoats && ModsList.BIOMES_O_PLENTY.isLoaded(), new ItemNewBoat("biomesoplenty", "jacaranda", () -> GameRegistry.findItem("BiomesOPlenty", "planks"), 13, false, false)),
	BOP_JACARANDA_CHEST_BOAT(ConfigBlocksItems.enableNewBoats && ModsList.BIOMES_O_PLENTY.isLoaded(), new ItemNewBoat("biomesoplenty", "jacaranda", () -> GameRegistry.findItem("BiomesOPlenty", "planks"), 13, true, false)),
	BOP_MAHOGANY_BOAT(ConfigBlocksItems.enableNewBoats && ModsList.BIOMES_O_PLENTY.isLoaded(), new ItemNewBoat("biomesoplenty", "mahogany", () -> GameRegistry.findItem("BiomesOPlenty", "planks"), 14, false, false)),
	BOP_MAHOGANY_CHEST_BOAT(ConfigBlocksItems.enableNewBoats && ModsList.BIOMES_O_PLENTY.isLoaded(), new ItemNewBoat("biomesoplenty", "mahogany", () -> GameRegistry.findItem("BiomesOPlenty", "planks"), 14, true, false)),

	WITCHERY_ROWAN_BOAT(ConfigBlocksItems.enableNewBoats && ModsList.WITCHERY.isLoaded(), new ItemNewBoat("witchery", "rowan", () -> GameRegistry.findItem("witchery", "witchwood"), 0, false, false)),
	WITCHERY_ROWAN_CHEST_BOAT(ConfigBlocksItems.enableNewBoats && ModsList.WITCHERY.isLoaded(), new ItemNewBoat("witchery", "rowan", () -> GameRegistry.findItem("witchery", "witchwood"), 0, true, false)),
	WITCHERY_ALDER_BOAT(ConfigBlocksItems.enableNewBoats && ModsList.WITCHERY.isLoaded(), new ItemNewBoat("witchery", "alder", () -> GameRegistry.findItem("witchery", "witchwood"), 1, false, false)),
	WITCHERY_ALDER_CHEST_BOAT(ConfigBlocksItems.enableNewBoats && ModsList.WITCHERY.isLoaded(), new ItemNewBoat("witchery", "alder", () -> GameRegistry.findItem("witchery", "witchwood"), 1, true, false)),
	WITCHERY_HAWTHORN_BOAT(ConfigBlocksItems.enableNewBoats && ModsList.WITCHERY.isLoaded(), new ItemNewBoat("witchery", "hawthorn", () -> GameRegistry.findItem("witchery", "witchwood"), 2, false, false)),
	WITCHERY_HAWTHORN_CHEST_BOAT(ConfigBlocksItems.enableNewBoats && ModsList.WITCHERY.isLoaded(), new ItemNewBoat("witchery", "hawthorn", () -> GameRegistry.findItem("witchery", "witchwood"), 2, true, false)),

	//legacy sign items -- new signs use their ItemBlock as the sign item instead
	SPRUCE_SIGN(ConfigBlocksItems.enableVanillaSigns, new ItemWoodSign((BlockWoodSign) ModBlocks.SPRUCE_SIGN.get())),
	BIRCH_SIGN(ConfigBlocksItems.enableVanillaSigns, new ItemWoodSign((BlockWoodSign) ModBlocks.BIRCH_SIGN.get())),
	JUNGLE_SIGN(ConfigBlocksItems.enableVanillaSigns, new ItemWoodSign((BlockWoodSign) ModBlocks.JUNGLE_SIGN.get())),
	ACACIA_SIGN(ConfigBlocksItems.enableVanillaSigns, new ItemWoodSign((BlockWoodSign) ModBlocks.ACACIA_SIGN.get())),
	DARK_OAK_SIGN(ConfigBlocksItems.enableVanillaSigns, new ItemWoodSign((BlockWoodSign) ModBlocks.DARK_OAK_SIGN.get())),

	//Mod Support
	MODDED_RAW_ORE(Utils.enableModdedRawOres(), new ItemGeneralModdedRawOre("raw_aluminum", "raw_tin", "raw_silver", "raw_lead", "raw_nickel", "raw_platinum", "raw_mythril",
			"raw_uranium", "raw_thorium", "raw_tungsten", "raw_titanium", "raw_zinc", "raw_magnesium", "raw_boron")),
	RAW_ADAMANTIUM(Utils.enableModdedRawOres(ModsList.SIMPLEORES), new BaseRawOre("simpleores", "adamantium")),

	//Debug Item
	DEBUGGING_TOOL(Reference.DEV_ENVIRONMENT, new DebugTestItem());

	public static final ModItems[] CHEST_BOATS = new ModItems[]{OAK_CHEST_BOAT, SPRUCE_CHEST_BOAT, BIRCH_CHEST_BOAT, JUNGLE_CHEST_BOAT, ACACIA_CHEST_BOAT, DARK_OAK_CHEST_BOAT, CHERRY_CHEST_BOAT, BAMBOO_CHEST_RAFT};
	public static final ModItems[] BOATS = new ModItems[]{OAK_BOAT, SPRUCE_BOAT, BIRCH_BOAT, JUNGLE_BOAT, ACACIA_BOAT, DARK_OAK_BOAT, CHERRY_BOAT, BAMBOO_RAFT};
	public static final ModItems[] OLD_SIGN_ITEMS = new ModItems[]{SPRUCE_SIGN, BIRCH_SIGN, JUNGLE_SIGN, ACACIA_SIGN, DARK_OAK_SIGN};

	/*
	 * Stand-in static final fields because some mods incorrectly referenced my code directly.
	 * They should be using GameRegistry.findItem but it is what it is I guess.
	 */

	//D-Mod
	@Deprecated
	public static final Item sweet_berries = SWEET_BERRIES.get();

	public static final ModItems[] VALUES = values();

	public static void init() {
		// Forge 1.7.10's GameRegistry.registerItem(item, name, modId) ignores the modId
		// parameter (deprecated, unused). The namespace is determined by the active
		// ModContainer via GameData.addPrefix(). We reflectively swap the active container
		// to the minecraft dummy so most mod items register under "minecraft:" and match
		// CreativeTabData lookups without alias mapping. If the name already exists in
		// vanilla 1.7.10 (detected dynamically via Item.getByNameOrId), fall back to
		// etfuturum: namespace to avoid "registered twice" crashes.
		ModContainer mcContainer = Utils.getMinecraftContainer();
		ModContainer oldContainer = Loader.instance().activeModContainer();
		for (ModItems item : VALUES) {
			if (item.isEnabled()) {
				String name = item.getRegName();
				boolean vanillaConflict = Item.itemRegistry.getObject("minecraft:" + name) != null;
				if (vanillaConflict) {
					Utils.setActiveModContainer(oldContainer);
				} else {
					Utils.setActiveModContainer(mcContainer);
				}
				// 冲突物品（名称含 _same）设置到 etfuturum.blocks 标签页
				if (name.endsWith("_same")) {
					item.get().setCreativeTab(ModdedCreativeTabs.BUILDING_BLOCKS);
				}
				GameRegistry.registerItem(item.get(), name);
			}
		}
		Utils.setActiveModContainer(oldContainer);
	}

	/**
	 * Returns the registration name for this item.
	 * Most items use the enum name lowercased, but some are overridden
	 * to match official Minecraft 1.21.4 registry names (eliminating alias mapping).
	 * DYE gets a "_same" suffix because "minecraft:dye" already exists in 1.7.10
	 * (16-metadata vanilla item); it is placed in the etfuturum.blocks creative tab.
	 */
	public String getRegName() {
		switch (this) {
			case DYE: return "dye_same";
			default:
			return name().toLowerCase();
		}
	}

	final private boolean isEnabled;
	final private Item theItem;

	ModItems(boolean enabled, Item item) {
		isEnabled = enabled;
		theItem = item;
	}

	public boolean isEnabled() {
		return isEnabled;
	}

	public Item get() {
		return theItem;
	}

	public ItemStack newItemStack() {
		return newItemStack(1);
	}

	public ItemStack newItemStack(int count) {
		return newItemStack(count, 0);
	}

	public ItemStack newItemStack(int count, int meta) {
		return new ItemStack(this.get(), count, meta);
	}
}
