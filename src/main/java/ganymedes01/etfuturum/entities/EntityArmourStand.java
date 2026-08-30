package ganymedes01.etfuturum.entities;

import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.lib.Reference;
import ganymedes01.etfuturum.network.ArmourStandInteractMessage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.play.server.S2APacketParticles;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.List;

public class EntityArmourStand extends EntityLiving {

	private static final Rotations DEFAULT_HEAD_ROTATION = new Rotations(0.0F, 0.0F, 0.0F);
	private static final Rotations DEFAULT_BODY_ROTATION = new Rotations(0.0F, 0.0F, 0.0F);
	private static final Rotations DEFAULT_LEFTARM_ROTATION = new Rotations(-10.0F, 0.0F, -10.0F);
	private static final Rotations DEFAULT_RIGHTARM_ROTATION = new Rotations(-15.0F, 0.0F, 10.0F);
	private static final Rotations DEFAULT_LEFTLEG_ROTATION = new Rotations(-1.0F, 0.0F, -1.0F);
	private static final Rotations DEFAULT_RIGHTLEG_ROTATION = new Rotations(1.0F, 0.0F, 1.0F);
	private boolean canInteract;
	private long punchCooldown;
	// wobble start time in ticks (mirror of vanilla lastHit); set on both sides via the entity-state event 32
	private int wobbleTime;
	private boolean wasSmall;
	private boolean wasMarker;
	private int disabledSlots;
	private Rotations headRotation;
	private Rotations bodyRotation;
	private Rotations leftArmRotation;
	private Rotations rightArmRotation;
	private Rotations leftLegRotation;
	private Rotations rightLegRotation;

	public EntityArmourStand(World world) {
		super(world);
		headRotation = DEFAULT_HEAD_ROTATION;
		bodyRotation = DEFAULT_BODY_ROTATION;
		leftArmRotation = DEFAULT_LEFTARM_ROTATION;
		rightArmRotation = DEFAULT_RIGHTARM_ROTATION;
		leftLegRotation = DEFAULT_LEFTLEG_ROTATION;
		rightLegRotation = DEFAULT_RIGHTLEG_ROTATION;
		noClip = hasNoGravity();
		setSize(0.5F, 1.975F);
	}

	public EntityArmourStand(World world, double posX, double posY, double posZ) {
		this(world);
		setPosition(posX, posY, posZ);
	}

	@Override
	protected void entityInit() {
		super.entityInit();
		addRotationsToDataWatcher(12, DEFAULT_HEAD_ROTATION);
		addRotationsToDataWatcher(15, DEFAULT_BODY_ROTATION);
		addRotationsToDataWatcher(18, DEFAULT_LEFTARM_ROTATION);
		addRotationsToDataWatcher(21, DEFAULT_RIGHTARM_ROTATION);
		addRotationsToDataWatcher(24, DEFAULT_LEFTLEG_ROTATION);
		addRotationsToDataWatcher(27, DEFAULT_RIGHTLEG_ROTATION);
		dataWatcher.addObject(30, (byte) 0);
		func_110163_bv(); // enablePersistence
	}

	private void addRotationsToDataWatcher(int index, Rotations rotations) {
		dataWatcher.addObject(index, rotations.getX());
		dataWatcher.addObject(index + 1, rotations.getY());
		dataWatcher.addObject(index + 2, rotations.getZ());
	}

	@Override
	protected void updateEntityActionState() {
	}

	@Override
	public ItemStack getPickedResult(MovingObjectPosition target) {
		return new ItemStack(ModItems.WOODEN_ARMORSTAND.get());
	}

	public ItemStack getCurrentArmor(int slotIn) {
		return getEquipmentInSlot(slotIn + 1);
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound nbt) {
		super.writeEntityToNBT(nbt);

		nbt.setBoolean("Invisible", isInvisible());
		nbt.setBoolean("Small", isSmall());
		nbt.setBoolean("ShowArms", getShowArms());
		nbt.setBoolean("NoGravity", hasNoGravity());
		nbt.setBoolean("NoBasePlate", hasNoBasePlate());
		nbt.setInteger("DisabledSlots", disabledSlots);
		if (isMarker())
			nbt.setBoolean("Marker", true);
		nbt.setTag("Pose", readPoseFromNBT());
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound nbt) {
		super.readEntityFromNBT(nbt);

		setInvisible(nbt.getBoolean("Invisible"));
		setSmall(nbt.getBoolean("Small"));
		setShowArms(nbt.getBoolean("ShowArms"));
		setNoGravity(nbt.getBoolean("NoGravity"));
		setNoBasePlate(nbt.getBoolean("NoBasePlate"));
		disabledSlots = nbt.getInteger("DisabledSlots");
		setMarker(nbt.getBoolean("Marker"));
		noClip = hasNoGravity() || isMarker();
		updateSize();
		NBTTagCompound nbttagcompound1 = nbt.getCompoundTag("Pose");
		writePoseToNBT(nbttagcompound1);
	}

