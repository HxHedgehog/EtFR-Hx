package ganymedes01.etfuturum.mixins.early.moderntextures;

import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.util.IIcon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.HashMap;
import java.util.Map;

/**
 * Swaps a handful of vanilla 1.7.10 item icons for the high-version (1.21+)
 * ones when {@code betterVanillaTextures} is enabled.
 * <p>
 * All of these items register their icon through the base
 * {@link Item#registerIcons} with the legacy texture names. The modern sprites
 * are bundled under their modern names (same convention as the mod's own
 * {@code dye_same} items; the cauldron uses a {@code _new} suffix because the
 * legacy and modern names are identical) and this mixin remaps the registered
 * names:
 * <ul>
 * <li>the 6 minecart items ({@code minecart_normal} -&gt; {@code minecart},
 * {@code minecart_chest} -&gt; {@code chest_minecart}, ...) — the minecart
 * entities' model texture is intentionally left vanilla</li>
 * <li>the fire charge ({@code fireball} -&gt; {@code fire_charge})</li>
 * <li>the cauldron item ({@code cauldron} -&gt; {@code cauldron_new})</li>
 * </ul>
 * <p>
 * The name-based match only rewrites these 8 vanilla texture names; any other
 * item sharing this registration path is untouched.
 * <p>
 * Client-only; loaded only when {@code betterVanillaTextures} is enabled.
 */
@Mixin(Item.class)
public class MixinItemModernIcons {

	/** 1.7.10 legacy icon name -&gt; modern (1.21+) icon name. */
	private static final Map<String, String> MODERN_NAMES = new HashMap<>();

	static {
		MODERN_NAMES.put("minecart_normal", "minecart");
		MODERN_NAMES.put("minecart_chest", "chest_minecart");
		MODERN_NAMES.put("minecart_furnace", "furnace_minecart");
		MODERN_NAMES.put("minecart_tnt", "tnt_minecart");
		MODERN_NAMES.put("minecart_hopper", "hopper_minecart");
		MODERN_NAMES.put("minecart_command_block", "command_block_minecart");
		MODERN_NAMES.put("fireball", "fire_charge");
		MODERN_NAMES.put("cauldron", "cauldron_new");
	}

	@Redirect(method = "registerIcons(Lnet/minecraft/client/renderer/texture/IIconRegister;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/IIconRegister;registerIcon(Ljava/lang/String;)Lnet/minecraft/util/IIcon;"))
	private IIcon etfuturum$modernItemIcon(IIconRegister register, String name) {
		// === 矿车/火焰弹/炼药锅等：config 开启时注册为 26.2 现代贴图名，其余物品原样放行 ===
		if (ConfigMixins.betterVanillaTextures) {
			String modern = MODERN_NAMES.get(name);
			if (modern != null) {
				return register.registerIcon(modern);
			}
		}
		return register.registerIcon(name);
	}
}
