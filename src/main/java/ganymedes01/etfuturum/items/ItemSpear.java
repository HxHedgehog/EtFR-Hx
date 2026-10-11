package ganymedes01.etfuturum.items;

import java.util.UUID;
import java.util.function.Supplier;

import com.google.common.collect.Multimap;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.client.handler.SpearClientTickHandler;
import ganymedes01.etfuturum.core.utils.Utils;
import ganymedes01.etfuturum.lib.Reference;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

/**
 * Spear from modern Minecraft, mirroring Item.Properties.spear(material, attackDuration,
 * damageMultiplier, delay, dismountTime, dismountThreshold, knockbackTime, knockbackThreshold,
 * damageTime, damageThreshold) with the per-tier numbers of Items.java L1198-1204 (ticks).
 *
 * Right-click stab: 26.2 runs KineticWeapon.damageEntities server side every use tick, but the
 * 1.7.10 server never learns player motion, so the local client detects hits, projects both speeds
 * along the look vector and reports them; the server re-tests the conditions before settling
 * (SpearStabHandler). Movement is not slowed while using (UseEffects canSprint/speedMultiplier 1.0)
 * and the wound-up spear deals damage scaled by the relative speed.
 */
public class ItemSpear extends Item {

	public static final int USE_DURATION = 72000; // 26.2 KineticWeapon useDuration

	private final String name;
	private final Supplier<ItemStack> repairMaterial;
	private final int attackDurationTicks; // 26.2 SwingAnimation(STAB, attackDuration * 20): the left-click stab length = the attack duration
	private final float damageMultiplier;
	private final float attackDamageBonus; // 26.2 ToolMaterial.attackDamageBonus, shown in the tooltip like the official attribute modifier
	private final int delayTicks;
	private final int dismountTicks;
	private final float dismountMinSpeed;
	private final int knockbackTicks;
	private final float knockbackMinSpeed;
	private final int damageTicks;
	private final float damageMinRelativeSpeed;
	private final String soundPrefix; // "spear" for every tier except wood, which uses "spear_wood"

	@SideOnly(Side.CLIENT)
	private IIcon inHandIcon;

	public ItemSpear(String name, int uses, Supplier<ItemStack> repairMaterial, int attackDurationTicks, float damageMultiplier, float attackDamageBonus, int delayTicks, int dismountTicks, float dismountMinSpeed, int knockbackTicks, float knockbackMinSpeed, int damageTicks, float damageMinRelativeSpeed, String soundPrefix) {
		this.name = name;
		this.repairMaterial = repairMaterial;
		this.attackDurationTicks = attackDurationTicks;
		this.damageMultiplier = damageMultiplier;
		this.attackDamageBonus = attackDamageBonus;
		this.delayTicks = delayTicks;
		this.dismountTicks = dismountTicks;
		this.dismountMinSpeed = dismountMinSpeed;
		this.knockbackTicks = knockbackTicks;
		this.knockbackMinSpeed = knockbackMinSpeed;
		this.damageTicks = damageTicks;
		this.damageMinRelativeSpeed = damageMinRelativeSpeed;
		this.soundPrefix = soundPrefix;
		setUnlocalizedName(Utils.getUnlocalisedName(name));
		setTextureName(name);
		setMaxDamage(uses); // also caps max stack size at 1, like official spears
	}

	/** Vanilla tool modifier UUID (CB3F55D3...), the same slot ItemSword registers its damage under. */
	private static final UUID ATTACK_DAMAGE_MODIFIER_UUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");

	/**
	 * 26.2 Properties.spear registers the ATTACK_DAMAGE modifier 0.0 + material.attackDamageBonus on
	 * MAINHAND. EntityLivingBase applies held-item modifiers on equipment change (the 1.7.10
	 * previousEquipment block), so the jab settles from the attribute value exactly like the
	 * official getAttributeValue(ATTACK_DAMAGE) - strength potions included, same as any weapon.
	 */
	@Override
	public Multimap<String, AttributeModifier> getAttributeModifiers(ItemStack stack) {
		Multimap<String, AttributeModifier> modifiers = super.getAttributeModifiers(stack);
		modifiers.put(SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName(), new AttributeModifier(ATTACK_DAMAGE_MODIFIER_UUID, "Weapon modifier", this.attackDamageBonus, 0));
		return modifiers;
	}

	public float getDamageMultiplier() {
		return damageMultiplier;
	}

	public float getAttackDamageBonus() {
		return attackDamageBonus;
	}

	public int getAttackDurationTicks() {
		return attackDurationTicks;
	}

	public int getDelayTicks() {
		return delayTicks;
	}

	public int getDismountTicks() {
		return dismountTicks;
	}

	public float getDismountMinSpeed() {
		return dismountMinSpeed;
	}

	public int getKnockbackTicks() {
		return knockbackTicks;
	}

	public float getKnockbackMinSpeed() {
		return knockbackMinSpeed;
	}

	public int getDamageTicks() {
		return damageTicks;
	}

	public float getDamageMinRelativeSpeed() {
		return damageMinRelativeSpeed;
	}

	public String getSoundPrefix() {
		return soundPrefix;
	}

	@Override
	public EnumAction getItemUseAction(ItemStack stack) {
		return EnumAction.none; // 26.2 spears keep the vanilla pose; the arm animation is driven by ArmPose.SPEAR
	}

	@Override
	public int getMaxItemUseDuration(ItemStack stack) {
		return USE_DURATION;
	}

	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		player.setItemInUse(stack, getMaxItemUseDuration(stack));
		if (!world.isRemote) {
			world.playSoundAtEntity(player, Reference.MOD_ID + ":" + soundPrefix + ".use", 1.0F, 1.0F); // 26.2 KineticWeapon.makeSound on use
		}
		return stack;
	}

	@Override
	public void onUsingTick(ItemStack stack, EntityPlayer player, int count) {
		if (player.worldObj.isRemote) {
			SpearClientTickHandler.onSpearUseTick(this, stack, player, count); // local-player hit detection + speed report
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister register) {
		super.registerIcons(register); // flat {tier}_spear icon: gui/ground/fixed/on_shelf contexts
		this.inHandIcon = register.registerIcon(name + "_in_hand"); // fallback context: held in hand
	}

	/**
	 * The wide {tier}_spear_in_hand sprite, consumed by ItemSpearRenderer.
	 */
	@SideOnly(Side.CLIENT)
	public IIcon getInHandIcon() {
		return inHandIcon;
	}

	/**
	 * Long-shaft weapon, rendered like swords in third person (RenderBiped isFull3D branch:
	 * S(0.625,-0.625,0.625) R(-100X) R(45Y) in front of the IItemRenderer call).
	 */
	@Override
	public boolean isFull3D() {
		return true;
	}

	@Override
	public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
		return repair.getItem() == repairMaterial.get().getItem() || super.getIsRepairable(toRepair, repair);
	}
}
