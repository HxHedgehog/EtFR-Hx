package ganymedes01.etfuturum.compat;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.core.utils.Logger;
import net.minecraft.block.Block;

import java.lang.reflect.Method;

/**
 * Bridges mod blocks into Angelica's shader tag registry so shader packs can
 * reference them with a single {@code tag:ocean_plants} line instead of a list
 * of registry names. Invoked via reflection with zero compile-time coupling —
 * upstream Angelica (2.1.49) has no ShaderBlockTags, only our Angelica-Hx fork
 * does. Everything is wrapped so a missing/old Angelica never breaks startup.
 */
public final class AngelicaCompat {

	/** Shared cross-mod tag for pseudo-waterlogged underwater plant blocks. */
	public static final String TAG_OCEAN_PLANTS = "ocean_plants";

	private AngelicaCompat() {
	}

	public static void registerShaderTags() {
		try {
			final Class<?> tags = Class.forName("com.gtnewhorizons.angelica.api.ShaderBlockTags");
			final Method register = tags.getMethod("register", Block.class, String.class);
			registerEntry(register, ModBlocks.SEAGRASS, TAG_OCEAN_PLANTS);
			registerEntry(register, ModBlocks.KELP, TAG_OCEAN_PLANTS);
			registerEntry(register, ModBlocks.KELP_PLANT, TAG_OCEAN_PLANTS);
			registerEntry(register, ModBlocks.SEA_PICKLE, TAG_OCEAN_PLANTS);
			registerEntry(register, ModBlocks.DEAD_SEA_PICKLE, TAG_OCEAN_PLANTS);
		} catch (Throwable t) {
			Logger.error("Angelica shader tag registration skipped:", t);
		}
	}

	private static void registerEntry(Method register, ModBlocks entry, String tag) {
		if (entry.isEnabled()) {
			try {
				register.invoke(null, entry.get(), tag);
			} catch (Throwable t) {
				Logger.error("Failed to register shader tag for " + tag + ":", t);
			}
		}
	}
}
