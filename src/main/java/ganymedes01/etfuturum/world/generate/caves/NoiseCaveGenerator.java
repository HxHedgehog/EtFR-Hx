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

	public enum FeatureType {
		NONE, CHAMBER, TUNNEL, PILLAR, LEDGE
	}

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

	public double sample(double noise, int y, int z, int x, double terrainDepth, double terrainScale) {
		boolean generateLimited = noise < 170.0D;
		double tunnelOffset = this.getTunnelOffsetNoise(x, y, z);
		double tunnel = this.getTunnelNoise(x, y, z);

		if (generateLimited) {
			return Math.min(noise, (tunnel + tunnelOffset) * 128.0D * 5.0D);
		} else {
			boolean ocean = terrainDepth < 0.0D;
			double mountainFactor = MathHelper.clamp((terrainScale - 0.4D) / 0.4D, 0.0D, 1.0D);

			double caveDensity = this.caveDensityNoise.sample((double) x, (double) y / 1.5D, (double) z);
			double scaledCaveDensity = MathHelper.clamp(caveDensity + 0.25D, -1.0D, 1.0D);
			double yScale = (float) (30 - y) / 8.0F;
			double caveOffset = scaledCaveDensity + MathHelper.clampedLerp(0.5D, 0.0D, yScale);

			double terrainAddition = this.getTerrainAdditionNoise(x, y, z);
			double caveNoise = this.getCaveNoise(x, y, z, ocean, mountainFactor);

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

	/**
	 * 仅用于调试标记：在给定坐标重新采样各噪声成分，判断该位置属于哪种洞穴结构。
	 * 与 {@link #sample} 的现代洞穴分支保持一致（不含原版蠕虫洞的 limited 分支）。
	 */
	public FeatureType classifyFeature(int y, int z, int x, double terrainDepth, double terrainScale) {
		boolean ocean = terrainDepth < 0.0D;
		double mountainFactor = MathHelper.clamp((terrainScale - 0.4D) / 0.4D, 0.0D, 1.0D);

		double tunnelOffset = this.getTunnelOffsetNoise(x, y, z);
		double tunnel = this.getTunnelNoise(x, y, z);

		double caveDensity = this.caveDensityNoise.sample((double) x, (double) y / 1.5D, (double) z);
		double scaledCaveDensity = MathHelper.clamp(caveDensity + 0.25D, -1.0D, 1.0D);
		double yScale = (float) (30 - y) / 8.0F;
		double caveOffset = scaledCaveDensity + MathHelper.clampedLerp(0.5D, 0.0D, yScale);

		double terrainAddition = this.getTerrainAdditionNoise(x, y, z);
		double caveNoise = this.getCaveNoise(x, y, z, ocean, mountainFactor);

		double offset = caveOffset * ConfigWorld.caveFillWeight + terrainAddition * ConfigWorld.caveLedgeStrength;
		double tunnelTerm = tunnel * ConfigWorld.caveTunnelWeight;
		double chamberTerm = caveNoise * ConfigWorld.caveCavityWeight;
		double inner = Math.min(tunnelTerm, chamberTerm) + tunnelOffset;
		double smallerNoise = Math.min(offset, inner);

		double pillarNoise = ConfigWorld.cavePillars ? this.getPillarNoise(x, y, z) : Double.NEGATIVE_INFINITY;
		double finalNoise = Math.max(smallerNoise, pillarNoise);

		double result = 128.0D * MathHelper.clamp(finalNoise, -1.0D, 1.0D);

		// 洞底平滑：与 sample 一致，洞底以下平滑推向实心
		double floorY = this.getFloorY(x, z);
		double margin = 3.0D;
		if (y < floorY + margin) {
			double t = MathHelper.clamp((floorY + margin - y) / margin, 0.0D, 1.0D);
			t = t * t * (3.0D - 2.0D * t);
			result = MathHelper.lerp(t, result, 128.0D);
		}

		// 地表衰减：与 generateNoiseCavesNoise 一致，接近/高于地表时强制实心，避免标记上天
		int sub = (int) ((56.0D + terrainDepth * 20.0D) / 8.0D);
		double surfaceDelta = (y / 8.0D - sub + 2.0D) / 2.0D;
		if (surfaceDelta >= 1.0D) {
			return FeatureType.NONE;
		}
		result = MathHelper.clampedLerp(result, terrainDepth * -30.0D + 20.0D, surfaceDelta);

		if (result < 0.0D) {
			return tunnelTerm < chamberTerm ? FeatureType.TUNNEL : FeatureType.CHAMBER;
		}

		// 石柱：本应是洞穴空气，却被上下联通的柱子顶成实心
		if (ConfigWorld.cavePillars && y >= floorY && pillarNoise > 0.03D && smallerNoise < 0.0D) {
			return FeatureType.PILLAR;
		}

		// 岩架：洞壁台阶带，由 terrainAddition 主导 offset 项形成
		if (y >= floorY && offset < inner && terrainAddition * ConfigWorld.caveLedgeStrength > 0.25D) {
			return FeatureType.LEDGE;
		}

		return FeatureType.NONE;
	}

	private double getFloorY(int x, int z) {
		double variation = this.floorNoise.sample(x * 0.2D, 0.0D, z * 0.2D);
		return ConfigWorld.caveFloorY + variation * ConfigWorld.caveFloorVariation;
	}

	private double getPillarNoise(int x, int y, int z) {
		double pillarFalloff = lerpFromProgress(this.pillarFalloffNoise, (double) x, (double) y, (double) z, 0.0D, 2.0D);
		double pillarScale = lerpFromProgress(this.pillarScaleNoise, (double) x, (double) y, (double) z, 0.0D, 1.1D);

		pillarScale = Math.pow(pillarScale, 3.0D);
		double pillarNoise = this.pillarNoise.sample((double) x * 25.0D, (double) y * 0.3D, (double) z * 25.0D);

		pillarNoise = pillarScale * (pillarNoise * 2.0D - pillarFalloff);

		// 石柱只出现在特定区域，形成上下联通的石柱
		return pillarNoise > 0.03D ? pillarNoise : Double.NEGATIVE_INFINITY;
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

	private double getCaveNoise(int x, int y, int z, boolean ocean, double mountainFactor) {
		double caveScaleNoise = this.caveScaleNoise.sample((x * 2), y, (z * 2));
		double caveScale = scaleCaves(caveScaleNoise);
		if (ocean) {
			caveScale *= 1.5D;
		}

		double caveFalloff = lerpFromProgress(this.caveFalloffNoise, (x * 2), y, (z * 2), 0.6D, 1.3D);
		// 高山 → 纵向拉伸洞穴，允许更高的洞室甚至上下重叠
		caveFalloff *= 1.0D + mountainFactor * (ConfigWorld.caveMountainVerticalScale - 1.0D);

		double caveNoise = sample(this.caveNoise, x, y, z, caveScale);
		double scaledCaveNoise = Math.abs(caveScale * caveNoise) - 0.083D * caveFalloff;

		int yStart = -4;
		double horizontalCaveNoise = lerpFromProgress(this.horizontalCaveNoise, x, 0.0D, z, yStart, 4.0D) + 4;

		// 水平带中线加 Y 方向扰动，避免洞顶平整
		double ceilingWobble = sample(this.caveNoise, x, y, z, caveScale * 0.5D) * 0.3D;
		double caveFalloffNoise = (Math.abs((horizontalCaveNoise + ceilingWobble - (double) y / 8.0D)) - (2.0D * caveFalloff));
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
