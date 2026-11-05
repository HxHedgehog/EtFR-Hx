package ganymedes01.etfuturum.mixins.early.chestpair;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ganymedes01.etfuturum.chestpair.ChestPairRuntime;
import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.world.World;

/**
 * 26.2 双箱语义的 TE 侧：配对判定 + 旧存档迁移（位布局见
 * {@link ChestPairRuntime}）。
 *
 * <p>1710 TileEntityChest 的双箱缓存（checkForAdjacentChests L276-326 →
 * func_145977_a L328-339）只查 "instanceof BlockChest 同类型"，无朝向检查
 * ——"朝向不一致也合并成大箱"的根因。26.2 的配对完全由 TYPE 状态决定
 * （combine L294-298 + DoubleBlockCombiner L17-47：自身 TYPE 定连接方向，
 * 伙伴须同朝向且 TYPE 对侧），此处把 func_145977_a 重定向为同一条规则：
 * SINGLE 恒 false，LEFT 只认顺时针侧伙伴格，RIGHT 只认逆时针侧伙伴格。
 * checkForAdjacentChests 的四方向扫描结构、func_145978_a 缓存一致性、
 * 渲染器/音效/numPlayersUsing 联动均不需要改动（每对箱子只有一格
 * adjacentChest* 字段被置位，主箱约定 XNeg/ZNeg 侧自动成立）。
 *
 * <p>旧存档迁移：bit7 MARKER 未置位的箱子（升级前放置的箱子）在 TE 缓存
 * 失效后的首次 checkForAdjacentChests 里按新规则重扫：顺时针侧优先寻找
 * 同朝向、同为旧数据的相邻箱，命中则按放置语义（自身 = 顺时针侧命中 →
 * LEFT）一次性写齐双方的 TYPE + MARKER。朝向轴方向贴放的旧箱、朝向不一
 * 致的旧双箱保持单箱——对应 26.2 "SINGLE 不主动合并"与"只在垂直轴配对"。
 * 已配对的旧双箱内容各自保留在两侧 TE 中，不会合并/丢失。
 */
@Mixin(TileEntityChest.class)
public abstract class MixinTileEntityChest extends TileEntity {

	/**
	 * func_145977_a 重定向：连接方向与伙伴合法性完全由 TYPE 决定。
	 */
	@Inject(method = "func_145977_a(III)Z", at = @At("HEAD"), cancellable = true)
	private void etfuturum$modernPartnerCheck(int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
		World world = this.worldObj;
		if (world == null) {
			cir.setReturnValue(false);
			return;
		}
		int meta = world.getBlockMetadata(this.xCoord, this.yCoord, this.zCoord);
		int type = ChestPairRuntime.getType(meta);
		if (type == ChestPairRuntime.TYPE_SINGLE) {
			cir.setReturnValue(false);
			return;
		}
		int facing = meta & 7;
		if (facing < 2) {
			cir.setReturnValue(false);
			return;
		}
		int sign = type == ChestPairRuntime.TYPE_LEFT ? 1 : -1;
		int px = this.xCoord + sign * ChestPairRuntime.cwDx(facing);
		int pz = this.zCoord + sign * ChestPairRuntime.cwDz(facing);
		if (px != x || pz != z) {
			cir.setReturnValue(false);
			return;
		}
		int chestType = ((TileEntityChest) (Object) this).func_145980_j();
		cir.setReturnValue(ChestPairRuntime.isPartnerChest(world, x, y, z, facing, chestType, type));
	}

	/**
	 * 旧存档迁移扫描：MARKER 未置位（升级前数据）才执行；命中即写齐双方
	 * TYPE + MARKER，此后本方法对双方恒早退。服务端独占，客户端读同步 meta。
	 */
	@Inject(method = "checkForAdjacentChests()V", at = @At("HEAD"))
	private void etfuturum$migrateLegacyPairing(CallbackInfo ci) {
		World world = this.worldObj;
		if (world == null || world.isRemote) {
			return;
		}
		int meta = world.getBlockMetadata(this.xCoord, this.yCoord, this.zCoord);
		if (!ChestPairRuntime.isLegacyChestMeta(meta)) {
			return;
		}
		int facing = meta & 7;
		if (facing < 2) {
			return;
		}
		Block self = world.getBlock(this.xCoord, this.yCoord, this.zCoord);
		if (!(self instanceof BlockChest)) {
			return;
		}
		int chestType = ((BlockChest) self).field_149956_a;

		int cwX = this.xCoord + ChestPairRuntime.cwDx(facing);
		int cwZ = this.zCoord + ChestPairRuntime.cwDz(facing);
		int sign;
		if (ChestPairRuntime.isLegacyPartnerChest(world, cwX, this.yCoord, cwZ, facing, chestType)) {
			sign = 1;
		} else {
			int ccwX = this.xCoord - ChestPairRuntime.cwDx(facing);
			int ccwZ = this.zCoord - ChestPairRuntime.cwDz(facing);
			if (!ChestPairRuntime.isLegacyPartnerChest(world, ccwX, this.yCoord, ccwZ, facing, chestType)) {
				return;
			}
			sign = -1;
		}

		int partnerX = this.xCoord + sign * ChestPairRuntime.cwDx(facing);
		int partnerZ = this.zCoord + sign * ChestPairRuntime.cwDz(facing);
		int selfType = sign == 1 ? ChestPairRuntime.TYPE_LEFT : ChestPairRuntime.TYPE_RIGHT;
		int partnerType = sign == 1 ? ChestPairRuntime.TYPE_RIGHT : ChestPairRuntime.TYPE_LEFT;
		int partnerMeta = world.getBlockMetadata(partnerX, this.yCoord, partnerZ);
		world.setBlockMetadataWithNotify(this.xCoord, this.yCoord, this.zCoord,
				facing | selfType | ChestPairRuntime.MARKER | (meta & WaterlogRuntime.WATERLOGGED_BIT), 3);
		world.setBlockMetadataWithNotify(partnerX, this.yCoord, partnerZ,
				facing | partnerType | ChestPairRuntime.MARKER | (partnerMeta & WaterlogRuntime.WATERLOGGED_BIT), 3);
	}
}
