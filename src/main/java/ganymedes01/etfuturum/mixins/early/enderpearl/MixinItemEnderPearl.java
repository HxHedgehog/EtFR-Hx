package ganymedes01.etfuturum.mixins.early.enderpearl;

import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Allows creative-mode players to throw ender pearls, matching modern
 * behavior. Vanilla 1.7.10 returns immediately in creative mode and does
 * nothing. As in modern versions, the pearl is thrown without consuming the
 * item stack; survival behavior is left unchanged.
 */
@Mixin(ItemEnderPearl.class)
public class MixinItemEnderPearl extends Item {

	@Inject(method = "onItemRightClick", at = @At("HEAD"), cancellable = true)
	private void etfuturum$allowCreativeThrow(ItemStack itemStackIn, World worldIn, EntityPlayer player,
											  CallbackInfoReturnable<ItemStack> cir) {
		if (!player.capabilities.isCreativeMode) {
			return; // keep vanilla survival behavior (consumes the pearl)
		}
		worldIn.playSoundAtEntity(player, "random.bow", 0.5F, 0.4F / (itemRand.nextFloat() * 0.4F + 0.8F));
		if (!worldIn.isRemote) {
			worldIn.spawnEntityInWorld(new EntityEnderPearl(worldIn, player));
		}
		cir.setReturnValue(itemStackIn);
	}
}