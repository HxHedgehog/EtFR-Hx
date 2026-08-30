package ganymedes01.etfuturum.world.generate.caves;

import ganymedes01.etfuturum.configuration.configs.ConfigWorld;
import ganymedes01.etfuturum.world.generate.caves.util.MathHelper;
import ganymedes01.etfuturum.world.generate.caves.noise.DoublePerlinNoiseSampler;

import java.util.Random;

public final class NoiseCaveGenerator {
	private final DoublePerlinNoiseSampler terrainAdditionNoise;
	private final DoublePerlinNoiseSampler pillarNoise;
	private final DoublePerlinNoiseSampler pillarFalloffNoise;
	private final DoublePerlinNoiseSampler pillarScaleNoise;
	private final DoublePerlinNoiseSampler caveNoise;
	private final DoublePerlinNoiseSampler horizontalCaveNoise;
	private final DoublePerlinNoiseSampler caveScaleNoise;
	private final DoublePerlinNoiseSampler caveFalloffNoise;
	private final DoublePerlinNoiseSampler tunnelNoise1;
	private final DoublePerlinNoiseSampler tunnelNoise2;
	private final DoublePerlinNoiseSampler tunnelScaleNoise;
	private final DoublePerlinNoiseSampler tunnelFalloffNoise;
	private final DoublePerlinNoiseSampler offsetNoise;
	private final DoublePerlinNoiseSampler offsetScaleNoise;
	private final DoublePerlinNoiseSampler caveDensityNoise;
	// 低频洞底噪声：让洞穴底部平坦并带小丘
	private final DoublePerlinNoiseSampler floorNoise;
	// 洞底高度的列级缓存：getFloorY 只依赖 (x,z)，同列 33 个 y 采样共享同一结果。
	// 采样循环为 x→z→y 顺序，单条目缓存即可命中 800/825；结果位级一致（sample 为无状态纯函数）
	private boolean hasFloorYCache;
	private int cachedFloorX;
	private int cachedFloorZ;
	private double cachedFloorY;

	// 石柱生成阈值
	private static final double PILLAR_THRESHOLD = 0.03D;

	public NoiseCaveGenerator(Random random) {
		this.pillarNoise = DoublePerlinNoiseSampler.create(new Random(random.nextLong()), -7, 1.0D, 1.0D);
		this.pillarFalloffNoise = DoublePerlinNoiseSampler.create(new Random(random.nextLong()), -8, 1.0D);
		this.pillarScaleNoise = DoublePerlinNoiseSampler.create(new Random(random.nextLong()), -8, 1.0D);
		this.caveNoise = DoublePerlinNoiseSampler.create(new Random(random.nextLong()), -7, 1.0D);
		this.horizontalCaveNoise = DoublePerlinNoiseSampler.create(new Random(random.nextLong()), -8, 1.0D);
		this.caveScaleNoise = DoublePerlinNoiseSampler.create(new Random(random.nextLong()), -11, 1.0D);
		this.caveFalloffNoise = DoublePerlinNoiseSampler.create(new Random(random.nextLong()), -11, 1.0D);
		this.tunnelNoise1 = DoublePerlinNoiseSampler.create(new Random(random.nextLong()), -7, 1.0D);
		this.tunnelNoise2 = DoublePerlinNoiseSampler.create(new Random(random.nextLong()), -7, 1.0D);
		this.tunnelScaleNoise = DoublePerlinNoiseSampler.create(new Random(random.nextLong()), -11, 1.0D);
		this.tunnelFalloffNoise = DoublePerlinNoiseSampler.create(new Random(random.nextLong()), -8, 1.0D);
		this.offsetNoise = DoublePerlinNoiseSampler.create(new Random(random.nextLong()), -5, 1.0D);
		this.offsetScaleNoise = DoublePerlinNoiseSampler.create(new Random(random.nextLong()), -8, 1.0D);
		this.terrainAdditionNoise = DoublePerlinNoiseSampler.create(new Random(random.nextLong()), -8, 1.0D);
		this.caveDensityNoise = DoublePerlinNoiseSampler.create(new Random(random.nextLong()), -8, 0.5D, 1.0D, 2.0D, 1.0D, 2.0D, 1.0D, 0.0D, 2.0D, 0.0D);
		this.floorNoise = DoublePerlinNoiseSampler.create(new Random(random.nextLong()), -7, 1.0D);
	}

