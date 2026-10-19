package ganymedes01.etfuturum.mixins.early.chestpair;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ganymedes01.etfuturum.chestpair.ChestPairRuntime;
import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * 26.2 ChestBlock 双箱语义复刻（meta 位布局见 {@link ChestPairRuntime}）。
 *
 * <p>对照点（26.2 源码行号）：
 * <ul>
 * <li>canPlaceBlockAt：26.2 无邻居箱子放置限制 → 放宽为基类 isReplaceable
 * 检查（1710 L319-349 的 "1 箱邻居可放、2+ 箱邻居/邻双箱拒绝" 整体退役，
 * 即"不能并排放大箱子"的根因）；</li>
 * <li>func_149954_e（onBlockAdded 双箱朝向修正，L201-314 无条件重写自身
 * 朝向并顺带重写邻居朝向）→ 整体取消：26.2 朝向只在放置时确定，永不重写
 * （updateShape L184-197 只增删 TYPE，不碰 FACING）；</li>
 * <li>onBlockPlacedBy → 重实现：保留 vanilla yaw→朝向映射（L138-158），
 * TYPE 按 26.2 getChestType L240-248 判定（顺时针侧候选优先，其次逆时针侧，
 * 候选须为 SINGLE），连同时把伙伴 TYPE 翻到对侧（updateShape L190-191 的
 * 配对半边，26.2 由邻居更新触发，此处等价直达）；只写自身与伙伴两格，
 * 不再重写无关邻居朝向；hasDisplayName 命名分支照旧保留（L195-198）；
 * bit16 含水纯保留式并入（含水 MixinBlockChest 的 @Redirect 在本方法被
 * cancel 后成为死代码，保留位由本实现接管）；</li>
 * <li>onNeighborBlockChange → 追加 TYPE 稳定性校验（updateShape L193-194
 * "连接对象失效回退 SINGLE"半边）：TYPE 非 SINGLE 而伙伴格不再合法时写回
 * SINGLE（保留朝向与 bit16）。SINGLE 状态不主动重配对（26.2 L190 要求
 * 邻居非 SINGLE），因此"打掉双箱之一、旁边第三只箱"不会自动三合二；</li>
 * <li>func_149951_m（GUI/漏斗/比较器统一入口）→ 重实现为按 TYPE 定向取
 * 伙伴（26.2 combine L294-298 + DoubleBlockCombiner L17-47）：自身或伙伴
 * 上方被挡（solid/猫）返回 null 的 vanilla 语义保留，其余邻居的挡位检查
 * 不再越权（26.2 只检查连接伙伴）；容器顺序 = CompoundContainer(first,
 * second)，first = FIRST = RIGHT 型箱 = 顺时针侧箱（ChestBlock L96-100、
 * DoubleBlockCombiner L41-43），即 GUI 上半 = 26.2 一致。</li>
 * </ul>
 *
 * <p>放置与潜行（26.2 getStateForPlacement L224-239）：潜行放置强制 SINGLE
 * 不与邻居合并（L235 !secondaryUse 门控，朝向仍为玩家反向）；L231"潜行 +
 * 侧面对箱贴放继承邻居朝向并配对"特例未移植（1710 onBlockPlacedBy 无
 * 点击面上下文，仅此一处偏离）。
 */
@Mixin(BlockChest.class)
public abstract class MixinBlockChest extends Block {

	protected MixinBlockChest(Material material) {
		super(material);
	}

	@Shadow
	private static boolean func_149953_o(World world, int x, int y, int z) {
		throw new AssertionError();
	}

	/**
	 * 26.2 无邻居箱子放置限制；基类 Block.canPlaceBlockAt 只做 isReplaceable。
	 */
	@Inject(method = "canPlaceBlockAt(Lnet/minecraft/world/World;III)Z", at = @At("HEAD"), cancellable = true)
	private void etfuturum$allowAdjacentChestPlacement(World world, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(world.getBlock(x, y, z).isReplaceable(world, x, y, z));
	}

	/**
	 * onBlockAdded 双箱朝向修正退役：朝向只在放置时确定（26.2 语义）。
	 * onBlockAdded 中对四个邻居的连带重写随之消失。
	 */
	@Inject(method = "func_149954_e(Lnet/minecraft/world/World;III)V", at = @At("HEAD"), cancellable = true)
	private void etfuturum$cancelLegacyMergeOrient(World world, int x, int y, int z, CallbackInfo ci) {
		ci.cancel();
	}

