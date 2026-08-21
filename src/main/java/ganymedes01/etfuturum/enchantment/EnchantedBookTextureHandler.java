package ganymedes01.etfuturum.enchantment;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import ganymedes01.etfuturum.core.utils.Logger;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.event.TextureStitchEvent;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Bundles the Bibliophilia enchanted-book sprites and applies one texture per
 * enchantment. The mapping between an enchantment key and its sprite is read
 * from <code>assets/etfuturum/bibliophilia/enchantments.txt</code> (each line:
 * <code>enchantKey&lt;TAB&gt;textureSubPath</code>, e.g.
 * <code>mending&lt;TAB&gt;vanilla/mending</code>).
 * <p>
 * The sprites are stored under <code>textures/items/book_vanilla/</code> and
 * <code>textures/items/book_mods/</code> (mod sprites keep their modid prefix so
 * the flattened name stays unique) and are registered against the item atlas
 * during the TextureStitchEvent.Pre phase. Other mods' book sprites are bundled
 * alongside ours (they remain in enchantments.txt even when the owning mod is
 * absent), so if a third-party mod later registers the matching enchantment, its
 * sprite is picked up automatically without any code change.
 */
public class EnchantedBookTextureHandler {

	public static final EnchantedBookTextureHandler INSTANCE = new EnchantedBookTextureHandler();

	private static final String MAPPING_RESOURCE = "/assets/etfuturum/bibliophilia/enchantments.txt";

	/** enchantment key -&gt; registered item atlas sprite. */
	private final Map<String, IIcon> icons = new HashMap<>();

	/**
	 * Maps 1.7.10 vanilla enchantment base names (from {@code getName()} minus
	 * the {@code "enchantment."} prefix) that differ from the modern snake_case
	 * key used in <code>enchantments.txt</code> to that modern key. Without this,
	 * e.g. {@code damageAll} would snake_case to {@code damage_all} and miss the
	 * {@code sharpness} entry, leaving the book on the default sprite.
	 */
	private static final Map<String, String> VANILLA_ALIASES = new HashMap<>();

	static {
		// Actual 1.7.10 enchantment base names (as Et Futurum Requiem exposes them
		// via Enchantment#getName(), minus the "enchantment." prefix) mapped to the
		// modern snake_case texture key used by the bundled Bibliophilia mapping.
		VANILLA_ALIASES.put("protect.all", "protection");
		VANILLA_ALIASES.put("protect.fire", "fire_protection");
		VANILLA_ALIASES.put("protect.fall", "feather_falling");
		VANILLA_ALIASES.put("protect.explosion", "blast_protection");
		VANILLA_ALIASES.put("protect.projectile", "projectile_protection");
		VANILLA_ALIASES.put("oxygen", "respiration");
		VANILLA_ALIASES.put("waterWorker", "aqua_affinity");
		VANILLA_ALIASES.put("thorns", "thorns");
		VANILLA_ALIASES.put("damage.all", "sharpness");
		VANILLA_ALIASES.put("damage.undead", "smite");
		VANILLA_ALIASES.put("damage.arthropods", "bane_of_arthropods");
		VANILLA_ALIASES.put("knockback", "knockback");
		VANILLA_ALIASES.put("fire", "fire_aspect");
		VANILLA_ALIASES.put("lootBonus", "looting");
		VANILLA_ALIASES.put("digging", "efficiency");
		VANILLA_ALIASES.put("untouching", "silk_touch");
		VANILLA_ALIASES.put("durability", "unbreaking");
		VANILLA_ALIASES.put("lootBonusDigger", "fortune");
		VANILLA_ALIASES.put("arrowDamage", "power");
		VANILLA_ALIASES.put("arrowKnockback", "punch");
		VANILLA_ALIASES.put("arrowFire", "flame");
		VANILLA_ALIASES.put("arrowInfinite", "infinity");
		VANILLA_ALIASES.put("lootBonusFishing", "luck_of_the_sea");
		VANILLA_ALIASES.put("fishingSpeed", "lure");
	}

	private EnchantedBookTextureHandler() {
	}

