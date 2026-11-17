package ganymedes01.etfuturum.mixins.early.waterfog.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import ganymedes01.etfuturum.client.waterfog.ModernWaterFog;
import ganymedes01.etfuturum.configuration.configs.ConfigWorld;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

/**
 * Makes vanilla's own fluid-state judgment camera-based (26.2 {@code Camera#getFluidInCamera}
 * semantics). Three vanilla call sites sample their block via
 * {@code ActiveRenderInfo#getBlockAtEntityViewpoint}, which uses the entity-eye viewpoint:
 * {@code setupFog} (fog type), {@code updateFogColor} (fog color) and {@code getFOVModifier}
 * (the underwater FOV narrowing, L496-501: f * 60/70). When the entity's eye is underwater but
 * the third person camera is above the surface, vanilla still triggers the water branches — the
 * GL_EXP fog paints the entire sky ({@code renderWorld} L1241-1243) and the FOV zooms in
 * prematurely ("flash" on entering water in third person). 26.2 judges both at the camera
 * (FOV: {@code Camera#modifyFovBasedOnDeathOrFluid} L251-254, ratio 0.85714287 = 60/70), so all
 * three call sites become camera-based here, without touching any bytecode Angelica injects.
 */
@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {

	// net.minecraft.client.renderer.EntityRenderer
	@Redirect(method = {"setupFog", "updateFogColor", "getFOVModifier"},
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/ActiveRenderInfo;getBlockAtEntityViewpoint(Lnet/minecraft/world/World;Lnet/minecraft/entity/EntityLivingBase;F)Lnet/minecraft/block/Block;"),
			require = 3)
	private static Block etfu$cameraBasedViewpoint(World world, EntityLivingBase entity, float partialTicks) {
		if (!ConfigWorld.modernWaterFog) {
			return ActiveRenderInfo.getBlockAtEntityViewpoint(world, entity, partialTicks);
		}
		return ModernWaterFog.getBlockAtCamera(world, entity, partialTicks);
	}
}
