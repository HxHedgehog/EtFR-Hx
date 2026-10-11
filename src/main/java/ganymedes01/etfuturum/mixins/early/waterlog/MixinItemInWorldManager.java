package ganymedes01.etfuturum.mixins.early.waterlog;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ganymedes01.etfuturum.waterlog.WaterlogFlow;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.world.World;

/**
 * 探针：服务端方块放置总入口（NetHandlerPlayServer → activateBlockOrUseItem）。
 * 1.7.10 的 ItemInWorldManager 没有名为 tryPlaceItemIntoWorld 的方法（那是
 * ItemStack 的方法，服务端从 activateBlockOrUseItem L422 调入）——放置入口
 * 是 activateBlockOrUseItem。打印玩家每次放置的物品身份（方块 id + meta +
 * 显示名）与坐标——用于核对爆炸测试中玩家实际放置的方块（是否为原版 TNT、
 * 是否为其他 mod 的爆炸物）。
 */
@Mixin(ItemInWorldManager.class)
public abstract class MixinItemInWorldManager {

	@Inject(method = "activateBlockOrUseItem", at = @At("HEAD"))
	private void etfuturum$logPlacement(EntityPlayer player, World world, ItemStack stack, int x, int y, int z,
			int side, float hitX, float hitY, float hitZ, CallbackInfoReturnable<Boolean> cir) {
		WaterlogFlow.notePlacement(stack, x, y, z);
	}
}
