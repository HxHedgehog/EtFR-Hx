package ganymedes01.etfuturum.mixins.early.moderntextures;

import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.HashMap;
import java.util.Map;

/**
 * Swaps a handful of vanilla 1.7.10 block textures for the high-version
 * (1.21+) ones when {@code betterVanillaTextures} is enabled.
 * <p>
 * All of these blocks register their icons through the base
 * {@link Block#registerBlockIcons} with the legacy texture names. The modern
 * sprites are bundled as {@code *_new.png} (the "_new" override convention
 * also used by the modern dyes and glazed terracotta) and this mixin remaps
 * the registered names:
 * <ul>
 * <li>the emerald block ({@code emerald_block} -&gt; {@code emerald_block_new})</li>
 * <li>the lily pad ({@code waterlily} -&gt; {@code waterlily_new}) — both the
 * world look and the item icon (the item inherits the block's icon), and the
 * vanilla biome tint still applies since both sprites are greyscale</li>
 * </ul>
 * <p>
 * The name-based match only rewrites these vanilla texture names; other blocks
 * sharing the same registration path (gold/iron/diamond/lapis ...) are
 * untouched.
 * <p>
 * Client-only; loaded only when {@code betterVanillaTextures} is enabled.
 */
@Mixin(Block.class)
public class MixinBlockModernIcons {

	/** 1.7.10 legacy texture name -&gt; modern (1.21+) texture name. */
	private static final Map<String, String> MODERN_NAMES = new HashMap<>();

	static {
		MODERN_NAMES.put("emerald_block", "emerald_block_new");
		MODERN_NAMES.put("waterlily", "waterlily_new");
	}

	@Redirect(method = "registerBlockIcons(Lnet/minecraft/client/renderer/texture/IIconRegister;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/IIconRegister;registerIcon(Ljava/lang/String;)Lnet/minecraft/util/IIcon;"))
	private IIcon etfuturum$modernBlockIcon(IIconRegister register, String name) {
		// === 绿宝石块/荷叶等方块：config 开启时注册为 26.2 现代（_new）贴图，其余方块原样放行 ===
		if (ConfigMixins.betterVanillaTextures) {
			String modern = MODERN_NAMES.get(name);
			if (modern != null) {
				return register.registerIcon(modern);
			}
		}
		return register.registerIcon(name);
	}
}
