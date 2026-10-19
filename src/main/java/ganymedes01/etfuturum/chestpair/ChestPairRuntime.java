package ganymedes01.etfuturum.chestpair;

import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.world.World;

/**
 * 26.2 ChestBlock 双箱语义的 meta 位布局与判定工具。
 *
 * <p>注意：本类是 mixin 运行时依赖，必须放在 mixins 包外——mixin 包
 * （mixins.early.*）是受保护命名空间，其中的非 mixin 类在运行时不可直接
 * 加载（IllegalClassLoadError，2026-10-19 首测崩溃根因），参照 waterlog 包
 * WaterlogRuntime 的位置约定。见 mixins.early.chestpair 下两个 mixin。
 *
 * <p>26.2 用 BlockState 属性 FACING + TYPE(SINGLE/LEFT/RIGHT) 存储配对状态
 * （ChestBlock L169 默认态、updateShape L184-197 状态机、getChestType
 * L240-248 放置判定）。1710 无 BlockState，改用 meta 空闲位存储（与含水
 * bit16 同一套 NEIDs 16-bit 扩展机制，仅朝向 2-5 占用 bit0-2）：
 * <ul>
 * <li>bit0-2：朝向（vanilla 2=N/3=S/4=W/5=E）；</li>
 * <li>bit4（16）：含水 WATERLOGGED_BIT（waterlog 包，本包只保留不解释）；</li>
 * <li>bit5-6：TYPE——0=SINGLE、32=LEFT（连接顺时针侧）、64=RIGHT（连接
 * 逆时针侧），对应 26.2 ChestType；</li>
 * <li>bit7（128）：MODERN 标记——TYPE 状态已初始化。旧存档箱子无此标记，
 * 由 MixinTileEntityChest 的迁移扫描按新规则补写（两只同朝向旧双箱在首次
 * 缓存失效时重新配对；朝向轴方向贴放的旧箱/朝向不一致的旧双箱保持单箱，
 * 与 26.2 "SINGLE 不主动合并"一致）。</li>
 * </ul>
 *
 * <p>顺时针方向表（Facing.NORTH.getClockWise() = EAST 等）：
 * meta2(N)→+X、meta3(S)→−X、meta4(W)→−Z、meta5(E)→+Z。
 * 26.2 getConnectedDirection L212-215：LEFT 连 facing.getClockWise()、
 * RIGHT 连 getCounterClockWise()——恒为垂直于朝向的水平轴，与 1710 朝向
 * 轴约定（meta2/3 沿 X 配对、4/5 沿 Z 配对）逐方向一致。
 */
public final class ChestPairRuntime {

	/** TYPE 属性位（bit5-6）。 */
	public static final int TYPE_BITS = 32 | 64;
	/** ChestType.SINGLE：未配对（TYPE 位全零）。 */
	public static final int TYPE_SINGLE = 0;
	/** ChestType.LEFT：连接顺时针侧邻居（26.2 getConnectedDirection L214）。 */
	public static final int TYPE_LEFT = 32;
	/** ChestType.RIGHT：连接逆时针侧邻居。 */
	public static final int TYPE_RIGHT = 64;
	/** bit7：TYPE 状态已初始化（旧存档迁移判据）。 */
	public static final int MARKER = 128;

	private ChestPairRuntime() {
	}

	/** 读取 TYPE 位段。 */
	public static int getType(int meta) {
		return meta & TYPE_BITS;
	}

	/** 顺时针侧的水平 X 偏移（按朝向 meta）。 */
	public static int cwDx(int facing) {
		switch (facing) {
			case 2:
				return 1;
			case 3:
				return -1;
			default:
				return 0;
		}
	}

	/** 顺时针侧的水平 Z 偏移（按朝向 meta）。 */
	public static int cwDz(int facing) {
		switch (facing) {
			case 4:
				return -1;
			case 5:
				return 1;
			default:
				return 0;
		}
	}

	/** 同类型（普通/陷阱）箱子判定，对应 26.2 chestCanConnectTo（is 同方块）+ 1710 field_149956_a。 */
	public static boolean isChestOfType(World world, int x, int y, int z, int chestType) {
		Block block = world.getBlock(x, y, z);
		return block instanceof BlockChest && ((BlockChest) block).field_149956_a == chestType;
	}

	/**
	 * 可配对的单箱：同类型 + 同朝向 + TYPE==SINGLE，对应 26.2
	 * candidatePartnerFacing L258-261（TYPE==SINGLE 是硬条件）。
	 */
	public static boolean isSinglePartnerChest(World world, int x, int y, int z, int facing, int chestType) {
		if (!isChestOfType(world, x, y, z, chestType)) {
			return false;
		}
		int meta = world.getBlockMetadata(x, y, z);
		return (meta & 7) == facing && getType(meta) == 0;
	}

	/**
	 * 已连接的合法伙伴：同类型 + 同朝向 + TYPE 互为对侧，对应 26.2
	 * combineWithNeigbour L35（同方块 + 非 SINGLE + 类型互异 + 同朝向）。
	 */
	public static boolean isPartnerChest(World world, int x, int y, int z, int facing, int chestType, int selfType) {
		if (!isChestOfType(world, x, y, z, chestType)) {
			return false;
		}
		int meta = world.getBlockMetadata(x, y, z);
		int partnerType = selfType == TYPE_LEFT ? TYPE_RIGHT : TYPE_LEFT;
		return (meta & 7) == facing && getType(meta) == partnerType;
	}

	/** 旧存档箱子：TYPE/MARKER 位全空（含水 bit4 允许存在）。 */
	public static boolean isLegacyChestMeta(int meta) {
		return (meta & (TYPE_BITS | MARKER)) == 0;
	}

	/** 旧存档可迁移伙伴：同类型 + 同朝向 + 同为无 TYPE/MARKER 的旧数据。 */
	public static boolean isLegacyPartnerChest(World world, int x, int y, int z, int facing, int chestType) {
		if (!isChestOfType(world, x, y, z, chestType)) {
			return false;
		}
		int meta = world.getBlockMetadata(x, y, z);
		return (meta & 7) == facing && isLegacyChestMeta(meta);
	}
}
