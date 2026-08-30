package ganymedes01.etfuturum.client.renderer.entity;

import ganymedes01.etfuturum.client.OpenGLHelper;
import ganymedes01.etfuturum.client.model.ModelArmorStand;
import ganymedes01.etfuturum.client.model.ModelArmorStandArmor;
import ganymedes01.etfuturum.entities.EntityArmourStand;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

public class ArmourStandRenderer extends RenderBiped {

	private static final ResourceLocation TEXTURE_ARMOUR_STAND = new ResourceLocation("textures/entity/armorstand/wood.png");

	public ArmourStandRenderer() {
		super(new ModelArmorStand(), 0.0F);
		modelBipedMain = (ModelBiped) mainModel;
		field_82423_g = new ModelArmorStandArmor(1.0F); // modelArmourChestplate
		field_82425_h = new ModelArmorStandArmor(0.5F);
	}

	@Override
	protected void func_82421_b() {
		field_82423_g = new ModelArmorStandArmor(1.0F); // modelArmourChestplate
		field_82425_h = new ModelArmorStandArmor(0.5F);
	}

	@Override
	protected void rotateCorpse(EntityLivingBase entity, float x, float y, float z) {
		OpenGLHelper.rotate(180.0F - y, 0.0F, 1.0F, 0.0F);
		if (entity instanceof EntityArmourStand stand) {
			// vanilla 26.2 wobble: sin(wiggle / 1.5 * PI) * 3° yaw shake for 5 ticks after a hit
			// z is the partialTicks argument (rotateCorpse's 4th param, x is limbSwing-related)
			float wiggle = (float) (entity.worldObj.getTotalWorldTime() - stand.getWobbleTime()) + z;
			if (stand.getWobbleTime() > 0 && wiggle < 5.0F)
				OpenGLHelper.rotate(MathHelper.sin(wiggle / 1.5F * (float) Math.PI) * 3.0F, 0.0F, 1.0F, 0.0F);
		}
	}

	@Override
	protected void preRenderCallback(EntityLivingBase entity, float partialTickTime) {
		if (entity instanceof EntityArmourStand stand && stand.isSmall())
			OpenGLHelper.scale(0.5F, 0.5F, 0.5F);
	}

	@Override
	public void doRender(EntityLivingBase entity, double x, double y, double z, float yaw, float partialTickTime) {
		// marker armor stands are invisible (vanilla still renders held items, simplified here)
		if (entity instanceof EntityArmourStand stand && stand.isMarker())
			return;
		super.doRender(entity, x, y, z, yaw, partialTickTime);
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity entity) {
		return TEXTURE_ARMOUR_STAND;
	}
}