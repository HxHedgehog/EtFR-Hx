package ganymedes01.etfuturum.mixins.early.waterlog.client;

import ganymedes01.etfuturum.client.waterfog.ModernWaterFog;
import ganymedes01.etfuturum.configuration.configs.ConfigWorld;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.opengl.GL11;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.7.10's {@code RenderGlobal.renderSky} draws the graded sky body with fog on,
 * then calls {@code glDisable(GL_FOG)} (line 1125) before painting the
 * sunrise/sunset colour band, so that band is never fogged. When the camera sits
 * underwater the resulting hard boundary between the fogged near water and the
 * unfogged far band shows up as a visible gap, most notably at sunrise/sunset.
 * Re-enabling the fog for exactly the sunrise/sunset band (from its
 * {@code glShadeModel(GL_SMOOTH)} setup to its {@code glShadeModel(GL_FLAT)}
 * teardown) lets the band blend onto the same EXP fog as the foreground, then
 * drops the fog again before the sun/moon/stars are drawn so those stay crisp.
 *
 * <p>The tail of the same method draws the sea-level horizon assembly (L1230-1276),
 * which 26.2 has no underwater equivalent of: the black void shaft (a 2x2 cap one
 * block below the camera - waist height for a swimming player - with walls running
 * down past the seabed), the black horizon ring (L1234) and the sky-colored
 * horizon ring (L1275), a +/-384 plane pinned at absolute y=64 tessellated in huge
 * 64x64 cells. All of it is drawn within a couple of blocks of the camera, where
 * fixed-function EXP fog (density 0.1) attenuates only ~4% per near vertex, so the
 * raw geometry reads as a fogless "glass box" around the player (inward faces only,
 * invisible from above). Underwater we skip the two rings and the void shaft
 * entirely; the fogged sky ceiling ({@code glSkyList}, camera+16, ~92% fog) already
 * provides the 26.2 "sky = fog color" look upward, and looking down is plain fogged
 * water.</p>
 *
 * <p>Both effects judge at the camera position (26.2
 * {@code Camera#getFluidInCamera} semantics, the same judgment as the water
 * fog/FOV redirects) instead of vanilla's entity-eye check, so third person
 * agrees with the fog the camera actually renders with. When
 * {@code modernWaterFog} is off, falls back to the vanilla eye check.</p>
 *
 * <p>26.2 reference (decompiled, {@code SkyRenderer#shouldRenderDarkDisc} L381-383): the dark
 * disc - the direct descendant of 1.7.10's black ring (same translate+12 bottom plane,
 * {@code renderDarkDisc} L385-399) - renders while {@code playerEye.y < horizon &&
 * !player.isUnderWater()} ({@code isUnderWater} = eye-in-water, Entity L1540-1542), i.e. it
 * keeps filling the far seabed-to-sea-level gap while the player merely floats at the surface
 * or wades, and is dropped only once the eye enters the fluid; the sky disc renders
 * unconditionally (LevelRenderer L332) and 26.2 has no void-shaft geometry at all. 1.7.10's
 * equivalents therefore keep their own vanilla gates (feet below horizon for the black ring,
 * unconditional for the sky-colored ring) and only add that fluid refinement, judged at the
 * camera (26.2 {@code Camera#getFluidInCamera} semantics, the same judgment as the water
 * fog/FOV redirects). Body-contact gates ({@code isInWater() || isSwimming()} then
 * {@code isInWater() && !onGround}) were wrong for the rings: they dropped the whole assembly
 * on body contact, leaving the gap empty while floating - exactly what 26.2 keeps filled.</p>
 */
@Mixin(RenderGlobal.class)
public abstract class MixinRenderGlobal {

	@Unique
	private float etfu$skyPartialTicks;

	@Inject(method = "renderSky(F)V", at = @At("HEAD"))
	private void etfu$captureSkyPartialTicks(float partialTicks, CallbackInfo ci) {
		this.etfu$skyPartialTicks = partialTicks;
	}

	@Redirect(
			method = "renderSky(F)V",
			at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glShadeModel(I)V", remap = false))
	private void etfuturum$fogSunriseBandOnly(int mode) {
		if (etfu$shouldHideSeaHorizon()) {
			if (mode == GL11.GL_SMOOTH) {
				// Sunrise/sunset band begins: fog it so it blends with the water.
				GL11.glEnable(GL11.GL_FOG);
			} else if (mode == GL11.GL_FLAT) {
				// Band ends: drop the fog so sun/moon/stars stay crisp.
				GL11.glDisable(GL11.GL_FOG);
			}
		}
		GL11.glShadeModel(mode);
	}

	/**
	 * Both {@code glSkyList2} horizon rings (black L1234, sky-colored L1275) are
	 * sea-level artifacts; skip them whenever the camera is underwater.
	 */
	@Redirect(
			method = "renderSky(F)V",
			at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glCallList(I)V", remap = false),
			slice = @Slice(from = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
					target = "Lnet/minecraft/client/renderer/RenderGlobal;glSkyList2:I", ordinal = 0)))
	private void etfu$hideHorizonRingsUnderwater(int list) {
		if (!etfu$shouldHideSeaHorizon()) {
			GL11.glCallList(list);
		}
	}

	/**
	 * The black void shaft tessellated between the two {@code glSkyList2} rings
	 * (cap at camera-1, walls down past the seabed) reads as a fogless box
	 * underwater; skip it there too. start/draw must be skipped as a pair or the
	 * Tessellator is left mid-draw ("Already tesselating!" on the next user); the
	 * stray addVertex calls in between append to rawBuffer and are wiped by the
	 * next startDrawing's reset().
	 */
	@Redirect(
			method = "renderSky(F)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Tessellator;startDrawingQuads()V"),
			slice = @Slice(
					from = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
							target = "Lnet/minecraft/client/renderer/RenderGlobal;glSkyList2:I", ordinal = 0),
					to = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
							target = "Lnet/minecraft/client/renderer/RenderGlobal;glSkyList2:I", ordinal = 1)))
	private void etfu$hideVoidBoxStartUnderwater(Tessellator tessellator) {
		if (!etfu$shouldHideVoidShaft()) {
			tessellator.startDrawingQuads();
		}
	}

	@Redirect(
			method = "renderSky(F)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Tessellator;draw()I"),
			slice = @Slice(
					from = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
							target = "Lnet/minecraft/client/renderer/RenderGlobal;glSkyList2:I", ordinal = 0),
					to = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
							target = "Lnet/minecraft/client/renderer/RenderGlobal;glSkyList2:I", ordinal = 1)))
	private int etfu$hideVoidBoxUnderwater(Tessellator tessellator) {
		if (!etfu$shouldHideVoidShaft()) {
			return tessellator.draw();
		}
		return 0;
	}

	@Unique
	private boolean etfu$shouldHideSeaHorizon() {
		Minecraft mc = Minecraft.getMinecraft();
		EntityLivingBase view = mc.renderViewEntity;
		if (view == null || mc.theWorld == null) {
			return false;
		}
		// 26.2's fluid refinement for the dark disc (!isUnderWater, eye-based), judged at the
		// camera like every other fluid judgment in this port. Vanilla's own gates (feet below
		// horizon for the black ring, unconditional for the sky-colored ring) stay untouched,
		// so floating at the surface and wading both keep the far-sea fill rendered.
		return ConfigWorld.modernWaterFog
				? ModernWaterFog.getBlockAtCamera(mc.theWorld, view, this.etfu$skyPartialTicks)
						.getMaterial() == Material.water
				: view.isInsideOfMaterial(Material.water);
	}

	/**
	 * 26.2 has no void-shaft geometry in any view ({@code SkyRenderer} draws only the sky disc
	 * and the conditional dark disc; the dark disc plane alone covers below the horizon and fog
	 * fills the near field). Hide it whenever the camera is underwater or the player has any
	 * body contact with water - a third-person camera floating above the surface while the
	 * player floats/wades otherwise sees the black walls rising out of the sea around the
	 * camera. Dry land below the horizon keeps the vanilla look.
	 */
	@Unique
	private boolean etfu$shouldHideVoidShaft() {
		Minecraft mc = Minecraft.getMinecraft();
		EntityLivingBase view = mc.renderViewEntity;
		if (view == null || mc.theWorld == null) {
			return false;
		}
		return etfu$shouldHideSeaHorizon() || view.isInWater();
	}
}
