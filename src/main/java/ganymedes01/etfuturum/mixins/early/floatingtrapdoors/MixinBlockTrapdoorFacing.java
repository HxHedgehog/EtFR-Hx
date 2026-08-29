package ganymedes01.etfuturum.mixins.early.floatingtrapdoors;

import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import net.minecraft.block.Block;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client-side prediction counterpart of the server's floating-trapdoor
 * placement handler (ServerEventHandler#onPlaceBlock, gated by
 * {@code enableFloatingTrapDoors}).
 * <p>
 * When a trapdoor is placed against the top or bottom face of a block, the
 * server handler re-orients it to the player's facing via
 * {@code BlockEvent.PlaceEvent} and sends a block update. But Forge only
 * builds placement snapshots on the server world
 * ({@code World#setBlock}: {@code captureBlockSnapshots && !isRemote}), so
 * PlaceEvent never fires during the client-side placement prediction. The
 * client therefore first renders the vanilla side-based orientation and then
 * visibly "snaps" to the corrected orientation when the server update
 * arrives — a one-frame flicker on every top/bottom trapdoor placement.
 * <p>
 * This mixin replicates the server handler's exact math inside
 * {@link BlockTrapDoor#onBlockPlaced} for the client world, so the predicted
 * metadata already matches the server's correction. It applies to every
 * trapdoor (vanilla and modded) since they all inherit from BlockTrapDoor and
 * none of them override {@code onBlockPlaced}. The server keeps using the
 * PlaceEvent handler, where the placing entity is authoritative.
 */
@Mixin(BlockTrapDoor.class)
public class MixinBlockTrapdoorFacing extends Block {

	protected MixinBlockTrapdoorFacing(Material materialIn) {
		super(materialIn);
	}

	@Inject(method = "onBlockPlaced", at = @At("HEAD"), cancellable = true)
	private void etfuturum$orientToPlayerFacing(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int meta, CallbackInfoReturnable<Integer> cir) {
		if (!world.isRemote || (side != 0 && side != 1)) {
			return;
		}
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.thePlayer == null) {
			return;
		}
		// Same computation as ServerEventHandler#onPlaceBlock so the predicted
		// metadata matches the server-side correction exactly.
		int l = (MathHelper.floor_double(mc.thePlayer.rotationYaw * 4.0F / 360.0F + 0.5D) + 1) & 3;
		if (l == 0) {
			l = 2;
		} else if (l == 3) {
			l = 1;
		} else if (l == 1) {
			l = 0;
		} else {
			l = 3;
		}
		if (side == 0) {
			l += 8;
		}
		cir.setReturnValue(l);
	}
}
