package ganymedes01.etfuturum.client.handler;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.entity.Entity;

/**
 * Client-side record of each entity's last spear hit feedback, the 26.2
 * LivingEntity#kineticHitFeedbackTick equivalent: SpearAnimations samples the elapsed ticks for
 * the hitFeedbackAmount envelope (0.4 * (outQuart(1..3) - inOutSine(3..10))).
 */
public class SpearHitFeedbackTracker {

	public static final int HIT_FEEDBACK_TICKS = 10; // 26.2 KineticWeapon.HIT_FEEDBACK_TICKS

	private static final Map<Entity, Long> FEEDBACKS = new WeakHashMap<>();

	public static void recordHit(Entity entity, long gameTime) {
		FEEDBACKS.put(entity, Long.valueOf(gameTime));
	}

	/** Ticks since the entity's last recorded hit, clamped so the envelope stays valid. */
	public static int getTicksSinceLastHit(Entity entity, long gameTime) {
		Long recorded = FEEDBACKS.get(entity);
		if (recorded == null)
			return Integer.MAX_VALUE;
		long elapsed = gameTime - recorded.longValue();
		if (elapsed < 0L)
			return 0;
		return elapsed > HIT_FEEDBACK_TICKS ? HIT_FEEDBACK_TICKS : (int) elapsed;
	}
}
