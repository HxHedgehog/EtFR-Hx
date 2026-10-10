package ganymedes01.etfuturum.mixins.early.waterlog;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ganymedes01.etfuturum.waterlog.WaterlogFamilies;
import ganymedes01.etfuturum.waterlog.WaterlogFlow;
import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * 26.2 的 {@code getStateForPlacement}：可含水方块放进精确水源（静态水
 * meta 0）时自带含水状态。
 *
 * <p>捕获点 = {@code ItemBlock.onItemUse} 里 {@code Block.onBlockPlaced}
 * 的调用（vanilla 方法，锚点稳定）：此时坐标参数已完成 side 偏移（vanilla
 * 直接改写参数槽）——目标格即最终放置格，且尚未 setBlock，内容仍是水，
 * 源判定准确。写入点 = 本类 {@code onItemUse} TAIL（统一写入点，楼梯/半砖
 * 共用）：楼梯的 onBlockPlacedBy 朝向重写、半砖的 onBlockPlaced 上/下半
 * 位（meta | 8）都在其之前完成，TAIL 补写 bit16 后无任何 vanilla 改写。
 * ItemSlab 的合成双层分支（func_150946_a 与目标格直接 setBlock double）
 * 不经过 ItemBlock.onItemUse，天然不写 bit16（双层不可含水，26.2 合成
 * DOUBLE 时 WATERLOGGED=false 丢水同款）。
 *
 * <p>曾尝试直接注入 Forge 添加的 {@code ItemBlock.placeBlockAt}（remap=false
 * + 人工核对签名），运行时未命中（HEAD 日志零输出），遂改挂全 vanilla 锚点。
 */
@Mixin(ItemBlock.class)
public abstract class MixinItemBlock {

	@Shadow
	public Block field_150939_a;

	@Redirect(method = "onItemUse(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/world/World;IIIIFFF)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;onBlockPlaced(Lnet/minecraft/world/World;IIIIFFFI)I"))
	private int etfuturum$capturePlacementCell(Block block, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int metadata) {
		// 物品永不携带含水位（26.2 铁律：ItemStack 无 waterlogged 概念）——
		// 修复前链路产出的含水 damage 物品（中键取块/掉落残留）在此剥离：
		// BlockStairs.onBlockPlaced 是 meta|4 保留输入位，bit16 会直通世界。
		// 家族格 vanilla meta 语义只用低 4 位，剥 bit16 对放置零影响。
		if (WaterlogFamilies.isWaterloggable(this.field_150939_a) && (metadata & WaterlogRuntime.WATERLOGGED_BIT) != 0) {
			metadata &= ~WaterlogRuntime.WATERLOGGED_BIT;
		}
		boolean enabled = WaterlogRuntime.isEnabled();
		boolean family = WaterlogFamilies.isWaterloggable(this.field_150939_a);
		boolean wasSource = enabled && family && WaterlogRuntime.isWaterCell(world, x, y, z);
		WaterlogRuntime.debug("CAPTURE " + x + "," + y + "," + z + " enabled=" + enabled + " family=" + family
				+ " cell=" + world.getBlock(x, y, z) + "/" + world.getBlockMetadata(x, y, z) + " -> wasSource=" + wasSource);
		// 无条件覆盖：上次 placeBlockAt 失败时残留的 true 会被本次捕获清除
		WaterlogRuntime.capturePlacementSource(wasSource);
		int result = block.onBlockPlaced(world, x, y, z, side, hitX, hitY, hitZ, metadata);
		// 原子含水放置（26.2 getStateForPlacement 语义：含水状态与方块同一
		// setBlock 落地）：bit16 并入 onBlockPlaced 返回值 → placeBlockAt 的
		// setBlock 一步写入。此前服务端两步写（setBlock 干块 + markWaterlogged）
		// 会让客户端预测先呈现干块外观，等服务端修正包到达才填水——快速放置
		// 可见闪烁（2026-10-07 用户实证）。result < 0 为取消语义，不动。楼梯
		// onBlockPlacedBy 的 meta 重写由 MixinBlockStairs 并回 bit16。
		if (wasSource && result >= 0) {
			result |= WaterlogRuntime.WATERLOGGED_BIT;
		}
		return result;
	}

	/**
	 * 统一写入点。@At("RETURN")（每条 return）而非 TAIL——ItemBlock.onItemUse
	 * 的成功路径 {@code return true}（placeBlockAt 之后）在最后一条
	 * {@code return false} 之前，TAIL 只挂最后一条 return，成功路径永不经过
	 * （2026-10-06 日志实证：CAPTURE wasSource=true 而无 WRITE，放置出干块）。
	 * 每条 return 都消费捕获标记（防残留自愈）。原子路径（捕获重定向把 bit16
	 * 并入放置 meta）已含水，此处只做兜底 mark（重写 meta 的 ItemBlock 子类等
	 * 未命中路径）并统一调度蔓延。
	 */
	@Inject(method = "onItemUse(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/world/World;IIIIFFF)Z", at = @At("RETURN"))
	private void etfuturum$writeWaterlogOnPlace(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, CallbackInfoReturnable<Boolean> cir) {
		boolean placed = cir.getReturnValue();
		boolean wasSource = WaterlogRuntime.consumePlacementSource();
		if (!placed || !wasSource) {
			return;
		}
		Block block = world.getBlock(x, y, z);
		if (!WaterlogFamilies.isWaterloggable(block)) {
			return;
		}
		if (!WaterlogRuntime.isWaterlogged(world, x, y, z)) {
			WaterlogRuntime.markWaterlogged(world, x, y, z);
		}
		WaterlogFlow.scheduleSpread(world, x, y, z, block);
		WaterlogRuntime.debug("WRITE " + x + "," + y + "," + z + " block=" + block);
	}
}
