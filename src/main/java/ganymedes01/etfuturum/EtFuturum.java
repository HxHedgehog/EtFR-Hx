package ganymedes01.etfuturum;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;

import cpw.mods.fml.common.event.FMLConstructionEvent;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLInterModComms.IMCEvent;
import cpw.mods.fml.common.event.FMLInterModComms.IMCMessage;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLMissingMappingsEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;

import ganymedes01.etfuturum.api.BeePlantRegistry;
import ganymedes01.etfuturum.api.BrewingFuelRegistry;
import ganymedes01.etfuturum.api.CompostingRegistry;
import ganymedes01.etfuturum.api.DeepslateOreRegistry;
import ganymedes01.etfuturum.api.HoeRegistry;
import ganymedes01.etfuturum.dispenser.DispenserBehaviourMudConversion;
import ganymedes01.etfuturum.api.MultiBlockSoundRegistry;
import ganymedes01.etfuturum.api.PistonBehaviorRegistry;
import ganymedes01.etfuturum.api.RawOreRegistry;
import ganymedes01.etfuturum.api.StrippedLogRegistry;
import ganymedes01.etfuturum.api.mappings.BasicMultiBlockSound;
import ganymedes01.etfuturum.blocks.BlockSculk;
import ganymedes01.etfuturum.blocks.BlockSculkCatalyst;
import net.minecraft.block.BlockSponge;
import ganymedes01.etfuturum.client.BuiltInResourcePack;
import ganymedes01.etfuturum.client.DynamicSoundsResourcePack;
import ganymedes01.etfuturum.client.GrayscaleWaterResourcePack;
import ganymedes01.etfuturum.client.sound.ModSounds;
import ganymedes01.etfuturum.command.CommandFill;

import ganymedes01.etfuturum.compat.CompatBaublesExpanded;
import ganymedes01.etfuturum.compat.CompatMisc;
import ganymedes01.etfuturum.compat.CompatRPLEEventHandler;
import ganymedes01.etfuturum.compat.CompatTinkersConstruct;
import ganymedes01.etfuturum.compat.CompatWaila;
import ganymedes01.etfuturum.compat.ExternalContent;
import ganymedes01.etfuturum.compat.ModsList;
import ganymedes01.etfuturum.configuration.ConfigBase;
import ganymedes01.etfuturum.creative.ItemCategoryHelper;
import ganymedes01.etfuturum.creative.ModdedCreativeTabs;

import ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems;
import ganymedes01.etfuturum.configuration.configs.ConfigExperiments;
import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import ganymedes01.etfuturum.configuration.configs.ConfigModCompat;
import ganymedes01.etfuturum.configuration.configs.ConfigSounds;
import ganymedes01.etfuturum.core.handlers.ServerEventHandler;
import ganymedes01.etfuturum.core.handlers.WorldEventHandler;
import ganymedes01.etfuturum.core.proxy.CommonProxy;
import ganymedes01.etfuturum.core.utils.IInitAction;
import ganymedes01.etfuturum.core.utils.Logger;

import ganymedes01.etfuturum.lib.Reference;

import ganymedes01.etfuturum.network.ArmourStandInteractHandler;
import ganymedes01.etfuturum.network.ArmourStandInteractMessage;
import ganymedes01.etfuturum.network.AttackYawHandler;
import ganymedes01.etfuturum.network.AttackYawMessage;
import ganymedes01.etfuturum.network.BlackHeartParticlesHandler;
import ganymedes01.etfuturum.network.BlackHeartParticlesMessage;
import ganymedes01.etfuturum.network.BoatMoveHandler;
import ganymedes01.etfuturum.network.BoatMoveMessage;
import ganymedes01.etfuturum.network.ChestBoatOpenInventoryHandler;
import ganymedes01.etfuturum.network.ChestBoatOpenInventoryMessage;
import ganymedes01.etfuturum.network.StartElytraFlyingHandler;
import ganymedes01.etfuturum.network.StartElytraFlyingMessage;
import ganymedes01.etfuturum.network.TotemParticlesHandler;
import ganymedes01.etfuturum.network.TotemParticlesMessage;
import ganymedes01.etfuturum.network.WoodSignOpenHandler;
import ganymedes01.etfuturum.network.WoodSignOpenMessage;
import ganymedes01.etfuturum.potion.ModPotions;
import ganymedes01.etfuturum.recipes.ModRecipes;
import ganymedes01.etfuturum.recipes.SmithingTableRecipes;
import ganymedes01.etfuturum.spectator.SpectatorMode;
import ganymedes01.etfuturum.world.EtFuturumLateWorldGenerator;
import ganymedes01.etfuturum.world.EtFuturumWorldGenerator;
import ganymedes01.etfuturum.world.end.dimension.DimensionProviderEFREnd;
import ganymedes01.etfuturum.world.nether.biome.utils.NetherBiomeManager;
import ganymedes01.etfuturum.world.nether.dimension.DimensionProviderEFRNether;
import ganymedes01.etfuturum.world.structure.OceanMonument;
import makamys.mclib.core.MCLib;
import makamys.mclib.core.MCLibModules;

