package ganymedes01.etfuturum.mixins.early.spear.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ganymedes01.etfuturum.core.utils.SpearChargeTicker;
import ganymedes01.etfuturum.core.utils.SpearEase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * 26.2 Player.tick (L262-269): attackStrengthTicker and itemSwapTicker count up every tick and
 * BOTH reset whenever the main-hand item changes (NBT-only changes keep counting; vanilla has
 * shipped this reset since 1.9). The attack gate reads attackStrengthTicker against the held
 * spear's attack duration - 26.2 derives the delay from the ATTACK_SPEED attribute
 * 1/attackDuration - 4, so gate length == SwingAnimation length; itemSwapTicker paces the
 * first-person equip raise (ItemInHandRenderer L565). Attacks clear just attackStrengthTicker
 * (26.2 resetOnlyAttackStrengthTicker), so attacking never replays the raise.
 */
@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer extends EntityLivingBase implements SpearChargeTicker {

	@Shadow
	public abstract ItemStack getCurrentEquippedItem();

	public MixinEntityPlayer(World world) {
		super(world);
	}

	@Unique
	private int etfu$attackChargeTicker;
	@Unique
	private int etfu$swapTicker;
	@Unique
	private Item etfu$lastHeldItem;

	@Inject(method = "onUpdate", at = @At("TAIL"))
	private void etfu$spearChargeTick(CallbackInfo ci) {
		++this.etfu$attackChargeTicker;
		++this.etfu$swapTicker;
		ItemStack held = this.getCurrentEquippedItem();
		Item item = held != null ? held.getItem() : null;
		if (item != this.etfu$lastHeldItem) {
			this.etfu$resetSpearChargeTickers();
			this.etfu$lastHeldItem = item;
		}
	}

	@Override
	public float etfu$spearAttackCharge(int tolerance, int durationTicks) {
		return SpearEase.clamp01((this.etfu$attackChargeTicker + tolerance) / (float) durationTicks);
	}

	@Override
	public float etfu$spearSwapCharge(int durationTicks) {
		return SpearEase.clamp01((this.etfu$swapTicker + 1.0F) / durationTicks); // 26.2 getItemSwapScale(1.0F)
	}

	@Override
	public void etfu$resetSpearAttackCharge() {
		this.etfu$attackChargeTicker = 0; // 26.2 resetOnlyAttackStrengthTicker
	}

	@Override
	public void etfu$resetSpearChargeTickers() {
		this.etfu$attackChargeTicker = 0; // 26.2 resetAttackStrengthTicker
		this.etfu$swapTicker = 0;
	}
}
