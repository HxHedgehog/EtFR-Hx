package ganymedes01.etfuturum.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.client.TotemOfUndyingAnimation;
import ganymedes01.etfuturum.client.particle.CustomParticles;
import ganymedes01.etfuturum.lib.Reference;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

/**
 * Mirrors 26.2's {@code ClientPacketListener.handleEntityEvent} case 35: spawn
 * a 30-tick tracking emitter of totem particles on the entity, play the
 * {@code item.totem.use} sound at fixed 1.0 pitch and, only when the entity is
 * the local player, trigger the 40-tick item activation screen animation.
 */
public class TotemParticlesHandler implements IMessageHandler<TotemParticlesMessage, IMessage> {

	@Override
	public IMessage onMessage(TotemParticlesMessage message, MessageContext ctx) {
		handleMessage(message);
		return null;
	}

	@SideOnly(Side.CLIENT)
	private void handleMessage(TotemParticlesMessage message) {
		Minecraft mc = Minecraft.getMinecraft();
		World world = mc.theWorld;
		if (world == null) {
			return;
		}
		Entity entity = world.getEntityByID(message.entityId);
		if (entity == null) {
			return;
		}

		CustomParticles.spawnTotemEmitter(world, entity);
		world.playSound(entity.posX, entity.posY, entity.posZ, Reference.MCAssetVer + ":item.totem.use", 1.0F, 1.0F, false);

		if (entity == mc.thePlayer) {
			TotemOfUndyingAnimation.start();
		}
	}
}
