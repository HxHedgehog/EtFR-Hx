package ganymedes01.etfuturum.mixins.early.waterlog;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import net.minecraft.block.BlockStainedGlassPane;

/**
 * 染色玻璃板的物品侧含水位剥离（26.2 铁律：ItemStack 无 waterlogged 概念，
 * BlockBehaviour.getCloneItemStack = new ItemStack(asItem())，不带任何 state）。
 *
 * <p>1.7.10 的物品 damage = {@code damageDropped(世界 meta)}，而染色板覆写
 * {@code damageDropped} 返回原 meta——含水格 bit16 直通物品：中键取块
 * （Minecraft.func_147112_ai → ForgeHooks.onPickBlock → getPickBlock →
 * getDamageValue → damageDropped）与挖掘掉落（getDrops → damageDropped）
 * 都产出含水 damage 物品，放置时经 BlockStairs 式 {@code meta | 4} 保留路径
 * 写回世界 → 干放也含水（2026-10-07 用户实证）。
 *
 * <p>其余家族类无需此注入：楼梯/玻璃板/铁栏杆 {@code damageDropped} 默认
 * 返回 0、半砖 {@code &7}，bit16 天然剥离；玻璃板精采的 createStackedBlock
 * 直通另由 MixinBlockPane 处理；放置侧兜底在 MixinItemBlock。
 */
@Mixin(BlockStainedGlassPane.class)
public class MixinBlockStainedGlassPane {

	@Inject(method = "damageDropped(I)I", at = @At("HEAD"), cancellable = true)
	private void etfuturum$stripWaterlogFromVariantDamage(int meta, CallbackInfoReturnable<Integer> cir) {
		if (WaterlogRuntime.isEnabled() && (meta & WaterlogRuntime.WATERLOGGED_BIT) != 0) {
			cir.setReturnValue(meta & ~WaterlogRuntime.WATERLOGGED_BIT);
		}
	}
}
