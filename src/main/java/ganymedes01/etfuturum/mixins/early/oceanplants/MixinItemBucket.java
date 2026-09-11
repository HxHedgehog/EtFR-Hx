package ganymedes01.etfuturum.mixins.early.oceanplants;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.blocks.BlockAquaticPlant;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Bucket interactions with the pseudo-waterlogged ocean plants. Their cells use
 * {@link net.minecraft.block.material.Material#water}, so vanilla {@link ItemBucket}
 * treats them like ordinary liquid: a water bucket falls through the isSolid check
 * and overwrites the plant, and an empty bucket's scoop (material == water &&
 * meta == 0) deletes seagrass/kelp cells outright. 26.2 semantics instead:
 * placeLiquid on an already-water cell does nothing and keeps the bucket, and
 * kelp/seagrass water cannot be picked up. The sea pickle is the only real water
 * container of the group - its water can be scooped (leaving it dry, matching
 * {@code BlockSeaPickle#onBlockActivated}) and a water bucket re-hydrates it.
 */
@Mixin(ItemBucket.class)
public abstract class MixinItemBucket {

	@Shadow
	public Block isFull;

	/**
	 * Pour path (also the sneak path for the dry pickle fill, since sneaking skips
	 * onBlockActivated): a water bucket on a wet plant cell does nothing and the
	 * bucket stays full; on the dry pickle it re-hydrates the pickle in place.
	 */
	@Inject(method = "tryPlaceContainedLiquid", at = @At("HEAD"), cancellable = true)
	private void etfuturum$oceanPlantsResistBuckets(World world, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
		Block block = world.getBlock(x, y, z);
		if (BlockAquaticPlant.isOceanPlant(block)) {
			cir.setReturnValue(false);
		} else if (block == ModBlocks.DEAD_SEA_PICKLE.get() && this.isFull == Blocks.flowing_water) {
			world.setBlock(x, y, z, ModBlocks.SEA_PICKLE.get(), world.getBlockMetadata(x, y, z), 3);
			world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "random.splash", 0.5F, 1.0F);
			cir.setReturnValue(true);
		}
	}

	/**
	 * Scoop path: kelp/seagrass never match the vanilla scoop branch (their fake
	 * material skips both the water and lava checks), so the cell and the bucket
	 * are left untouched.
	 */
	@Redirect(method = "onItemRightClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlock(III)Lnet/minecraft/block/Block;"))
	private Block etfuturum$oceanPlantsAreNotScoopable(World world, int x, int y, int z) {
		Block block = world.getBlock(x, y, z);
		if (BlockAquaticPlant.isOceanPlant(block) && block != ModBlocks.SEA_PICKLE.get()) {
			return Blocks.air;
		}
		return block;
	}

	/**
	 * Wet pickles scoop regardless of the pickle count stored in metadata - the
	 * vanilla source check (meta == 0) only sees single-pickle stacks otherwise.
	 */
	@Redirect(method = "onItemRightClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockMetadata(III)I"))
	private int etfuturum$pickleScoopLevel(World world, int x, int y, int z) {
		return world.getBlock(x, y, z) == ModBlocks.SEA_PICKLE.get() ? 0 : world.getBlockMetadata(x, y, z);
	}

	/**
	 * Scooping a wet pickle un-waterlogs it instead of deleting the cell: the
	 * pickles stay in place (now dry) with their count intact, and the vanilla
	 * code still hands out the filled water bucket.
	 */
	@Redirect(method = "onItemRightClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockToAir(III)Z"))
	private boolean etfuturum$scoopUnwaterlogsPickle(World world, int x, int y, int z) {
		if (world.getBlock(x, y, z) == ModBlocks.SEA_PICKLE.get()) {
			world.setBlock(x, y, z, ModBlocks.DEAD_SEA_PICKLE.get(), world.getBlockMetadata(x, y, z), 3);
			return false;
		}
		return world.setBlockToAir(x, y, z);
	}
}
