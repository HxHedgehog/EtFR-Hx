package ganymedes01.etfuturum.client;

import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.core.utils.RandomXoshiro256StarStar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import java.util.Random;

/**
 * Client-side replica of 26.2's item activation animation
 * ({@code ScreenEffectRenderer#renderItemActivationAnimation}) as used by the
 * totem of undying: for 40 ticks the totem item swoops in front of the camera.
 * All the transform math below is ported verbatim:
 * <pre>
 * tick  = 40 - itemActivationTicks
 * s     = (tick + partialTicks) / 40
 * ss    = 10.25*s^5 - 24.95*s^4 + 25.5*s^3 - 13.8*s^2 + 4*s
 * ps    = ss * PI
 * translate(offX * 0.3 * aspect * |sin(ps * 2)|, offY * 0.3 * |sin(ps * 2)|, -10 + 9 * sin(ps))
 * scale(0.8); rotateY(900 * |sin(ps)|); rotateX(6 * cos(s * 8)); rotateZ(6 * cos(s * 8))
 * </pre>
 * The item is drawn as a 1x1 sprite quad at the vanilla "fixed" display scale
 * (0.5) with forced full brightness, rendered on top of the world (depth test
 * disabled) but before the GUI, matching the modern screen-effect pass.
 */
public class TotemOfUndyingAnimation {

	public static final int LENGTH = 40;

	private static final Random RAND = new RandomXoshiro256StarStar();
	private static ItemStack item;
	private static int ticks;
	private static float offX;
	private static float offY;

	public static void start() {
		item = new ItemStack(ModItems.TOTEM_OF_UNDYING.get());
		ticks = LENGTH;
		offX = RAND.nextFloat() * 2.0F - 1.0F;
		offY = RAND.nextFloat() * 2.0F - 1.0F;
	}

	public static void tick() {
		if (ticks > 0 && --ticks == 0) {
			item = null;
		}
	}

	public static boolean isActive() {
		return item != null && ticks > 0;
	}

	public static void render(Minecraft mc, float partialTicks) {
		// 26.2 skips the item activation animation when the GUI is hidden (F1)
		if (mc.gameSettings.hideGUI || !isActive()) {
			return;
		}

		float tick = LENGTH - ticks;
		float s = (tick + partialTicks) / 40.0F;
		float ts = s * s;
		float tc = s * ts;
		float smooth = 10.25F * tc * ts - 24.95F * ts * ts + 25.5F * tc - 13.8F * ts + 4.0F * s;
		float ps = smooth * (float) Math.PI;
		float aspect = (float) mc.displayWidth / (float) mc.displayHeight;

		IIcon icon = item.getIconIndex();

		// 26.2 renders hand + screen effects under a dedicated perspective
		// projection: setupPerspective(0.05, 100, hudFov=70, width, height)
		// (GameRenderer line 583, Camera#calculateHudFov). Under this projection
		// the vanilla z range (-10 .. -1) makes the item visibly grow ~10x as it
		// flies toward the camera. Switch the world's matrices out while drawing.
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glPushMatrix();
		GL11.glLoadIdentity();
		GLU.gluPerspective(70.0F, aspect, 0.05F, 100.0F);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glPushMatrix();
		GL11.glLoadIdentity();

		mc.getTextureManager().bindTexture(TextureMap.locationItemsTexture);

		float sin2 = MathHelper.abs(MathHelper.sin(ps * 2.0F));
		float dx = TotemOfUndyingAnimation.offX * 0.3F * aspect;
		float dy = TotemOfUndyingAnimation.offY * 0.3F;
		GL11.glTranslatef(dx * sin2, dy * sin2, -10.0F + 9.0F * MathHelper.sin(ps));
		GL11.glScalef(0.8F, 0.8F, 0.8F);
		GL11.glRotatef(900.0F * MathHelper.abs(MathHelper.sin(ps)), 0.0F, 1.0F, 0.0F);
		GL11.glRotatef(6.0F * MathHelper.cos(s * 8.0F), 1.0F, 0.0F, 0.0F);
		GL11.glRotatef(6.0F * MathHelper.cos(s * 8.0F), 0.0F, 0.0F, 1.0F);
		GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F); // "fixed" display transform: rotation Y 180

		// GL_ENABLE_BIT saves the enabled/disabled state of cull, fog, lighting,
		// depth test etc., GL_COLOR_BUFFER_BIT saves the blend func state, so we
		// can freely set up a flat double-sided quad render: modern item models
		// are two-sided, and the 900-degree Y spin (which stalls and reverses
		// mid-animation) would otherwise cull the quad away from the camera for
		// the whole stall window.
		GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_LIGHTING_BIT | GL11.GL_FOG_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_FOG);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

		// "fixed" display of a generated item model: scale [1,1,1] (see
		// generated.json) with the sprite unit being 1 → half extent 0.5
		float half = 0.5F;
		double u0 = icon.getMinU();
		double u1 = icon.getMaxU();
		double v0 = icon.getMinV();
		double v1 = icon.getMaxV();

		Tessellator tess = Tessellator.instance;
		tess.startDrawingQuads();
		tess.setBrightness(0xF000F0);
		tess.setColorOpaque_F(1.0F, 1.0F, 1.0F);
		tess.addVertexWithUV(-half, -half, 0.0D, u0, v1);
		tess.addVertexWithUV(half, -half, 0.0D, u1, v1);
		tess.addVertexWithUV(half, half, 0.0D, u1, v0);
		tess.addVertexWithUV(-half, half, 0.0D, u0, v0);
		tess.draw();

		GL11.glPopAttrib();

		GL11.glPopMatrix();
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glPopMatrix();
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
	}
}
