package ganymedes01.etfuturum.elytra;

public interface IElytraPlayer extends ganymedes01.etfuturum.api.elytra.IElytraPlayer {
	void etfu$setElytraFlying(boolean flag);

	float etfu$getTicksElytraFlying();

	boolean etfu$lastElytraFlying();

	void etfu$setLastElytraFlying(boolean flag);

	void tickElytra();

	/* Previous tick's motion, captured in tickElytra; used by the client renderer
	 * to interpolate the glide Y-rot like 26.2's deltaMovementOnPreviousTick */
	void etfu$setPrevMotion(double x, double y, double z);

	double etfu$getPrevMotionX();

	double etfu$getPrevMotionY();

	double etfu$getPrevMotionZ();
}