import net.minecraft.block.Block;
import net.minecraft.block.Block.SoundType;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockHay;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLilyPad;
import net.minecraft.block.BlockNetherWart;
import net.minecraft.block.BlockOre;
import net.minecraft.block.BlockStem;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.BlockVine;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraft.world.World;
import net.minecraftforge.common.ChestGenHooks;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;


import static ganymedes01.etfuturum.lib.Reference.MOD_GROUP;
import static ganymedes01.etfuturum.lib.Reference.MOD_ID;
import static ganymedes01.etfuturum.lib.Reference.MOD_NAME;

@Mod(
		modid = MOD_ID,
		name = MOD_NAME,
		version = Tags.VERSION,
		dependencies = Reference.DEPENDENCIES

)

public class EtFuturum {

	@Instance(MOD_ID)
	public static EtFuturum instance;

	@SidedProxy(clientSide = MOD_GROUP + ".core.proxy.ClientProxy", serverSide = MOD_GROUP + ".core.proxy.CommonProxy")
	public static CommonProxy proxy;

	public static SimpleNetworkWrapper networkWrapper;

	@EventHandler
	public void onConstruction(FMLConstructionEvent event) {
		if(Reference.SNAPSHOT_BUILD && !Reference.DEV_ENVIRONMENT) {
			Logger.info(MOD_ID + " is in snapshot mode. Disabling update checker... Other features may also be different.");
		}

		MCLib.init();
	}

	static final String NETHER_FORTRESS = "netherFortress";
	private Field fortressWeightedField;

	@EventHandler
	@SuppressWarnings("unchecked")
	public void preInit(FMLPreInitializationEvent event) {
		try {
			Field chestInfo = ChestGenHooks.class.getDeclaredField("chestInfo");
			chestInfo.setAccessible(true);
			if (!((HashMap<String, ChestGenHooks>) chestInfo.get(null)).containsKey(NETHER_FORTRESS)) {
				fortressWeightedField = Class.forName("net.minecraft.world.gen.structure.StructureNetherBridgePieces$Piece").getDeclaredField("field_111019_a");
				fortressWeightedField.setAccessible(true);
				((HashMap<String, ChestGenHooks>) chestInfo.get(null)).put(NETHER_FORTRESS, new ChestGenHooks(NETHER_FORTRESS, (WeightedRandomChestContent[]) fortressWeightedField.get(null), 2, 5));
			}
		} catch (Exception e) {
			System.out.println("Failed to get Nether fortress loot table:");
			e.printStackTrace();
		}

		for (ModBlocks block : ModBlocks.VALUES) {
			if (block.isEnabled() && block.get() instanceof IInitAction) {
				((IInitAction) block.get()).preInitAction();
			}
		}
		for (ModItems item : ModItems.VALUES) {
			if (item.isEnabled() && item.get() instanceof IInitAction) {
				((IInitAction) item.get()).preInitAction();
			}
		}

		ModBlocks.init();
		ModItems.init();
		ModEnchantments.init();
		ModPotions.init();
		SpectatorMode.init();

		if (event.getSide() == Side.CLIENT) {

			BuiltInResourcePack.register("etfr_sounds");

			if (ConfigFunctions.enableNewTextures || ConfigFunctions.enableLangReplacements) {
				BuiltInResourcePack.register("vanilla_overrides");
			}

			GrayscaleWaterResourcePack.inject();

			DynamicSoundsResourcePack.inject();
		}

		if (ConfigExperiments.netherDimensionProvider) {
			NetherBiomeManager.init();
		}

		GameRegistry.registerWorldGenerator(EtFuturumWorldGenerator.INSTANCE, 0);
		GameRegistry.registerWorldGenerator(EtFuturumLateWorldGenerator.INSTANCE, Integer.MAX_VALUE);

		OceanMonument.makeMap();

		NetworkRegistry.INSTANCE.registerGuiHandler(instance, proxy);
		networkWrapper = NetworkRegistry.INSTANCE.newSimpleChannel(MOD_ID);
		networkWrapper.registerMessage(ArmourStandInteractHandler.class, ArmourStandInteractMessage.class, 0, Side.SERVER);
		networkWrapper.registerMessage(BlackHeartParticlesHandler.class, BlackHeartParticlesMessage.class, 1, Side.CLIENT);
		networkWrapper.registerMessage(WoodSignOpenHandler.class, WoodSignOpenMessage.class, 3, Side.CLIENT);
		networkWrapper.registerMessage(BoatMoveHandler.class, BoatMoveMessage.class, 4, Side.SERVER);
		networkWrapper.registerMessage(ChestBoatOpenInventoryHandler.class, ChestBoatOpenInventoryMessage.class, 5, Side.SERVER);
		networkWrapper.registerMessage(StartElytraFlyingHandler.class, StartElytraFlyingMessage.class, 6, Side.SERVER);
		networkWrapper.registerMessage(AttackYawHandler.class, AttackYawMessage.class, 7, Side.CLIENT);
		networkWrapper.registerMessage(TotemParticlesHandler.class, TotemParticlesMessage.class, 8, Side.CLIENT);

		if (!Reference.SNAPSHOT_BUILD && !Reference.DEV_ENVIRONMENT) {
			MCLibModules.updateCheckAPI.submitModTask(MOD_ID, Reference.VERSION_NUMBER, Reference.VERSION_URL);
		}

		CompatMisc.runModHooksPreInit();

		if(ModsList.RPLE.isLoaded()) {
			CompatRPLEEventHandler.registerRPLECompat();
		}
	}

