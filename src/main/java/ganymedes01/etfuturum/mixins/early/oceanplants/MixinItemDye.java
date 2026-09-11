package ganymedes01.etfuturum.mixins.early.oceanplants;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.blocks.BlockAquaticPlant;
import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

/**
 * 26.2 BoneMealItem#useOn: when the ordinary crop path fails, bone meal used on
 * a sturdy face grows a patch of seagrass in the full water cell behind that
 * face (growWaterPlant: 128 random-walk attempts, each seeding a short seagrass
 * into a full water cell with a sturdy floor, or - 1/10 of the time - doubling
 * an existing short seagrass into the tall variant). One bone meal is consumed
 * no matter how much actually grows. There are no coral blocks in 1.7.10 yet,
 * so the coral biome branches are reduced to always planting seagrass.
 */
@Mixin(ItemDye.class)
public abstract class MixinItemDye {

	@Inject(method = "onItemUse", at = @At("HEAD"), cancellable = true)
	private void etfuturum$growWaterPlants(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, CallbackInfoReturnable<Boolean> cir) {
		if (stack.getItemDamage() != 15) {
			return; // the other dyes and cocoa keep vanilla behaviour
		}
		// 26.2 ordering: the clicked block itself gets the crop treatment first
		Block clicked = world.getBlock(x, y, z);
		if (clicked instanceof IGrowable && ((IGrowable) clicked).func_149851_a(world, x, y, z, world.isRemote)) {
			return;
		}
		// 26.2 solidBlockFace: the clicked face must be a full square
		if (!BlockAquaticPlant.isFaceSturdy(world, x, y, z, side)) {
			return;
		}
		int wx = x;
		int wy = y;
		int wz = z;
		switch (side) {
			case 0: --wy; break;
			case 1: ++wy; break;
			case 2: --wz; break;
			case 3: ++wz; break;
			case 4: --wx; break;
			case 5: ++wx; break;
		}
		if (!player.canPlayerEdit(wx, wy, wz, side, stack) || !BlockAquaticPlant.isFullWater(world, wx, wy, wz)) {
			return;
		}
		// 26.2 growWaterPlant succeeds on the client without touching the
		// inventory; the server does the actual growth and consumes the bone meal
		// (vanilla applyBonemeal decrements unconditionally the same way)
		if (world.isRemote) {
			cir.setReturnValue(true);
			return;
		}

		Random rand = world.rand;
		Block seagrass = ModBlocks.SEAGRASS.get();
		for (int j = 0; j < 128; ++j) {
			int tx = wx;
			int ty = wy;
			int tz = wz;
			boolean blocked = false;
			for (int i = 0; i < j / 16; ++i) {
				tx += rand.nextInt(3) - 1;
				ty += (rand.nextInt(3) - 1) * rand.nextInt(3) / 2;
				tz += rand.nextInt(3) - 1;
				if (BlockAquaticPlant.isCollisionShapeFullBlock(world, tx, ty, tz)) {
					blocked = true;
					break;
				}
			}
			if (blocked) {
				continue;
			}
			// stateToGrow.canSurvive: seagrass needs a sturdy UP face below
			if (!BlockAquaticPlant.hasSolidGround(world, tx, ty, tz)) {
				continue;
			}
			Block target = world.getBlock(tx, ty, tz);
			if (BlockAquaticPlant.isFullWater(world, tx, ty, tz)) {
				world.setBlock(tx, ty, tz, seagrass, 0, 3);
			} else if (target == seagrass) {
				IGrowable growable = (IGrowable) target;
				// 1/10: an existing short seagrass doubles into tall seagrass
				if (growable.func_149851_a(world, tx, ty, tz, false) && rand.nextInt(10) == 0) {
					growable.func_149853_b(world, rand, tx, ty, tz);
				}
			}
		}
		world.playAuxSFX(2005, wx, wy, wz, 0);
		stack.stackSize--;
		cir.setReturnValue(true);
	}
}
