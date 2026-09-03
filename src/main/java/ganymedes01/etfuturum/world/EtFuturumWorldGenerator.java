package ganymedes01.etfuturum.world;

import com.google.common.collect.Lists;
import cpw.mods.fml.common.IWorldGenerator;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.blocks.BlockChorusFlower;
import ganymedes01.etfuturum.compat.ModsList;
import ganymedes01.etfuturum.configuration.configs.ConfigWorld;
import ganymedes01.etfuturum.core.utils.Utils;
import ganymedes01.etfuturum.world.end.dimension.WorldProviderEFREnd;
import ganymedes01.etfuturum.world.generate.WorldGenDeepslateLayerBlob;
import ganymedes01.etfuturum.world.generate.WorldGenMinableCustom;
import ganymedes01.etfuturum.world.generate.decorate.WorldGenBamboo;
import ganymedes01.etfuturum.world.generate.decorate.WorldGenCaveVines;
import ganymedes01.etfuturum.world.generate.decorate.WorldGenCherryTrees;
import ganymedes01.etfuturum.world.generate.decorate.WorldGenGlowLichen;
import ganymedes01.etfuturum.world.generate.decorate.WorldGenLushCave;
import ganymedes01.etfuturum.world.generate.decorate.WorldGenPinkPetals;
import ganymedes01.etfuturum.world.generate.feature.WorldGenFossil;
import ganymedes01.etfuturum.world.generate.feature.WorldGenGeode;
import ganymedes01.etfuturum.world.structure.OceanMonument;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderEnd;
import net.minecraft.world.WorldProviderHell;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderFlat;

