package ganymedes01.etfuturum.configuration.configs;

import ganymedes01.etfuturum.configuration.ConfigBase;

import java.io.File;

public class ConfigEnchantsPotions extends ConfigBase {

	public static boolean enableFrostWalker;
	public static boolean enableMending;
	public static boolean enableSwiftSneak;
	public static boolean elytraOceanEnchant;
	public static int mendingID;
	public static int frostWalkerID;
	public static int swiftSneakID;
	public static int waterWalkerID;
	public static int levitationID;

	static final String catEnchants = "enchantments";
	static final String catPotions = "potions";


	public ConfigEnchantsPotions(File file) {
		super(file);
		setCategoryComment(catEnchants, "Settings for enchantments.\nBy default, only IDs up to 255 are allowed, only assign a larger value if an ID extension is present.");
		setCategoryComment(catPotions, "Settings for potions.\nBy default, only IDs up to 31 are allowed, only assign a larger value if an ID extension is present.");

		configCats.add(getCategory(catEnchants));
		configCats.add(getCategory(catPotions));
	}

	@Override
	protected void syncConfigOptions() {
		//enchants
		enableFrostWalker = getBoolean("frostWalker", catEnchants, true, "");
		frostWalkerID = getInt("frostWalkerID", catEnchants, 200, 0, Short.MAX_VALUE, "");
		enableMending = getBoolean("mending", catEnchants, true, "");
		mendingID = getInt("mendingID", catEnchants, 201, 0, Short.MAX_VALUE, "");
		enableSwiftSneak = getBoolean("swiftSneak", catEnchants, true, "");
		swiftSneakID = getInt("swiftSneakID", catEnchants, 202, 0, Short.MAX_VALUE, "");
		elytraOceanEnchant = getBoolean("elytraOceanEnchant", catEnchants, true, "When enabled, an elytra without Depth Strider stops gliding in water (pre-26.2 behavior) while an elytra enchanted with Depth Strider keeps gliding underwater: the glide flag persists, the 26.2 water travel physics apply with the elytra's Depth Strider easing the drag, and the boosting firework rocket scales its thrust to 35/55/75% per level. When disabled, the elytra fully matches 26.2: water never stops the glide (the glide physics hand over to water travel and resume once the entity leaves the water) and Depth Strider works from boots only. The Depth Strider enchantment itself is always registered and always works on boots.");
		waterWalkerID = getInt("waterWalkerID", catEnchants, 203, 0, Short.MAX_VALUE, "");

		//potions
		levitationID = getInt("levitationID", catPotions, 27, 0, Short.MAX_VALUE, "Since this is essential for Shulkers, this is tied to Shulkers being enabled instead of having its own option.");
	}

}
