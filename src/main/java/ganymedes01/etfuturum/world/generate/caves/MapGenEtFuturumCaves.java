package ganymedes01.etfuturum.world.generate.caves;

import ganymedes01.etfuturum.configuration.configs.ConfigWorld;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.MapGenCaves;
import net.minecraft.world.gen.NoiseGeneratorOctaves;

import java.util.Random;

public class MapGenEtFuturumCaves extends MapGenCaves {
	private double[] caveNoise;
	private float[] biomeWeightTable;
	private NoiseCaveGenerator noiseCaves;
	public NoiseGeneratorOctaves noiseGen6;
	private NoiseGeneratorOctaves field_147431_j;
	private NoiseGeneratorOctaves field_147432_k;
	private NoiseGeneratorOctaves interpolationNoise;
	private double[] interpolationNoises;
	private double[] lowerInterpolatedNoises;
	private double[] upperInterpolatedNoises;
	private double[] depthNoises;
	// 5x5 网格上每个采样点的地形深度/起伏，供调试标记分类复用
	private final double[] terrainDepths = new double[25];
	private final double[] terrainScales = new double[25];

	@Override
	public void func_151539_a(IChunkProvider provider, World world, int chunkX, int chunkZ, Block[] blocks) {
		if (this.worldObj != world) {
			this.caveNoise = new double[825];
			this.biomeWeightTable = new float[25];
			this.field_147431_j = new NoiseGeneratorOctaves(this.rand, 16);
			this.field_147432_k = new NoiseGeneratorOctaves(this.rand, 16);
			this.interpolationNoise = new NoiseGeneratorOctaves(this.rand, 8);
			this.noiseGen6 = new NoiseGeneratorOctaves(this.rand, 16);
			// 现代噪声洞穴必须绑定世界种子：用独立 Random(world.getSeed()) 派生，避免同一种子跨会话得到不同洞穴
			this.noiseCaves = new NoiseCaveGenerator(new Random(world.getSeed()));
			for (int j = -2; j <= 2; ++j) {
				for (int k = -2; k <= 2; ++k) {
					float f = 10.0F / MathHelper.sqrt_float((float) (j * j + k * k) + 0.2F);
					this.biomeWeightTable[j + 2 + (k + 2) * 5] = f;
				}
			}
		}
		this.worldObj = world;
		this.rand.setSeed(world.getSeed());
		int range = this.range;
		long l = this.rand.nextLong();
		long i1 = this.rand.nextLong();
		BlockFalling.fallInstantly = true;
		for (int j1 = chunkX - range; j1 <= chunkX + range; ++j1) {
			for (int k1 = chunkZ - range; k1 <= chunkZ + range; ++k1) {
				long l1 = (long) j1 * l;
				long i2 = (long) k1 * i1;
				this.rand.setSeed(l1 ^ i2 ^ world.getSeed());
				this.func_151538_a(world, j1, k1, chunkX, chunkZ, blocks);
			}
		}
		this.generateNoiseCaves(chunkX, chunkZ, blocks);
		BlockFalling.fallInstantly = false;
	}

	@Override
	protected void func_151538_a(World world, int noiseX, int noiseZ, int chunkX, int chunkZ, Block[] blocks) {
		super.func_151538_a(world, noiseX, noiseZ, chunkX, chunkZ, blocks);
		// 调试：在原版蠕虫洞里随机放红石块标记
		if (ConfigWorld.debugCaveMarkers && noiseX == chunkX && noiseZ == chunkZ) {
			for (int i = 0; i < 3; i++) {
				tryPlaceDebugMarker(blocks, this.rand.nextInt(16), 5 + this.rand.nextInt(50), this.rand.nextInt(16), Blocks.redstone_block);
			}
		}
	}

	private void tryPlaceDebugMarker(Block[] blocks, int x, int y, int z, Block marker) {
		for (int dx = 0; dx < 2; dx++) {
			for (int dy = 0; dy < 2; dy++) {
				for (int dz = 0; dz < 2; dz++) {
					int bx = x + dx;
					int by = y + dy;
					int bz = z + dz;
					if (bx < 16 && bz < 16 && by > 0 && by < 256) {
						int idx = (bx * 16 + bz) * 256 + by;
						Block existing = blocks[idx];
						if (existing == null || existing == Blocks.stone) {
							blocks[idx] = marker;
						}
					}
				}
			}
		}
	}

