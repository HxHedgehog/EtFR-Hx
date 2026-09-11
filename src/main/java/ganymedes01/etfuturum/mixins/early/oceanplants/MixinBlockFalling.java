package ganymedes01.etfuturum.mixins.early.oceanplants;

import ganymedes01.etfuturum.blocks.BlockAquaticPlant;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.7.10 falling blocks fall through any Material.water block. Ocean plants
 * are water-material but are actual plants, so falling sand/gravel must rest
 * on top of them instead of passing through and destroying them.
 */
@Mixin(BlockFalling.class)
public abstract class MixinBlockFalling {

	@Inject(method = "func_149831_e", at = @At("HEAD"), cancellable = true)
	private static void etfuturum$stopFallingOnOceanPlants(World world, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
		Block block = world.getBlock(x, y, z);
		if (BlockAquaticPlant.isOceanPlant(block)) {
			cir.setReturnValue(false);
		}
	}
}
