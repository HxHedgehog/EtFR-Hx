package ganymedes01.etfuturum.swimming;

/**
 * A duck for {@link net.minecraft.entity.player.EntityPlayer} exposing the 26.2 swimming state.
 *
 * <p>Usage:</p>
 * <blockquote><tt>((ISwimmingPlayer) entityPlayer).etfu$isSwimming()</tt></blockquote>
 */
public interface ISwimmingPlayer {
	/**
	 * The synced swimming flag (dedicated watchable object byte, id
	 * {@link ganymedes01.etfuturum.swimming.SwimmingRuntime#getSwimmingSlot()}).
	 */
	boolean etfu$isSwimming();

	void etfu$setSwimming(boolean swimming);

	/**
	 * True when the player pose is {@code Pose.SWIMMING}, which includes crawling on land.
	 */
	boolean etfu$isVisuallySwimming();

	/**
	 * True when the player has the swimming pose but is not actually swimming (26.2 crawling).
	 */
	boolean etfu$isVisuallyCrawling();

	/**
	 * The tick value of the eye height above the feet for the current pose (1.62 standing, 1.27 crouching, 0.4 swimming).
	 */
	float etfu$getEyeHeightAboveFeet();

	/**
	 * Partial-tick interpolated eye height above the feet, used by the camera.
	 */
	float etfu$getCameraEyeHeight(float partialTicks);

	/**
	 * Partial-tick interpolated {@code swimAmount} (0.09 per tick animation value).
	 */
	float etfu$getSwimAmount(float partialTicks);

	/**
	 * Local-authoritative sprint state, captured from the client's own
	 * {@code setSprinting} calls. In single player the integrated server recomputes sprint
	 * one or two ticks behind the client and syncs data watcher byte 0 back, stomping the
	 * sprint bit (bit 3) that {@code isSprinting()} reads mid-transition, so the client's
	 * swimming decision must read this field instead (26.2 keeps the local sprint
	 * client-authoritative). The server ignores this field and keeps using the C0B-driven
	 * {@code isSprinting()}.
	 */
	boolean etfu$isLocalSprinting();

	void etfu$setLocalSprinting(boolean sprinting);
}
