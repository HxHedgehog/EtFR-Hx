package ganymedes01.etfuturum.client.renderer.item;

import ganymedes01.etfuturum.enchantment.EnchantedBookTextureHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * Renders an enchanted book held by an entity using the Bibliophilia
 * per-enchantment sprite (falling back to the vanilla book icon when no mapping
 * exists). It handles the {@link ItemRenderType#EQUIPPED} and
 * {@link ItemRenderType#EQUIPPED_FIRST_PERSON} types so the custom sprite is
 * shown both in third-person and first-person hands. All other render types are
 * left to the vanilla pipeline, which already shows the sprite via the icon
 * mixins, so GUI and dropped items stay untouched.
 * <p>
 * {@code shouldUseRenderHelper} returns {@code false}, so Forge's
 * {@code renderEquippedItem} applies the standard 2D item transformation before
 * calling {@link #renderItem}; this renderer therefore only draws the sprite
 * quad and the enchanted glint, matching the vanilla {@code ItemRenderer} size.
 */
public class EnchantedBookHandRenderer implements IItemRenderer {

	private static final ResourceLocation RES_ITEM_GLINT = new ResourceLocation("textures/misc/enchanted_item_glint.png");

	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		return type == ItemRenderType.EQUIPPED || type == ItemRenderType.EQUIPPED_FIRST_PERSON;
	}

	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		return false;
	}

	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		IIcon iicon = EnchantedBookTextureHandler.INSTANCE.getIcon(item);
		if (iicon == null) {
			iicon = item.getIconIndex();
		}
		if (iicon == null) {
			return;
		}

		TextureManager texturemanager = Minecraft.getMinecraft().getTextureManager();
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		texturemanager.bindTexture(texturemanager.getResourceLocation(item.getItemSpriteNumber()));
		TextureUtil.func_152777_a(false, false, 1.0F);
		Tessellator tessellator = Tessellator.instance;
		float f = iicon.getMinU();
		float f1 = iicon.getMaxU();
		float f2 = iicon.getMinV();
		float f3 = iicon.getMaxV();
		ItemRenderer.renderItemIn2D(tessellator, f1, f2, f, f3, iicon.getIconWidth(), iicon.getIconHeight(), 0.0625F);

		if (item.hasEffect(0)) {
			GL11.glDepthFunc(GL11.GL_EQUAL);
			GL11.glDisable(GL11.GL_LIGHTING);
			texturemanager.bindTexture(RES_ITEM_GLINT);
			GL11.glEnable(GL11.GL_BLEND);
			OpenGlHelper.glBlendFunc(768, 1, 1, 0);
			float f7 = 0.76F;
			GL11.glColor4f(0.5F * f7, 0.25F * f7, 0.8F * f7, 1.0F);
			GL11.glMatrixMode(GL11.GL_TEXTURE);
			GL11.glPushMatrix();
			float f8 = 0.125F;
			GL11.glScalef(f8, f8, f8);
			float f9 = (float) (Minecraft.getSystemTime() % 3000L) / 3000.0F * 8.0F;
			GL11.glTranslatef(f9, 0.0F, 0.0F);
			GL11.glRotatef(-50.0F, 0.0F, 0.0F, 1.0F);
			ItemRenderer.renderItemIn2D(tessellator, 0.0F, 0.0F, 1.0F, 1.0F, 256, 256, 0.0625F);
			GL11.glPopMatrix();
			GL11.glPushMatrix();
			GL11.glScalef(f8, f8, f8);
			f9 = (float) (Minecraft.getSystemTime() % 4873L) / 4873.0F * 8.0F;
			GL11.glTranslatef(-f9, 0.0F, 0.0F);
			GL11.glRotatef(10.0F, 0.0F, 0.0F, 1.0F);
			ItemRenderer.renderItemIn2D(tessellator, 0.0F, 0.0F, 1.0F, 1.0F, 256, 256, 0.0625F);
			GL11.glPopMatrix();
			GL11.glMatrixMode(GL11.GL_MODELVIEW);
			GL11.glDisable(GL11.GL_BLEND);
			GL11.glEnable(GL11.GL_LIGHTING);
			GL11.glDepthFunc(GL11.GL_LEQUAL);
		}

		// Re-bind the item atlas (the glint pass swapped it for RES_ITEM_GLINT)
		// and restore the texture filter parameters captured by func_152777_a.
		texturemanager.bindTexture(texturemanager.getResourceLocation(item.getItemSpriteNumber()));
		TextureUtil.func_147945_b();
	}
}