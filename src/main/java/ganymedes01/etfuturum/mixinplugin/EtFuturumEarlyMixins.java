package ganymedes01.etfuturum.mixinplugin;

import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import ganymedes01.etfuturum.Tags;
import ganymedes01.etfuturum.compat.CompatMisc;
import ganymedes01.etfuturum.configuration.ConfigBase;
import ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems;
import ganymedes01.etfuturum.configuration.configs.ConfigEnchantsPotions;
import ganymedes01.etfuturum.configuration.configs.ConfigEntities;
import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.configuration.configs.ConfigTweaks;
import ganymedes01.etfuturum.configuration.configs.ConfigWorld;
import ganymedes01.etfuturum.core.utils.Logger;
import ganymedes01.etfuturum.lib.Reference;
import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import net.minecraft.launchwrapper.Launch;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ganymedes01.etfuturum.lib.Reference.MOD_ID;

@IFMLLoadingPlugin.Name("EtFuturumEarlyMixins")
@IFMLLoadingPlugin.MCVersion("1.7.10")
public class EtFuturumEarlyMixins implements IFMLLoadingPlugin, IEarlyMixinLoader {

	public static final MixinEnvironment.Side side = MixinEnvironment.getCurrentEnvironment().getSide();

	public void initConfigs() {
		final String configDir = "config" + File.separator + MOD_ID;

//	  File from before Et Futurum Requiem (Not in a subdirectory)
		File olderFile = new File(Launch.minecraftHome, "config" + File.separator + "etfuturum.cfg");
//	  File from before Et Futurum Requiem 2.2.2
		File oldFile = new File(Launch.minecraftHome, configDir + File.separator + "etfuturum.cfg");

		oldFile.getParentFile().mkdirs();
		if (olderFile.exists()) {
			try {
				Files.copy(olderFile.toPath(), oldFile.toPath());
			} catch (Exception e) {
				Logger.error("Failed to copy old config file: " + e, e);
			}
			olderFile.delete();
			Reference.launchConfigWarning = true;
		}

		if (oldFile.exists()) {
			Reference.launchConfigWarning = true;
		}

		ConfigBase.initializeConfigs();

		CompatMisc.doLwjgl3ifyCompat();
	}

	@Override
	public String getMixinConfig() {
		return "mixins." + MOD_ID + ".early.json";
	}

