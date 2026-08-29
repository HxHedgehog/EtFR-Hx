package ganymedes01.etfuturum.client.particle;

import net.minecraft.world.World;

/**
 * Client-side replica of 26.2's {@code TotemParticle} (the totem-of-undying
 * activation sparkle). Ported constants:
 * <ul>
 * <li>sprite set {@code minecraft:glitter_7 ... glitter_0}, animated by age via
 * {@code setSpriteFromAge}: list index = age * (size - 1) / lifetime, so the
 * frame file index is {@code 7 - age * 7 / lifetime}</li>
 * <li>lifetime 60 + rand(12), quad size 0.1 * 0.75</li>
 * <li>friction 0.6 (1.7.10's EntityFX already applies 0.98 drag, so the extra
 * multiplier is 0.6 / 0.98), gravity 1.25 (motionY -= 0.04 * 1.25 per tick)</li>
 * <li>alpha fade in the second half of the lifetime
 * ({@code 1 - (age - lifetime / 2) / lifetime}), translucency blend</li>
 * <li>25% of particles get a yellow-ish tint (0.6-0.8, 0.6-0.9, 0-0.2), the
 * rest a green one (0.1-0.3, 0.4-0.7, 0-0.2)</li>
 * <li>forced full brightness ({@code getLightCoords} returns 0xF000F0)</li>
 * </ul>
 */
public class TotemFX extends EtFuturumFXParticle {

	/** 0.6 modern friction / 0.98 vanilla EntityFX drag. */
	private static final double FRICTION_RATIO = 0.6D / 0.98D;

	public TotemFX(World world, double x, double y, double z, double xa, double ya, double za) {
		super(world, x, y, z, xa, ya, za, 60 + particleRand.nextInt(12), 0.75F, 0xFFFFFFFF, "textures/particle/glitter.png", 8);
		particleGravity = 1.25F;
		fadeAway = true;
		noClip = false;
		currentTexture = 7; // setSpriteFromAge at age 0 → glitter_7
		if (particleRand.nextInt(4) == 0) {
			setRBGColorF(0.6F + particleRand.nextFloat() * 0.2F, 0.6F + particleRand.nextFloat() * 0.3F, particleRand.nextFloat() * 0.2F);
		} else {
			setRBGColorF(0.1F + particleRand.nextFloat() * 0.2F, 0.4F + particleRand.nextFloat() * 0.3F, particleRand.nextFloat() * 0.2F);
		}
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		motionX *= FRICTION_RATIO;
		motionY *= FRICTION_RATIO;
		motionZ *= FRICTION_RATIO;
		currentTexture = 7 - (int) (particleAge * 7D / particleMaxAge);
	}

	@Override
	public int getBrightnessForRender(float partialTicks) {
		return 0xF000F0;
	}
}
