package ganymedes01.etfuturum.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import ganymedes01.etfuturum.core.utils.Logger;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.event.TextureStitchEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Bundles the high-version (1.20 "Spawn Egg Faces") per-entity spawn-egg
 * textures and makes {@link net.minecraft.item.ItemMonsterPlacer} render them as
 * a single full-colour egg instead of the vanilla two-pass (base + tinted
 * overlay) representation.
 * <p>
 * Each sprite is a complete, already-coloured egg so it replaces both vanilla
 * passes: pass 0 uses it with a white tint and pass 1 (the tinted overlay) is
 * skipped entirely by making {@code getRenderPasses} return 1.
 * <p>
 * The 1.7.10 egg metadata is mapped to the modern entity key the textures were
 * authored for (e.g. 57 -&gt; zombified_piglin). Only these mapped metas are
 * intercepted by the mixin; spawn eggs not in the mapping (including those of
 * other mods) keep their original vanilla rendering.
 */
public class SpawnEggFaceTextureHandler {

	public static final SpawnEggFaceTextureHandler INSTANCE = new SpawnEggFaceTextureHandler();

	/** 1.7.10 egg metadata -&gt; modern entity key (I. the sprite sub-path). */
	private static final Map<Integer, String> EGG_META = new HashMap<>();

	static {
		// === 原版 1.7.10 实体 ===
		EGG_META.put(50, "creeper");
		EGG_META.put(51, "skeleton");
		EGG_META.put(52, "spider");
		EGG_META.put(54, "zombie");
		EGG_META.put(55, "slime");
		EGG_META.put(56, "ghast");
		EGG_META.put(57, "zombified_piglin"); // 1.7.10: PigZombie
		EGG_META.put(58, "enderman");
		EGG_META.put(59, "cave_spider");
		EGG_META.put(60, "silverfish");
		EGG_META.put(61, "blaze");
		EGG_META.put(62, "magma_cube");       // 1.7.10: LavaSlime
		EGG_META.put(65, "bat");
		EGG_META.put(66, "witch");
		EGG_META.put(90, "pig");
		EGG_META.put(91, "sheep");
		EGG_META.put(92, "cow");
		EGG_META.put(93, "chicken");
		EGG_META.put(94, "squid");
		EGG_META.put(95, "wolf");
		EGG_META.put(96, "mooshroom");
		EGG_META.put(97, "snow_golem");
		EGG_META.put(98, "ocelot");
		EGG_META.put(99, "iron_golem");
		EGG_META.put(100, "horse");
		EGG_META.put(120, "villager");
		// === Mod 新增实体（egg ID 从 500 开始）===
		EGG_META.put(500, "rabbit");
		EGG_META.put(501, "endermite");
		EGG_META.put(502, "husk");
		EGG_META.put(503, "stray");
		EGG_META.put(504, "zombie_villager");
		EGG_META.put(505, "shulker");
		EGG_META.put(506, "bee");
		EGG_META.put(507, "fox");
	}

	/** egg metadata -&gt; registered item atlas sprite. */
	private final Map<Integer, IIcon> icons = new HashMap<>();

	private SpawnEggFaceTextureHandler() {
	}

	@SubscribeEvent
	public void onTextureStitch(TextureStitchEvent.Pre event) {
		// Only the item spritesheet is relevant (1 == items, 0 == blocks).
		if (event.map.getTextureType() != 1) {
			return;
		}
		icons.clear();
		int registered = 0;
		for (Map.Entry<Integer, String> entry : EGG_META.entrySet()) {
			try {
				IIcon icon = event.map.registerIcon("etfuturum:spawn_egg_new/" + entry.getValue());
				icons.put(entry.getKey(), icon);
				registered++;
			} catch (Exception ignored) {
				// A missing sprite simply keeps the vanilla look for that egg.
			}
		}
		Logger.debug("Registered " + registered + " spawn-egg face sprites on the item atlas");
	}

	/** Whether the given egg metadata has a bundled face sprite. */
	public boolean hasIcon(int meta) {
		return meta >= 0 && icons.containsKey(meta);
	}

	/** Returns the full-colour face sprite for {@code meta}, or null when unmapped. */
	public IIcon getIcon(int meta) {
		return icons.get(meta);
	}
}