	@EventHandler
	public void init(FMLInitializationEvent event) {
		for (ModBlocks block : ModBlocks.VALUES) {
			if (block.isEnabled() && block.get() instanceof IInitAction) {
				((IInitAction) block.get()).initAction();
			}
		}
		for (ModItems item : ModItems.VALUES) {
			if (item.isEnabled() && item.get() instanceof IInitAction) {
				((IInitAction) item.get()).initAction();
			}
		}

		if (ModsList.WAILA.isLoaded()) {
			CompatWaila.register();
		}

		proxy.registerEvents();
		proxy.registerEntities();
		proxy.registerRenderers();

		CompatMisc.runModHooksInit();

		ModRecipes.init();

		// 注册发射器水瓶转化泥土/粗泥/缠根泥土为泥巴
		if (ConfigBlocksItems.enableMud) {
			BlockDispenser.dispenseBehaviorRegistry.putObject(Items.potionitem, new DispenserBehaviourMudConversion());
		}
	}

	@EventHandler
	public void processIMCRequests(IMCEvent event) {
		for (IMCMessage message : event.getMessages()) {
			if (message.key.equals("register-brewing-fuel")) {
				NBTTagCompound nbt = message.getNBTValue();
				ItemStack stack = ItemStack.loadItemStackFromNBT(nbt.getCompoundTag("Fuel"));
				int brews = nbt.getInteger("Brews");
				BrewingFuelRegistry.registerFuel(stack, brews);
			}
		}
	}