	private Block markerForFeature(NoiseCaveGenerator.FeatureType type) {
		switch (type) {
			case CHAMBER:
				return Blocks.gold_block;
			case TUNNEL:
				return Blocks.lapis_block;
			case PILLAR:
				return Blocks.diamond_block;
			case LEDGE:
				return Blocks.iron_block;
			default:
				return null;
		}
	}

	private void generateNoiseCaves(int chunkX, int chunkZ, Block[] blocks) {
		generateNoiseCavesNoise(chunkX, chunkZ);

		for (int noiseX = 0; noiseX < 4; ++noiseX) {
			int ix0 = noiseX * 5;
			int ix1 = (noiseX + 1) * 5;

			for (int noiseZ = 0; noiseZ < 4; ++noiseZ) {
				int ix0z0 = (ix0 + noiseZ) * 33;
				int ix0z1 = (ix0 + noiseZ + 1) * 33;
				int ix1z0 = (ix1 + noiseZ) * 33;
				int ix1z1 = (ix1 + noiseZ + 1) * 33;

				for (int noiseY = 0; noiseY < 32; ++noiseY) {
					double x0z0 = this.caveNoise[ix0z0 + noiseY];
					double x0z1 = this.caveNoise[ix0z1 + noiseY];
					double x1z0 = this.caveNoise[ix1z0 + noiseY];
					double x1z1 = this.caveNoise[ix1z1 + noiseY];
					double x0z0Add = (this.caveNoise[ix0z0 + noiseY + 1] - x0z0) * 0.125D;
					double x0z1Add = (this.caveNoise[ix0z1 + noiseY + 1] - x0z1) * 0.125D;
					double x1z0Add = (this.caveNoise[ix1z0 + noiseY + 1] - x1z0) * 0.125D;
					double x1z1Add = (this.caveNoise[ix1z1 + noiseY + 1] - x1z1) * 0.125D;

					for (int pieceY = 0; pieceY < 8; ++pieceY) {
						double z0 = x0z0;
						double z1 = x0z1;
						double z0Add = (x1z0 - x0z0) * 0.25D;
						double z1Add = (x1z1 - x0z1) * 0.25D;

						for (int pieceX = 0; pieceX < 4; ++pieceX) {
							int index = pieceX + noiseX * 4 << 12 | noiseZ * 4 << 8 | noiseY * 8 + pieceY;
							short idAdd = 256;
							index -= idAdd;
							double densityAdd = (z1 - z0) * 0.25D;
							double density = z0 - densityAdd;

							for (int pieceZ = 0; pieceZ < 4; ++pieceZ) {
								index += idAdd;
								int y = noiseY * 8 + pieceY;
								if ((density += densityAdd) < 0) {
									if (y > 0) {
										if (blocks[index] == Blocks.bedrock) {
											blocks[index] = Blocks.stone;
										} else {
											blocks[index] = null;
										}
									}
								}

								// 调试：按结构类型随机放置对应的 2x2x2 方块堆标记
								if (ConfigWorld.debugCaveMarkers && y > 0 && y < 256 && this.rand.nextInt(2000) == 0) {
									double td = this.terrainDepths[noiseX + noiseZ * 5];
									double ts = this.terrainScales[noiseX + noiseZ * 5];
									int worldX = chunkX * 16 + noiseX * 4 + pieceX;
									int worldZ = chunkZ * 16 + noiseZ * 4 + pieceZ;
									NoiseCaveGenerator.FeatureType type = this.noiseCaves.classifyFeature(y, worldZ, worldX, td, ts);
									Block marker = markerForFeature(type);
									if (marker != null) {
										tryPlaceDebugMarker(blocks, noiseX * 4 + pieceX, y, noiseZ * 4 + pieceZ, marker);
									}
								}
							}

							z0 += z0Add;
							z1 += z1Add;
						}

						x0z0 += x0z0Add;
						x0z1 += x0z1Add;
						x1z0 += x1z0Add;
						x1z1 += x1z1Add;
					}
				}
			}
		}
	}

