package ganymedes01.etfuturum.mixins.early.oceanplants;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.blocks.BlockAquaticPlant;
import ganymedes01.etfuturum.lib.Reference;
import ganymedes01.etfuturum.waterlog.WaterlogFamilies;
import ganymedes01.etfuturum.waterlog.WaterlogFlow;
import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
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
 *
 * <p>The same three scoop redirects also serve true waterlogging (bit16 family
 * blocks): a waterlogged stair is rock material, so the vanilla scoop branch
 * (material == water {@code &&} meta == 0) never sees it. The redirects mask the
 * cell as {@code Blocks.water} + meta 0 for the check, then the setBlockToAir
 * redirect drains the bit16 instead of deleting the block (26.2
 * {@code pickupBlock}: {@code setBlock(WATERLOGGED=false, 3)}; stairs always
 * {@code canSurvive}, so no destroy). Registered when either ocean plants or the
 * waterlog feature is on; all pickle identity checks are null-safe when the
 * ocean-plant blocks are absent.
 */
@Mixin(ItemBucket.class)
public abstract class MixinItemBucket {

	@Shadow
	public Block isFull;

	/**
	 * Staged bucket result for waterlogged-family clicks (see the canMineBlock
	 * redirect): consumed at onItemRightClick's RETURN points. ThreadLocal -
	 * client and server threads run their own interaction predictions.
	 */
	private static final ThreadLocal<ItemStack> etfuturum$pendingBucketResult = new ThreadLocal<>();

	/**
	 * Set when tryPlaceContainedLiquid returns true - the pour succeeded
	 * regardless of game mode (creative pours return the original stack, so the
	 * return-stack-differs check can't see them). Consumed at onItemRightClick's
	 * RETURN points for the swing.
	 */
	private static final ThreadLocal<Boolean> etfuturum$pourSucceeded = new ThreadLocal<>();