	@EventHandler
	@SuppressWarnings("unchecked")
	public void postInit(FMLPostInitializationEvent event) {
		if (ConfigFunctions.enableUpdatedFoodValues) {
			((ItemFood) Items.carrot).healAmount = 3;
			((ItemFood) Items.baked_potato).healAmount = 5;
		}

		if (ConfigFunctions.enableUpdatedHarvestLevels) {
			Blocks.packed_ice.setHarvestLevel("pickaxe", 0);
			Blocks.ladder.setHarvestLevel("axe", 0);
			Blocks.melon_block.setHarvestLevel("axe", 0);
		}

		if (ConfigFunctions.enableFloatingTrapDoors) {
			BlockTrapDoor.disableValidation = true;
		}

		CompatMisc.runModHooksPostInit();

		Items.blaze_rod.setFull3D();
		Blocks.trapped_chest.setCreativeTab(CreativeTabs.tabRedstone);

		if (ConfigBlocksItems.enableOtherside) {
			ChestGenHooks.addItem(ChestGenHooks.STRONGHOLD_CORRIDOR, new WeightedRandomChestContent(ModItems.MUSIC_DISC_OTHERSIDE.get(), 0, 1, 1, 1));
			ChestGenHooks.addItem(ChestGenHooks.DUNGEON_CHEST, new WeightedRandomChestContent(ModItems.MUSIC_DISC_OTHERSIDE.get(), 0, 1, 1, 1));
		}

		if (ConfigBlocksItems.enablePrecipice) {
			ChestGenHooks.addItem(ChestGenHooks.STRONGHOLD_CORRIDOR, new WeightedRandomChestContent(ModItems.MUSIC_DISC_PRECIPICE.get(), 0, 1, 1, 1));
			ChestGenHooks.addItem(ChestGenHooks.DUNGEON_CHEST, new WeightedRandomChestContent(ModItems.MUSIC_DISC_PRECIPICE.get(), 0, 1, 1, 1));
		}

		if (ConfigBlocksItems.enableCreatorMusicBox) {
			ChestGenHooks.addItem(ChestGenHooks.STRONGHOLD_CORRIDOR, new WeightedRandomChestContent(ModItems.MUSIC_DISC_CREATOR_MUSIC_BOX.get(), 0, 1, 1, 1));
			ChestGenHooks.addItem(ChestGenHooks.DUNGEON_CHEST, new WeightedRandomChestContent(ModItems.MUSIC_DISC_CREATOR_MUSIC_BOX.get(), 0, 1, 1, 1));
		}

		if (ConfigBlocksItems.enableCreator) {
			ChestGenHooks.addItem(ChestGenHooks.STRONGHOLD_CORRIDOR, new WeightedRandomChestContent(ModItems.MUSIC_DISC_CREATOR.get(), 0, 1, 1, 1));
			ChestGenHooks.addItem(ChestGenHooks.DUNGEON_CHEST, new WeightedRandomChestContent(ModItems.MUSIC_DISC_CREATOR.get(), 0, 1, 1, 1));
		}

		if (ConfigBlocksItems.enable5) {
			ChestGenHooks.addItem(ChestGenHooks.STRONGHOLD_CORRIDOR, new WeightedRandomChestContent(ModItems.DISC_FRAGMENT_5.get(), 0, 1, 1, 1));
			ChestGenHooks.addItem(ChestGenHooks.DUNGEON_CHEST, new WeightedRandomChestContent(ModItems.DISC_FRAGMENT_5.get(), 0, 1, 1, 1));
		}

		if (ConfigBlocksItems.enablePigstep) {
			ChestGenHooks.addItem(NETHER_FORTRESS, new WeightedRandomChestContent(ModItems.MUSIC_DISC_PIGSTEP.get(), 0, 1, 1, 5));

			if (fortressWeightedField != null) {
				try {
					Field contents = ChestGenHooks.class.getDeclaredField("contents");
					contents.setAccessible(true);
					ArrayList<WeightedRandomChestContent> fortressContentList;
					fortressContentList = (ArrayList<WeightedRandomChestContent>) contents.get(ChestGenHooks.getInfo("netherFortress"));
					if (!fortressContentList.isEmpty()) {
						WeightedRandomChestContent[] fortressChest = new WeightedRandomChestContent[fortressContentList.size()];
						for (int i = 0; i < fortressContentList.size(); i++) {
							fortressChest[i] = fortressContentList.get(i);
						}
						fortressWeightedField.set(null, fortressChest);
					}
				} catch (Exception e) {
					System.out.println("Failed to fill Nether fortress loot table:");
					e.printStackTrace();
				}
			}
		}

		for (ModBlocks block : ModBlocks.VALUES) {
			if (block.isEnabled() && block.get() instanceof IInitAction) {
				((IInitAction) block.get()).postInitAction();
			}
		}
		for (ModItems item : ModItems.VALUES) {
			if (item.isEnabled() && item.get() instanceof IInitAction) {
				((IInitAction) item.get()).postInitAction();
			}
		}

		if (ConfigModCompat.elytraBaublesExpandedCompat > 0 && ModsList.BAUBLES_EXPANDED.isLoaded()) {
			CompatBaublesExpanded.postInit();
		}

		EtFuturumLootTables.init();

		ModRecipes.postInit();
		DeepslateOreRegistry.init();
		StrippedLogRegistry.init();
		RawOreRegistry.init();
		SmithingTableRecipes.init();
		CompostingRegistry.init();
		BeePlantRegistry.init();
		PistonBehaviorRegistry.init();

		// Initialize modern creative tab system
		ModdedCreativeTabs.init();
		ItemCategoryHelper.reassignAllItems();

		if (ModsList.TINKERS_CONSTRUCT.isLoaded()) {
			CompatTinkersConstruct.postInit();
		}
	}

