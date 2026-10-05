package ganymedes01.etfuturum.waterlog.client;

import ganymedes01.etfuturum.blocks.BlockAquaticPlant;
import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

/**
 * 真含水（bit16）与海洋植物共用的水面助手与整格画水器。
 *
 * <p>262 蓝本：含水格内部是整格静水水体（不是按方块形状裁剪的补集体积），
 * 表面高度 8/9、与相邻真实水通过同一套角高平均与同流体面剔除无缝衔接。
 * 三个成员感知点保证"假水"与真水一致：
 * <ul>
 * <li>角高采样把海洋植物与含水格都按 meta 0 源水计权（真水侧由
 * MixinRenderBlocks 重定向 {@code RenderBlocks#getLiquidHeight} 走同一实现）；</li>
 * <li>画水器自己的面剔除把含水邻居当同流体剔除；</li>
 * <li>真水对含水格的剔除由 MixinBlockLiquid 桥接（water 材质对 rock 材质
 * 含水格本会画面）。</li>
 * </ul>
 * 贴图保持原版水图标引用，Angelica 的 sprite retag 对这些 quads 自动生效。
 */
public class StairWaterRenderer {

	/**
	 * 角高采样：{@code RenderBlocks#getLiquidHeight}（vanilla L4343-4381）的
	 * 成员感知移植。真水按 meta 计权；海洋植物与含水格（自身格与邻居格）强制
	 * 按 meta 0 源水计权——26.2 {@code getHeight}（FluidRenderer L289-301）里
	 * 含水格内部是 8/9 静水源，走 same-fluid 分支；1.7.10 侧它材质是 rock，
	 * 若不显式分支会落进实心 skip：全实心样本集（玻璃板围 8 格）时
	 * {@code l==0 → NaN → 全顶点丢失整格不显示}，部分实心时角高被拉到 0
	 * （斜对角方块致表面塌陷）、开口相对的楼梯互相计不进角高。上方有真水或
	 * 含水格时角高直接取满格；无特殊成员时结果与 vanilla 完全一致。
	 */
	public static double getLiquidHeight(IBlockAccess world, int x, int y, int z) {
		int l = 0;
		float f = 0.0F;

		for (int i1 = 0; i1 < 4; ++i1) {
			int j1 = x - (i1 & 1);
			int k1 = z - (i1 >> 1 & 1);

			if (world.getBlock(j1, y + 1, k1).getMaterial() == Material.water || WaterlogRuntime.isWaterlogged(world, j1, y + 1, k1)) {
				return 1.0D; // the water body continues above: full height
			}

			Block block = world.getBlock(j1, y, k1);
			Material material = block.getMaterial();

			if (material == Material.water) {
				int meta = world.getBlockMetadata(j1, y, k1);
				if (BlockAquaticPlant.isOceanPlant(block)) {
					meta = 0;
				}
				if (meta >= 8 || meta == 0) {
					f += BlockLiquid.getLiquidHeightPercent(meta) * 10.0F;
					l += 10;
				}

				f += BlockLiquid.getLiquidHeightPercent(meta);
				++l;
			} else if (WaterlogRuntime.isWaterlogged(world, j1, y, k1)) {
				// 含水家族格（rock 材质，含角点采样中的自身格）：内部是 8/9
				// 静水源，与 262 same-fluid 分支同权（meta 0 双计权）
				f += BlockLiquid.getLiquidHeightPercent(0) * 10.0F;
				l += 10;

				f += BlockLiquid.getLiquidHeightPercent(0);
				++l;
			} else if (!material.isSolid()) {
				f += 1;
				++l;
			}
		}

		if (l == 0) {
			return 1.0D; // 防御：全实心样本（含水格自身在场时不可达；真水侧此时面已被剔除）
		}
		return 1.0D - (double) (f / (float) l);
	}

