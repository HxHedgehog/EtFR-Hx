package ganymedes01.etfuturum.mixins.early.waterlog;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import ganymedes01.etfuturum.waterlog.WaterlogFamilies;
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
 * 源判定准确。写入点在 {@code BlockStairs.onBlockPlacedBy} TAIL（见
 * MixinBlockStairs）：vanilla 在那里用 {@code facing | (meta & 4)} 重写
 * metadata，必须在其后补写 bit16。
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
		boolean enabled = WaterlogRuntime.isEnabled();
		boolean family = WaterlogFamilies.isWaterloggable(this.field_150939_a);
		boolean wasSource = enabled && family && WaterlogRuntime.isSourceWater(world, x, y, z);
		WaterlogRuntime.debug("CAPTURE " + x + "," + y + "," + z + " enabled=" + enabled + " family=" + family
				+ " cell=" + world.getBlock(x, y, z) + "/" + world.getBlockMetadata(x, y, z) + " -> wasSource=" + wasSource);
		// 无条件覆盖：上次 placeBlockAt 失败时残留的 true 会被本次捕获清除
		WaterlogRuntime.capturePlacementSource(wasSource);
		return block.onBlockPlaced(world, x, y, z, side, hitX, hitY, hitZ, metadata);
	}
}
