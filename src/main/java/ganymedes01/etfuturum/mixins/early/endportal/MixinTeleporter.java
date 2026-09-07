package ganymedes01.etfuturum.mixins.early.endportal;

import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.core.utils.helpers.WorldHelper;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The vanilla End branch of placeInPortal silently overwrites the 5x5x4 region around the arrival point.
 * Modern versions instead skip blocks that already match and break mismatched ones with drops,
 * which is what lets the modern sand duplicator collect its End-side sand when the platform refreshes.
 * The positioning (entity at floor(posY) - 1, motion zeroed) is kept identical to vanilla.
 */
@Mixin(Teleporter.class)
public class MixinTeleporter {

	@Shadow
	@Final
	private WorldServer worldServerInstance;

	@Inject(method = "placeInPortal", at = @At("HEAD"), cancellable = true)
	public void createEndPlatform(Entity entityIn, double oldX, double oldY, double oldZ, float oldYaw, CallbackInfo ci) {
		if (!ConfigMixins.endPortalSandDuping || worldServerInstance.provider.dimensionId != 1) {
			return;
		}

		int i = MathHelper.floor_double(entityIn.posX);
		int j = MathHelper.floor_double(entityIn.posY) - 1;
		int k = MathHelper.floor_double(entityIn.posZ);

		WorldHelper.createEndPlatform(worldServerInstance, i, j, k, true);

		entityIn.setLocationAndAngles(i, j, k, entityIn.rotationYaw, 0.0F);
		entityIn.motionX = entityIn.motionY = entityIn.motionZ = 0.0D;
		ci.cancel();
	}
}
