package ganymedes01.etfuturum.mixins.early.noteblockupdate;

import net.minecraft.block.Block;
import net.minecraft.block.BlockNote;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

/**
 * Makes a note block emit an observable block update when it is triggered,
 * matching modern behavior:
 * <ul>
 * <li>Right-click and redstone activation produce an instantly detectable
 * change even when a block above mutes the sound.</li>
 * <li>Left-click (mining) still plays but does NOT trigger the update.</li>
 * <li>Redstone de-activation also pulses, so a BUD piston underneath extends
 * on power-up and retracts on power-down.</li>
 * </ul>
 * <p>
 * In 1.7.10 the note/redstone state lives only in the tile entity (never in
 * metadata), so observers see nothing - we emulate the modern "POWERED" state
 * flip with a transient metadata bit. The note value occupies metadata bits
 * 0-4 (0-24), so bit 5 (32) is free for this transient, 1-tick observable
 * toggle (see updateTick below for the reset). The bit is toggled with notify
 * flag 3 (1|2): 2 refreshes the client, 1 fires notifyBlockChange so
 * neighbours actually receive onNeighborBlockChange.
 */
@Mixin(BlockNote.class)
public abstract class MixinBlockNote extends Block {

	private static final int OBSERVABLE_BIT = 32;

	protected MixinBlockNote(Material materialIn) {
		super(materialIn);
	}

	// Anchor to the previousRedstoneState store (fires on both edges), not
	// triggerNote (rising edge only), so power-down also notifies the piston.
	@Inject(method = "onNeighborBlockChange",
			at = @At(value = "FIELD",
					target = "Lnet/minecraft/tileentity/TileEntityNote;previousRedstoneState:Z",
					opcode = Opcodes.PUTFIELD,
					shift = At.Shift.BEFORE))
	private void etfuturum$pulseRedstone(World worldIn, int x, int y, int z, Block neighbor, CallbackInfo ci) {
		etfuturum$pulse(worldIn, x, y, z);
	}

	@Inject(method = "onBlockActivated",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/tileentity/TileEntityNote;triggerNote(Lnet/minecraft/world/World;III)V",
					shift = At.Shift.AFTER))
	private void etfuturum$pulseRightClick(World worldIn, int x, int y, int z, EntityPlayer player,
										   int side, float subX, float subY, float subZ,
										   CallbackInfoReturnable<Boolean> cir) {
		etfuturum$pulse(worldIn, x, y, z);
	}

	@Unique
	private void etfuturum$pulse(World worldIn, int x, int y, int z) {
		if (worldIn.isRemote) {
			return;
		}
		int meta = worldIn.getBlockMetadata(x, y, z);
		if ((meta & OBSERVABLE_BIT) == 0) {
			// Flag 3 = 1|2: 2 marks for client render, 1 fires notifyBlockChange so
			// neighbours (e.g. a BUD piston below) receive onNeighborBlockChange.
			// Flag 2 alone only does a client update and never reaches the piston.
			worldIn.setBlockMetadataWithNotify(x, y, z, meta | OBSERVABLE_BIT, 3);
			worldIn.scheduleBlockUpdate(x, y, z, this, 1);
		}
	}

	@Override
	public void updateTick(World worldIn, int x, int y, int z, Random random) {
		int meta = worldIn.getBlockMetadata(x, y, z);
		if ((meta & OBSERVABLE_BIT) != 0) {
			worldIn.setBlockMetadataWithNotify(x, y, z, meta & ~OBSERVABLE_BIT, 2);
		}
		super.updateTick(worldIn, x, y, z, random);
	}
}