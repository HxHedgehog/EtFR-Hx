package ganymedes01.etfuturum.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.client.handler.SpearHitFeedbackTracker;
import ganymedes01.etfuturum.items.ItemSpear;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Server -> Client hit feedback, the 26.2 broadcastEntityEvent(attacker, 2) equivalent. Every
 * watcher (the attacker included) starts the spear hitFeedback animation and plays the local hit
 * sound from the wielded spear, exactly like LivingEntity#onKineticHit.
 */
public class SpearHitFeedbackMessage implements IMessage {

	private int attackerId;

	public SpearHitFeedbackMessage() {
	}

	public SpearHitFeedbackMessage(int attackerId) {
		this.attackerId = attackerId;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		attackerId = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(attackerId);
	}

	public static class Handler implements IMessageHandler<SpearHitFeedbackMessage, IMessage> {

		@Override
		public IMessage onMessage(SpearHitFeedbackMessage message, MessageContext ctx) {
			handleMessage(message);
			return null;
		}

		@SideOnly(Side.CLIENT)
		private void handleMessage(SpearHitFeedbackMessage message) {
			World world = Minecraft.getMinecraft().theWorld;
			Entity attacker = world.getEntityByID(message.attackerId);
			if (attacker == null)
				return;
			SpearHitFeedbackTracker.recordHit(attacker, world.getTotalWorldTime());
			String prefix = "spear";
			ItemStack held = attacker instanceof EntityLivingBase ? ((EntityLivingBase) attacker).getHeldItem() : null;
			if (held != null && held.getItem() instanceof ItemSpear)
				prefix = ((ItemSpear) held.getItem()).getSoundPrefix();
			world.playSoundAtEntity(attacker, "etfuturum:" + prefix + ".hit", 1.0F, 1.0F); // 26.2 makeLocalHitSound
		}
	}
}