	private void writePoseToNBT(NBTTagCompound tagCompound) {
		NBTTagList nbttaglist = tagCompound.getTagList("Head", 5);

		if (nbttaglist.tagCount() > 0)
			setHeadRotation(new Rotations(nbttaglist));
		else
			setHeadRotation(DEFAULT_HEAD_ROTATION);

		NBTTagList nbttaglist1 = tagCompound.getTagList("Body", 5);

		if (nbttaglist1.tagCount() > 0)
			setBodyRotation(new Rotations(nbttaglist1));
		else
			setBodyRotation(DEFAULT_BODY_ROTATION);

		NBTTagList nbttaglist2 = tagCompound.getTagList("LeftArm", 5);

		if (nbttaglist2.tagCount() > 0)
			setLeftArmRotation(new Rotations(nbttaglist2));
		else
			setLeftArmRotation(DEFAULT_LEFTARM_ROTATION);

		NBTTagList nbttaglist3 = tagCompound.getTagList("RightArm", 5);

		if (nbttaglist3.tagCount() > 0)
			setRightArmRotation(new Rotations(nbttaglist3));
		else
			setRightArmRotation(DEFAULT_RIGHTARM_ROTATION);

		NBTTagList nbttaglist4 = tagCompound.getTagList("LeftLeg", 5);

		if (nbttaglist4.tagCount() > 0)
			setLeftLegRotation(new Rotations(nbttaglist4));
		else
			setLeftLegRotation(DEFAULT_LEFTLEG_ROTATION);

		NBTTagList nbttaglist5 = tagCompound.getTagList("RightLeg", 5);

		if (nbttaglist5.tagCount() > 0)
			setRightLegRotation(new Rotations(nbttaglist5));
		else
			setRightLegRotation(DEFAULT_RIGHTLEG_ROTATION);
	}

	private NBTTagCompound readPoseFromNBT() {
		NBTTagCompound nbt = new NBTTagCompound();

		if (!DEFAULT_HEAD_ROTATION.equals(headRotation))
			nbt.setTag("Head", headRotation.writeToNBT());
		if (!DEFAULT_BODY_ROTATION.equals(bodyRotation))
			nbt.setTag("Body", bodyRotation.writeToNBT());
		if (!DEFAULT_LEFTARM_ROTATION.equals(leftArmRotation))
			nbt.setTag("LeftArm", leftArmRotation.writeToNBT());
		if (!DEFAULT_RIGHTARM_ROTATION.equals(rightArmRotation))
			nbt.setTag("RightArm", rightArmRotation.writeToNBT());
		if (!DEFAULT_LEFTLEG_ROTATION.equals(leftLegRotation))
			nbt.setTag("LeftLeg", leftLegRotation.writeToNBT());
		if (!DEFAULT_RIGHTLEG_ROTATION.equals(rightLegRotation))
			nbt.setTag("RightLeg", rightLegRotation.writeToNBT());

		return nbt;
	}

	@Override
	public boolean canBePushed() {
		return false;
	}

	@Override
	protected void collideWithEntity(Entity entity) {
	}

	@Override
	protected void collideWithNearbyEntities() {
		List<Entity> list = worldObj.getEntitiesWithinAABBExcludingEntity(this, boundingBox.expand(0.2, 0, 0.2));

		if (list != null && !list.isEmpty())
			for (int i = 0; i < list.size(); i++) {
				Entity entity = list.get(i);

				if (entity instanceof EntityMinecart && ((EntityMinecart) entity).getMinecartType() == 0)
					entity.applyEntityCollision(this);
			}

	}

