package ganymedes01.etfuturum.mixins.early.oceanplants.client;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.renderer.ItemRenderer;

/**
 * Held-item rendering for pass-1 (translucent) blocks unconditionally enables
 * GL_BLEND at the top of ItemRenderer.renderItem. That state is only meaningful
 * for the 3D block-model path below (which pairs it with depthMask(false));
 * it never matters in vanilla 1.7.10 because no vanilla pass-1 block (water,
 * ice, stained glass...) exposes a flat 2D item icon.
 * <p>
 * The ocean plants are pass-1 blocks (water material) whose items render as
 * flat 2D sprites. With blending on, the pixel-slice "thickness" geometry of
 * renderItemIn2D blends instead of being alpha-tested: the whole comb mesh
 * becomes visible and the card looks shattered under shaders. Their icons are
 * hard-cutout (alpha is strictly 0 or 255), so skip blending for flat sprites
 * and let the normal alpha-test path handle them. The enchanted-glint branch
 * manages its own blend state and is unaffected.
 */
@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer {

	@Redirect(
			method = "renderItem(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;ILnet/minecraftforge/client/IItemRenderer$ItemRenderType;)V",
			at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glEnable(I)V", ordinal = 0, remap = false),
			remap = false)
	private void etfuturum$skipBlendForFlatIconsOfTranslucentBlocks(int capability, EntityLivingBase entity, ItemStack stack, int renderPass, ItemRenderType type) {
		if (stack.getItemSpriteNumber() == 0) {
			GL11.glEnable(capability);
		}
	}
}