	/**
	 * 画水面的面剔除。底面/侧面（26.2 FluidRenderer L85/L225、原版
	 * BlockLiquid side!=1 走 Block.shouldSideBeRendered）：满格不透明邻居、
	 * 水材质邻居（同流体不互画）与含水邻居（同流体语义）都剔除。
	 *
	 * <p>顶面例外（26.2 L84/L119：{@code renderUp = !isNeighborSameFluid}，
	 * 满格邻居只在 height==1.0 时挡；原版 L170 {@code side == 1 ? true}）：
	 * 只对同流体剔除——上方是固体方块时 8/9 水面照画（面藏在邻格墙内，
	 * 有开放侧面时可见），保证含水格被方块围住/上方盖方块时外观不消失。
	 */
	public static boolean isFaceCulled(IBlockAccess world, int x, int y, int z, boolean topFace) {
		Block neighbour = world.getBlock(x, y, z);
		if (topFace) {
			return neighbour.getMaterial() == Material.water || WaterlogRuntime.isWaterlogged(world, x, y, z);
		}
		return neighbour.isOpaqueCube() || neighbour.getMaterial() == Material.water || WaterlogRuntime.isWaterlogged(world, x, y, z);
	}

	/**
	 * 整格静水画水（plan §3.4 渲染）：从海洋植物画水器迁移而来，逐面复刻
	 * vanilla 液体几何——顶/底面用静水贴图、侧面用流动贴图与 vanilla 一致，
	 * 生物群系染色 + 逐面明暗 + 顶面 eps 下沉、侧面 V 映射跟随角高；水面
	 * 高度用共享角高平均与周边真水齐平。
	 *
	 * @return 是否实际发射了 quad：六面全被剔除（如完全没入水中）时返回
	 * false，pass 1 据此不得声明有内容（空 pass-1 排序崩溃）。
	 */
	public static boolean renderStillWaterVolume(IBlockAccess world, int x, int y, int z) {
		Tessellator tessellator = Tessellator.instance;
		IIcon still = Blocks.water.getIcon(0, 0);
		IIcon flow = Blocks.water.getIcon(2, 0);
		double eps = 0.001D;
		boolean emitted = false;

		int colour = Blocks.water.colorMultiplier(world, x, y, z);
		float r = (colour >> 16 & 255) / 255.0F;
		float g = (colour >> 8 & 255) / 255.0F;
		float b = (colour & 255) / 255.0F;

		// Corner heights, vanilla renderBlockLiquid L4164-4167. Like vanilla, the
		// eps offset is subtracted in place when the top face renders, so side
		// faces share the slightly lowered top edge (L4184-4187); when the top is
		// covered the sides keep the raw full height.
		double d2 = getLiquidHeight(world, x, y, z);
		double d3 = getLiquidHeight(world, x, y, z + 1);
		double d4 = getLiquidHeight(world, x + 1, y, z + 1);
		double d5 = getLiquidHeight(world, x + 1, y, z);

		if (!isFaceCulled(world, x, y + 1, z, true)) {
			emitted = true;
			d2 -= eps;
			d3 -= eps;
			d4 -= eps;
			d5 -= eps;
			tessellator.setBrightness(Blocks.water.getMixedBrightnessForBlock(world, x, y, z));
			tessellator.setColorOpaque_F(r, g, b);
			quad(tessellator, still,
					x, y + d2, z, 0, 0,
					x, y + d3, z + 1, 0, 16,
					x + 1, y + d4, z + 1, 16, 16,
					x + 1, y + d5, z, 16, 0);
		}

		// Vanilla shades the bottom face grey without biome tint
		if (!isFaceCulled(world, x, y - 1, z, false)) {
			emitted = true;
			tessellator.setBrightness(Blocks.water.getMixedBrightnessForBlock(world, x, y - 1, z));
			tessellator.setColorOpaque_F(0.5F, 0.5F, 0.5F);
			quad(tessellator, still,
					x, y + eps, z, 0, 0,
					x + 1, y + eps, z, 16, 0,
					x + 1, y + eps, z + 1, 16, 16,
					x, y + eps, z + 1, 0, 16);
		}

		int[] dx = {0, 0, -1, 1};
		int[] dz = {-1, 1, 0, 0};
		for (int dir = 0; dir < 4; dir++) {
			if (isFaceCulled(world, x + dx[dir], y, z + dz[dir], false)) {
				continue;
			}
			emitted = true;
			tessellator.setBrightness(Blocks.water.getMixedBrightnessForBlock(world, x + dx[dir], y, z + dz[dir]));
			float shade = dir < 2 ? 0.8F : 0.6F;
			tessellator.setColorOpaque_F(shade * r, shade * g, shade * b);
			// V mapping follows vanilla L4319-4321: V_top = (1 - cornerHeight) * 8,
			// V_bottom = 8, so side quads meet the lowered top edge seamlessly
			switch (dir) {
			case 0: // north, corners (x,z) -> (x+1,z)
				quad(tessellator, flow,
						x, y + d2, z + eps, 0, (1.0D - d2) * 8.0D,
						x + 1, y + d5, z + eps, 8, (1.0D - d5) * 8.0D,
						x + 1, y, z + eps, 8, 8,
						x, y, z + eps, 0, 8);
				break;
			case 1: // south, corners (x+1,z+1) -> (x,z+1)
				quad(tessellator, flow,
						x + 1, y + d4, z + 1 - eps, 0, (1.0D - d4) * 8.0D,
						x, y + d3, z + 1 - eps, 8, (1.0D - d3) * 8.0D,
						x, y, z + 1 - eps, 8, 8,
						x + 1, y, z + 1 - eps, 0, 8);
				break;
			case 2: // west, corners (x,z+1) -> (x,z)
				quad(tessellator, flow,
						x + eps, y + d3, z + 1, 0, (1.0D - d3) * 8.0D,
						x + eps, y + d2, z, 8, (1.0D - d2) * 8.0D,
						x + eps, y, z, 8, 8,
						x + eps, y, z + 1, 0, 8);
				break;
			default: // east, corners (x+1,z) -> (x+1,z+1)
				quad(tessellator, flow,
						x + 1 - eps, y + d5, z, 0, (1.0D - d5) * 8.0D,
						x + 1 - eps, y + d4, z + 1, 8, (1.0D - d4) * 8.0D,
						x + 1 - eps, y, z + 1, 8, 8,
						x + 1 - eps, y, z, 0, 8);
				break;
			}
		}

		return emitted;
	}

