package ganymedes01.etfuturum.mixins.early.emeraldblock;

import ganymedes01.etfuturum.configuration.configs.ConfigMixins;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Makes the vanilla 1.7.10 emerald block render the bundled high-version
 * (1.21+) texture when {@code betterEmeraldBlockTexture} is enabled.
 * <p>
 * The emerald block is a plain {@code BlockCompressed} that registers its single
 * icon via the base {@link Block#registerBlockIcons} using its texture name
 * {@code emerald_block}. Following the same "_new" override convention as the
 * modern dyes and glazed terracotta, the modern sprite is bundled as
 * {@code assets/minecraft/textures/blocks/emerald_block_new.png} and this mixin
 * swaps the registered name when the config is on; turning it off falls back to
 * the vanilla look.
 * <p>
 * The name-based match only rewrites {@code emerald_block}: other blocks sharing
 * the same base registration path (gold/iron/diamond/lapis) are untouched.
 * <p>
 * Client-only; loaded only when {@code betterEmeraldBlockTexture} is enabled.
 */
@Mixin(Block.class)
public class MixinBlockEmerald {

	private static final String MODERN_NAME = "emerald_block_new";

	@Redirect(method = "registerBlockIcons(Lnet/minecraft/client/renderer/texture/IIconRegister;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/IIconRegister;registerIcon(Ljava/lang/String;)Lnet/minecraft/util/IIcon;"))
	private IIcon etfuturum$modernEmeraldBlockIcon(IIconRegister register, String name) {
		// === 绿宝石块：config 开启时注册为 26.2 现代（_new）贴图，其余方块原样放行 ===
		if (ConfigMixins.betterEmeraldBlockTexture && "emerald_block".equals(name)) {
			return register.registerIcon(MODERN_NAME);
		}
		return register.registerIcon(name);
	}
}
