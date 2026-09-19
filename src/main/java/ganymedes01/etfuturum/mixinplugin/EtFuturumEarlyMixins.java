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

		if (ConfigBlocksItems.enableOceanPlants) {
			mixins.add("oceanplants.MixinBlockLiquid");
			mixins.add("oceanplants.MixinBlockDynamicLiquid");
			mixins.add("oceanplants.MixinBlockDynamicLiquidBackfill");
			mixins.add("oceanplants.MixinItemBucket");
			mixins.add("oceanplants.MixinBlockFalling");
			mixins.add("oceanplants.MixinItemDye");
			mixins.add("oceanplants.MixinWorld");
			if (side == MixinEnvironment.Side.CLIENT) {
				mixins.add("oceanplants.client.MixinRenderBlocks");
				mixins.add("oceanplants.client.MixinItemRenderer");
				// Angelica's shader hand pipeline routes pass-1 blocks to the translucent
				// hand pass; flat-icon items must be exempted. Only registered when
				// Angelica's loading plugin is present (its HandRenderer is targeted).
				if (angelicaLoaded) {
					mixins.add("oceanplants.client.MixinHandRenderer");
				}
			}
		}

		if (ConfigMixins.stairWaterlogging) {
			// Stateless waterlogged stairs: the look re-derives on section rebuilds,
			// which are triggered by client-side water changes next to stairs.
			mixins.add("waterlog.MixinWorld");
			if (side == MixinEnvironment.Side.CLIENT) {
				// Water shell suppression applies to both pipelines.
				mixins.add("waterlog.client.MixinRenderBlocksShell");
				if (angelicaLoaded) {
					// Celeritas owns chunk building when Angelica is loaded: reroute the
					// qualifying stairs' single render call into the translucent pass and
					// paint the complement water before the buffer capture (Angelica's own
					// HEAD gate skips func_147805_b for vanilla render types, so the
					// RenderBlocks TAIL never fires for stairs there).
					mixins.add("waterlog.client.MixinAngelicaChunkBuilderMeshingTask");
				} else {
									// Vanilla chunk builder: the stair keeps
									// rendering normally in pass 0; its water
									// complement is a translucent volume, so the
									// pass gates make qualifying stairs report
									// pass 1, where the stair's pass-1 slot
									// paints the complement water instead of a
									// geometry copy (MixinWorldRenderer +
									// MixinRenderBlocks).
									mixins.add("waterlog.client.MixinWorldRenderer");
									mixins.add("waterlog.client.MixinRenderBlocks");
								}
			}
		}

		if (ConfigFunctions.enableSilkTouchingMushrooms) {
			mixins.add("mushroom.MixinBlockHugeMushroom");
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