	@EventHandler
	@SuppressWarnings("unchecked")
	public void onLoadComplete(FMLLoadCompleteEvent e) {
		for (ModBlocks block : ModBlocks.VALUES) {
			if (block.isEnabled() && block.get() instanceof IInitAction) {
				((IInitAction) block.get()).onLoadAction();
			}
		}
		for (ModItems item : ModItems.VALUES) {
			if (item.isEnabled() && item.get() instanceof IInitAction) {
				((IInitAction) item.get()).onLoadAction();
			}
		}

		ConfigBase.postInit();

		EtFuturumWorldGenerator.INSTANCE.postInit();
		WorldEventHandler.INSTANCE.postInit();

		if (ConfigSounds.newBlockSounds) {
			Blocks.jukebox.setStepSound(Block.soundTypeWood);
			Blocks.noteblock.setStepSound(Block.soundTypeWood);
			Blocks.heavy_weighted_pressure_plate.setStepSound(Block.soundTypeMetal);
			Blocks.light_weighted_pressure_plate.setStepSound(Block.soundTypeMetal);
			Blocks.tripwire_hook.setStepSound(Block.soundTypeWood);
			Blocks.lever.setStepSound(Block.soundTypeStone);
			Blocks.powered_repeater.setStepSound(Block.soundTypeStone);
			Blocks.unpowered_repeater.setStepSound(Block.soundTypeStone);
			Blocks.powered_comparator.setStepSound(Block.soundTypeStone);
			Blocks.unpowered_comparator.setStepSound(Block.soundTypeStone);
			Blocks.sponge.setStepSound(ModSounds.soundSponge);
		}
		if (ConfigSounds.paintingItemFramePlacing) {
			Block block = GameRegistry.findBlock("torchLevers", "paintingDoor");
			if(block != null) {
				block.stepSound = ModSounds.soundPainting;
			}
		}
		if (ConfigBlocksItems.enableDyedBeds) {
			Blocks.bed.blockMaterial = Material.wood;
			Blocks.bed.setStepSound(Block.soundTypeWood);
		}

		//Block registry iterator
		for (Block block : (Iterable<Block>) Block.blockRegistry) {
			if (ConfigFunctions.enableHoeMining) {
				if (block instanceof BlockLeaves || block instanceof BlockHay || block instanceof BlockSponge || block instanceof BlockNetherWart
						|| block instanceof BlockSculk || block instanceof BlockSculkCatalyst) {
					HoeRegistry.addToHoeArray(block);
				}
				HoeRegistry.addToHoeArray(ModBlocks.SHROOMLIGHT.get());
				HoeRegistry.addToHoeArray(ModBlocks.WET_SPONGE.get());
			}

			if (ConfigSounds.newBlockSounds) {
				/*
				 * SOUNDS
				 */
				String blockID = Block.blockRegistry.getNameForObject(block).split(":")[1].toLowerCase();

				SoundType sound = getCustomStepSound(block, blockID);
				if (sound != null) {
					block.setStepSound(sound);
				}

				setupMultiBlockSoundRegistry();
			}
		}

		CompatMisc.runModHooksLoadComplete();

		if (ConfigExperiments.netherDimensionProvider && !ModsList.NETHERLICIOUS.isLoaded()) {
			DimensionProviderEFRNether.init();
		}

		if (ConfigExperiments.endDimensionProvider) {
			DimensionProviderEFREnd.init(); // Come back to
		}
	}