	@Override
	public List<String> getMixins(Set<String> loadedCoreMods) {

		initConfigs();

		List<String> mixins = new ArrayList<>();

		// Angelica is shipped as a launch tweaker, not an FML coremod, so its name
		// never shows up in loadedCoreMods; probe the classloader for its tweaker
		// class instead (its jar is on the launch classpath by mixin evaluation time).
		final boolean angelicaLoaded = loadedCoreMods.stream().anyMatch(name -> name.contains("angelica"))
				|| isClassPresent("com.gtnewhorizons.angelica.loading.AngelicaClientTweaker")
				|| Launch.classLoader.getResource("mixins.angelica.early.json") != null;

		if (ConfigMixins.endPortalSandDuping) {
			mixins.add("endportal.MixinBlockEndPortal");
			mixins.add("endportal.MixinTeleporter");
		}

		if (ConfigBlocksItems.enableNewBoats) {
			mixins.add("boatstat.MixinEntityPlayer");
			if (side == MixinEnvironment.Side.CLIENT) {
				mixins.add("boatcamera.client.MixinEntity");
			}
		}

		if (ConfigMixins.fenceWallConnectFix) {
			mixins.add("fencewallconnect.MixinBlockWall");
			mixins.add("fencewallconnect.MixinBlockFence");
		}

		if (ConfigMixins.stepHeightFix) {
			mixins.add("stepfix.MixinEntity");
		}

		if (ConfigMixins.enableSpectatorMode) {
			mixins.add("spectator.MixinGameType");
			mixins.add("spectator.MixinEntity");
			mixins.add("spectator.MixinWorld");
			mixins.add("spectator.MixinWorldServer");
			mixins.add("spectator.MixinEntityPlayer");
			mixins.add("spectator.MixinNetHandlerPlayServer");
			mixins.add("spectator.MixinInventoryPlayer");
			mixins.add("spectator.MixinContainerChest");
			mixins.add("spectator.MixinSlot");
			mixins.add("spectator.MixinCommandGameMode");
			if (side == MixinEnvironment.Side.CLIENT) {
				mixins.add("spectator.client.MixinEntityRenderer");
				mixins.add("spectator.client.MixinEntityPlayer");
				mixins.add("spectator.client.MixinWorldRenderer");
			}
		}

		if (ConfigMixins.avoidDroppingItemsWhenClosing) {
			mixins.add("closedrops.MixinEntityPlayerMP");
		}

		if (ConfigMixins.enableElytra) {
			mixins.add("backlytra.MixinEntityPlayer");
			mixins.add("backlytra.MixinEntityLivingBase");
			mixins.add("backlytra.MixinNetHandlerPlayServer");
			mixins.add("backlytra.MixinEntityTrackerEntry");
			if (loadedCoreMods.stream().anyMatch(name -> name.contains("thaumcraft"))) {
				mixins.add("backlytra.thaumcraft.MixinEventHandlerEntity");
			}
			if (side == MixinEnvironment.Side.CLIENT) {
				mixins.add("backlytra.client.MixinAbstractClientPlayer");
				mixins.add("backlytra.client.MixinEntityPlayerSP");
				mixins.add("backlytra.client.MixinRenderPlayer");
				mixins.add("backlytra.client.MixinModelBiped");
				mixins.add("backlytra.client.MixinEntityRenderer");
			}
		}

		if (ConfigMixins.enableDoWeatherCycle) {
			mixins.add("doweathercycle.MixinCommandHandler");
			mixins.add("doweathercycle.MixinWorldInfo");
		}

		if (ConfigMixins.enableRandomTickSpeed) {
			mixins.add("randomtickspeed.MixinWorldServer");
			mixins.add("randomtickspeed.MixinGameRules");
		}

		if ((ConfigWorld.endFlashes || ConfigWorld.modernEndAmbientColor || ConfigWorld.modernBlockLightTint)
				&& side == MixinEnvironment.Side.CLIENT) {
			mixins.add("endflashes.client.MixinEntityRenderer");
		}

		if (ConfigWorld.endFlashes && side == MixinEnvironment.Side.CLIENT) {
			mixins.add("endflashes.client.MixinRenderGlobal");
		}

		if (ConfigWorld.modernLightmapGamma && side == MixinEnvironment.Side.CLIENT) {
			mixins.add("modernlightmap.client.MixinEntityRenderer");
		}

		if ((ConfigWorld.modernBlockLightTint || ConfigWorld.modernNightVision)
				&& side == MixinEnvironment.Side.CLIENT) {
			mixins.add("modernblocklight.client.MixinEntityRenderer");
		}

		if (ConfigMixins.creativeFlightSpeedModifier > 1 || ConfigTweaks.creativeFlightVerticalModifier > 1) {
			mixins.add("flyspeed.MixinEntityPlayer");
		}

		if (ConfigMixins.bouncyBeds) {
			mixins.add("bouncybeds.MixinBlockBed");
		}

		if (ConfigMixins.newHurtSounds) {
			mixins.add("sounds.MixinEntityPlayer");
			if (side == MixinEnvironment.Side.CLIENT) {
				mixins.add("sounds.client.MixinEntityClientPlayerMP");
			}
		}

		if (ConfigMixins.newMobSounds) {
			mixins.add("sounds.MixinEntitySnowman");
			mixins.add("sounds.MixinEntitySkeleton");
			mixins.add("sounds.MixinEntitySquid");
			mixins.add("sounds.MixinEntityWitch");
		}

		if (ConfigMixins.floorCeilingButtons) {
			mixins.add("floorceilbutton.MixinBlockButton");
		}

		if (ConfigMixins.newEnderEyeSounds) {
			mixins.add("sounds.MixinItemEnderEye");
			mixins.add("sounds.MixinEntityEnderEye");
		}

		if (ConfigMixins.newEnchantingSounds) {
			mixins.add("sounds.MixinContainerEnchantment");
		}

		if (ConfigMixins.newFishingRodSounds) {
			mixins.add("sounds.MixinItemFishingRod");
		}

		if (ConfigMixins.newBeaconSounds) {
			mixins.add("sounds.MixinTileEntityBeacon");
		}

		if (ConfigMixins.hoeTilling) {
			mixins.add("sounds.MixinItemHoe");
		}

		if (ConfigMixins.enableObservers) {
			mixins.add("observer.MixinWorld");
			mixins.add("observer.MixinWorldServer");
			mixins.add("observer.MixinChunk");
		}

		if (ConfigMixins.noteBlockObservable) {
			mixins.add("noteblockupdate.MixinBlockNote");
		}

		if (ConfigMixins.creativeEnderPearls) {
			mixins.add("enderpearl.MixinItemEnderPearl");
		}

		if (ConfigMixins.betterBookTextures && side == MixinEnvironment.Side.CLIENT) {
			mixins.add("enchantedbook.MixinItemEnchantedBook");
			mixins.add("enchantedbook.MixinEntityLivingBaseEnchantedBook");
		}

		if (ConfigMixins.betterSpawnEggTextures && side == MixinEnvironment.Side.CLIENT) {
			mixins.add("spawn_egg.MixinItemMonsterPlacerSpawnEgg");
		}

		if (ConfigMixins.betterDyeTextures && side == MixinEnvironment.Side.CLIENT) {
			mixins.add("dye.MixinItemDye");
			mixins.add("dye.MixinBaseSubtypesItemDye");
		}

		if ((ConfigMixins.betterVanillaTextures || ConfigMixins.secretTotem) && side == MixinEnvironment.Side.CLIENT) {
			if (ConfigMixins.betterVanillaTextures) {
				mixins.add("moderntextures.MixinBlockModernIcons");
			}
			mixins.add("moderntextures.MixinItemModernIcons");
		}

		if (ConfigMixins.arrowFallingFix) {
			mixins.add("fallingarrowfix.MixinEntityArrow");
		}

		if (ConfigMixins.blockHopperInteraction) {
			mixins.add("blockinventories.MixinTileEntityHopper");
		}

		if (ConfigMixins.collidedThrowableFix) {
			mixins.add("projectilecollidefix.MixinEntityThrowable");
		}

		if (ConfigMixins.postTreeGenEvent && ConfigEntities.enableBees) {
			mixins.add("posttreegen.MixinWorldGenAbstractTree");
		}

		if (ConfigMixins.megaSprucePodzol) {
			mixins.add("megapine.MixinWorldGenMegaPineTree");
		}

		if (ConfigMixins.ladderTrapdoors) {
			mixins.add("laddertrapdoors.MixinBlockLadder");
			mixins.add("laddertrapdoors.MixinBlockTrapdoor");
		}

		if (ConfigMixins.betterPistons) {
			mixins.add("backinslime.MixinBlockPistonBase");
		}

		if (ConfigMixins.soulFire) {
			mixins.add("soulfire.MixinBlockFire");
			if(loadedCoreMods.stream().anyMatch(name -> name.contains("rple"))) { //RPLE compat
				mixins.add("soulfire.MixinBlockFireRPLE");
			}
			mixins.add("soulfire.MixinEntity");
			if (side == MixinEnvironment.Side.CLIENT) {
				mixins.add("soulfire.MixinRenderBlocks");
			}
		}

		if (ConfigMixins.hideSingleLevelEnchants) {
			mixins.add("singlelevel.MixinEnchantment");
		}

		if (ConfigMixins.fireproofItems) {
			mixins.add("uninflammableitem.MixinEntityItem");
		}

        if (ConfigMixins.enableJumpClimbing)
        {
            mixins.add("isLadderFix.MixinEntityLivingBase");
        }

		if (ConfigBlocksItems.enableScaffolding) {
			mixins.add("scaffolding.MixinEntityLivingBase");
		}

		if (ConfigMixins.adjustedLiquidPhysics) {
			mixins.add("liquidphysics.MixinEntity");
			mixins.add("liquidphysics.MixinWorld");
			mixins.add("liquidphysics.MixinBlockLiquid");
		}

		if (ConfigMixins.liquidItemFloat) {
			mixins.add("liquidphysics.MixinEntityItem");
		}

		if (ConfigMixins.riddenHorsesInWater) {
			mixins.add("horsewater.MixinEntityHorse");
		}

		if (side == MixinEnvironment.Side.CLIENT) {
			// Creative tab layout derives from array position instead of the (final)
			// tabIndex field, so UNCLASSIFIED can be fixed as the first tab of page 2.
			mixins.add("creativetabs.MixinCreativeTabs");
			// Creative tab row count fix: always applied.
			mixins.add("creative.MixinGuiContainerCreative");
			mixins.add("creative.MixinGuiContainerCreative$ContainerCreative");

			if (ConfigMixins.dustUnderFallingBlocks) {
				mixins.add("blockfallingparticles.client.MixinBlockFalling");
			}

			if (ConfigMixins.adjustedAttenuation) {
				mixins.add("sounds.client.MixinSoundManager_Attenuation");
			}

			if (ConfigMixins.boundedBlockBreakingParticles) {
				mixins.add("boundedparticles.client.MixinEffectRenderer");
			}

			if (ConfigMixins.furnaceCrackle) {
				mixins.add("sounds.client.MixinBlockFurnace");
			}

			if (ConfigEnchantsPotions.enableSwiftSneak) {
				mixins.add("swiftsneak.client.MixinMovementInputFromOptions");
			}

			if (ConfigMixins.flowerPotFixes) {
				mixins.add("flowerpotfix.client.MixinRenderBlocks");
			}

			if (ConfigMixins.colorGrassBlockItemSides) {
				mixins.add("coloredgrassitem.client.MixinRenderBlocks");
			}

			if (ConfigMixins.worldSaveThumbnails) {
				mixins.add("worldthumbnail.client.MixinGuiSelectWorld");
				mixins.add("worldthumbnail.client.MixinGuiSelectWorldList");
			}

			if (ConfigMixins.modernLoadingScreen) {
				mixins.add("worldthumbnail.client.MixinLoadingScreenRenderer");
				mixins.add("worldthumbnail.client.MixinGuiDownloadTerrain");
				mixins.add("worldthumbnail.client.MixinMinecraft_LoadingBridge");
				mixins.add("worldthumbnail.MixinMinecraftServer");
				mixins.add("worldthumbnail.MixinChunkProviderServer");
			}
		}

		if (ConfigMixins.thinPanes) {
			mixins.add("thinpanes.MixinBlockPane");
			if (side == MixinEnvironment.Side.CLIENT) {
				mixins.add("thinpanes.MixinRenderBlocks");
			}
		}

		if (ConfigMixins.enablePlayersSleepingPecentageGamerule) {
			mixins.add("playerssleepingpercentage.MixinWorldServer");
		}

		if (ConfigEntities.enableFoxes) {
			mixins.add("foxes.MixinEntityLivingBase");
			mixins.add("foxes.MixinEntityWolf");
		}

		if (ConfigBlocksItems.enableSponge) {
			mixins.add("sponge.MixinBlockSponge");
		}

		// Real waterlogging (26.2-style, metadata bit 16): needs NEIDs' 16-bit
		// metadata patches for storage/sync (probed via its mixin config resource,
		// same trick as the Angelica probe above) on both sides, and the
		// disableWaterlogging config off. The bucket + liquid-ray mixins below are
		// shared with the ocean plants feature and registered on the union of both
		// feature flags.
		final boolean neidPresent = Launch.classLoader.getResource("mixins.neid.early.json") != null;
		final boolean waterlogOn = !ConfigMixins.disableWaterlogging && neidPresent;
		WaterlogRuntime.debug("probe: neidPresent=" + neidPresent + ", disableWaterlogging=" + ConfigMixins.disableWaterlogging);

		if (ConfigBlocksItems.enableOceanPlants) {
			mixins.add("oceanplants.MixinBlockLiquid");
			mixins.add("oceanplants.MixinBlockDynamicLiquidBackfill");
			mixins.add("oceanplants.MixinBlockFalling");
			mixins.add("oceanplants.MixinItemDye");
			if (side == MixinEnvironment.Side.CLIENT) {
				mixins.add("oceanplants.client.MixinRenderBlocks");
				mixins.add("oceanplants.client.MixinItemRenderer");
			}
		}

		// The flow-invoker interface (backing the sea pickle and waterlog spread)
		// plus the bucket scoop/drain + full-cell liquid ray clip, serving both the
		// sea pickle (needs the blocks, so only with ocean plants on) and the true
		// waterlogging family (needs NEIDs + config). All identity checks inside
		// are null-safe for whichever feature is absent.
		if (ConfigBlocksItems.enableOceanPlants || waterlogOn) {
			mixins.add("oceanplants.MixinBlockDynamicLiquid");
			mixins.add("oceanplants.MixinItemBucket");
			mixins.add("oceanplants.MixinWorld");
		}

		// Angelica's shader hand pipeline routes pass-1 blocks to the translucent
		// hand pass (enableBlend + depthMask(false)), which shatters the flat 2D
		// card geometry of renderItemIn2D into a pixel-slice mesh. Exempt flat-icon
		// items from the translucency check: items-atlas sprites (ocean plants) and
		// block items drawn as 2D cards rather than 3D models (the backported
		// stained glass panes, renderType 41). Independent of the ocean plants
		// feature; only registered when Angelica's loading plugin is present (its
		// HandRenderer is targeted).
		if (side == MixinEnvironment.Side.CLIENT && angelicaLoaded) {
			mixins.add("oceanplants.client.MixinHandRenderer");
		}

		// Underwater visual fixes for real water, independent of any waterlogged
		// block feature: fog the sunrise/sunset sky band while the eye is
		// underwater (closes the water/sky seam, keeps sun/moon/stars crisp) and
		// dim the underwater overlay to 26.2's alpha 0.1.
		if (side == MixinEnvironment.Side.CLIENT) {
			mixins.add("waterlog.client.MixinRenderGlobal");
			mixins.add("waterlog.client.MixinItemRenderer");
		}

		if (waterlogOn) {
			WaterlogRuntime.debug("=== BUILD " + WaterlogRuntime.BUILD_TAG + " ===");
			mixins.add("waterlog.MixinItemBlock");
			mixins.add("waterlog.MixinBlockStairs");
			mixins.add("waterlog.MixinBlockSlab");
			mixins.add("waterlog.MixinBlockPane");
			mixins.add("waterlog.MixinBlockFence");
			mixins.add("waterlog.MixinBlockWall");
			mixins.add("waterlog.MixinBlockLadder");
			mixins.add("waterlog.MixinBlockTrapDoor");
			// Chest/EnderChest family extension (26.2 ChestBlock/EnderChestBlock
			// WATERLOGGED): base-class generic like stairs - modded wooden chests
			// extending BlockChest inherit automatically (trapped chest = separate
			// Block instance, covered too). Placement/merge meta rewrites preserve
			// bit16 (pure preservation - they may target neighbour cells); TESR
			// facing switches get & 15 masks client-side.
			mixins.add("waterlog.MixinBlockChest");
			mixins.add("waterlog.MixinBlockEnderChest");
			// Lantern/Chain family extension (26.2 LanternBlock/ChainBlock
			// implements SimpleWaterloggedBlock): lantern uses mixed-mode
			// (declared onNeighborBlockChange via @Inject HEAD, rest via
			// extends + @Override), chain is pure extends + @Override. Bare
			// meta strip points live in BlockLantern source (bounds/placement
			// support checks); both renderers are bit16-transparent
			// (lantern %2, chain switch+default).
			mixins.add("waterlog.MixinBlockLantern");
			mixins.add("waterlog.MixinBlockChain");
			mixins.add("waterlog.MixinBlockStainedGlassPane");
			mixins.add("waterlog.MixinBlockDynamicLiquid");
			mixins.add("waterlog.MixinBlockLiquid");
			mixins.add("waterlog.MixinEntity");
			mixins.add("waterlog.MixinEntityLivingBase");
			mixins.add("waterlog.MixinEntityFallingBlock");
			mixins.add("waterlog.MixinWorld");
			// 26.2 explosion semantics (waterlogged blocks block blast rays via
			// their water FluidState resistance).
			mixins.add("waterlog.MixinExplosion");
			WaterlogRuntime.debug("registered: ItemBlock/BlockStairs/BlockSlab/BlockPane/Fence/Wall/Ladder/TrapDoor/Chest/EnderChest/Lantern/Chain/StainedGlassPane/Entity/EntityLivingBase/World waterlog mixins + Explosion");
			if (side == MixinEnvironment.Side.CLIENT) {
				mixins.add("waterlog.client.MixinBlockLiquid");
				// Wall getIcon is @SideOnly(CLIENT) (SideTransformer strips it on
				// dedicated servers) - the bit16 strip must be a client mixin.
				mixins.add("waterlog.client.MixinBlockWall");
				// EnderChest TESR face-switches on raw metadata - mask reads so
				// waterlogged cells (16+meta) keep facing (trapdoor Bug B class).
				// The regular chest TESR mask registers below on the
				// waterlog ∪ chestpair union (bits 32/64/128 need it too).
				mixins.add("waterlog.client.MixinTileEntityEnderChestRenderer");
				// Underwater fog (density + colour) goes through
				// ActiveRenderInfo.getBlockAtEntityViewpoint in both pipelines.
				mixins.add("waterlog.client.MixinActiveRenderInfo");
				// Paint hook + member-aware liquid heights: shared by the vanilla
				// pipeline and Angelica's celeritas pipeline alike.
				mixins.add("waterlog.client.MixinRenderBlocks");
				// 20261018-A 水闪烁修复：S23/S22 迟到回声过滤（客户端预测登记在
				// MixinItemBucket 桶路径 + WaterlogRuntime.markWaterlogged）
				mixins.add("waterlog.client.MixinNetHandlerPlayClient");
				if (angelicaLoaded) {
					mixins.add("waterlog.client.MixinAngelicaChunkBuilderMeshingTask");
					WaterlogRuntime.debug("registered: celeritas paint mixins (angelicaLoaded=true)");
				} else {
					mixins.add("waterlog.client.MixinWorldRenderer");
					WaterlogRuntime.debug("registered: vanilla-pipeline paint mixins (angelicaLoaded=false)");
				}
			}
		}

		if (ConfigFunctions.enableSilkTouchingMushrooms) {
			mixins.add("mushroom.MixinBlockHugeMushroom");
		}

		// Modern chest pairing (26.2 ChestBlock TYPE state machine: same-facing
		// perpendicular merge, TYPE stored in meta bits 32/64/128, never auto
		// re-merging SINGLEs). Shares the NEIDs extended-metadata gate with
		// waterlogging since the type bits need the same storage/sync patches.
		final boolean chestPairOn = ConfigMixins.modernChestPairing && neidPresent;
		if (chestPairOn) {
			mixins.add("chestpair.MixinBlockChest");
			mixins.add("chestpair.MixinTileEntityChest");
		}
		// Chest TESR face-switch masks: waterlog (bit 16) and chestpair
		// (bits 32/64/128) both live above the vanilla facing bits, so the
		// mask registers on the union of both features.
		if (side == MixinEnvironment.Side.CLIENT && (waterlogOn || chestPairOn)) {
			mixins.add("waterlog.client.MixinTileEntityChestRenderer");
		}

		if (ConfigFunctions.enableFloatingTrapDoors && side == MixinEnvironment.Side.CLIENT) {
			mixins.add("floatingtrapdoors.MixinBlockTrapdoorFacing");
		}

		if (ConfigMixins.mushroomGrowBreaksEndPortalFrame) {
			mixins.add("mushroom.MixinWorldGenBigMushroom");
		}

		// Shears durability aligned with 26.2: always applied.
		mixins.add("shears.MixinItemShears");

		// Enchanted golden apple gets its own display name (like modern versions): always applied.
		mixins.add("goldenapple.MixinItemAppleGold");

		return mixins;
	}

	private static boolean isClassPresent(String className) {
		try {
			Launch.classLoader.loadClass(className);
			return true;
		} catch (Throwable t) {
			return false;
		}
	}

	@Override
	public String[] getASMTransformerClass() {
		return null;
	}

	@Override
	public String getModContainerClass() {
		return null;
	}

	@Override
	public String getSetupClass() {
		return null;
	}

	@Override
	public void injectData(Map<String, Object> data) {

	}

	@Override
	public String getAccessTransformerClass() {
		return null;
	}
}