	/**
	 * 26.2 placeLiquid on the true waterlogging family (BucketItem.use L75-87 +
	 * emptyContents L148-153): a water bucket right-clicking a LiquidBlockContainer
	 * targets the hit cell itself, and emptyContents IGNORES placeLiquid's return
	 * value - an already waterlogged cell still plays the empty sound and still
	 * consumes the bucket. Sneaking falls through to the offset-cell placement.
	 * Hooked on the {@code canMineBlock} call inside {@code onItemRightClick} -
	 * the ONLY spot where the mop cell coordinates are still un-offset (the pour
	 * branch shifts them by the hit side right after; {@code ItemBucket} inherits
	 * {@code onItemUse} from Item without declaring it, so an injection there is
	 * dead bytecode). The empty sound for the dry→wet write comes from this
	 * handler (onFillBucketEvent fires earlier, while the cell is still dry);
	 * for the already-wet click onFillBucketEvent's isWaterlogged branch covers
	 * it. Swings on success like 26.2's InteractionResult.SUCCESS.
	 */
	@Redirect(method = "onItemRightClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;canMineBlock(Lnet/minecraft/entity/player/EntityPlayer;III)Z"))
	private boolean etfuturum$waterlogFamilyAtMop(World world, EntityPlayer player, int x, int y, int z) {
		Block block = world.getBlock(x, y, z);
		boolean family = WaterlogFamilies.isWaterloggable(block);
		if (family) {
			WaterlogRuntime.debug("MOP-HIT " + x + "," + y + "," + z + " isFullWater=" + (this.isFull == Blocks.flowing_water)
					+ " sneak=" + player.isSneaking() + " wet=" + WaterlogRuntime.isWaterlogged(world, x, y, z) + " remote=" + world.isRemote);
		}
		if (WaterlogRuntime.isEnabled() && this.isFull == Blocks.flowing_water && !player.isSneaking() && family) {
			// 20261013-J 闪烁追踪：玩家点名家族格倒水（含已含水格）→ 广播坐标
			WaterlogRuntime.debugTrack(world, WaterlogRuntime.TRACK_POUR, x, y, z);
			boolean alreadyWet = WaterlogRuntime.isWaterlogged(world, x, y, z);
			// Both sides: client prediction + server write, idempotent bit set.
			// (The H experiment proved the pour flash is NOT this dual write - it
			// persists with server-only writes and zero data changes - so restore
			// the responsive client prediction.)
			if (!alreadyWet) {
				WaterlogRuntime.markWaterlogged(world, x, y, z);
				WaterlogFlow.scheduleSpread(world, x, y, z, world.getBlock(x, y, z));
			}
			ItemStack held = player.getCurrentEquippedItem();
			boolean swapped = false;
			if (!player.capabilities.isCreativeMode && held != null) {
				// Instance swap on the held slot on BOTH sides - a server-only
				// change gets swallowed by isChangingQuantityOnly during C08
				// processing, and the slot-reference change triggers the equip
				// animation
				if (--held.stackSize <= 0) {
					player.inventory.mainInventory[player.inventory.currentItem] = new ItemStack(Items.bucket);
					swapped = true;
				} else if (!player.inventory.addItemStackToInventory(new ItemStack(Items.bucket))) {
					player.dropPlayerItemWithRandomChoice(new ItemStack(Items.bucket), false);
					swapped = true;
				}
			}
			WaterlogRuntime.debug("WATERLOG-USE " + x + "," + y + "," + z + " alreadyWet=" + alreadyWet
					+ " creative=" + player.capabilities.isCreativeMode + " swapped=" + swapped + " remote=" + world.isRemote);
			// The bucket result must flow through onItemRightClick's RETURN value:
			// NetHandlerPlayServer writes the RETURNED stack back into the held
			// slot after the call, which silently overwrote the swapped empty
			// bucket (the "eaten bucket"). Stage it and swap at every RETURN.
			etfuturum$pendingBucketResult.set(player.capabilities.isCreativeMode ? held : new ItemStack(Items.bucket));
			return false; // swallow vanilla's offset-cell pour
		}
		return world.canMineBlock(player, x, y, z);
	}

	/**
	 * The held-slot swap above is undone by NetHandlerPlayServer's trailing
	 * {@code mainInventory[currentItem] = returnedStack} unless the RETURNED
	 * stack is the empty bucket. @At("RETURN") covers every exit of
	 * onItemRightClick: waterlogged-family clicks return the staged empty
	 * bucket and swing like 26.2's InteractionResult.SUCCESS; ordinary
	 * pour/scoop successes (returned stack differs from the input) swing too.
	 * Both sides - the client call drives the first-person animation, the
	 * server call broadcasts to other players.
	 */
	@Inject(method = "onItemRightClick", at = @At("RETURN"), cancellable = true)
	private void etfuturum$waterlogBucketResult(ItemStack stack, World world, EntityPlayer player, CallbackInfoReturnable<ItemStack> cir) {
		ItemStack staged = etfuturum$pendingBucketResult.get();
		Boolean pourOk = etfuturum$pourSucceeded.get();
		etfuturum$pourSucceeded.remove();
		if (staged != null) {
			etfuturum$pendingBucketResult.remove();
			cir.setReturnValue(staged);
			player.swingItem();
		} else if (Boolean.TRUE.equals(pourOk)) {
			// Creative pour: the returned stack IS the input (no consumption), so
			// the return-stack-differs check never fires - 26.2 still swings on
			// InteractionResult.SUCCESS regardless of consumption
			player.swingItem();
		} else {
			ItemStack result = cir.getReturnValue();
			if (result != null && result != stack) {
				player.swingItem();
			}
		}
	}

	/**
	 * Marks a successful pour for the swing (see etfuturum$waterlogBucketResult).
	 */
	@Inject(method = "tryPlaceContainedLiquid", at = @At("RETURN"))
	private void etfuturum$markPourResult(World world, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ()) {
			// 20261013-J 闪烁追踪：vanilla 偏移格倒水成功（普通倒水/潜行倒入家族格）
			// → 广播坐标；岩浆桶不追。注意家族邻面分支在 HEAD 早退，不经过这里。
			if (this.isFull == Blocks.flowing_water) {
				WaterlogRuntime.debugTrack(world, WaterlogRuntime.TRACK_POUR, x, y, z);
			}
			etfuturum$pourSucceeded.set(Boolean.TRUE);
		}
	}

	/**
	 * 20261018-A 预测登记：vanilla 偏移格倒水的瞬时预测写（液体/0, flag 3），20261018-B
	 * 起通用化到水+岩浆（岩浆倒/舀回声闪烁与水完全同构）。只挂 tryPlaceContainedLiquid
	 * 内唯一的 setBlock——写真正发生时才登记，失败的倒水（目标格 solid 早退）不留空登记；
	 * 地狱 fizz 分支不写方块也不经过这里。family 格直接点击倒水与邻面倒入都走
	 * markWaterlogged（内部已登记），不经过本方法。
	 */
	@Redirect(method = "tryPlaceContainedLiquid", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlock(IIILnet/minecraft/block/Block;II)Z"))
	private boolean etfuturum$recordPourPrediction(World world, int x, int y, int z, Block block, int meta, int flag) {
		// 注意桶的 isFull 存的是流动液体：water_bucket=flowing_water、lava_bucket=
		// flowing_lava（Item.java L175-176 构造），==Blocks.lava 永远为假（20261018-C 修复）
		if (world.isRemote && (this.isFull == Blocks.flowing_water || this.isFull == Blocks.flowing_lava)) {
			WaterlogRuntime.recordPrediction(x, y, z, Block.getIdFromBlock(world.getBlock(x, y, z)), world.getBlockMetadata(x, y, z), world.getTotalWorldTime());
		}
		return world.setBlock(x, y, z, block, meta, flag);
	}

	/**
	 * 26.2's bucket use has a hand-swap animation: vanilla 1.7.10 converts the
	 * held stack IN PLACE (func_150996_a setItem), so the ItemStack reference
	 * never changes and ItemRenderer's equip animation never triggers - the item
	 * texture sits dead on the hand. Replacing the held SLOT instance instead
	 * makes the animation fire (same mechanism the pickle fill path already
	 * shows). Vanilla semantics otherwise mirrored exactly. This method is the
	 * single funnel for successful scoops (water/lava) - flag it so the
	 * onItemRightClick RETURN handler swings in EVERY game mode (creative
	 * returns the input stack unchanged, so the return-differs heuristic never
	 * fires there; 26.2 still swings on a successful pickup).
	 */
	@Inject(method = "func_150910_a(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/item/Item;)Lnet/minecraft/item/ItemStack;", at = @At("HEAD"), cancellable = true)
	private void etfuturum$swapInstanceForEquipAnimation(ItemStack stack, EntityPlayer player, net.minecraft.item.Item filled, CallbackInfoReturnable<ItemStack> cir) {
		etfuturum$pourSucceeded.set(Boolean.TRUE);
		if (player.capabilities.isCreativeMode) {
			return; // vanilla: keep the full bucket in creative
		}
		if (--stack.stackSize <= 0) {
			ItemStack filledStack = new ItemStack(filled);
			player.inventory.mainInventory[player.inventory.currentItem] = filledStack;
			cir.setReturnValue(filledStack);
		} else {
			if (!player.inventory.addItemStackToInventory(new ItemStack(filled))) {
				player.dropPlayerItemWithRandomChoice(new ItemStack(filled), false);
			}
			cir.setReturnValue(stack);
		}
	}

	/**
	 * Pour path (also the sneak path for the dry pickle fill, since sneaking skips
	 * onBlockActivated): a water bucket on a wet plant cell does nothing and the
	 * bucket stays full; on the dry pickle it re-hydrates the pickle in place.
	 * The pour target here is the OFFSET cell of the ray hit (onItemRightClick
	 * shifts by the hit side before calling), which CAN be a waterlogged-family
	 * cell - the neighbor-face pour onto toggle blocks (26.2: a water bucket on a
	 * trapdoor cell's neighboring face fills the trapdoor, since a direct click
	 * toggles instead; emptyContents L148-153 fills any LiquidBlockContainer the
	 * offset lands in, ignoring placeLiquid's return - sound + consumption even
	 * when already wet). Mark dry cells bit16 + schedule the spread; vanilla's
	 * own return path consumes the bucket and the markPourResult inject swings.
	 */
	@Inject(method = "tryPlaceContainedLiquid", at = @At("HEAD"), cancellable = true)
	private void etfuturum$oceanPlantsResistBuckets(World world, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
		Block block = world.getBlock(x, y, z);
		if (BlockAquaticPlant.isOceanPlant(block)) {
			cir.setReturnValue(false);
		} else if (block == ModBlocks.DEAD_SEA_PICKLE.get() && this.isFull == Blocks.flowing_water) {
			// 20261013-J 闪烁追踪：死泡菜补水（干→湿，画水量变化）
			WaterlogRuntime.debugTrack(world, WaterlogRuntime.TRACK_POUR, x, y, z);
			// 20261018-A 预测登记：补水预测写（死泡菜→湿泡菜）的预测前状态
			if (world.isRemote) {
				WaterlogRuntime.recordPrediction(x, y, z, Block.getIdFromBlock(block), world.getBlockMetadata(x, y, z), world.getTotalWorldTime());
			}
			world.setBlock(x, y, z, ModBlocks.SEA_PICKLE.get(), world.getBlockMetadata(x, y, z), 3);
			world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, Reference.MCAssetVer + ":item.bucket.empty", 1.0F, 1.0F);
			cir.setReturnValue(true);
		} else if (this.isFull == Blocks.flowing_water && WaterlogRuntime.isEnabled() && WaterlogFamilies.isWaterloggable(block)) {
			// 20261013-J 闪烁追踪：邻面倒入家族格（活板门等开关方块）→ 广播坐标
			WaterlogRuntime.debugTrack(world, WaterlogRuntime.TRACK_POUR, x, y, z);
			// Both sides: client prediction + server write, idempotent bit set
			// (the H server-only experiment proved the flash is not this dual
			// write - restore the responsive prediction)
			if (!WaterlogRuntime.isWaterlogged(world, x, y, z)) {
				WaterlogRuntime.markWaterlogged(world, x, y, z);
				WaterlogFlow.scheduleSpread(world, x, y, z, block);
			}
			// This branch cancels tryPlaceContainedLiquid at HEAD - an early
			// return does NOT pass through @At("RETURN") injection points, so
			// markPourResult never fires; flag the success explicitly for the
			// RETURN swing (26.2 swings on emptyContents in every game mode).
			// The empty sound is played by ServerEventHandler#onFillBucketEvent
			// (its familyNeighbor gate covers this exact case) - a local sound
			// here would double up with it.
			etfuturum$pourSucceeded.set(Boolean.TRUE);
			cir.setReturnValue(true);
		}
	}

	/**
	 * Scoop path: kelp/seagrass never match the vanilla scoop branch (their fake
	 * material skips both the water and lava checks), so the cell and the bucket
	 * are left untouched. A waterlogged family cell masks as water so the branch
	 * can match and the setBlockToAir redirect below drains it.
	 */
	@Redirect(method = "onItemRightClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlock(III)Lnet/minecraft/block/Block;"))
	private Block etfuturum$oceanPlantsAreNotScoopable(World world, int x, int y, int z) {
		Block block = world.getBlock(x, y, z);
		if (BlockAquaticPlant.isOceanPlant(block) && block != ModBlocks.SEA_PICKLE.get()) {
			return Blocks.air;
		}
		if (WaterlogRuntime.isWaterlogged(world, x, y, z)) {
			return Blocks.water;
		}
		return block;
	}

	/**
	 * Wet pickles scoop regardless of the pickle count stored in metadata - the
	 * vanilla source check (meta == 0) only sees single-pickle stacks otherwise.
	 * Waterlogged family cells report meta 0 for the same source check.
	 */
	@Redirect(method = "onItemRightClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockMetadata(III)I"))
	private int etfuturum$pickleScoopLevel(World world, int x, int y, int z) {
		if (world.getBlock(x, y, z) == ModBlocks.SEA_PICKLE.get() || WaterlogRuntime.isWaterlogged(world, x, y, z)) {
			return 0;
		}
		return world.getBlockMetadata(x, y, z);
	}

	/**
	 * Scooping a wet pickle un-waterlogs it instead of deleting the cell: the
	 * pickles stay in place (now dry) with their count intact, and the vanilla
	 * code still hands out the filled water bucket. Scooping a waterlogged family
	 * block clears bit16 (flag 3 = neighbour notify + client sync) and keeps the
	 * block in place; a leftover spread tick on the cell self-cancels at its
	 * waterlogged re-check.
	 */
	@Redirect(method = "onItemRightClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockToAir(III)Z"))
	private boolean etfuturum$scoopUnwaterlogsPickle(World world, int x, int y, int z) {
		Block block = world.getBlock(x, y, z);
		// 20261018-A 预测登记：舀液体的瞬时预测写（family bit16 清除/湿泡菜→死泡菜/
		// 水源→air）的预测前状态，供包侧回声过滤；20261018-B 起通用化到岩浆舀取。
		if (world.isRemote && (block == Blocks.water || block == Blocks.flowing_water
				|| block == Blocks.lava || block == Blocks.flowing_lava
				|| WaterlogRuntime.isWaterlogged(world, x, y, z) || block == ModBlocks.SEA_PICKLE.get())) {
			WaterlogRuntime.recordPrediction(x, y, z, Block.getIdFromBlock(block), world.getBlockMetadata(x, y, z), world.getTotalWorldTime());
		}
		if (block == ModBlocks.SEA_PICKLE.get()) {
			// 20261013-J 闪烁追踪：舀湿泡菜（湿→干，画水量变化）
			WaterlogRuntime.debugTrack(world, WaterlogRuntime.TRACK_SOAK, x, y, z);
			world.setBlock(x, y, z, ModBlocks.DEAD_SEA_PICKLE.get(), world.getBlockMetadata(x, y, z), 3);
			return false;
		}
		if (WaterlogRuntime.isWaterlogged(world, x, y, z)) {
			// 20261013-J 闪烁追踪：舀含水家族格（bit16 清除）
			WaterlogRuntime.debugTrack(world, WaterlogRuntime.TRACK_SOAK, x, y, z);
			world.setBlockMetadataWithNotify(x, y, z, world.getBlockMetadata(x, y, z) & ~WaterlogRuntime.WATERLOGGED_BIT, 3);
			return false;
		}
		// 20261013-J 闪烁追踪：舀普通水源（vanilla 路径，岩浆不追）
		if (block == Blocks.water || block == Blocks.flowing_water) {
			WaterlogRuntime.debugTrack(world, WaterlogRuntime.TRACK_SOAK, x, y, z);
		}
		return world.setBlockToAir(x, y, z);
	}
}
