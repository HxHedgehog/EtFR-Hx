package ganymedes01.etfuturum.elytra;

/**
 * Client-side duck for player entities holding the elytra wing animation state,
 * mirroring 26.2's ElytraAnimationState: the wing angles are smoothed once per
 * game tick with a factor of 0.3 and interpolated with partialTicks at render time.
 */
public interface IClientElytraPlayer {
	/**
	 * Advances the wing animation one (or more, if ticks were skipped) game tick
	 * towards the given target angles. Called every render frame with the entity's
	 * current ticksExisted as the tick marker.
	 */
	void etfu$elytraAnimTick(float targetX, float targetY, float targetZ, int tick);

	float etfu$getElytraRotX(float partialTicks);

	float etfu$getElytraRotY(float partialTicks);

	float etfu$getElytraRotZ(float partialTicks);
}