	private void setupMultiBlockSoundRegistry() {
		MultiBlockSoundRegistry.addBasic(Blocks.stone_slab, ModSounds.soundNetherBricks, 6, 14);
		MultiBlockSoundRegistry.addBasic(Blocks.double_stone_slab, ModSounds.soundNetherBricks, 6, 14);

		MultiBlockSoundRegistry.addBasic(ExternalContent.Blocks.TCON_MULTIBRICK.get(), ModSounds.soundNetherrack, 2);
		MultiBlockSoundRegistry.addBasic(ExternalContent.Blocks.TCON_MULTIBRICK.get(), ModSounds.soundBoneBlock, 9);
		MultiBlockSoundRegistry.addBasic(ExternalContent.Blocks.TCON_MULTIBRICK_FANCY.get(), ModSounds.soundNetherrack, 2);
		MultiBlockSoundRegistry.addBasic(ExternalContent.Blocks.TCON_MULTIBRICK_FANCY.get(), ModSounds.soundBoneBlock, 9);

		MultiBlockSoundRegistry.addBasic(ModBlocks.DEEPSLATE_BRICK_WALL.get(), ModSounds.soundDeepslateTiles, 1);
		MultiBlockSoundRegistry.addBasic(ModBlocks.DEEPSLATE_BRICKS.get(), ModSounds.soundDeepslateTiles, 2, 3);
		MultiBlockSoundRegistry.addBasic(ModBlocks.DEEPSLATE_BRICK_SLAB.get(), ModSounds.soundDeepslateTiles, 1, 9);

		MultiBlockSoundRegistry.addBasic(ModBlocks.TUFF.get(), ModSounds.soundPolishedTuff, 1);
		MultiBlockSoundRegistry.addBasic(ModBlocks.TUFF.get(), ModSounds.soundTuffBricks, 2, 4);
		MultiBlockSoundRegistry.addBasic(ModBlocks.TUFF_WALL.get(), ModSounds.soundPolishedTuff, 1);
		MultiBlockSoundRegistry.addBasic(ModBlocks.TUFF_WALL.get(), ModSounds.soundTuffBricks, 2);
		MultiBlockSoundRegistry.addBasic(ModBlocks.TUFF_SLAB.get(), ModSounds.soundPolishedTuff, 1, 9);
		MultiBlockSoundRegistry.addBasic(ModBlocks.TUFF_SLAB.get(), ModSounds.soundTuffBricks, 2, 10);
		MultiBlockSoundRegistry.addBasic(ModBlocks.DOUBLE_TUFF_SLAB.get(), ModSounds.soundPolishedTuff, 1, 9);
		MultiBlockSoundRegistry.addBasic(ModBlocks.DOUBLE_TUFF_SLAB.get(), ModSounds.soundTuffBricks, 2, 10);

		MultiBlockSoundRegistry.addBasic(ModBlocks.AMETHYST_CLUSTER_1.get(), ModSounds.soundAmethystBudSmall, 0, 1, 2, 3, 4, 5, 6);
		MultiBlockSoundRegistry.addBasic(ModBlocks.AMETHYST_CLUSTER_2.get(), ModSounds.soundAmethystBudLrg, 0, 1, 2, 3, 4, 5, 6);

		MultiBlockSoundRegistry.addBasic(ModBlocks.WET_SPONGE.get(), ModSounds.soundWetSponge, 0);
		MultiBlockSoundRegistry.addBasic(Blocks.sponge, ModSounds.soundWetSponge, 1);

		MultiBlockSoundRegistry.addBasic(ModBlocks.CHERRY_SAPLING.get(), ModSounds.soundCherrySapling, 0, 8);
		MultiBlockSoundRegistry.addBasic(ModBlocks.CHERRY_LEAVES.get(), ModSounds.soundCherryLeaves, 0, 4, 8, 12);

		MultiBlockSoundRegistry.addBasic(ModBlocks.WOOD_PLANKS.get(), ModSounds.soundNetherWood, 0, 1);
		MultiBlockSoundRegistry.addBasic(ModBlocks.WOOD_PLANKS.get(), ModSounds.soundCherryWood, 3);
		MultiBlockSoundRegistry.addBasic(ModBlocks.WOOD_PLANKS.get(), ModSounds.soundBambooWood, 4);
		MultiBlockSoundRegistry.addBasic(ModBlocks.WOOD_FENCE.get(), ModSounds.soundNetherWood, 0, 1);
		MultiBlockSoundRegistry.addBasic(ModBlocks.WOOD_FENCE.get(), ModSounds.soundCherryWood, 3);
		MultiBlockSoundRegistry.addBasic(ModBlocks.WOOD_FENCE.get(), ModSounds.soundBambooWood, 4);

		MultiBlockSoundRegistry.addBasic(ModBlocks.WOOD_SLAB.get(), ModSounds.soundNetherWood, 0, 1, 8, 9);
		MultiBlockSoundRegistry.addBasic(ModBlocks.WOOD_SLAB.get(), ModSounds.soundCherryWood, 3, 11);
		MultiBlockSoundRegistry.addBasic(ModBlocks.WOOD_SLAB.get(), ModSounds.soundBambooWood, 4, 12);
		MultiBlockSoundRegistry.addBasic(ModBlocks.DOUBLE_WOOD_SLAB.get(), ModSounds.soundNetherWood, 0, 1, 8, 9);
		MultiBlockSoundRegistry.addBasic(ModBlocks.DOUBLE_WOOD_SLAB.get(), ModSounds.soundCherryWood, 3, 11);
		MultiBlockSoundRegistry.addBasic(ModBlocks.DOUBLE_WOOD_SLAB.get(), ModSounds.soundBambooWood, 4, 12);

		MultiBlockSoundRegistry.addBasic(ModBlocks.PACKED_MUD.get(), ModSounds.soundMudBricks, 1);

		if(ModsList.IRON_CHEST.isLoaded() && ModsList.IRON_CHEST.isVersionNewerOrEqual("6.0.78")) { // Version netherite chests were added in
			MultiBlockSoundRegistry.addBasic(ExternalContent.Blocks.IRON_CHEST.get(), ModSounds.soundNetherite, 8);
		}

		if (ExternalContent.Blocks.TCON_METAL.get() != null) {
			{
				BasicMultiBlockSound mbs = new BasicMultiBlockSound() {
					@Override
					public float getPitch(World world, int x, int y, int z, float pitch, MultiBlockSoundRegistry.BlockSoundType type) {
						if (type != MultiBlockSoundRegistry.BlockSoundType.WALK) {
							return pitch * .67F;
						}
						return 1;
					}
				};
				mbs.setTypes(ModSounds.soundCopper, 3);
				mbs.setTypes(ModSounds.soundCopper, 5);
				MultiBlockSoundRegistry.multiBlockSounds.put(ExternalContent.Blocks.TCON_METAL.get(), mbs);
			}
		}
	}

