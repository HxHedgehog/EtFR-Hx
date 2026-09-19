package ganymedes01.etfuturum.mixins.early.scaffolding;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import ganymedes01.etfuturum.blocks.BlockScaffolding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 26.2 LivingEntity.handleOnClimbable scaffolding behavior on top of the
 * 1.7.10 ladder branch in moveEntityWithHeading:
 * - scaffolding is excluded from the sneak-stop-slide of climbables (sneaking descends);
 * - climbing up works with (horizontalCollision || jumping), where 1.7.10 only
 *   checks isCollidedHorizontally;
 * - scaffolding only counts as climbable while sneaking / touching a wall /
 *   jumping, so free falls through it keep their fall distance (fall damage).
 */
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase extends Entity {

	@Shadow
	protected boolean isJumping;

	public MixinEntityLivingBase(World worldIn) {
		super(worldIn);
	}

	@Definition(id = "isSneaking", method = "Lnet/minecraft/entity/EntityLivingBase;isSneaking()Z")
	@Expression("this.isSneaking()")
	@ModifyExpressionValue(method = "moveEntityWithHeading", at = @At(value = "MIXINEXTRAS:EXPRESSION"))
	private boolean etfu$disableSneakStopOnScaffolding(boolean original) {
		return original && !etfu$isOnScaffolding();
	}

	@Definition(id = "isCollidedHorizontally", field = "Lnet/minecraft/entity/EntityLivingBase;isCollidedHorizontally:Z")
	@Expression("this.isCollidedHorizontally")
	@ModifyExpressionValue(method = "moveEntityWithHeading", at = @At(value = "MIXINEXTRAS:EXPRESSION", ordinal = 2))
	private boolean etfu$expandClimbCondition(boolean original) {
		return original || (this.isJumping && etfu$isOnScaffolding());
	}

	@Definition(id = "isOnLadder", method = "Lnet/minecraft/entity/EntityLivingBase;isOnLadder()Z")
	@Expression("this.isOnLadder()")
	@ModifyExpressionValue(method = "moveEntityWithHeading", at = @At(value = "MIXINEXTRAS:EXPRESSION"))
	private boolean etfu$conditionClimbOnScaffolding(boolean original) {
		// 26.2: scaffolding only behaves as a climbable while descending by sneaking,
		// sliding along a wall (horizontal collision) or climbing up (jumping); a free
		// fall through the open centre must NOT zero the fall distance, so it deals
		// fall damage just like on 26.2 (whose airborne travel path never resets it).
		// Covers both isOnLadder() call sites in moveEntityWithHeading - the
		// slide/clamp branch (which sets fallDistance = 0) and the climb-up check.
		if (!etfu$isOnScaffolding()) {
			return original;
		}
		return this.isSneaking() || this.isJumping || this.isCollidedHorizontally;
	}

	private boolean etfu$isOnScaffolding() {
		int x = MathHelper.floor_double(this.posX);
		int y = MathHelper.floor_double(this.boundingBox.minY);
		int z = MathHelper.floor_double(this.posZ);
		return this.worldObj.getBlock(x, y, z) instanceof BlockScaffolding;
	}
}
