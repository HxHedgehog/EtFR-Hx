package ganymedes01.etfuturum.client.particle;

import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

/**
 * Client-side replica of 26.2's {@code TrackingEmitter} as used for the
 * totem-of-undying activation ({@code createTrackingEmitter(entity,
 * ParticleTypes.TOTEM_OF_UNDYING, 30)}): an invisible "particle" that lives 30
 * ticks and, every tick, tries to spawn 16 totem particles at uniformly random
 * points inside the entity's bounding box sphere:
 * <ul>
 * <li>{@code x = entity.getX(xa / 4)} = posX + (xa / 4) * width</li>
 * <li>{@code y = entity.getY(0.5 + ya / 4)} = posY + (0.5 + ya / 4) * height</li>
 * <li>{@code z = entity.getZ(za / 4)} = posZ + (za / 4) * width</li>
 * </ul>
 * where (xa, ya, za) is a random point in the unit sphere (rejected if
 * xa^2+ya^2+za^2 &gt; 1), moving with velocity (xa, ya + 0.2, za). Like the
 * modern constructor, this emitter ticks once immediately on creation.
 */
public class TotemFXEmitter extends EntityFX {

	private final Entity entity;
	private int life;

	public TotemFXEmitter(World world, Entity entity) {
		super(world, entity.posX, entity.posY + entity.height * 0.5D, entity.posZ, 0, 0, 0);
		this.entity = entity;
		this.particleMaxAge = 30;
		this.noClip = true;
		this.onUpdate();
	}

	@Override
	public void renderParticle(Tessellator tessellator, float partialTicks, float rx, float rxz, float rz, float ryz, float rxy) {
		// NoRenderParticle: only spawns the actual totem particles
	}

	@Override
	public int getFXLayer() {
		return 0;
	}

	@Override
	public void onUpdate() {
		for (int i = 0; i < 16; i++) {
			double xa = rand.nextFloat() * 2.0F - 1.0F;
			double ya = rand.nextFloat() * 2.0F - 1.0F;
			double za = rand.nextFloat() * 2.0F - 1.0F;
			if (xa * xa + ya * ya + za * za > 1.0D) {
				continue;
			}
			double x = entity.posX + xa / 4.0D * entity.width;
			double y = entity.posY + (0.5D + ya / 4.0D) * entity.height;
			double z = entity.posZ + za / 4.0D * entity.width;
			CustomParticles.spawnTotemParticle(worldObj, x, y, z, xa, ya + 0.2D, za);
		}
		if (++life >= particleMaxAge) {
			setDead();
		}
	}
}
