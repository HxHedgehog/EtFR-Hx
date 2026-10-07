package ganymedes01.etfuturum.mixins.early.backlytra;

import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import ganymedes01.etfuturum.elytra.IElytraPlayer;
import ganymedes01.etfuturum.items.equipment.ItemArmorElytra;
import ganymedes01.etfuturum.potion.ModPotions;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer extends EntityLivingBase implements IElytraPlayer {
	@Override
	@Shadow
	public abstract boolean isPlayerSleeping();

	@Shadow
	public PlayerCapabilities capabilities;

	@Shadow
	protected abstract void setHideCape(int par1, boolean par2);

	@Shadow
	public InventoryPlayer inventory;

	public MixinEntityPlayer(World p_i1594_1_) {
		super(p_i1594_1_);
	}

	public void tickElytra() {
		boolean flag = etfu$isElytraFlying();

		ItemStack itemstack = ItemArmorElytra.getElytra(this);
		this.setHideCape(1, itemstack != null);
		/* Capture the previous tick's motion for the client-side glide Y-rot interpolation (26.2 deltaMovementOnPreviousTick) */
		this.etfu$setPrevMotion(this.motionX, this.motionY, this.motionZ);
		if (itemstack != null && !capabilities.isFlying && !ItemArmorElytra.isBroken(itemstack)) {
			/* 26.2 LivingEntity.canGlide() + travelFallFlying():
			 * - onGround / passenger / levitation stop the glide;
			 * - touching a climbable block stops the glide;
			 * - water does NOT stop the glide (fluid travel takes precedence,
			 *   and the glide resumes once the entity leaves the water). */
			if (flag && !this.onGround && !this.isRiding() && !this.isOnLadder() && !etfu$hasLevitation()) {
				if (!this.worldObj.isRemote && (this.etfu$ticksElytraFlying + 1) % 20 == 0) {
					itemstack.damageItem(1, this);
				}
			} else {
				flag = false;
			}
		} else {
			flag = false;
		}


		if (!this.worldObj.isRemote) {
			this.etfu$setElytraFlying(flag);
		}

		if (this.etfu$isElytraFlying()) {
			this.etfu$ticksElytraFlying += 1;
		} else {
			this.etfu$ticksElytraFlying = 0;
		}
	}

	private boolean etfu$hasLevitation() {
		return ModPotions.levitation != null && this.getActivePotionEffect(ModPotions.levitation) != null;
	}

	@Unique
	private double etfu$prevMotionX, etfu$prevMotionY, etfu$prevMotionZ;

	@Override
	public void etfu$setPrevMotion(double x, double y, double z) {
		etfu$prevMotionX = x;
		etfu$prevMotionY = y;
		etfu$prevMotionZ = z;
	}

	@Override
	public double etfu$getPrevMotionX() {
		return etfu$prevMotionX;
	}

	@Override
	public double etfu$getPrevMotionY() {
		return etfu$prevMotionY;
	}

	@Override
	public double etfu$getPrevMotionZ() {
		return etfu$prevMotionZ;
	}

	@Inject(method = "getEyeHeight", at = @At("HEAD"), cancellable = true)
	private void getElytraEyeHeight(CallbackInfoReturnable<Float> cir) {
		// Do NOT change getEyeHeight for elytra flight.
		// Changing it alters the stance value in C03PacketPlayer,
		// causing "Illegal stance" kicks when the server hasn't
		// processed the elytra flag yet.
		// The camera offset is handled in MixinEntityRenderer instead.
	}

	private float etfu$ticksElytraFlying = 0;
	private boolean etfu$lastElytraFlying = false;

	@Override
	public boolean etfu$isElytraFlying() {
		return getFlag(ConfigFunctions.elytraDataWatcherFlag);
	}

	@Override
	public void etfu$setElytraFlying(boolean flag) {
		setFlag(ConfigFunctions.elytraDataWatcherFlag, flag);
	}

	@Override
	public float etfu$getTicksElytraFlying() {
		return etfu$ticksElytraFlying;
	}

	@Override
	public boolean etfu$lastElytraFlying() {
		return etfu$lastElytraFlying;
	}

	@Override
	public void etfu$setLastElytraFlying(boolean flag) {
		etfu$lastElytraFlying = flag;
	}

	@Inject(method = "writeEntityToNBT", at = @At("TAIL"))
	private void writeElytra(NBTTagCompound tagCompound, CallbackInfo ci) {
		tagCompound.setBoolean("FallFlying", etfu$isElytraFlying());
	}

	@Inject(method = "readEntityFromNBT", at = @At("TAIL"))
	private void readElytra(NBTTagCompound tagCompound, CallbackInfo ci) {
		if (tagCompound.getBoolean("FallFlying"))
			etfu$setElytraFlying(true);
	}
}