	@Override
	public boolean interact(EntityPlayer player) {
		if (worldObj.isRemote) {
			EtFuturum.networkWrapper.sendToServer(new ArmourStandInteractMessage(worldObj.provider.dimensionId, this, player));
			return true;
		}
		return false;
	}

	public boolean interact(EntityPlayer player, Vec3 hitPos) {
		if (!worldObj.isRemote) {
			// vanilla lets the name tag's own item logic handle interaction on markers
			byte b0 = 0;
			ItemStack itemstack = player.getCurrentEquippedItem();
			if (itemstack != null && itemstack.getItem() == Items.name_tag)
				return false;
			// mod feature: sneaking + use toggles ShowArms (placement default is armless);
			// hiding the arms ejects the held item (slot 0; slots 1-4 are armor in 1.7.10)
			if (player.isSneaking()) {
				if (getShowArms() && getEquipmentInSlot(0) != null) {
					entityDropItem(getEquipmentInSlot(0), 0.0F);
					setCurrentItemOrArmor(0, null);
				}
				setShowArms(!getShowArms());
				return true;
			}
			if (isMarker())
				return true;
			if (itemstack != null) {
				if (itemstack.getItem() instanceof ItemArmor itemarmor) {

					if (itemarmor.armorType == 3)
						b0 = 1;
					else if (itemarmor.armorType == 2)
						b0 = 2;
					else if (itemarmor.armorType == 1)
						b0 = 3;
					else if (itemarmor.armorType == 0)
						b0 = 4;
				}
				if (itemstack.getItem() == Items.skull || itemstack.getItem() == Item.getItemFromBlock(Blocks.pumpkin))
					b0 = 4;
			}


			byte b1 = 0;
			boolean isSmall = isSmall();
			double d3 = isSmall ? hitPos.yCoord * 2.0D : hitPos.yCoord;

			if (d3 >= 0.1D && d3 < 0.1D + (isSmall ? 0.8D : 0.45D) && getEquipmentInSlot(1) != null)
				b1 = 1;
			else if (d3 >= 0.9D + (isSmall ? 0.3D : 0.0D) && d3 < 0.9D + (isSmall ? 1.0D : 0.7D) && getEquipmentInSlot(3) != null)
				b1 = 3;
			else if (d3 >= 0.4D && d3 < 0.4D + (isSmall ? 1.0D : 0.8D) && getEquipmentInSlot(2) != null)
				b1 = 2;
			else if (d3 >= 1.6D && getEquipmentInSlot(4) != null)
				b1 = 4;

			boolean flag2 = getEquipmentInSlot(b1) != null;

			if (itemstack != null && b0 == 0 && !getShowArms())
				return true;
			int targetSlot = itemstack != null ? b0 : (flag2 ? b1 : 0);
			if (isSlotDisabled(targetSlot, itemstack != null))
				return true;
			if (itemstack != null)
				func_175422_a(player, b0);
			else if (flag2)
				func_175422_a(player, b1);

			return true;
		}
		return true;
	}

	/**
	 * Vanilla 26.2 DisabledSlots bit layout: bit (1 &lt;&lt; slot) = interaction locked,
	 * bit (1 &lt;&lt; (slot + 8)) = taking locked, bit (1 &lt;&lt; (slot + 16)) = putting locked.
	 * Slot indices match 1.7.10 equipment slots (0 = held item, 1-4 = armor).
	 */
	public boolean isSlotDisabled(int slot, boolean putting) {
		if (putting)
			return (disabledSlots & 1 << slot) != 0 || (disabledSlots & 1 << (slot + 16)) != 0;
		return (disabledSlots & 1 << slot) != 0 || (disabledSlots & 1 << (slot + 8)) != 0;
	}

	private void func_175422_a(EntityPlayer player, int slot) {
		ItemStack itemstack = getEquipmentInSlot(slot);

		int j = player.inventory.currentItem;
		ItemStack itemstack1 = player.inventory.getStackInSlot(j);
		ItemStack itemstack2;

		if (player.capabilities.isCreativeMode && (itemstack == null || itemstack.getItem() == Item.getItemFromBlock(Blocks.air)) && itemstack1 != null) {
			itemstack2 = itemstack1.copy();
			itemstack2.stackSize = 1;
			setCurrentItemOrArmor(slot, itemstack2);
		} else if (itemstack1 != null && itemstack1.stackSize > 1) {
			if (itemstack == null) {
				itemstack2 = itemstack1.copy();
				itemstack2.stackSize = 1;
				setCurrentItemOrArmor(slot, itemstack2);
				itemstack1.stackSize--;
			}
		} else {
			setCurrentItemOrArmor(slot, itemstack1);
			player.inventory.setInventorySlotContents(j, itemstack);
		}

		// vanilla 26.2 LivingEntity.onEquipItem: play the equip sound when the slot content changes
		playEquipSound(itemstack1);
	}

