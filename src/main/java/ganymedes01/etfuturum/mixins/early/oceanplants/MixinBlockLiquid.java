package ganymedes01.etfuturum.mixins.early.oceanplants;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.blocks.BlockAquaticPlant;
import net.minecraft.block.Block;
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
 * shallow/falling water and bend currents around it. Treat the plants as
 * non-liquid obstacles for flow decay lookups instead - except the wet sea
 * pickle, whose cell anchors the flow network as a level-0 water source
 * (26.2 waterlogged getFluidState semantics).
 */
@Mixin(BlockLiquid.class)
public abstract class MixinBlockLiquid {

	@Inject(method = "func_149804_e", at = @At("HEAD"), cancellable = true)
	private void etfuturum$oceanPlantsAreNotFlowSources(World world, int x, int y, int z, CallbackInfoReturnable<Integer> cir) {
		if ((Object) this != Blocks.water && (Object) this != Blocks.flowing_water) {
			return; // non-water liquids read the vanilla material checks
		}
		Block block = world.getBlock(x, y, z);
		if (block == ModBlocks.SEA_PICKLE.get()) {
			// The wet pickle is the source anchor; the dry one has Material.coral
			// and never matches here
			cir.setReturnValue(0);
		} else if (BlockAquaticPlant.isOceanPlant(block)) {
			cir.setReturnValue(-1);
		}
	}

	@Inject(method = "getEffectiveFlowDecay", at = @At("HEAD"), cancellable = true)
	private void etfuturum$oceanPlantsHaveNoFlowDecay(IBlockAccess access, int x, int y, int z, CallbackInfoReturnable<Integer> cir) {
		if ((Object) this != Blocks.water && (Object) this != Blocks.flowing_water) {
			return; // non-water liquids read the vanilla material checks
		}
		Block block = access.getBlock(x, y, z);
		if (block == ModBlocks.SEA_PICKLE.get()) {
			cir.setReturnValue(0);
		} else if (BlockAquaticPlant.isOceanPlant(block)) {
			cir.setReturnValue(-1);
		}
	}
}
