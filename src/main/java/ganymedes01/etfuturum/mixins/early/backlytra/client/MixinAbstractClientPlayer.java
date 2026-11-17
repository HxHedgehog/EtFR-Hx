package ganymedes01.etfuturum.mixins.early.backlytra.client;

import ganymedes01.etfuturum.elytra.IClientElytraPlayer;
import net.minecraft.client.entity.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AbstractClientPlayer.class)
public class MixinAbstractClientPlayer implements IClientElytraPlayer {
	@Unique
	private static final float etfuturum$SMOOTHING = 0.3F; // 26.2 ElytraAnimationState smoothing factor

	@Unique
	private float etfuturum$rotX, etfuturum$rotY, etfuturum$rotZ;

	@Unique
	private float etfuturum$rotXOld, etfuturum$rotYOld, etfuturum$rotZOld;

	@Unique
	private int etfuturum$lastAnimTick = Integer.MIN_VALUE;

	@Override
	public void etfu$elytraAnimTick(float targetX, float targetY, float targetZ, int tick) {
		if (tick == etfuturum$lastAnimTick) {
			return; // already advanced this game tick
		}
		boolean firstTick = etfuturum$lastAnimTick == Integer.MIN_VALUE;
		etfuturum$rotXOld = etfuturum$rotX;
		etfuturum$rotYOld = etfuturum$rotY;
		etfuturum$rotZOld = etfuturum$rotZ;
		if (firstTick || tick - etfuturum$lastAnimTick == 1) {
			/* one game tick elapsed: single 0.3 smoothing step, like 26.2 */
			etfuturum$rotX += (targetX - etfuturum$rotX) * etfuturum$SMOOTHING;
			etfuturum$rotY += (targetY - etfuturum$rotY) * etfuturum$SMOOTHING;
			etfuturum$rotZ += (targetZ - etfuturum$rotZ) * etfuturum$SMOOTHING;
		} else {
			/* multiple ticks skipped (entity was not rendered): settle directly on the target */
			etfuturum$rotX = targetX;
			etfuturum$rotY = targetY;
			etfuturum$rotZ = targetZ;
		}
		etfuturum$lastAnimTick = tick;
	}

	@Override
	public float etfu$getElytraRotX(float partialTicks) {
		return etfuturum$rotXOld + (etfuturum$rotX - etfuturum$rotXOld) * partialTicks;
	}

	@Override
	public float etfu$getElytraRotY(float partialTicks) {
		return etfuturum$rotYOld + (etfuturum$rotY - etfuturum$rotYOld) * partialTicks;
	}

	@Override
	public float etfu$getElytraRotZ(float partialTicks) {
		return etfuturum$rotZOld + (etfuturum$rotZ - etfuturum$rotZOld) * partialTicks;
	}
}