	/**
	 * Vanilla 26.2 LivingEntity.onEquipItem: plays the equip sound for the new stack
	 * (empty stack = unequip = silent). 26.2 derives the sound from the EQUIPPABLE data
	 * component (material-specific, default generic); on 1.7.10 the ArmorMaterial of
	 * ItemArmor maps to the same sound events, non-armor wearables (skulls, pumpkins)
	 * fall back to the generic sound.
	 */
	private void playEquipSound(ItemStack newStack) {
		if (newStack == null)
			return;
		playSound(Reference.MCAssetVer + ":item.armor.equip_" + getEquipSoundSuffix(newStack), 1.0F, 1.0F);
	}

	private static String getEquipSoundSuffix(ItemStack itemstack) {
		if (itemstack.getItem() instanceof ItemArmor armor)
			switch (armor.getArmorMaterial()) {
				case CLOTH:
					return "leather";
				case CHAIN:
					return "chain";
				case IRON:
					return "iron";
				case GOLD:
					return "gold";
				case DIAMOND:
					return "diamond";
				default:
					return "generic";
			}
		return "generic";
	}

	@Override
	public boolean attackEntityFrom(DamageSource source, float amount) {
		if (!worldObj.isRemote && !canInteract) {
			if (DamageSource.outOfWorld.equals(source)) {
				setDead();
				return false;
			} else if (isEntityInvulnerable())
				return false;
			// vanilla mobGriefing gate: damage caused by mobs is ignored when disabled
			else if (source.getEntity() instanceof EntityLiving && !worldObj.getGameRules().getGameRuleBooleanValue("mobGriefing"))
				return false;
			else if (source.isExplosion()) {
				playBrokenSound();
				dropequipment();
				setDead();
				return false;
			} else if (DamageSource.inFire.equals(source)) {
				if (!isBurning())
					setFire(5);
				else
					damageArmorStand(0.15F);

				return false;
			} else if (DamageSource.onFire.equals(source) && getHealth() > 0.5F) {
				damageArmorStand(4.0F);
				return false;
			} else {
				boolean flag = "arrow".equals(source.getDamageType());
				boolean flag1 = "player".equals(source.getDamageType());

				if (!flag1 && !flag)
					return false;
				if (source.getSourceOfDamage() instanceof EntityArrow)
					source.getSourceOfDamage().setDead();

				if (source.getEntity() instanceof EntityPlayer && !((EntityPlayer) source.getEntity()).capabilities.allowEdit)
					return false;
				else if (source.getEntity() instanceof EntityPlayer && ((EntityPlayer) source.getEntity()).capabilities.isCreativeMode) {
					playBrokenSound();
					playParticles();
					setDead();
					return false;
				} else {
					long i = worldObj.getTotalWorldTime();

					if (i - punchCooldown <= 5L || flag) {
						dropBlock();
						playParticles();
						setDead();
					} else {
						punchCooldown = i;
						// vanilla 26.2 broadcasts entity event 32: client plays the hit sound and starts the wobble
						worldObj.setEntityState(this, (byte) 32);
					}

					return false;
				}
			}
		}
		return false;
	}

	/**
	 * Vanilla 26.2 showBreakingParticles: 10 oak plank block dust particles with AABB spread.
	 * 1.7.10's server-side World.spawnParticle is a no-op, so the S2A particle packet is
	 * sent directly to nearby players (blockcrack_5_0 = oak planks).
	 */
	private void playParticles() {
		S2APacketParticles packet = new S2APacketParticles("blockcrack_5_0", (float) posX, (float) (posY + 0.667D), (float) posZ, width / 4.0F, height / 4.0F, width / 4.0F, 0.05F, 10);

		for (Object obj : worldObj.playerEntities)
			if (obj instanceof EntityPlayerMP player && player.getDistanceSqToEntity(this) < 1024.0D)
				player.playerNetServerHandler.sendPacket(packet);
	}

