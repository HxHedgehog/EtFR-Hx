package ganymedes01.etfuturum.client.renderer.item;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.items.ItemSpear;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;

/**
 * Replicates the official 26.2 spear rendering when held (models/item/spear_in_hand.json:
 * "parent": "item/generated" + display transforms). The wide {tier}_spear_in_hand sprite is
 * extruded with the vanilla flat item renderer, exactly like modern versions render
 * item/generated models in a hand. INVENTORY/ENTITY/DROPPED keep the flat {tier}_spear icon.
 *
 * Source values (official 26.2 decompiled sources):
 * firstperson_righthand:  rot[-20, 90, -35] trans[3.13, 2.0, 0.13]/16 scale[1.36, 1.36, 0.68]
 * thirdperson_righthand:  rot[5, 270, -40]  trans[0, 2, 2]/16        scale[1.7, 1.7, 0.85]
 * ItemTransform.apply:    translate, rotationXYZ(X->Y->Z), scale, translate(-0.5)
 * ItemInHandLayer (third person only): rotateX(-90), rotateY(180), translate(1/16, 2/16, -10/16)
 *
 * The sprite quad is drawn NON-mirrored (minU first) unlike vanilla 1.7.10 flat items: the
 * official item/generated sprite maps u=0 to x=0, so the spear tip (u~0, top-left of the
 * 32x32 sprite) lands at x=0/top and points up-forward after the display rotations.
 * renderItemIn2D's quad spans z[-1/16, 0] while the official generated sprite extrudes
 * z[7.5/16, 8.5/16] (ItemModelGenerator MIN_Z/MAX_Z: the plate is centred in the unit cube);
 * a z shift of 0.5 + 1/32 lands our plate on the same spot before centring. An off-centre
 * plate would leak into a lateral x offset after the 90/270 degree display Y rotations.
 */
@SideOnly(Side.CLIENT)
public class ItemSpearRenderer implements IItemRenderer {

	private static final ResourceLocation RES_ITEM_GLINT = new ResourceLocation("textures/misc/enchanted_item_glint.png");

