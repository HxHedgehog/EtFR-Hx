package ganymedes01.etfuturum.waterlog;

import cpw.mods.fml.common.Loader;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * 真含水运行时谓词（plan §3.1）。编码：{@code waterloggedMeta = baseMeta | 0x10}，
 * 低 4 位永远保留原版语义；bit16 的持久化（区块存储/NBT/网络）完全依赖
 * NotEnoughIDs 的 16-bit meta 补丁，无任何编译依赖。
 *
 * <p>门控 = NEIDs 已装 && {@code disableWaterlogging=false}，二者缺一不可；
 * 门控关闭时所有判定短路返回 false → 零行为、零渲染、零写入，旧存档中
 * 的 bit16 方块自动按干方块表现（nibble {@code &15} 完美退化），无需迁移。
 */
public final class WaterlogRuntime {

	public static final int WATERLOGGED_BIT = 0x10;

	private static boolean enabled;
	private static boolean resolved;

	private WaterlogRuntime() {}

	/** 配置不能游戏中途变更，首次调用后缓存（客户端渲染线程与服务端线程并发首调结果是确定的，无需同步）。 */
	public static boolean isEnabled() {
		if (!resolved) {
			enabled = Loader.isModLoaded("neid") && !ConfigMixins.disableWaterlogging;
			resolved = true;
		}
		return enabled;
	}

	public static boolean isWaterlogged(IBlockAccess world, int x, int y, int z) {
		if (!isEnabled()) {
			return false;
		}
		Block block = world.getBlock(x, y, z);
		return WaterlogFamilies.isWaterloggable(block) && (world.getBlockMetadata(x, y, z) & WATERLOGGED_BIT) != 0;
	}

	/** 26.2 只认精确水源（{@code is(Fluids.WATER)}）：1.7.10 对应静态水 Blocks.water 且 meta 0（流水/跌水均不算）。 */
	public static boolean isSourceWater(IBlockAccess world, int x, int y, int z) {
		return world.getBlock(x, y, z) == Blocks.water && world.getBlockMetadata(x, y, z) == 0;
	}

	/** 写入含水位。只允许在 {@link #isEnabled()} 为真时调用（无 NEIDs 时写入会被 nibble 静默截断，禁止走到）。 */
	public static void markWaterlogged(World world, int x, int y, int z) {
		world.setBlockMetadataWithNotify(x, y, z, world.getBlockMetadata(x, y, z) | WATERLOGGED_BIT, 2);
	}
}
