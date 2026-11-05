package ganymedes01.etfuturum.mixins.early.oceanplants;

import ganymedes01.etfuturum.blocks.BlockAquaticPlant;
import net.minecraft.block.BlockLiquid;
import net.minecraft.init.Blocks;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ocean plants are Material.water blocks that carry their own metadata
 * (kelp age, seagrass half flags, pickle count). The liquid flow math reads
 * neighbouring water metadata as flow levels, so a kelp block would look like
 * shallow/falling water and bend currents around it. 26.2 gives every one of
 * these cells a permanent source FluidState (getFluidState =
 * getSource(false)), so all of them anchor the flow network as level-0 water -
 * wet sea pickle included; the dry pickle has Material.coral and never matches
 * here. (The plants used to read as -1, "non-liquid obstacle", which predates
 * the kelp/seagrass spread feature and left their spread water unfed next to
 * non-zero plant metadata.)
 */
@Mixin(BlockLiquid.class)
public abstract class MixinBlockLiquid {

	@Inject(method = "func_149804_e", at = @At("HEAD"), cancellable = true)
	private void etfuturum$oceanPlantsAreFlowSources(World world, int x, int y, int z, CallbackInfoReturnable<Integer> cir) {
		if ((Object) this != Blocks.water && (Object) this != Blocks.flowing_water) {
			return; // non-water liquids read the vanilla material checks
		}
		if (BlockAquaticPlant.isOceanPlant(world.getBlock(x, y, z))) {
			cir.setReturnValue(0);
		}
	}

	@Inject(method = "getEffectiveFlowDecay", at = @At("HEAD"), cancellable = true)
	private void etfuturum$oceanPlantsHaveSourceDecay(IBlockAccess access, int x, int y, int z, CallbackInfoReturnable<Integer> cir) {
		if ((Object) this != Blocks.water && (Object) this != Blocks.flowing_water) {
			return; // non-water liquids read the vanilla material checks
		}
		if (BlockAquaticPlant.isOceanPlant(access.getBlock(x, y, z))) {
			cir.setReturnValue(0);
		}
	}
}