	private void playBrokenSound() {
		playSound(Reference.MCAssetVer + ":entity.armor_stand.break", 1.0F, 1.0F);
	}

	/**
	 * Vanilla 26.2 handleEntityEvent(32): on the client, play the hit sound (0.3 / 1.0)
	 * and record the wobble start time. Uses the 7-arg playSound because WorldClient only
	 * overrides that one (playSoundEffect dead-ends in RenderGlobal's empty impl).
	 */
	@Override
	public void handleHealthUpdate(byte id) {
		if (id == 32) {
			if (worldObj.isRemote) {
				worldObj.playSound(posX, posY, posZ, Reference.MCAssetVer + ":entity.armor_stand.hit", 0.3F, 1.0F, false);
				wobbleTime = (int) worldObj.getTotalWorldTime();
			}
		} else
			super.handleHealthUpdate(id);
	}

	private void damageArmorStand(float p_175406_1_) {
		float f1 = getHealth();
		f1 -= p_175406_1_;

		if (f1 <= 0.5F) {
			playBrokenSound();
			dropequipment();
			setDead();
		} else
			setHealth(f1);
	}

	private void dropBlock() {
		playBrokenSound();
		ItemStack drop = new ItemStack(ModItems.WOODEN_ARMORSTAND.get());
		// vanilla 26.2 keeps the custom name on the dropped item
		if (hasCustomNameTag())
			drop.setStackDisplayName(getCustomNameTag());
		entityDropItem(drop, 0.0F);
		dropequipment();
	}

	public int getWobbleTime() {
		return wobbleTime;
	}

	private void dropequipment() {
		for (int i = 0; i < 5; i++)
			if (getEquipmentInSlot(i) != null && getEquipmentInSlot(i).stackSize > 0)
				if (getEquipmentInSlot(i) != null) {
					entityDropItem(getEquipmentInSlot(i), 0.0F);
					setCurrentItemOrArmor(i, null);
				}
	}

	@Override
	protected float func_110146_f(float p_110146_1_, float p_110146_2_) {
		prevRenderYawOffset = prevRotationYaw;
		renderYawOffset = rotationYaw;
		return 0.0F;
	}

	@Override
	public float getEyeHeight() {
		return isChild() ? height * 0.5F : height * 0.9F;
	}

	@Override
	public void moveEntityWithHeading(float p_70612_1_, float p_70612_2_) {
		if (!hasNoGravity()) {
			double prevY = posY;
			super.moveEntityWithHeading(p_70612_1_, p_70612_2_);
			// vanilla 26.2 plays the fall sound on landing
			if (onGround && !worldObj.isRemote && prevY - posY > 0.05D)
				playSound(Reference.MCAssetVer + ":entity.armor_stand.fall", 1.0F, 1.0F);
		}
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		if (isSmall() != wasSmall || isMarker() != wasMarker)
			updateSize();
		Rotations rotations = getRotations(12);
		if (!headRotation.equals(rotations))
			setHeadRotation(rotations);

		Rotations rotations1 = getRotations(15);
		if (!bodyRotation.equals(rotations1))
			setBodyRotation(rotations1);

		Rotations rotations2 = getRotations(18);
		if (!leftArmRotation.equals(rotations2))
			setLeftArmRotation(rotations2);

		Rotations rotations3 = getRotations(21);
		if (!rightArmRotation.equals(rotations3))
			setRightArmRotation(rotations3);

		Rotations rotations4 = getRotations(24);
		if (!leftLegRotation.equals(rotations4))
			setLeftLegRotation(rotations4);

		Rotations rotations5 = getRotations(27);
		if (!rightLegRotation.equals(rotations5))
			setRightLegRotation(rotations5);
	}

	private Rotations getRotations(int index) {
		return new Rotations(dataWatcher.getWatchableObjectFloat(index), dataWatcher.getWatchableObjectFloat(index + 1), dataWatcher.getWatchableObjectFloat(index + 2));
	}

	@Override
	public void setInvisible(boolean invisible) {
		canInteract = invisible;
		super.setInvisible(invisible);
	}

	@Override
	public boolean isChild() {
		return isSmall();
	}

	private void setSmall(boolean p_175420_1_) {
		byte b0 = dataWatcher.getWatchableObjectByte(30);

		if (p_175420_1_)
			b0 = (byte) (b0 | 1);
		else
			b0 &= -2;

		dataWatcher.updateObject(30, b0);
	}