	/**
	 * 放置：yaw→朝向映射保留，TYPE 判定 + 伙伴翻转型 + 命名分支，bit16 保留式。
	 */
	@Inject(method = "onBlockPlacedBy(Lnet/minecraft/world/World;IIILnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;)V",
			at = @At("HEAD"), cancellable = true)
	private void etfuturum$placeWithModernPairing(World world, int x, int y, int z, EntityLivingBase placer, ItemStack itemIn, CallbackInfo ci) {
		int l = MathHelper.floor_double((double) (placer.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
		int facing = l == 0 ? 2 : l == 1 ? 5 : l == 2 ? 3 : 4;

		int selfMeta = world.getBlockMetadata(x, y, z);
		int chestType = ((BlockChest) (Object) this).field_149956_a;

		int type = ChestPairRuntime.TYPE_SINGLE;
		int partnerX = 0;
		int partnerZ = 0;

		// 26.2 getStateForPlacement L235：潜行放置跳过配对判定，强制 SINGLE
		// （朝向仍为玩家反向，L227 默认规则不受潜行影响）。
		if (!placer.isSneaking()) {
			int cwX = x + ChestPairRuntime.cwDx(facing);
			int cwZ = z + ChestPairRuntime.cwDz(facing);
			int ccwX = x - ChestPairRuntime.cwDx(facing);
			int ccwZ = z - ChestPairRuntime.cwDz(facing);

			if (ChestPairRuntime.isSinglePartnerChest(world, cwX, y, cwZ, facing, chestType)) {
				type = ChestPairRuntime.TYPE_LEFT;
				partnerX = cwX;
				partnerZ = cwZ;
			} else if (ChestPairRuntime.isSinglePartnerChest(world, ccwX, y, ccwZ, facing, chestType)) {
				type = ChestPairRuntime.TYPE_RIGHT;
				partnerX = ccwX;
				partnerZ = ccwZ;
			}
		}

		world.setBlockMetadataWithNotify(x, y, z,
				facing | type | ChestPairRuntime.MARKER | (selfMeta & WaterlogRuntime.WATERLOGGED_BIT), 3);

		if (type != ChestPairRuntime.TYPE_SINGLE) {
			int partnerMeta = world.getBlockMetadata(partnerX, y, partnerZ);
			int partnerType = type == ChestPairRuntime.TYPE_LEFT ? ChestPairRuntime.TYPE_RIGHT : ChestPairRuntime.TYPE_LEFT;
			world.setBlockMetadataWithNotify(partnerX, y, partnerZ,
					facing | partnerType | ChestPairRuntime.MARKER | (partnerMeta & WaterlogRuntime.WATERLOGGED_BIT), 3);
		}

		if (itemIn.hasDisplayName()) {
			((TileEntityChest) world.getTileEntity(x, y, z)).func_145976_a(itemIn.getDisplayName());
		}
		ci.cancel();
	}

	/**
	 * TYPE 非 SINGLE 而伙伴失效（被挖/朝向被改/类型位损坏）→ 回退 SINGLE
	 * （updateShape L193-194 半边）。SINGLE 不主动配对（L190 半边，见类注释）。
	 */
	@Inject(method = "onNeighborBlockChange(Lnet/minecraft/world/World;IIILnet/minecraft/block/Block;)V", at = @At("HEAD"))
	private void etfuturum$validatePairingOnNeighborChange(World world, int x, int y, int z, Block neighbor, CallbackInfo ci) {
		if (world.isRemote) {
			return;
		}
		int meta = world.getBlockMetadata(x, y, z);
		int type = ChestPairRuntime.getType(meta);
		if (type == ChestPairRuntime.TYPE_SINGLE) {
			return;
		}
		int facing = meta & 7;
		if (facing < 2) {
			return;
		}
		int sign = type == ChestPairRuntime.TYPE_LEFT ? 1 : -1;
		int px = x + sign * ChestPairRuntime.cwDx(facing);
		int pz = z + sign * ChestPairRuntime.cwDz(facing);
		int chestType = ((BlockChest) (Object) this).field_149956_a;
		if (!ChestPairRuntime.isPartnerChest(world, px, y, pz, facing, chestType, type)) {
			world.setBlockMetadataWithNotify(x, y, z,
					facing | ChestPairRuntime.MARKER | (meta & WaterlogRuntime.WATERLOGGED_BIT), 3);
		}
	}

	/**
	 * GUI/漏斗/比较器统一入口：按 TYPE 定向取伙伴（DoubleBlockCombiner 语义）。
	 * 上半 = RIGHT 型箱（顺时针侧），与 26.2 CompoundContainer(first, second)
	 * 的 first = FIRST 完全一致。
	 */
	@Inject(method = "func_149951_m(Lnet/minecraft/world/World;III)Lnet/minecraft/inventory/IInventory;",
			at = @At("HEAD"), cancellable = true)
	private void etfuturum$modernChestInventory(World world, int x, int y, int z, CallbackInfoReturnable<IInventory> cir) {
		TileEntityChest self = (TileEntityChest) world.getTileEntity(x, y, z);
		if (self == null) {
			cir.setReturnValue(null);
			return;
		}
		if (world.isSideSolid(x, y + 1, z, ForgeDirection.DOWN) || func_149953_o(world, x, y, z)) {
			cir.setReturnValue(null);
			return;
		}

		int meta = world.getBlockMetadata(x, y, z);
		int type = ChestPairRuntime.getType(meta);
		if (type == ChestPairRuntime.TYPE_SINGLE) {
			cir.setReturnValue(self);
			return;
		}
		int facing = meta & 7;
		if (facing < 2) {
			cir.setReturnValue(self);
			return;
		}

		int sign = type == ChestPairRuntime.TYPE_LEFT ? 1 : -1;
		int px = x + sign * ChestPairRuntime.cwDx(facing);
		int pz = z + sign * ChestPairRuntime.cwDz(facing);
		int chestType = ((BlockChest) (Object) this).field_149956_a;
		if (!ChestPairRuntime.isPartnerChest(world, px, y, pz, facing, chestType, type)) {
			cir.setReturnValue(self);
			return;
		}
		if (world.isSideSolid(px, y + 1, pz, ForgeDirection.DOWN) || func_149953_o(world, px, y, pz)) {
			cir.setReturnValue(null);
			return;
		}
		TileEntityChest partner = (TileEntityChest) world.getTileEntity(px, y, pz);
		if (partner == null) {
			cir.setReturnValue(self);
			return;
		}

		cir.setReturnValue(type == ChestPairRuntime.TYPE_RIGHT
				? new InventoryLargeChest("container.chestDouble", self, partner)
				: new InventoryLargeChest("container.chestDouble", partner, self));
	}
}
