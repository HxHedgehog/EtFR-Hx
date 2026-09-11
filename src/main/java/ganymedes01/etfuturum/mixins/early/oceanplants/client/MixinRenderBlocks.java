package ganymedes01.etfuturum.mixins.early.oceanplants.client;

import ganymedes01.etfuturum.blocks.BlockAquaticPlant;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * getLiquidHeight() samples neighbour block metadata to interpolate the water
 * surface height. Ocean plants are water-material with unrelated metadata
 * (kelp age etc.), which would carve notches into the rendered water surface
 * around them. They behave like full still water for height purposes.
 */
@Mixin(RenderBlocks.class)
public abstract class MixinRenderBlocks {

	@Redirect(method = "getLiquidHeight",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/IBlockAccess;getBlockMetadata(III)I"))
	private int etfuturum$oceanPlantsReadAsFullWater(IBlockAccess access, int x, int y, int z) {
		Block block = access.getBlock(x, y, z);
		if (BlockAquaticPlant.isOceanPlant(block)) {
			return 0;
		}
		return access.getBlockMetadata(x, y, z);
	}
}
