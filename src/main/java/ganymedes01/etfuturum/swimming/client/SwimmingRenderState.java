package ganymedes01.etfuturum.swimming.client;

/**
 * Tick state shared between the render mixins. It deliberately lives outside the
 * {@code ganymedes01.etfuturum.mixins} package: classes inside a mixin package cannot be
 * referenced directly at runtime (IllegalClassLoadError), and the injected render code
 * needs to read {@code partialTicks} from {@code ModelBiped}.
 */
public final class SwimmingRenderState {
	public static float partialTicks;

	private SwimmingRenderState() {
	}
}