	/**
	 * As of 2.5.0, I removed some ItemBlocks that are just technical blocks (EG, lit EFR furnaces)
	 * We need to use this event since unregistering specifically an ItemBlock from a block makes Forge mistakenly think a save is corrupted.
	 * I add the EFR name check at the beginning just as a safety precaution.
	 * <p>
	 * Forge does some bad checks on if the item is an ItemBlock before letting you run ignoreItemBlock, leading to erroneous errors.
	 * It doesn't look much different than what I do above but their code rarely spits out "Cannot skip an ItemBlock that doesn't have a Block"
	 * Which makes no sense since if the block != null then we're skipping an ItemBlock that DOES have a block, if my block check != null then what else would it be?
	 * So their check must be wrong. Some of Forge's many registry finders are a little faulty at times.
	 * I already know we're running this on an item, and the only other requirement has a broken check.
	 * So I use reflection to force my way. It's rare for a save to actually throw an error, but just in case....
	 * They really should have just had an ITEMBLOCK mapping type to avoid all these hacky checks.
	 * <p>
	 * All this because Forge falsely declares a world corrupt if you remove an ItemBlock from an existing block.
	 * Gee, all that for removing ItemBlocks.
	 * I wrote this bad code to get around Forge's bad code, only to reveal EVEN MORE bad code in Forge I have to write even worse code to avoid.
	 */

	@EventHandler
	public void onMissingMapping(FMLMissingMappingsEvent e) {
		for (FMLMissingMappingsEvent.MissingMapping mapping : e.getAll()) {
			if (mapping.name.startsWith("etfuturum")) {
				if (Block.getBlockById(mapping.id) != null && mapping.type == GameRegistry.Type.ITEM) {
					mapping.ignore();
					ReflectionHelper.setPrivateValue(FMLMissingMappingsEvent.MissingMapping.class, mapping, FMLMissingMappingsEvent.Action.BLOCKONLY, "action");
				}
			}
			if (mapping.name.equals("etfuturum:glow_berries")) {
				mapping.ignore();
			}
		}
	}


	public SoundType getCustomStepSound(Block block, String namespace) {
		if (block.stepSound == Block.soundTypePiston || block.stepSound == Block.soundTypeStone) {
			if (namespace.contains("nether") && namespace.contains("brick")) {
				return ModSounds.soundNetherBricks;
			} else if (namespace.contains("netherrack") || namespace.contains("hellfish")) {
				return ModSounds.soundNetherrack;
			} else if (block == Blocks.quartz_ore || (namespace.contains("nether") && (block instanceof BlockOre || namespace.contains("ore")))) {
				return ModSounds.soundNetherOre;
			} else if (namespace.contains("deepslate")) {
				return namespace.contains("brick") ? ModSounds.soundDeepslateBricks : ModSounds.soundDeepslate;
			} else if (block instanceof BlockNetherWart || (namespace.contains("nether") && namespace.contains("wart"))) {
				return ModSounds.soundCropWarts;
			} else if (namespace.contains("bone") || namespace.contains("ivory")) {
				return ModSounds.soundBoneBlock;
			}
		}

		if (block.stepSound == Block.soundTypeGrass) {
			if (block instanceof BlockVine) {
				return ModSounds.soundVines;
			}

			if (block instanceof BlockLilyPad) {
				return ModSounds.soundWetGrass;
			}
		}

		if (block instanceof BlockCrops || block instanceof BlockStem) {
			return ModSounds.soundCrops;
		}

		if (block.stepSound == Block.soundTypeSand && namespace.contains("soul") && namespace.contains("sand")) {
			return ModSounds.soundSoulSand;
		}

		if (block.stepSound == Block.soundTypeMetal && (namespace.contains("copper") || namespace.contains("tin"))) {
			return ModSounds.soundCopper;
		}

		if (block.getMaterial() == Material.iron && block instanceof BlockHopper) {
			return Block.soundTypeMetal;
		}

		return null;
	}

