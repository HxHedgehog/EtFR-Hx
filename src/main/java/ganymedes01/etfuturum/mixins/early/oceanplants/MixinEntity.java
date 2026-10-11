package ganymedes01.etfuturum.mixins.early.oceanplants;

import ganymedes01.etfuturum.blocks.BlockAquaticPlant;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Entity.class)
public abstract class MixinEntity {

	@Shadow
	public AxisAlignedBB boundingBox;

	/**
	 * The pseudo-waterlogged ocean plants occupy the water cell itself, so they
	 * already answer vanilla water detection wherever the detection band
	 * reaches. That band (Entity#handleWaterMovement) shrinks the bounding box
	 * by 0.4 on both vertical ends, so gliders with the feet in the lower part
	 * of a plant cell - or skimming just above short plant tops - miss every
	 * Material.water cell and keep moving at air speed. 26.2 keeps the fluid
	 * state in the full bounding box: OR the vanilla verdict with a full-box
	 * scan for ocean plant cells. Real water keeps the vanilla band behaviour.
	 * <p>
	 * TODO waterlog: real waterlogged stairs/fences (26.2 waterlogged blocks
	 * carry a water fluidstate over the whole cell) belong in this same OR -
	 * extend the full-box scan with their cell predicate once the feature
	 * lands; {@link BlockAquaticPlant#isTouchingOceanPlant} is the pattern.
	 */
	@Redirect(method = "handleWaterMovement",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/World;handleMaterialAcceleration(Lnet/minecraft/util/AxisAlignedBB;Lnet/minecraft/block/material/Material;Lnet/minecraft/entity/Entity;)Z"),
			require = 1)
	private boolean etfu$oceanPlantWaterDetection(World world, AxisAlignedBB bb, Material material, Entity entity) {
		if (world.handleMaterialAcceleration(bb, material, entity)) {
			return true;
		}
		return material == Material.water && BlockAquaticPlant.isTouchingOceanPlant(world, this.boundingBox);
	}
}
