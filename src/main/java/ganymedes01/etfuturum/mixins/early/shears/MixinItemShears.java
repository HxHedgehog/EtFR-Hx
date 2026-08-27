package ganymedes01.etfuturum.mixins.early.shears;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Aligns shears durability with 26.2 (modern): breaking any block other than
 * fire costs 1 durability (26.2 ShearsItem.mineBlock, damagePerBlock = 1).
 * Vanilla 1.7.10 only costs durability on the IShearable shear path, so
 * breaking e.g. dirt was free.
 *
 * IShearable blocks already cost 1 via ItemShears.onBlockStartBreak, so they
 * are skipped here to avoid double damage. Soul fire is the same block as
 * vanilla fire in this mod (metadata state), so Blocks.fire covers it too.
 */
@Mixin(ItemShears.class)
public class MixinItemShears {

	@Inject(method = "onBlockDestroyed", at = @At("HEAD"), cancellable = true)
	private void etfuturum$modernShearsDurability(ItemStack stack, World worldIn, Block blockIn, int x, int y, int z,
												  EntityLivingBase player, CallbackInfoReturnable<Boolean> cir) {
		if (blockIn != Blocks.fire && !(blockIn instanceof IShearable)) {
			stack.damageItem(1, player);
		}
		cir.setReturnValue(true);
	}
}
