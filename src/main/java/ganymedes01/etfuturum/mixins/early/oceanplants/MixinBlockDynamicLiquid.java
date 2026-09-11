package ganymedes01.etfuturum.mixins.early.oceanplants;

import net.minecraft.block.BlockDynamicLiquid;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes BlockDynamicLiquid's private spread helpers. Wet sea pickles "ignite"
 * the vanilla spread as if their cell were a level-0 water source (the same
 * state a bucket-placed flowing_water block starts from); everything downstream
 * - cascade placement, decay refresh, settling to static water, receding once
 * the anchor disappears - then runs entirely through vanilla code.
 */
@Mixin(BlockDynamicLiquid.class)
public interface MixinBlockDynamicLiquid {

	@Invoker("func_149809_q")
	boolean etfuturum$canFlowInto(World world, int x, int y, int z);

	@Invoker("func_149808_o")
	boolean[] etfuturum$findFlowDirections(World world, int x, int y, int z);

	@Invoker("func_149813_h")
	void etfuturum$flowInto(World world, int x, int y, int z, int meta);
}
