package ganymedes01.etfuturum.mixins.early.oceanplants;

import ganymedes01.etfuturum.blocks.BlockAquaticPlant;
import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
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
	 * The waterlogged volumes occupy their whole cell - pseudo-waterlogged
	 * ocean plants are Material.water blocks, real waterlogged blocks answer
	 * inside {@code World#handleMaterialAcceleration} via waterlog.MixinWorld -
	 * so they both already answer vanilla water detection wherever the
	 * detection band reaches. That band (Entity#handleWaterMovement) shrinks
	 * the bounding box by 0.4 on both vertical ends, so gliders with the feet
	 * in the lower part of a cell - or skimming just above short tops - miss
	 * every water cell and keep moving at air speed. 26.2 keeps the fluid
	 * state in the full bounding box: OR the vanilla verdict with a full-box
	 * scan for waterlogged volumes (see
	 * {@link BlockAquaticPlant#isTouchingWaterVolume}). Real water keeps the
	 * vanilla band behaviour.
	 *
	 * <p>The swimming port's water probe is merged into this redirect because a
	 * single call site only supports one {@code @Redirect}. 26.2 Entity#isInWater
	 * counts the whole slightly-deflated bounding box, while the 1.7.10 band probe
	 * trims 0.4 off BOTH vertical ends and INVERTS the 0.6-high swimming/crawling
	 * box into an empty rectangle: the cached inWater flag would read false on
	 * every swimming tick, making the player flip-flop between poses and between
	 * water/air physics (camera jitter, splash bubbles on every re-entry) and
	 * stripping the water drag and gravity from moveEntityWithHeading. Only the
	 * player is widened and only while swimming is enabled; vanilla entities keep
	 * their exact 1.7.10 band behaviour. The fluid push side effect is preserved
	 * by delegating with the corrected box.</p>
	 */
	@Redirect(method = "handleWaterMovement",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/World;handleMaterialAcceleration(Lnet/minecraft/util/AxisAlignedBB;Lnet/minecraft/block/material/Material;Lnet/minecraft/entity/Entity;)Z"),
			require = 1)
	private boolean etfu$oceanPlantWaterDetection(World world, AxisAlignedBB bb, Material material, Entity entity) {
		if (entity instanceof EntityPlayer && ConfigMixins.enableSwimming) {
			bb = entity.boundingBox.contract(0.001D, 0.001D, 0.001D);
		}
		if (world.handleMaterialAcceleration(bb, material, entity)) {
			return true;
		}
		return material == Material.water && BlockAquaticPlant.isTouchingWaterVolume(world, this.boundingBox);
	}
}