	private void generateNoiseCavesNoise(int chunkX, int chunkZ) {
		int cx = chunkX * 4, cz = chunkZ * 4;
		this.depthNoises = this.noiseGen6.generateNoiseOctaves(this.depthNoises, cx, cz, 5, 5, 200.0D, 200.0D, 0.5D);
		this.interpolationNoises = this.interpolationNoise.generateNoiseOctaves(this.interpolationNoises, cx, 0, cz, 5, 33, 5, 8.555150000000001D, 4.277575000000001D, 8.555150000000001D);
		this.lowerInterpolatedNoises = this.field_147431_j.generateNoiseOctaves(this.lowerInterpolatedNoises, cx, 0, cz, 5, 33, 5, 684.412D, 684.412D, 684.412D);
		this.upperInterpolatedNoises = this.field_147432_k.generateNoiseOctaves(this.upperInterpolatedNoises, cx, 0, cz, 5, 33, 5, 684.412D, 684.412D, 684.412D);
		BiomeGenBase[] biomes = null;
		biomes = this.worldObj.getWorldChunkManager().getBiomesForGeneration(biomes, cx - 2, cz - 2, 10, 10);
		int i = 0, j = 0;

		for (int x = 0; x < 5; ++x) {
			for (int z = 0; z < 5; ++z) {
				float scale = 0.0F;
				float depth = 0.0F;
				float weight = 0.0F;
				double lowestScaledDepth = 0;

				BiomeGenBase biome0 = biomes[x + 2 + (z + 2) * 10];
				// 遍历周边区域，确保不会靠近海洋生成
				for (int x1 = -2; x1 <= 2; ++x1) {
					for (int z1 = -2; z1 <= 2; ++z1) {
						BiomeGenBase biome = biomes[x + x1 + 2 + (z + z1 + 2) * 10];
						float depthHere = biome.rootHeight;
						float scaleHere = biome.heightVariation;

						float weightHere = this.biomeWeightTable[x1 + 2 + (z1 + 2) * 5] / (depthHere + 2.0F);

						if (biome.rootHeight > biome0.rootHeight) {
							weightHere /= 2.0F;
						}

						scale += scaleHere * weightHere;
						depth += depthHere * weightHere;
						weight += weightHere;
						// 海洋中禁用
						lowestScaledDepth = Math.min(lowestScaledDepth, biome.rootHeight);
					}
				}
				scale /= weight;
				depth /= weight;
				scale = scale * 0.9F + 0.1F;
				depth = (depth * 4.0F - 1.0F) / 8.0F;
				double depthNoise = this.depthNoises[j] / 8000;

				if (depthNoise < 0.0D) {
					depthNoise = -depthNoise * 0.3D;
				}

				depthNoise = depthNoise * 3.0D - 2.0D;

				if (depthNoise < 0.0D) {
					depthNoise /= 2.0D;

					if (depthNoise < -1.0D) {
						depthNoise = -1.0D;
					}

					depthNoise /= 1.4D;
					depthNoise /= 2.0D;
				} else {
					if (depthNoise > 1.0D) {
						depthNoise = 1.0D;
					}

					depthNoise /= 8.0D;
				}

				++j;
				double scaledDepth = depth;
				double scaledScale = scale;
				scaledDepth += depthNoise * 0.2D;
				scaledDepth = scaledDepth * 8.5D / 8.0D;
				double terrainHeight = 8.5D + scaledDepth * 4.0D;

				double startLevel = 56 + (lowestScaledDepth * 20);
				int sub = (int) (startLevel / 8);

				this.terrainDepths[x + z * 5] = lowestScaledDepth;
				this.terrainScales[x + z * 5] = scaledScale;

				for (int y = 0; y < 33; y++) {
					double falloff = ((double) y - terrainHeight) * 12.0D * 128.0D / 256.0D / scaledScale;

					if (falloff < 0.0D) {
						falloff *= 4.0D;
					}

					double lowerNoise = this.lowerInterpolatedNoises[i] / 512.0D;
					double upperNoise = this.upperInterpolatedNoises[i] / 512.0D;
					double interpolation = (this.interpolationNoises[i] / 10.0D + 1.0D) / 2.0D;
					double noise = MathHelper.denormalizeClamp(lowerNoise, upperNoise, interpolation) - falloff;

					if (y > 29) {
						double lerp = (float) (y - 29) / 3.0F;
						noise = noise * (1.0D - lerp) + -10.0D * lerp;
					}

					double caveNoise = this.noiseCaves.sample(noise, y * 8, chunkZ * 16 + (z * 4), chunkX * 16 + (x * 4), lowestScaledDepth, scaledScale);

					// 衰减，避免挖穿地表
					caveNoise = ganymedes01.etfuturum.world.generate.caves.util.MathHelper.clampedLerp(caveNoise, (lowestScaledDepth * -30) + 20, (y - sub + 2) / 2.0);

					this.caveNoise[i] = caveNoise;
					i++;
				}
			}
		}
	}
}