import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraft.world.gen.feature.WorldGenClay;
import net.minecraft.world.gen.feature.WorldGenFlowers;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraft.world.gen.feature.WorldGenerator;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.util.ForgeDirection;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class EtFuturumWorldGenerator implements IWorldGenerator {

	public static final EtFuturumWorldGenerator INSTANCE = new EtFuturumWorldGenerator();

	protected final List<WorldGenMinable> stoneGen = new LinkedList<WorldGenMinable>();

	protected final WorldGenMinable copperGen = new WorldGenMinable(ModBlocks.COPPER_ORE.get(), ConfigWorld.maxCopperPerCluster);

	protected final WorldGenMinable magmaGen = new WorldGenMinable(ModBlocks.MAGMA.get(), ConfigWorld.maxMagmaPerCluster, Blocks.netherrack);
	protected final WorldGenMinable netherGoldGen = new WorldGenMinable(ModBlocks.NETHER_GOLD_ORE.get(), ConfigWorld.maxNetherGoldPerCluster, Blocks.netherrack);
	protected final WorldGenMinable debrisGen = new WorldGenMinableCustom(ModBlocks.ANCIENT_DEBRIS.get(), ConfigWorld.debrisMax, Blocks.netherrack);
	protected final WorldGenMinable smallDebrisGen = new WorldGenMinableCustom(ModBlocks.ANCIENT_DEBRIS.get(), ConfigWorld.smallDebrisMax, Blocks.netherrack);
	protected final WorldGenMinable mesaGoldGen = new WorldGenMinable(Blocks.gold_ore, 8);

	protected final WorldGenMinable deepslateBlobGen = new WorldGenDeepslateLayerBlob(ConfigWorld.maxDeepslatePerCluster, false);
	protected final WorldGenMinable tuffGen = new WorldGenDeepslateLayerBlob(ConfigWorld.maxTuffPerCluster, true);

	protected WorldGenerator amethystGen;
	protected WorldGenerator fossilGen;
	protected WorldGenerator berryBushGen;
	protected WorldGenerator cornflowerGen;
	protected WorldGenerator lilyValleyGen;
	protected WorldGenerator pinkPetalsGen;
	protected WorldGenerator bambooGen;
	protected WorldGenerator glowLichenGen;
	protected WorldGenerator caveVineGen;
	protected WorldGenerator mudGen;
	protected WorldGenerator lushCaveGen;

	private List<BiomeGenBase> fossilBiomes;
	private List<BiomeGenBase> berryBushBiomes;
	private List<BiomeGenBase> cornflowerBiomes;
	private List<BiomeGenBase> lilyValleyBiomes;
	private List<BiomeGenBase> bambooBiomes;
	private List<BiomeGenBase> mudBiomes;

	//trees
	protected WorldGenAbstractTree cherryTreeGen;
	private List<BiomeGenBase> cherryBiomes;

	protected EtFuturumWorldGenerator() {
		stoneGen.add(new WorldGenMinableCustom(ModBlocks.GRANITE.get(), 0, ConfigWorld.maxStonesPerCluster, Blocks.stone));
		stoneGen.add(new WorldGenMinableCustom(ModBlocks.DIORITE.get(), 0, ConfigWorld.maxStonesPerCluster, Blocks.stone));
		stoneGen.add(new WorldGenMinableCustom(ModBlocks.ANDESITE.get(), 0, ConfigWorld.maxStonesPerCluster, Blocks.stone));
	}

	public void postInit() {
		if (ConfigWorld.enableAmethystGeodes && ModBlocks.AMETHYST_BLOCK.isEnabled() && ModBlocks.AMETHYST_CLUSTER_1.isEnabled() && ModBlocks.AMETHYST_CLUSTER_2.isEnabled()
				&& ModBlocks.BUDDING_AMETHYST.isEnabled() && ConfigWorld.amethystOuterBlock != null && ConfigWorld.amethystMiddleBlock != null) {
			amethystGen = new WorldGenGeode(ConfigWorld.amethystOuterBlock, ConfigWorld.amethystMiddleBlock);
		}

		if (ConfigWorld.enableFossils && ConfigWorld.fossilBlock != null) {
			//Add biomes that are only hot, AND dry AND sandy, and add all swamps.
			BiomeGenBase[] fossilBiomesArray = BiomeDictionary.getBiomesForType(Type.SANDY);
			for (BiomeGenBase biome : fossilBiomesArray) {
				if (!ArrayUtils.contains(BiomeDictionary.getBiomesForType(Type.HOT), biome) || !ArrayUtils.contains(BiomeDictionary.getBiomesForType(Type.DRY), biome)) {
					fossilBiomesArray = ArrayUtils.removeElement(fossilBiomesArray, biome);
					break;
				}
			}
			fossilBiomesArray = ArrayUtils.addAll(fossilBiomesArray, BiomeDictionary.getBiomesForType(Type.SWAMP));
			fossilBiomesArray = Utils.excludeBiomesFromTypes(fossilBiomesArray, Type.NETHER, Type.END);
			fossilBiomes = Arrays.asList(fossilBiomesArray);
			fossilGen = new WorldGenFossil();
		}

		if (ModBlocks.SWEET_BERRY_BUSH.isEnabled()) {
			berryBushBiomes = Arrays.asList(Utils.excludeBiomesFromTypesWithDefaults(BiomeDictionary.getBiomesForType(Type.CONIFEROUS)));
			berryBushGen = new WorldGenFlowers(ModBlocks.SWEET_BERRY_BUSH.get());
			((WorldGenFlowers) berryBushGen).func_150550_a(ModBlocks.SWEET_BERRY_BUSH.get(), 3);
		}

		if (ModBlocks.LILY_OF_THE_VALLEY.isEnabled()) {
			BiomeGenBase[] lilyValleyBiomeArray = BiomeDictionary.getBiomesForType(Type.FOREST);
			lilyValleyBiomeArray = Utils.excludeBiomesFromTypes(lilyValleyBiomeArray, Type.JUNGLE, Type.DRY, Type.HOT, Type.SNOWY, Type.COLD);
			lilyValleyBiomes = Arrays.asList(lilyValleyBiomeArray);
			lilyValleyGen = new WorldGenFlowers(ModBlocks.LILY_OF_THE_VALLEY.get());
		}

		if (ModBlocks.CORNFLOWER.isEnabled()) {
			BiomeGenBase[] cornflowerBiomeArray = BiomeDictionary.getBiomesForType(Type.PLAINS);
			cornflowerBiomeArray = Utils.excludeBiomesFromTypes(cornflowerBiomeArray, Type.SAVANNA, Type.SNOWY, Type.SAVANNA);
			cornflowerBiomeArray = ArrayUtils.add(cornflowerBiomeArray, BiomeGenBase.getBiome(BiomeGenBase.forest.biomeID + 128));
			cornflowerBiomes = Arrays.asList(cornflowerBiomeArray);
			cornflowerGen = new WorldGenFlowers(ModBlocks.CORNFLOWER.get());
			for (BiomeGenBase biome : cornflowerBiomes) {
				biome.addFlower(ModBlocks.CORNFLOWER.get(), 0, 5);
			}
		}

		if (ModBlocks.BAMBOO.isEnabled() && ConfigWorld.bambooWorldgen) {
			if (ModsList.BIOMES_O_PLENTY.isLoaded()) { //BoP replaces vanilla jungles with a BoP version but forgets to tag them
				BiomeDictionary.registerBiomeType(BiomeGenBase.getBiome(21), Type.JUNGLE); //Gets biomes by ID so we get the BOP version
				BiomeDictionary.registerBiomeType(BiomeGenBase.getBiome(22), Type.JUNGLE);
				BiomeDictionary.registerBiomeType(BiomeGenBase.getBiome(23), Type.JUNGLE);
				BiomeDictionary.registerBiomeType(BiomeGenBase.getBiome(149), Type.JUNGLE);
				BiomeDictionary.registerBiomeType(BiomeGenBase.getBiome(151), Type.JUNGLE);
			}
			bambooGen = new WorldGenBamboo(ModBlocks.BAMBOO.get());
			bambooBiomes = Arrays.asList(Utils.excludeBiomesFromTypesWithDefaults(BiomeDictionary.getBiomesForType(Type.JUNGLE)));
		}

		if (ModBlocks.GLOW_LICHEN.isEnabled())
		{
			glowLichenGen = new WorldGenGlowLichen(ModBlocks.GLOW_LICHEN.get());
		}

		if (ModBlocks.CAVE_VINE.isEnabled())
		{
			caveVineGen = new WorldGenCaveVines(ModBlocks.CAVE_VINE.get());
		}

		if (ConfigWorld.enableLushCaves && ModBlocks.MOSS_BLOCK.isEnabled() && ModBlocks.ROOTED_DIRT.isEnabled()
				&& ModBlocks.HANGING_ROOTS.isEnabled() && ModBlocks.AZALEA_LEAVES.isEnabled()) {
			lushCaveGen = new WorldGenLushCave();
		}
        
		if (ModBlocks.CHERRY_LOG.isEnabled() && ModBlocks.CHERRY_LEAVES.isEnabled()) {
			BiomeGenBase[] cherryBiomeArray = BiomeDictionary.getBiomesForType(Type.MOUNTAIN);
			cherryBiomeArray = Utils.excludeBiomesFromTypesWithDefaults(cherryBiomeArray, Type.SNOWY, Type.HOT, Type.SANDY, Type.MESA, Type.SPARSE, Type.JUNGLE);
			cherryBiomes = Arrays.asList(cherryBiomeArray);
			cherryTreeGen = new WorldGenCherryTrees(false);
		}

		if (ModBlocks.PINK_PETALS.isEnabled()) {
			pinkPetalsGen = new WorldGenPinkPetals(ModBlocks.PINK_PETALS.get());
			for (BiomeGenBase biome : cherryBiomes) {
				biome.addFlower(ModBlocks.PINK_PETALS.get(), 0, 1);
				biome.addFlower(ModBlocks.PINK_PETALS.get(), 4, 1);
				biome.addFlower(ModBlocks.PINK_PETALS.get(), 8, 1);
				biome.addFlower(ModBlocks.PINK_PETALS.get(), 12, 1);
			}
		}

		if (ModBlocks.MUD.isEnabled()) {
			mudGen = new WorldGenClay(4);
			((WorldGenClay) mudGen).field_150546_a/*block*/ = ModBlocks.MUD.get();

			if (ModsList.BIOMES_O_PLENTY.isLoaded()) { //BoP replaces vanilla swamps with a BoP version but forgets to tag them
				BiomeDictionary.registerBiomeType(BiomeGenBase.getBiome(6), Type.SWAMP); //Gets biomes by ID so we get the BOP version
				BiomeDictionary.registerBiomeType(BiomeGenBase.getBiome(134), Type.SWAMP);
			}

			mudBiomes = Lists.newArrayList(BiomeDictionary.getBiomesForType(Type.SWAMP));
		}
	}


	@Override
	public void generate(Random rand, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator, IChunkProvider chunkProvider) {
		if (!isFlatWorld(chunkGenerator) || world.getWorldInfo().getGeneratorOptions().contains("decoration")) {
			int x;
			int z;

			if (amethystGen != null && ArrayUtils.contains(ConfigWorld.amethystDimensionBlacklist, world.provider.dimensionId) == ConfigWorld.amethystDimensionBlacklistAsWhitelist) {
				x = (chunkX << 4) + rand.nextInt(16) + 8;
				z = (chunkZ << 4) + rand.nextInt(16) + 8;
				if (ConfigWorld.amethystRarity == 1 || rand.nextInt(ConfigWorld.amethystRarity) == 0) {
					amethystGen.generate(world, rand, x, MathHelper.getRandomIntegerInRange(rand, 5, ConfigWorld.amethystMaxY - 5), z);
				}
			}

			if (ModBlocks.COPPER_ORE.isEnabled()) {
				generateOre(copperGen, world, rand, chunkX, chunkZ, 8, 4, 80);
			}

			if (ConfigWorld.enableExtraMesaGold) {
				if (ArrayUtils.contains(BiomeDictionary.getTypesForBiome(world.getBiomeGenForCoords(chunkX << 4, chunkZ << 4)), Type.MESA)) {
					generateOre(mesaGoldGen, world, rand, chunkX, chunkZ, 20, 32, 80);
				}
			}

			if (lilyValleyGen != null) {
				x = (chunkX << 4) + rand.nextInt(16) + 8;
				z = (chunkZ << 4) + rand.nextInt(16) + 8;
				if (world.getHeightValue(x, z) > 0 && lilyValleyBiomes.contains(world.getBiomeGenForCoords(x, z))) {
					lilyValleyGen.generate(world, rand, x, nextHeightInt(rand, world.getHeightValue(x, z) * 2), z);
				}
			}

			if (cornflowerGen != null) {
				x = (chunkX << 4) + rand.nextInt(16) + 8;
				z = (chunkZ << 4) + rand.nextInt(16) + 8;
				if (world.getHeightValue(x, z) > 0 && cornflowerBiomes.contains(world.getBiomeGenForCoords(x, z))) {
					cornflowerGen.generate(world, rand, x, nextHeightInt(rand, world.getHeightValue(x, z) * 2), z);
				}
			}

			if (berryBushGen != null) {
				x = (chunkX << 4) + rand.nextInt(16) + 8;
				z = (chunkZ << 4) + rand.nextInt(16) + 8;
				if (world.getHeightValue(x, z) > 0 && berryBushBiomes.contains(world.getBiomeGenForCoords(x, z))) {
					berryBushGen.generate(world, rand, x, nextHeightInt(rand, world.getHeightValue(x, z) * 2), z);
				}
			}

			if (bambooGen != null) {
				x = (chunkX << 4) + rand.nextInt(16) + 8;
				z = (chunkZ << 4) + rand.nextInt(16) + 8;
				int y = world.getHeightValue(x, z);
				if (y > 0 && bambooBiomes.contains(world.getBiomeGenForCoords(x, z))) {
					int count = rand.nextInt(256);
					count = count < 240 ? 16 : count;
					for (int i = 0; i < count; i++) {
						int xoff = x + rand.nextInt(10) - rand.nextInt(10);
						int yoff = y + rand.nextInt(4) - rand.nextInt(4);
						int zoff = z + rand.nextInt(10) - rand.nextInt(10);
						bambooGen.generate(world, rand, xoff, yoff, zoff);
					}
				}
			}

			if (glowLichenGen != null && world.provider.dimensionId == 0)
			{
				x = (chunkX << 4) + rand.nextInt(16) + 8;
				z = (chunkZ << 4) + rand.nextInt(16) + 8;
                for (int tries = 0; tries < 40; tries++) {
                    int xoff = x + rand.nextInt(10) - rand.nextInt(10);
                    int yoff = rand.nextInt(128);
                    int zoff = z + rand.nextInt(10) - rand.nextInt(10);
                    glowLichenGen.generate(world, rand, xoff, yoff, zoff);
                }
			}

			if (caveVineGen != null && world.provider.dimensionId == 0)
			{
				x = (chunkX << 4) + rand.nextInt(16) + 8;
				z = (chunkZ << 4) + rand.nextInt(16) + 8;
                for (int tries = 0; tries < 20; tries++) {
                    int xoff = x + rand.nextInt(10) - rand.nextInt(10);
                    int yoff = rand.nextInt(128);
                    int zoff = z + rand.nextInt(10) - rand.nextInt(10);
                    caveVineGen.generate(world, rand, xoff, yoff, zoff);
                }
			}

			if (cherryTreeGen != null) {
				x = (chunkX << 4) + rand.nextInt(16) + 8;
				z = (chunkZ << 4) + rand.nextInt(16) + 8;
				int y = world.getHeightValue(x, z);
				Block block = world.getBlock(x, y - 1, z);
				if (y > 0 && block.canSustainPlant(world, x, y - 1, z, ForgeDirection.UP, (IPlantable) ModBlocks.CHERRY_SAPLING.get())) {
					BiomeGenBase biome = world.getBiomeGenForCoords(x, z);
					int rng = cherryBiomes.contains(biome) ? ConfigWorld.cherryTreeRarity : 0;
					if (rng > 0 && rand.nextInt(rng) == 0) {
						if (cherryTreeGen.generate(world, rand, x, y, z)) {
							cherryTreeGen.func_150524_b(world, rand, x, y, z);
							if (pinkPetalsGen != null) {
								pinkPetalsGen.generate(world, rand, x, y, z);
							}
						}
					}
				}
			}

		if (lushCaveGen != null && world.provider.dimensionId == 0) {
			// 锚点必须取区块中心（不能随机偏移）：装饰写入按区块内不越界设计，
			// 随机偏移会把锚点甩进邻区块导致补丁越界写入未装饰区块
			x = (chunkX << 4) + 8;
			z = (chunkZ << 4) + 8;
			// 空间分布由繁茂选择噪声决定（WorldGenLushCave 内部判定），每区块 1 个装饰点
			lushCaveGen.generate(world, rand, x, world.getHeightValue(x, z), z);
		}

		if (ConfigWorld.enableModernCaves && world.provider.dimensionId == 0) {
			updateExposedLavaFaces(world, chunkX, chunkZ, rand);
		}

		if (mudGen != null) {
			x = (chunkX << 4) + rand.nextInt(16) + 8;
			z = (chunkZ << 4) + rand.nextInt(16) + 8;
			int y = world.getHeightValue(x, z);
			if (y > 0 && mudBiomes.contains(world.getBiomeGenForCoords(x, z))) {
				mudGen.generate(world, rand, x, world.getTopSolidOrLiquidBlock(x, z), z);
			}
		}

		if (fossilGen != null && rand.nextInt(64) == 0 && ArrayUtils.contains(ConfigWorld.fossilDimensionBlacklist, world.provider.dimensionId) == ConfigWorld.fossilDimensionBlacklistAsWhitelist) {
			x = (chunkX << 4) + rand.nextInt(16) + 8;
			z = (chunkZ << 4) + rand.nextInt(16) + 8;
			if (fossilBiomes.contains(world.getBiomeGenForCoords(x, z))) {
				fossilGen.generate(world, rand, x, MathHelper.getRandomIntegerInRange(rand, 40, 49), z);
			}
		}

		if (ConfigWorld.enableOceanMonuments && ModBlocks.PRISMARINE_BLOCK.isEnabled() && ModBlocks.SEA_LANTERN.isEnabled()
				&& !(world.provider instanceof WorldProviderEnd) && !(world.provider instanceof WorldProviderHell)) {
			if (OceanMonument.canSpawnAt(world, chunkX, chunkZ)) {
				x = (chunkX << 4) + rand.nextInt(16) + 8;
				z = (chunkZ << 4) + rand.nextInt(16) + 8;
				int y;
				for (y = world.getActualHeight(); y > 0; y--)
					if (!world.isAirBlock(x, y, z))
						break;
				int monumentCeiling = y - (1 + rand.nextInt(3));
				OceanMonument.buildTemple(world, x, monumentCeiling - 22, z);
				return;
			}
		}
	}

		if (world.provider instanceof WorldProviderHell) {
			if (ModBlocks.MAGMA.isEnabled()) {
				this.generateOre(magmaGen, world, rand, chunkX, chunkZ, 4, 23, 37);
			}

//          if(ConfigurationHandler.enableBlackstone)
//              this.generateOre(ModBlocks.blackstone, 0, world, rand, chunkX, chunkZ, 1, ConfigurationHandler.maxBlackstonePerCluster, 2, 5, 28, Blocks.netherrack);

			if (ModBlocks.NETHER_GOLD_ORE.isEnabled()) {
				this.generateOre(netherGoldGen, world, rand, chunkX, chunkZ, 10, 10, 117);
			}

			if (ModBlocks.ANCIENT_DEBRIS.isEnabled()) {
				this.generateOre(debrisGen, world, rand, chunkX, chunkZ, 1, 8, 22);
				this.generateOre(smallDebrisGen, world, rand, chunkX, chunkZ, 1, 8, 119);
			}
		}

		if (ModBlocks.CHORUS_PLANT.isEnabled() && ModBlocks.CHORUS_FLOWER.isEnabled() && !(world.provider instanceof WorldProviderEFREnd) && world.provider instanceof WorldProviderEnd) {
			int x = (chunkX << 4) + rand.nextInt(16) + 8;
			int y = world.getActualHeight();
			int z = (chunkZ << 4) + rand.nextInt(16) + 8;
			for (; y > 0; y--) {
				if (!world.getBlock(x, y, z).isAir(world, x, y, z)) {
					if (BlockChorusFlower.canPlantStay(world, x, y + 1, z)) {
						BlockChorusFlower.generatePlant(world, x, y + 1, z, rand, 8);
						break;
					}
				}
			}
		}
	}

	/**
	 * 激活被现代洞穴掏出的岩浆暴露面。
	 *
	 * 背景：原版岩浆湖（WorldGenLakes）以 flag=2 静态放置；1.7.10 中静态岩浆的 updateTick
	 * 只做随机点火检查、从不流淌 —— 流淌的唯一入口是 onNeighborBlockChange 触发的
	 * setNotStationary：把静态方块换成 flowing 岩浆（保留 meta）再对 flowing 方块排程。
	 * 所以这里对每个暴露的静态岩浆复刻该路径；flowing 方块被 tick 后开始蔓延（flag=3 通知），
	 * 邻接的静态岩浆收到通知会自行转换 —— 湖面一格激活即级联整片暴露面，
	 * 含跨区块边界与尚未生成的 -x/-z 邻块方向的漏扫格子。
	 *
	 * 行为对齐原版"玩家靠近才流淌"：排程的更新只在区块开始 tick（玩家在附近）时执行；
	 * 转换本身用 flag=2，装饰阶段不触发任何液体模拟。装饰阶段（populate 末尾）是能排程的
	 * 最早时机 —— 雕刻阶段写的是裸 Block[] 数组，无 tick 基础设施。
	 */
	private void updateExposedLavaFaces(World world, int chunkX, int chunkZ, Random rand) {
		int baseX = chunkX << 4;
		int baseZ = chunkZ << 4;
		// 现代洞穴只在 y≈56 以下成腔，岩浆湖（地下）y<63；扫描上限 62 已覆盖全部可能的掏空暴露
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = 1; y < 62; y++) {
					Block block = world.getBlock(baseX + x, y, baseZ + z);
					if (block.getMaterial() != Material.lava) {
						continue;
					}
					if (!hasExposedAirNeighbor(world, baseX + x, y, baseZ + z)) {
						continue;
					}
					if (block == Blocks.lava) {
						// 原版 setNotStationary 同款：静态 → flowing（ID-1），保留 meta，flag=2 不通知
						int meta = world.getBlockMetadata(baseX + x, y, baseZ + z);
						world.setBlock(baseX + x, y, baseZ + z, Blocks.flowing_lava, meta, 2);
						block = Blocks.flowing_lava;
					}
					world.scheduleBlockUpdate(baseX + x, y, baseZ + z, block, rand.nextInt(10));
				}
			}
		}
	}

	private boolean hasExposedAirNeighbor(World world, int x, int y, int z) {
		// 跨区块读取用 blockExists 守卫：-x/-z 邻区块可能尚未生成，直接 getBlock 会级联
		// 生成地形（与世界生成流程冲突）；该侧的暴露由连通岩浆面的级联通知兜底
		return isAirExisting(world, x - 1, y, z) || isAirExisting(world, x + 1, y, z)
				|| isAirExisting(world, x, y - 1, z) || isAirExisting(world, x, y + 1, z)
				|| isAirExisting(world, x, y, z - 1) || isAirExisting(world, x, y, z + 1);
	}

	private boolean isAirExisting(World world, int x, int y, int z) {
		if (!world.blockExists(x, y, z)) {
			return false;
		}
		return world.getBlock(x, y, z).getMaterial() == Material.air;
	}

	public void generateSingleOre(Block block, int meta, World world, Random random, int chunkX, int chunkZ, float chance, int minY, int maxY, Block generateIn) {
		if (maxY <= 0 || minY < 0 || maxY < minY || chance <= 0)
			return;

		for (int i = 0; i < (chance < 1 ? 1 : chance); i++) {
			if (chance > 1 || random.nextFloat() < chance) {
				int xRand = (chunkX << 4) + random.nextInt(16);
				int yRand = MathHelper.getRandomIntegerInRange(random, minY, maxY);
				int zRand = (chunkZ << 4) + random.nextInt(16);
				if (world.getBlock(xRand, yRand, zRand).isReplaceableOreGen(world, xRand, yRand, zRand, generateIn))
					world.setBlock(xRand, yRand, zRand, block, meta, 3);
			}
		}
	}

	public void generateOre(WorldGenMinable gen, World world, Random random, int chunkX, int chunkZ, float chance, int minY, int maxY) {
		if (maxY <= 0 || minY < 0 || maxY < minY || gen.numberOfBlocks <= 0 || chance <= 0)
			return;

		for (int i = 0; i < (chance < 1 ? 1 : (int) chance); i++) {
			if (chance >= 1 || random.nextFloat() < chance) {
				int xRand = (chunkX << 4) + random.nextInt(16);
				int yRand = MathHelper.getRandomIntegerInRange(random, minY, maxY);
				int zRand = (chunkZ << 4) + random.nextInt(16);

				gen.generate(world, random, xRand, yRand, zRand);
			}
		}
	}

	protected int nextHeightInt(Random rand, int i) {
		if (i <= 1)
			return 1;
		return rand.nextInt(i);
	}

	protected final boolean isFlatWorld(IChunkProvider chunkProvider) {
		return chunkProvider instanceof ChunkProviderFlat && !chunkProvider.getClass().getName().equals("com.rwtema.extrautils.worldgen.Underdark.ChunkProviderUnderdark");
	}
}