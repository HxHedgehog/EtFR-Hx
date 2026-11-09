package ganymedes01.etfuturum.client.waterfog;

import ganymedes01.etfuturum.elytra.IElytraPlayer;
import ganymedes01.etfuturum.swimming.ISwimmingPlayer;
import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**
 * Camera-based water judgment for vanilla's fog logic (26.2 {@code Camera#getFluidInCamera}
 * semantics). Used as the redirect target for {@code ActiveRenderInfo#getBlockAtEntityViewpoint}
 * in {@code EntityRenderer#setupFog}/{@code updateFogColor}.
 *
 * <p>Vanilla 1.7.10 judges the fog type from the entity-eye viewpoint, which is what leaks the
 * underwater fog onto the sky in third person when the camera is above the surface. Judging at
 * the camera position fixes that leak at the source for every vanilla branch (water, lava, fog
 * colors) without touching any bytecode Angelica injects.</p>
 *
 * <p>Deliberately <b>no</b> fog density/color takeover: shader packs read the {@code fogColor}
 * uniform (Angelica {@code CommonUniforms} L153) while keeping their own fog distances, so an
 * overridden bright color misaligns with whatever distance the pack decides ("fog wall close in"
 * on short-fog packs, washed out on modern-style packs). Vanilla density/color stay untouched and
 * vanilla's own night vision brightening (updateFogColor L1888-1906) applies unchanged.</p>
 */
public final class ModernWaterFog {

	private ModernWaterFog() {
	}

	/**
	 * The block the vanilla fog logic judges, but sampled at the <b>camera</b> position instead of
	 * the entity-eye viewpoint (26.2 {@code Camera#getFluidInCamera} semantics).
	 *
	 * <p>Replicates 1.7.10 {@code EntityRenderer#orientCamera} (L552-650) so the judgment happens
	 * where the camera really is, including the swimming pose anchor (same math as the swimming
	 * mixin's camera eye height constant replacement), the third person look offsets and the
	 * anti-clip ray. The eye anchor follows {@code posY = boundingBox.minY + yOffset - ySize}
	 * (Entity L591), i.e. {@code posY} is already at eye level for a standing player.</p>
	 */
	public static Block getBlockAtCamera(World world, EntityLivingBase entity, float partialTicks) {
		float f1 = entity.yOffset - 1.62F;
		if (entity.isPlayerSleeping()) {
			f1 += 1.0F;
		} else if (entity instanceof EntityPlayer && entity instanceof ISwimmingPlayer
				&& !(entity instanceof IElytraPlayer elytra && elytra.etfu$isElytraFlying())) {
			f1 = entity.yOffset - entity.ySize - ((ISwimmingPlayer) entity).etfu$getCameraEyeHeight(partialTicks);
		}
		double camX = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
		double camY = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks - f1;
		double camZ = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks;
		if (entity.isPlayerSleeping()) {
			camY += 0.3F;
		}
		GameSettings settings = Minecraft.getMinecraft().gameSettings;
		if (settings.thirdPersonView != 0 && !settings.debugCamEnable) {
			double distance = 4.0F; // EntityRenderer.thirdPersonDistance (settled value)
			float sign = settings.thirdPersonView == 2 ? 1.0F : -1.0F;
			Vec3 look = entity.getLook(partialTicks);
			// Single-ray anti-clip (vanilla orientCamera L609-619 casts 8 jittered rays and takes
			// the min distance; the center ray is sufficient for a material judgment)
			MovingObjectPosition mop = world.rayTraceBlocks(Vec3.createVectorHelper(camX, camY, camZ),
					Vec3.createVectorHelper(camX + look.xCoord * sign * distance, camY + look.yCoord * sign * distance,
							camZ + look.zCoord * sign * distance));
			boolean clamped = false;
			if (mop != null) {
				double hit = MathHelper.sqrt_double(mop.hitVec.squareDistanceTo(camX, camY, camZ));
				if (hit < distance) {
					distance = hit;
					clamped = true;
				}
			}
			camX += look.xCoord * sign * distance;
			camY += look.yCoord * sign * distance;
			camZ += look.zCoord * sign * distance;
			if (clamped) {
				// The clamp lands on the occluding face to floating point precision, and the
				// intersection floats around the exact block boundary as partialTicks varies
				// per frame, so floor_double straddles the two blocks and the material
				// judgment flickers (third person looking up from the seabed clamps on the
				// sand top face = water/sand boundary: fog, sky-skip and FOV all toggle every
				// frame). Nudge the sample back toward the eye, into the last fluid the ray
				// passed through; 0.1 matches vanilla orientCamera's ray jitter scale.
				camX -= look.xCoord * sign * 0.1D;
				camY -= look.yCoord * sign * 0.1D;
				camZ -= look.zCoord * sign * 0.1D;
			}
		}
		return blockAtPoint(world, camX, camY, camZ);
	}

	/**
	 * The block at a world point, judged on 26.2 {@code Camera#getFluidInCamera} semantics
	 * (L436-438: fluid at the camera block, gated on the fluid's <b>rendered</b> height — the
	 * visible surface — with a fluid-above fallback).
	 *
	 * <p>Vanilla 1.7.10 judges on the eye-corrected line {@code by+1-(percent-1/9)}, which is the
	 * <b>block top</b> for source water and thus sits 1/9 block <i>above</i> the visible surface
	 * (8/9) — the view turns blue while the camera is still visibly above the water ("half in
	 * water" flips too early). The line here is {@code (by+1) - getLiquidHeightPercent(meta)}
	 * instead: 8/9 for source, decreasing per flow meta, which matches 26.2's fluid height for
	 * every meta. A waterlogged block is a source-water fluid in modern semantics, judged on the
	 * same 8/9 line as the waterlog eye bridge, with the vanilla-style above fallback closing
	 * waterlogged column seams.</p>
	 */
	private static Block blockAtPoint(World world, double x, double y, double z) {
		int bx = MathHelper.floor_double(x);
		int by = MathHelper.floor_double(y);
		int bz = MathHelper.floor_double(z);
		Block block = world.getBlock(bx, by, bz);
		if (block.getMaterial().isLiquid()) {
			float surface = (by + 1) - BlockLiquid.getLiquidHeightPercent(world.getBlockMetadata(bx, by, bz));
			if (y >= surface) {
				block = world.getBlock(bx, by + 1, bz);
				if (WaterlogRuntime.isWaterlogged(world, bx, by + 1, bz)) {
					block = Blocks.water;
				}
			}
			return block;
		}
		if (WaterlogRuntime.isWaterlogged(world, bx, by, bz)) {
			float surface = (by + 1) - BlockLiquid.getLiquidHeightPercent(0);
			if (y < surface) {
				return Blocks.water;
			}
			Block above = world.getBlock(bx, by + 1, bz);
			return above.getMaterial().isLiquid() || WaterlogRuntime.isWaterlogged(world, bx, by + 1, bz)
					? Blocks.water : above;
		}
		return block;
	}
}