	public boolean isSmall() {
		return (dataWatcher.getWatchableObjectByte(30) & 1) != 0;
	}

	private void setNoGravity(boolean p_175425_1_) {
		byte b0 = dataWatcher.getWatchableObjectByte(30);

		if (p_175425_1_)
			b0 = (byte) (b0 | 2);
		else
			b0 &= -3;

		dataWatcher.updateObject(30, b0);
	}

	public boolean hasNoGravity() {
		return (dataWatcher.getWatchableObjectByte(30) & 2) != 0;
	}

	private void setShowArms(boolean p_175413_1_) {
		byte b0 = dataWatcher.getWatchableObjectByte(30);

		if (p_175413_1_)
			b0 = (byte) (b0 | 4);
		else
			b0 &= -5;

		dataWatcher.updateObject(30, b0);
	}

	public boolean getShowArms() {
		return (dataWatcher.getWatchableObjectByte(30) & 4) != 0;
	}

	private void setNoBasePlate(boolean p_175426_1_) {
		byte b0 = dataWatcher.getWatchableObjectByte(30);

		if (p_175426_1_)
			b0 = (byte) (b0 | 8);
		else
			b0 &= -9;

		dataWatcher.updateObject(30, b0);
	}

	public boolean hasNoBasePlate() {
		return (dataWatcher.getWatchableObjectByte(30) & 8) != 0;
	}

	public void setMarker(boolean value) {
		byte b0 = dataWatcher.getWatchableObjectByte(30);

		if (value)
			b0 = (byte) (b0 | 16);
		else
			b0 &= -17;

		dataWatcher.updateObject(30, b0);
		noClip = hasNoGravity() || value;
	}

	public boolean isMarker() {
		return (dataWatcher.getWatchableObjectByte(30) & 16) != 0;
	}

	/**
	 * Mirrors vanilla refreshDimensions: swaps the collision box between
	 * normal (0.5×1.975), small (0.25×0.9875) and marker (0×0) states.
	 */
	private void updateSize() {
		float w = 0.5F;
		float h = 1.975F;
		if (isMarker()) {
			w = 0.0F;
			h = 0.0F;
		} else if (isSmall()) {
			w = 0.25F;
			h = 0.9875F;
		}
		setSize(w, h);
		wasSmall = isSmall();
		wasMarker = isMarker();
	}

	@Override
	public boolean canBeCollidedWith() {
		return super.canBeCollidedWith() && !isMarker();
	}

	public void setHeadRotation(Rotations p_175415_1_) {
		headRotation = p_175415_1_;
		updateRotations(12, p_175415_1_);
	}

	public void setBodyRotation(Rotations p_175424_1_) {
		bodyRotation = p_175424_1_;
		updateRotations(15, p_175424_1_);
	}

	public void setLeftArmRotation(Rotations p_175405_1_) {
		leftArmRotation = p_175405_1_;
		updateRotations(18, p_175405_1_);
	}

	public void setRightArmRotation(Rotations p_175428_1_) {
		rightArmRotation = p_175428_1_;
		updateRotations(21, p_175428_1_);
	}

	public void setLeftLegRotation(Rotations p_175417_1_) {
		leftLegRotation = p_175417_1_;
		updateRotations(24, p_175417_1_);
	}

	public void setRightLegRotation(Rotations p_175427_1_) {
		rightLegRotation = p_175427_1_;
		updateRotations(27, p_175427_1_);
	}

	private void updateRotations(int index, Rotations rotations) {
		dataWatcher.updateObject(index, rotations.getX());
		dataWatcher.updateObject(index + 1, rotations.getY());
		dataWatcher.updateObject(index + 2, rotations.getZ());
	}

	public Rotations getHeadRotation() {
		return headRotation;
	}

	public Rotations getBodyRotation() {
		return bodyRotation;
	}

	public Rotations getLeftArmRotation() {
		return leftArmRotation;
	}

	public Rotations getRightArmRotation() {
		return rightArmRotation;
	}

	public Rotations getLeftLegRotation() {
		return leftLegRotation;
	}

	public Rotations getRightLegRotation() {
		return rightLegRotation;
	}

	@Override
	protected void updatePotionEffects() {
	}
}