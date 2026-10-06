package ganymedes01.etfuturum.mixins.early.liquidphysics;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityItem.class, priority = 1002)
public abstract class MixinEntityItem extends Entity {

    public MixinEntityItem(World worldIn) {
        super(worldIn);
    }

    @Inject(method = "onUpdate", at = @At("HEAD"))
    private void floatItem(CallbackInfo ci) {
        if (isInFluid(this.posX, this.posY, this.posZ, this.worldObj)) {
            // 26.2 ItemEntity#tick: inside a fluid, gravity is REPLACED by
            // setUnderwaterMovement / setUnderLavaMovement (setFluidMovement) -
            // X/Z drag (0.99 water / 0.95 lava) plus a tiny upward creep
            // (motionY += 5e-4 while motionY < 0.06). There is NO velocity gate:
            // an item tossed into water never sinks to the bottom first, its
            // momentum just decays while it gently rises to the surface
            // (equilibrium of creep vs. drag is ~0.0245/tick upward). The +0.04
            // pre-cancels the vanilla gravity subtraction further down onUpdate,
            // so the net per-tick motion is exactly 26.2's setFluidMovement; the
            // vanilla post-move drag (motionY *= 0.98) mirrors 26.2's own.
            boolean lava = isLava(this.posX, this.posY, this.posZ, this.worldObj);
            double drag = lava ? 0.95D : 0.99D;
            this.motionX *= drag;
            this.motionZ *= drag;
            if (this.motionY < 0.06D) {
                this.motionY += 5.0E-4D + 0.03999999910593033D;
            }
        }
    }

    private static boolean isInFluid(double posX, double posY, double posZ, World world) {
        return getBlockAt(posX, posY, posZ, world).getMaterial().isLiquid();
    }

    private static boolean isLava(double posX, double posY, double posZ, World world) {
        return getBlockAt(posX, posY, posZ, world).getMaterial() == Material.lava;
    }

    private static Block getBlockAt(double posX, double posY, double posZ, World world) {
        return world.getBlock(MathHelper.floor_double(posX), MathHelper.floor_double(posY), MathHelper.floor_double(posZ));
    }
}
