package ganymedes01.etfuturum.mixins.early.waterlog;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ganymedes01.etfuturum.waterlog.WaterlogFlow;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemFlintAndSteel;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * 探针：打火石使用（TNT 的原版点燃方式）。若用户自述"点燃了 TNT"但
 * TNT-SPAWN 没响，此探针判定点燃动作本身是否发生、用的是什么工具。
 */
@Mixin(ItemFlintAndSteel.class)
public abstract class MixinItemFlintAndSteel {

	@Inject(method = "onItemUse", at = @At("HEAD"))
	private void etfuturum$logFlintUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
			float hitX, float hitY, float hitZ, CallbackInfoReturnable<Boolean> cir) {
		WaterlogFlow.noteFlintUse(x, y, z);
	}
}
