package ganymedes01.etfuturum.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

/**
 * Equivalent of 26.2's entity event 35 (totem of undying activation): tells
 * clients around the entity to spawn the totem particle emitter, play the use
 * sound and (for the local player) play the item activation animation.
 */
public class TotemParticlesMessage implements IMessage {

	public int entityId;

	public TotemParticlesMessage() {
	}

	public TotemParticlesMessage(int entityId) {
		this.entityId = entityId;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		entityId = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(entityId);
	}
}