	/** Emits one double-sided quad (both windings, like the vanilla liquid renderer). */
	private static void quad(Tessellator tessellator, IIcon icon,
			double x1, double y1, double z1, double u1, double v1,
			double x2, double y2, double z2, double u2, double v2,
			double x3, double y3, double z3, double u3, double v3,
			double x4, double y4, double z4, double u4, double v4) {
		tessellator.addVertexWithUV(x1, y1, z1, icon.getInterpolatedU(u1), icon.getInterpolatedV(v1));
		tessellator.addVertexWithUV(x2, y2, z2, icon.getInterpolatedU(u2), icon.getInterpolatedV(v2));
		tessellator.addVertexWithUV(x3, y3, z3, icon.getInterpolatedU(u3), icon.getInterpolatedV(v3));
		tessellator.addVertexWithUV(x4, y4, z4, icon.getInterpolatedU(u4), icon.getInterpolatedV(v4));
		tessellator.addVertexWithUV(x1, y1, z1, icon.getInterpolatedU(u1), icon.getInterpolatedV(v1));
		tessellator.addVertexWithUV(x4, y4, z4, icon.getInterpolatedU(u4), icon.getInterpolatedV(v4));
		tessellator.addVertexWithUV(x3, y3, z3, icon.getInterpolatedU(u3), icon.getInterpolatedV(v3));
		tessellator.addVertexWithUV(x2, y2, z2, icon.getInterpolatedU(u2), icon.getInterpolatedV(v2));
	}
}