	@SubscribeEvent
	public void onTextureStitch(TextureStitchEvent.Pre event) {
		// Only the item spritesheet is relevant here (texture type 1 == items,
		// 0 == blocks). If icons were registered on the wrong atlas, the returned
		// sprite would be sampled from a different sheet and render as garbage.
		if (event.map.getTextureType() != 1) {
			return;
		}
		icons.clear();
		try (InputStream in = EnchantedBookTextureHandler.class.getResourceAsStream(MAPPING_RESOURCE);
			 BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
			String line;
			int registered = 0;
			while ((line = reader.readLine()) != null) {
				String trimmed = line.trim();
				// Skip blank lines, full-line comments and "[section]" banners so the
				// mapping file can carry documentation for maintainability.
				if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("[")) {
					continue;
				}
				String[] parts = split(trimmed);
				if (parts == null) {
					continue;
				}
				String key = parts[0];
			String subPath = parts[1];
			// Sprites are stored in two sub-directories under the item atlas:
			//   book_vanilla/*  ->  Bibliophilia vanilla set (e.g. "vanilla/mending")
			//   book_mods/*     ->  other mods, kept with their modid prefix so the
			//                       flattened name stays unique (e.g. "aileron/cloudskipper"
			//                       becomes "book_mods/aileron_cloudskipper").
			// Without the modid prefix, distinct enchantments that share a bare name
			// (e.g. power/rebound/frost) would collide and one sprite would replace
			// the other for third-party books.
			String iconName;
			if (subPath.startsWith("vanilla/")) {
				iconName = "etfuturum:book_vanilla/" + subPath.substring("vanilla/".length());
			} else {
				iconName = "etfuturum:book_mods/" + subPath.replace('/', '_');
			}
			IIcon icon = event.map.registerIcon(iconName);
				icons.put(key, icon);
				// Mod-owned keys carry a "modid:name" prefix. When such a mod registers an
				// enchantment, the enchantment only exposes its bare snake_case name, so also
				// index the sprite under the bare name (unless a vanilla/bare key already won).
				int colon = key.indexOf(':');
				if (colon > 0 && key.indexOf(':', colon + 1) < 0) {
					String bare = key.substring(colon + 1);
					if (!bare.isEmpty() && !icons.containsKey(bare)) {
						icons.put(bare, icon);
					}
				}
				registered++;
			}
			// Only surfaced in dev/snapshot builds; harmless at runtime.
			Logger.debug("Registered " + registered + " enchanted-book sprites on the item atlas");
		} catch (Exception ignored) {
			// Resource missing on server or during jar reload; simply keep the
			// default enchanted-book sprite in that case.
		}
	}

	/**
	 * Resolves the sprite for the first stored enchantment (matched by its
	 * unlocalised name mapped to snake_case, minus the "enchantment." prefix).
	 * Returns {@code null} to signal "keep the default sprite".
	 */
	public IIcon getIcon(ItemStack stack) {
		if (stack == null || icons.isEmpty()) {
			return null;
		}
		NBTTagCompound tag = stack.getTagCompound();
		if (tag == null || !tag.hasKey("StoredEnchantments", 9)) {
			return null;
		}
		NBTTagList list = tag.getTagList("StoredEnchantments", 10);
		for (int i = 0; i < list.tagCount(); i++) {
			NBTTagCompound enchantTag = list.getCompoundTagAt(i);
			short id = enchantTag.getShort("id");
			Enchantment ench = id >= 0 && id < Enchantment.enchantmentsList.length
					? Enchantment.enchantmentsList[id] : null;
			if (ench == null) {
				continue;
			}
			String key = lookupKey(ench);
			IIcon icon = key != null ? icons.get(key) : null;
			if (icon != null) {
				return icon;
			}
		}
		return null;
	}

	/**
	 * Deduces the best mapping key for an enchantment. The enchantment is
	 * resolved from the book's <code>StoredEnchantments</code> NBT, then matched
	 * in priority order:
	 * <ol>
	 *   <li>the snake_case of its 1.7.10 base name (e.g. {@code aquaAffinity}
	 *       -&gt; {@code aqua_affinity});</li>
	 *   <li>the {@link #VANILLA_ALIASES vanilla alias} when the 1.7.10 base name
	 *       differs from the modern key (e.g. {@code damageAll} -&gt;
	 *       {@code sharpness});</li>
	 *   <li>the bare final name node for {@code modid:name} mod entries
	 *       (e.g. {@code ars_nouveau.mana_boost} -&gt; {@code mana_boost}).</li>
	 * </ol>
	 */
	private String lookupKey(Enchantment ench) {
		String raw = ench.getName();
		if (raw == null) {
			return null;
		}
		String base = raw.startsWith("enchantment.") ? raw.substring("enchantment.".length()) : raw;
		// Exact vanilla alias first, mapping this mod's actual 1.7.10 base name to
		// the modern key (e.g. "digging" -> efficiency). Checked first so a bare-name
		// collision can never point a vanilla book at another mod's sprite.
		String alias = VANILLA_ALIASES.get(base);
		if (alias != null && icons.containsKey(alias)) {
			return alias;
		}
		String snake = toSnakeCase(base);
		if (icons.containsKey(snake)) {
			return snake;
		}
		int dot = base.lastIndexOf('.');
		String last = dot >= 0 && dot < base.length() - 1 ? base.substring(dot + 1) : null;
		if (last != null && icons.containsKey(last)) {
			return last;
		}
		return null;
	}

	private static String toSnakeCase(String input) {
		StringBuilder sb = new StringBuilder(input.length());
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			if (Character.isUpperCase(c)) {
				sb.append('_').append(Character.toLowerCase(c));
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	private static String[] split(String line) {
		int tab = line.indexOf('\t');
		if (tab > 0 && tab < line.length() - 1) {
			return new String[]{line.substring(0, tab), line.substring(tab + 1)};
		}
		String[] ws = line.split("\\s+");
		if (ws.length >= 2) {
			return new String[]{ws[0], ws[1]};
		}
		return null;
	}
}