	@Override
	public boolean handleRenderType(ItemStack stack, ItemRenderType type) {
		return type == ItemRenderType.EQUIPPED || type == ItemRenderType.EQUIPPED_FIRST_PERSON;
	}

	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack stack, ItemRendererHelper helper) {
		return false; // no Forge pre-transforms, the official chain is applied below
	}

	@Override
	public void renderItem(ItemRenderType type, ItemStack stack, Object... data) {
		IIcon icon = ((ItemSpear) stack.getItem()).getInHandIcon();
		if (icon == null)
			return;

		TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
		textureManager.bindTexture(TextureMap.locationItemsTexture);

		boolean firstPerson = type == ItemRenderType.EQUIPPED_FIRST_PERSON;
		float rotX = firstPerson ? -20.0F : 5.0F;
		float rotY = firstPerson ? 90.0F : 270.0F;
		float rotZ = firstPerson ? -35.0F : -40.0F;
		float transX = firstPerson ? 3.13F / 16.0F : 0.0F;
		float transY = firstPerson ? 2.0F / 16.0F : 2.0F / 16.0F;
		float transZ = firstPerson ? 0.13F / 16.0F : 2.0F / 16.0F;
		float scaleX = firstPerson ? 1.36F : 1.7F;
		float scaleY = firstPerson ? 1.36F : 1.7F;
		float scaleZ = firstPerson ? 0.68F : 0.85F;

		GL11.glEnable(GL12.GL_RESCALE_NORMAL);

		// GTNH's ForgeHooksClient.renderEquippedItem wraps every custom renderer in the vanilla flat
		// item frame T(0,-0.3,0) S(1.5) R(50Y) R(335Z) T(-0.9375,-0.0625,0). Undo it to get back to
		// the plain held item anchor space (order = inverses of the frame, last applied first).
		GL11.glTranslatef(0.9375F, 0.0625F, 0.0F);
		GL11.glRotatef(-335.0F, 0.0F, 0.0F, 1.0F);
		GL11.glRotatef(-50.0F, 0.0F, 1.0F, 0.0F);
		GL11.glScalef(2.0F / 3.0F, 2.0F / 3.0F, 2.0F / 3.0F);
		GL11.glTranslatef(0.0F, 0.3F, 0.0F);

		// Convert 1.7.10's held item anchor space into 26.2's, right before the display transform.
		if (firstPerson) {
			// 1.7.10 wraps first person items in R(45Y)...S(0.4) (ItemRenderer.renderItemInFirstPerson
			// L435/L443); 26.2 has R(45Y)...R(-45Y) instead and no 0.4 scale (its scale lives in the
			// display transform). Invert both differences to land in 26.2's space.
			GL11.glScalef(2.5F, 2.5F, 2.5F);
			GL11.glRotatef(-45.0F, 0.0F, 1.0F, 0.0F);
		} else {
			// Undo the whole RenderBiped isFull3D branch on top of the frame (RenderBiped.java
			// L287-301, applied after T(-0.0625, 0.4375, 0.0625) at L264), then apply 26.2's
			// ItemInHandLayer pre-transform (rotateX(-90), rotateY(180), translate(1/16, 2/16, -10/16)).
			GL11.glRotatef(-45.0F, 0.0F, 1.0F, 0.0F);
			GL11.glRotatef(100.0F, 1.0F, 0.0F, 0.0F);
			GL11.glScalef(1.6F, -1.6F, 1.6F);
			GL11.glTranslatef(0.0F, -0.1875F, 0.0F);
			GL11.glTranslatef(0.0625F, -0.4375F, -0.0625F);
			GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
			GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
			GL11.glTranslatef(1.0F / 16.0F, 2.0F / 16.0F, -10.0F / 16.0F);
		}

		// ItemTransform.apply: translate(/16), rotateXYZ, scale, centre the model
		GL11.glTranslatef(transX, transY, transZ);
		GL11.glRotatef(rotX, 1.0F, 0.0F, 0.0F);
		GL11.glRotatef(rotY, 0.0F, 1.0F, 0.0F);
		GL11.glRotatef(rotZ, 0.0F, 0.0F, 1.0F);
		GL11.glScalef(scaleX, scaleY, scaleZ);
		// Official generated sprites extrude the plate at z[7.5/16, 8.5/16] (ItemModelGenerator
		// MIN_Z/MAX_Z = centred in the unit cube); renderItemIn2D spans z[-1/16, 0] (centre -1/32),
		// so shift by 0.5 + 1/32 to put our plate on the official spot before centring.
		GL11.glTranslatef(0.0F, 0.0F, 0.5F + 1.0F / 32.0F);
		GL11.glTranslatef(-0.5F, -0.5F, -0.5F);

		TextureUtil.func_152777_a(false, false, 1.0F);
		Tessellator tessellator = Tessellator.instance;
		// Non-mirrored UV order (minU first): matches the official item/generated sprite mapping,
		// keeping the sprite tip (u~0) at local -x/-y so it points forward after the Y rotations.
		ItemRenderer.renderItemIn2D(tessellator, icon.getMinU(), icon.getMinV(), icon.getMaxU(), icon.getMaxV(), icon.getIconWidth(), icon.getIconHeight(), 0.0625F);

		if (stack.hasEffect(0)) {
			float f7 = 0.76F;
			GL11.glDepthFunc(GL11.GL_EQUAL);
			GL11.glDisable(GL11.GL_LIGHTING);
			textureManager.bindTexture(RES_ITEM_GLINT);
			GL11.glEnable(GL11.GL_BLEND);
			OpenGlHelper.glBlendFunc(768, 1, 1, 0);
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

		textureManager.bindTexture(TextureMap.locationItemsTexture);
		TextureUtil.func_147945_b();
		GL11.glDisable(GL12.GL_RESCALE_NORMAL);
	}
}
