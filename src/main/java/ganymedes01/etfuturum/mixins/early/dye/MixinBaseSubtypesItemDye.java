package ganymedes01.etfuturum.mixins.early.dye;

import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import ganymedes01.etfuturum.items.BaseSubtypesItem;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.HashMap;
import java.util.Map;

/**
 * Makes the mod's {@code dye_same} item (the modern white/blue/brown/black
 * dyes) use the bundled high-version (1.21+) sprites when
 * {@code betterDyeTextures} is enabled.
 * <p>
 * The item normally registers {@code white_dye}, {@code blue_dye},
 * {@code brown_dye} and {@code black_dye}, which resolve to the upstream 1.14-era
 * sprites bundled as {@code assets/minecraft/textures/items/<colour>_dye.png}
 * (the "_new" suffix convention, as used for glazed terracotta). When the config
 * is on, the registered name is swapped to {@code <colour>_dye_new}, which resolves
 * to the modern 1.21.4 sprites, so turning the config off simply falls back to the
 * original icons.
 * <p>
 * Name-based mapping is safe: no other {@link BaseSubtypesItem} uses these names
 * (e.g. raw ores register {@code raw_copper} etc.). Client-only; loaded only when
 * {@code betterDyeTextures} is enabled.
 */
@Mixin(BaseSubtypesItem.class)
public class MixinBaseSubtypesItemDye {

	/** dye_same subtype name → bundled modern (1.21+) sprite name. */
	private static final Map<String, String> MODERN_NAMES = new HashMap<>();

	static {
		// === dye_same 子类型名 → 现代 1.21+ 贴图（_new 后缀，同釉陶惯例） ===
		MODERN_NAMES.put("white_dye", "white_dye_new");
		MODERN_NAMES.put("blue_dye", "blue_dye_new");
		MODERN_NAMES.put("brown_dye", "brown_dye_new");
		MODERN_NAMES.put("black_dye", "black_dye_new");
	}

	@Redirect(method = "registerIcons(Lnet/minecraft/client/renderer/texture/IIconRegister;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/IIconRegister;registerIcon(Ljava/lang/String;)Lnet/minecraft/util/IIcon;"))
	private IIcon etfuturum$modernDyeIcon(IIconRegister register, String name) {
		if (ConfigMixins.betterDyeTextures) {
			String modern = MODERN_NAMES.get(name);
			if (modern != null) {
				return register.registerIcon(modern);
			}
		}
		return register.registerIcon(name);
	}
}
