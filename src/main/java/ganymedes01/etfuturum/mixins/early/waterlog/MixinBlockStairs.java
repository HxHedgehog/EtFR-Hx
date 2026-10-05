package ganymedes01.etfuturum.mixins.early.waterlog;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import net.minecraft.block.BlockStairs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * 含水写入点：{@code BlockStairs.onBlockPlacedBy}（vanilla 方法，TAIL
 * 锚点稳定）用 {@code facing | (meta & 4)} 重写 metadata，bit16 必丢——
 * 在其后按 {@code MixinItemBlock} 捕获的源判定补写 bit16。onBlockPlacedBy
 * 只在 placeBlockAt 成功路径被调用（{@code getBlock(x,y,z) == block}）。
 *
 * <p>模组楼梯若 override onBlockPlacedBy 且不调 super 则不会含水，Phase 3
 * 模组子类回归时再处理。
 */
@Mixin(BlockStairs.class)
public abstract class MixinBlockStairs {

	@Inject(method = "onBlockPlacedBy(Lnet/minecraft/world/World;IIILnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;)V", at = @At("TAIL"))
	private void etfuturum$waterlogAfterOrientation(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack, CallbackInfo ci) {
		if (WaterlogRuntime.consumePlacementSource()) {
			int meta = world.getBlockMetadata(x, y, z);
			WaterlogRuntime.debug("WRITE " + x + "," + y + "," + z + " meta=" + meta + " -> " + (meta | 0x10));
			WaterlogRuntime.markWaterlogged(world, x, y, z);
		}
	}
}