	@EventHandler
	public void serverStarting(FMLServerStartingEvent event) {
		if (ConfigFunctions.enableFillCommand) {
			event.registerServerCommand(new CommandFill());
		}
	}

	@EventHandler
	public void serverStopped(FMLServerStoppedEvent event) {
		ServerEventHandler.INSTANCE.onServerStopped();
	}

	/**
	 * Utility for running string.contains() on a list of strings.
	 */
	public static boolean stringListContainsPhrase(Set<String> set, String string) {
		for (String stringInSet : set) {
			if (string.contains(stringInSet)) {
				return true;
			}
		}
		return false;
	}

	public static List<String> getOreStrings(ItemStack stack) {
		final List<String> list = new ArrayList<>();
		for (int oreID : OreDictionary.getOreIDs(stack)) {
			list.add(OreDictionary.getOreName(oreID));
		}
		return list;
	}

	public static boolean hasDictTag(Block block, String... tags) {
		return hasDictTag(new ItemStack(block), tags);
	}

	public static boolean hasDictTag(Item item, String... tags) {
		return hasDictTag(new ItemStack(item), tags);
	}

	public static boolean hasDictTag(ItemStack stack, String... tags) {
		for (String oreName : getOreStrings(stack)) {
			if (ArrayUtils.contains(tags, oreName)) {
				return true;
			}
		}
		return false;
	}

	public static boolean dictTagsStartWith(Block block, String stringToFind) {
		return dictTagsContain(new ItemStack(block), stringToFind);
	}

	public static boolean dictTagsStartWith(Item item, String stringToFind) {
		return dictTagsContain(new ItemStack(item), stringToFind);
	}

	public static boolean dictTagsStartWith(ItemStack stack, String stringToFind) {
		for (String oreName : getOreStrings(stack)) {
			if (oreName.startsWith(stringToFind)) {
				return true;
			}
		}
		return false;
	}

	public static boolean dictTagsContain(Block block, String stringToFind) {
		return dictTagsContain(new ItemStack(block), stringToFind);
	}

	public static boolean dictTagsContain(Item item, String stringToFind) {
		return dictTagsContain(new ItemStack(item), stringToFind);
	}

	public static boolean dictTagsContain(ItemStack stack, String stringToFind) {
		for (String oreName : getOreStrings(stack)) {
			if (oreName.contains(stringToFind)) {
				return true;
			}
		}
		return false;
	}

	public static PotionEffect getSuspiciousStewEffect(ItemStack stack) {

		if (stack == null)
			return null;

		Item item = stack.getItem();

		if (item == Item.getItemFromBlock(Blocks.red_flower)) {
			switch (stack.getItemDamage()) {
				default:
				case 0:
					return new PotionEffect(Potion.nightVision.id, 100, 0);
				case 1:
					return new PotionEffect(Potion.field_76443_y.id, 7, 0); // saturation
				case 2:
					return new PotionEffect(Potion.fireResistance.id, 80, 0);
				case 3:
					return new PotionEffect(Potion.blindness.id, 160, 0);
				case 4:
				case 5:
				case 6:
				case 7:
					return new PotionEffect(Potion.weakness.id, 180, 0);
				case 8:
					return new PotionEffect(Potion.regeneration.id, 160, 0);
			}
		}

		if (item == Item.getItemFromBlock(Blocks.yellow_flower)) {
			return new PotionEffect(Potion.field_76443_y.id, 7, 0); // saturation
		}

		if (item == Item.getItemFromBlock(ModBlocks.CORNFLOWER.get())) {
			return new PotionEffect(Potion.jump.id, 120, 0);
		}

		if (item == Item.getItemFromBlock(ModBlocks.LILY_OF_THE_VALLEY.get())) {
			return new PotionEffect(Potion.poison.id, 240, 0);
		}

		if (item == Item.getItemFromBlock(ModBlocks.WITHER_ROSE.get())) {
			return new PotionEffect(Potion.wither.id, 160, 0);
		}
		return null;
	}

}
