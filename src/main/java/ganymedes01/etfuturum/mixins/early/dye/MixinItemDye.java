package ganymedes01.etfuturum.mixins.early.dye;

import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.ItemDye;
import net.minecraft.util.IIcon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.HashMap;
import java.util.Map;

/**
 * Makes the vanilla 1.7.10 dye ({@code minecraft:dye}, item ID 351) render the
 * bundled high-version per-colour dye sprites for its "pure" dye metadata values.
 * <p>
 * Vanilla registers one icon per metadata as {@code dye_powder_<name>}. Following
 * the same override mechanism the mod uses for its own {@code dye_same} item, the
 * modern sprites are bundled in the minecraft asset domain
 * ({@code assets/minecraft/textures/items/<colour>_dye.png}) and this mixin simply
 * swaps the registered icon name for the 12 pure dye colours during
 * {@code registerIcons}, so the icon flows through vanilla's own
 * {@code getIconFromDamage} without any extra event handling.
 * <p>
 * The material metadata slots that 1.7.10 reuses from the same item - ink sac
 * ({@code dye_powder_black}), cocoa beans ({@code dye_powder_brown}), lapis lazuli
 * ({@code dye_powder_blue}) and bone meal ({@code dye_powder_white}) - are not
 * mapped, so they keep their original vanilla icons. The modern
 * white/blue/brown/black dyes are instead provided by the separate mod item
 * {@code dye_same}, which already bundles its own sprites.
 * <p>
 * Client-only; loaded only when {@code betterDyeTextures} is enabled.
 */
@Mixin(ItemDye.class)
public class MixinItemDye {

	/**
	 * Vanilla 1.7.10 dye icon names ({@code dye_powder_<name>}) mapped to the
	 * bundled modern (1.21+) per-colour dye sprites. Note 1.7.10 uses "silver"
	 * and "lightBlue" where modern versions say light_gray / light_blue.
	 */
	private static final Map<String, String> VANILLA_TO_MODERN = new HashMap<>();

	static {
		// === 1.7.10 纯色染料贴图名 → 现代 1.21+ 染料贴图（不含墨囊/可可豆/青金石/骨粉） ===
		VANILLA_TO_MODERN.put("dye_powder_red", "red_dye");            // 玫瑰红
		VANILLA_TO_MODERN.put("dye_powder_green", "green_dye");        // 仙人掌绿
		VANILLA_TO_MODERN.put("dye_powder_purple", "purple_dye");
		VANILLA_TO_MODERN.put("dye_powder_cyan", "cyan_dye");
		VANILLA_TO_MODERN.put("dye_powder_silver", "light_gray_dye");  // 1.7.10: silver
		VANILLA_TO_MODERN.put("dye_powder_gray", "gray_dye");
		VANILLA_TO_MODERN.put("dye_powder_pink", "pink_dye");
		VANILLA_TO_MODERN.put("dye_powder_lime", "lime_dye");
		VANILLA_TO_MODERN.put("dye_powder_yellow", "yellow_dye");      // 蒲公英黄
		// 注：1.7.10 jar 中的实际贴图名为 dye_powder_light_blue.png（全小写带下划线），
		// 部分反编译源码显示为 "lightBlue"，两个键都做映射以保万一。
		VANILLA_TO_MODERN.put("dye_powder_light_blue", "light_blue_dye");
		VANILLA_TO_MODERN.put("dye_powder_lightBlue", "light_blue_dye");
		VANILLA_TO_MODERN.put("dye_powder_magenta", "magenta_dye");
		VANILLA_TO_MODERN.put("dye_powder_orange", "orange_dye");
	}

	@Redirect(method = "registerIcons(Lnet/minecraft/client/renderer/texture/IIconRegister;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/IIconRegister;registerIcon(Ljava/lang/String;)Lnet/minecraft/util/IIcon;"))
	private IIcon etfuturum$modernDyeIcon(IIconRegister register, String name) {
		if (ConfigMixins.betterDyeTextures) {
			String modern = VANILLA_TO_MODERN.get(name);
			if (modern != null) {
				return register.registerIcon(modern);
			}
		}
		return register.registerIcon(name);
	}
}
