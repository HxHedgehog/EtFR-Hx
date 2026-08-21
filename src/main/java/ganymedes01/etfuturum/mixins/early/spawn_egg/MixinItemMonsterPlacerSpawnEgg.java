package ganymedes01.etfuturum.mixins.early.spawn_egg;

import ganymedes01.etfuturum.client.SpawnEggFaceTextureHandler;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes {@link ItemMonsterPlacer} render the bundled high-version single
 * full-colour egg texture instead of the vanilla two-pass (base + tinted
 * overlay) representation, for the egg metadata that has a bundled face sprite
 * (see {@link SpawnEggFaceTextureHandler}).
 * <p>
 * The render paths in {@code RenderItem} (GUI/dropped items) and
 * {@code ItemRenderer} (held item) all branch on
 * {@code requiresMultipleRenderPasses()} / {@code getRenderPasses(meta)} and
 * fetch the icon per pass through {@code getIcon(stack, pass)} -&gt;
 * {@code getIconFromDamageForRenderPass(meta, pass)} with a per-pass tint from
 * {@code getColorFromItemStack}. By (a) collapsing the passes to 1 for mapped
 * eggs, (b) supplying the full-colour sprite as pass 0 and (c) forcing a white
 * tint, the complete egg renders once and the vanilla overlay tint never
 * applies.
 * <p>
 * {@code getIconFromDamageForRenderPass} and {@code getColorFromItemStack} are
 * overridden by {@link ItemMonsterPlacer} itself, while
 * {@code getRenderPasses} is only inherited from {@link net.minecraft.item.Item}.
 * That inherited method is a Forge-added API with no {@code @SideOnly}
 * annotation and no MCP-to-SRG mapping, so it must be injected with
 * {@code remap = false} (the mixin will generate an override on
 * {@link ItemMonsterPlacer}). Spawn eggs whose metadata is not mapped -
 * including eggs added by other mods - are left completely untouched and keep
 * their original vanilla appearance.
 * <p>
 * Client-only; loaded only when {@code betterSpawnEggTextures} is enabled.
 */
@Mixin(ItemMonsterPlacer.class)
public class MixinItemMonsterPlacerSpawnEgg {

	@Inject(method = "getRenderPasses(I)I", at = @At("HEAD"), cancellable = true, remap = false)
	private void etfuturum$collapsePasses(int metadata, CallbackInfoReturnable<Integer> cir) {
		if (SpawnEggFaceTextureHandler.INSTANCE.hasIcon(metadata)) {
			// Render the full-colour egg in a single pass; skip the overlay pass.
			cir.setReturnValue(1);
		}
	}

	@Inject(method = "getIconFromDamageForRenderPass(II)Lnet/minecraft/util/IIcon;", at = @At("HEAD"), cancellable = true)
	private void etfuturum$faceSprite(int meta, int pass, CallbackInfoReturnable<IIcon> cir) {
		IIcon icon = SpawnEggFaceTextureHandler.INSTANCE.getIcon(meta);
		if (icon != null) {
			cir.setReturnValue(icon);
		}
	}

	@Inject(method = "getColorFromItemStack(Lnet/minecraft/item/ItemStack;I)I", at = @At("HEAD"), cancellable = true)
	private void etfuturum$whiteTint(ItemStack stack, int pass, CallbackInfoReturnable<Integer> cir) {
		if (stack != null && SpawnEggFaceTextureHandler.INSTANCE.hasIcon(stack.getItemDamage())) {
			// The sprite is already fully coloured; never re-tint it.
			cir.setReturnValue(16777215);
		}
	}
}