	public double sample(double noise, int y, int z, int x, double terrainScale) {
		boolean generateLimited = noise < 170.0D;
		double tunnelOffset = this.getTunnelOffsetNoise(x, y, z);
		double tunnel = this.getTunnelNoise(x, y, z);

		if (generateLimited) {
			return Math.min(noise, (tunnel + tunnelOffset) * 128.0D * 5.0D);
		} else {
			double mountainFactor = MathHelper.clamp((terrainScale - 0.4D) / 0.4D, 0.0D, 1.0D);

			double caveDensity = this.caveDensityNoise.sample((double) x, (double) y / 1.5D, (double) z);
			double scaledCaveDensity = MathHelper.clamp(caveDensity + 0.25D, -1.0D, 1.0D);
			double yLimit = 30.0D + 30.0D * mountainFactor;
			double yScale = (float) (yLimit - y) / 8.0F;
			double caveOffset = scaledCaveDensity + MathHelper.clampedLerp(0.5D, 0.0D, yScale);

			double terrainAddition = this.getTerrainAdditionNoise(x, y, z);
			double caveNoise = this.getCaveNoise(x, y, z, mountainFactor);

			double offset = caveOffset * ConfigWorld.caveFillWeight + terrainAddition * ConfigWorld.caveLedgeStrength;
			double smallerNoise = Math.min(offset,
					Math.min(tunnel * ConfigWorld.caveTunnelWeight, caveNoise * ConfigWorld.caveCavityWeight) + tunnelOffset);

			double finalNoise;
			if (ConfigWorld.cavePillars) {
				finalNoise = Math.max(smallerNoise, this.getPillarNoise(x, y, z));
			} else {
				finalNoise = smallerNoise;
			}

			double result = 128.0D * MathHelper.clamp(finalNoise, -1.0D, 1.0D);

			// 洞底平滑：接近基岩层时用中低频噪声定义平坦小丘，丘下平滑推向实心
			result = this.applyFloorSmoothing(y, x, z, result);

			return result;
		}
	}

	private double applyFloorSmoothing(int y, int x, int z, double result) {
		double floorY = this.getFloorY(x, z);
		double margin = 3.0D;
		if (y < floorY + margin) {
			double t = MathHelper.clamp((floorY + margin - y) / margin, 0.0D, 1.0D);
			// smoothstep 平滑过渡，避免墙根出现一格空隙或把墙抹平
			t = t * t * (3.0D - 2.0D * t);
			result = MathHelper.lerp(t, result, 128.0D);
		}
		return result;
	}

	private double getFloorY(int x, int z) {
		if (!this.hasFloorYCache || x != this.cachedFloorX || z != this.cachedFloorZ) {
			double variation = this.floorNoise.sample(x * 0.2D, 0.0D, z * 0.2D);
			this.cachedFloorY = ConfigWorld.caveFloorY + variation * ConfigWorld.caveFloorVariation;
			this.cachedFloorX = x;
			this.cachedFloorZ = z;
			this.hasFloorYCache = true;
		}
		return this.cachedFloorY;
	}

	private double getPillarNoise(int x, int y, int z) {
		double pillarFalloff = lerpFromProgress(this.pillarFalloffNoise, (double) x, (double) y, (double) z, 0.0D, 2.0D);
		double pillarScale = lerpFromProgress(this.pillarScaleNoise, (double) x, (double) y, (double) z, 0.0D, 1.1D);

		pillarScale = Math.pow(pillarScale, 3.0D);
		double pillarNoise = this.pillarNoise.sample((double) x * 25.0D, (double) y * 0.3D, (double) z * 25.0D);

		pillarNoise = pillarScale * (pillarNoise * 2.0D - pillarFalloff);

		// 石柱只出现在特定区域，形成上下联通的石柱
		return pillarNoise > PILLAR_THRESHOLD ? pillarNoise : Double.NEGATIVE_INFINITY;
	}

