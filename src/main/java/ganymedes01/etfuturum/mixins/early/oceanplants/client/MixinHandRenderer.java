package ganymedes01.etfuturum.mixins.early.oceanplants.client;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Angelica's shader hand pipeline classifies held items by their block's
 * vanilla render pass: pass-1 blocks are routed to the translucent hand pass
 * (enableBlend + depthMask(false)), which shatters the flat 2D icon cards of
 * renderItemIn2D into their pixel-slice comb mesh. Anything vanilla
 * ItemRenderer.renderItem would draw through the flat card path is opaque
 * cutout geometry, so exclude it from the translucency check:
 * <ul>
 * <li>flat sprites from the items atlas (ocean plants), and</li>
 * <li>block items whose render type is not 3D-renderable — e.g. the
 * backported stained glass panes (renderType 41), whose icons come from the
 * blocks atlas and are therefore missed by the sprite-number check.</li>
 * </ul>
 * True 3D translucent blocks (stained glass, ice) keep the vanilla blend
 * behavior.
 * <p>
 * Targets Angelica's {@code net.coderbot.iris.pipeline.HandRenderer} by name;
 * registered only when Angelica is present. No compile-time dependency.
 */
@Mixin(targets = "net.coderbot.iris.pipeline.HandRenderer", remap = false)
public abstract class MixinHandRenderer {

	@Inject(method = "isItemTranslucent", at = @At("HEAD"), cancellable = true, remap = false)
	private void etfuturum$flatIconBlocksAreNotTranslucent(ItemStack heldItem, CallbackInfoReturnable<Boolean> cir) {
		if (heldItem == null) {
			return;
		}
		// Flat sprites from the items atlas.
		if (heldItem.getItemSpriteNumber() != 0) {
			cir.setReturnValue(Boolean.FALSE);
			return;
		}
		// Block items drawn as flat cards instead of 3D block models; mirror
		// vanilla ItemRenderer.renderItem's branch (blocks-atlas icons, so the
		// check above misses them).
		final Item item = heldItem.getItem();
		if (item instanceof ItemBlock) {
			final Block block = Block.getBlockFromItem(item);
			if (block != null && !RenderBlocks.renderItemIn3d(block.getRenderType())) {
				cir.setReturnValue(Boolean.FALSE);
			}
		}
	}
}
