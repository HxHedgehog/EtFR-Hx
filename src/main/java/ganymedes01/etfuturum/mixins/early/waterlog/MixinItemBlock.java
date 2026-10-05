package ganymedes01.etfuturum.mixins.early.waterlog;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ganymedes01.etfuturum.waterlog.WaterlogFamilies;
import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * 26.2 的 {@code getStateForPlacement}：可含水方块放进精确水源（静态水
 * meta 0）时自带含水状态。挂点选在 {@link ItemBlock#placeBlockAt}：
 * <ul>
 * <li>不能在 setBlock 的 meta 里直接带 bit16——{@code BlockStairs.onBlockPlacedBy}
 * 会在放置后用 {@code facing | (meta & 4)} 重写 metadata，bit16 必丢；
 * 模组楼梯子类可能有自己的 onBlockPlacedBy，所以必须挂在其后的TAIL；</li>
 * <li>HEAD 用 ThreadLocal 捕获目标格放置前的状态（TAIL 时格子已是新方块，
 * 无法再判断原来是不是水源），TAIL（= 成功路径的最后一条 return，setBlock
 * 失败走不到）在所有放置回调完成后补写 bit16；</li>
 * <li>客户端预测与服务端执行都会走这条链路，两侧一致写位；</li>
 * <li>水格可被替换（MaterialLiquid 构造器 setReplaceable），vanilla 放置链
 * 天然支持把方块放进水格，目标格即水源格本身。</li>
 * </ul>
 */
@Mixin(ItemBlock.class)
public abstract class MixinItemBlock {

	@Shadow
	public Block field_150939_a;

	@Unique
	private static final ThreadLocal<Boolean> etfuturum$targetWasSource = new ThreadLocal<>();

	// placeBlockAt 是 Forge 添加的方法：不在 SRG 表、运行时保持 MCP 名，remap=false；
	// 签名已对照 ItemBlock.java L229 人工核对（remap=false 跳过 AP 校验，靠人工保证）。
	@Inject(method = "placeBlockAt(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/world/World;IIIIFFFIZ)Z", at = @At("HEAD"), remap = false)
	private void etfuturum$captureTargetCell(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int metadata, CallbackInfo ci) {
		if (!WaterlogRuntime.isEnabled() || !WaterlogFamilies.isWaterloggable(this.field_150939_a)) {
			etfuturum$targetWasSource.remove();
		} else {
			etfuturum$targetWasSource.set(WaterlogRuntime.isSourceWater(world, x, y, z));
		}
	}

	@Inject(method = "placeBlockAt(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/world/World;IIIIFFFIZ)Z", at = @At("TAIL"), remap = false)
	private void etfuturum$waterlogPlacedBlock(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int metadata, CallbackInfo ci) {
		Boolean wasSource = etfuturum$targetWasSource.get();
		etfuturum$targetWasSource.remove();
		if (Boolean.TRUE.equals(wasSource) && world.getBlock(x, y, z) == this.field_150939_a) {
			WaterlogRuntime.markWaterlogged(world, x, y, z);
		}
	}
}