	private double getTerrainAdditionNoise(int x, int y, int z) {
		// 在洞壁上形成岩架/台阶，让洞穴不再圆滑
		double addition = this.terrainAdditionNoise.sample(x, y * 8, z);
		return addition * addition * 4.0D;
	}

	private double getTunnelNoise(int x, int y, int z) {
		double tunnelScaleNoise = this.tunnelScaleNoise.sample(x * 2, y, z * 2);
		double tunnelScale = scaleTunnels(tunnelScaleNoise);

		double tunnelFalloff = lerpFromProgress(this.tunnelFalloffNoise, x, y, z, 0.065D, 0.088D);

		double tunnelNoise1 = sample(this.tunnelNoise1, x, y, z, tunnelScale);
		double scaledTunnelNoise1 = Math.abs(tunnelScale * tunnelNoise1) - tunnelFalloff;

		double tunnelNoise2 = sample(this.tunnelNoise2, x, y, z, tunnelScale);
		double scaledTunnelNoise2 = Math.abs(tunnelScale * tunnelNoise2) - tunnelFalloff;

		// 取两个隧道噪声的较大值，形成交叉隧道
		return clamp(Math.max(scaledTunnelNoise1, scaledTunnelNoise2));
	}

	private double getCaveNoise(int x, int y, int z, double mountainFactor) {
		double caveScaleNoise = this.caveScaleNoise.sample((x * 2), y, (z * 2));
		double caveScale = scaleCaves(caveScaleNoise) * ConfigWorld.caveCavityScale;

		double caveFalloff = lerpFromProgress(this.caveFalloffNoise, (x * 2), y, (z * 2), 0.6D, 1.3D);
		// 高山 → 纵向拉伸洞穴，允许更高的洞室甚至上下重叠
		caveFalloff *= 1.0D + mountainFactor * (ConfigWorld.caveMountainVerticalScale - 1.0D);

		double caveNoise = sample(this.caveNoise, x, y, z, caveScale);
		double scaledCaveNoise = Math.abs(caveScale * caveNoise) - 0.083D * caveFalloff;

		int yStart = -4;
		double horizontalCaveNoise = lerpFromProgress(this.horizontalCaveNoise, x, 0.0D, z, yStart, 4.0D) + 4;

		double caveFalloffNoise = (Math.abs((horizontalCaveNoise - (double) y / 8.0D)) - (2.0D * caveFalloff));
		caveFalloffNoise = caveFalloffNoise * caveFalloffNoise * caveFalloffNoise;
		return clamp(Math.max(caveFalloffNoise, scaledCaveNoise));
	}

	private double getTunnelOffsetNoise(int x, int y, int z) {
		double scale = lerpFromProgress(this.offsetScaleNoise, x, y, z, 0.0D, 0.1D);
		return (0.4D - Math.abs(this.offsetNoise.sample(x, y, z))) * scale;
	}

	private static double clamp(double value) {
		return MathHelper.clamp(value, -1.0D, 1.0D);
	}

	private static double sample(DoublePerlinNoiseSampler sampler, double x, double y, double z, double scale) {
		return sampler.sample(x / scale, y / scale, z / scale);
	}

	public static double lerpFromProgress(DoublePerlinNoiseSampler sampler, double x, double y, double z, double start, double end) {
		double value = sampler.sample(x, y, z);
		return MathHelper.lerpFromProgress(value, -1.0D, 1.0D, start, end);
	}

	private static double scaleCaves(double value) {
		if (value < -0.75D) {
			return 0.5D;
		} else if (value < -0.5D) {
			return 0.75D;
		} else if (value < 0.5D) {
			return 1.0D;
		} else {
			return value < 0.75D ? 2.0D : 3.0D;
		}
	}

	private static double scaleTunnels(double value) {
		if (value < -0.5D) {
			return 0.75D;
		} else if (value < 0.0D) {
			return 1.0D;
		} else {
			return value < 0.5D ? 1.5D : 2.0D;
		}
	}
}
