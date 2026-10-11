package ganymedes01.etfuturum.mixins.early.waterlog;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import ganymedes01.etfuturum.waterlog.WaterlogFlow;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

/**
 * 26.2 ServerExplosion 射线采样的中央抗性计算器
 * （ExplosionDamageCalculator#getBlockExplosionResistance =
 * {@code max(方块抗性, 流体抗性)}）：含水家族格与水生植物格的 FluidState 是
 * 水（抗性 100），射线一步耗掉 {@code (100 + 0.3) * 0.3 ≈ 30} 点威力——TNT 级
 * （~5 点）一格就停，含水方块进不了破坏名单（"含水墙防爆"）。
 *
 * <p>1.7.10 {@code doExplosionA} 的采样双分支（有 exploder 走
 * {@code Entity.func_145772_a} → 8 参 hook，无 exploder 直调 8 参 hook）都只
 * 报宿主方块抗性——两条各 @Redirect 后统一过
 * {@link WaterlogFlow#explosionResistance} 补齐 fluid 那一半；真实水/岩浆
 * 自身抗性已是 100，原值返回零行为差异。
 */
@Mixin(Explosion.class)
public abstract class MixinExplosion {

	@Redirect(method = "doExplosionA", at = @At(value = "INVOKE", remap = false,
			target = "Lnet/minecraft/entity/Entity;func_145772_a(Lnet/minecraft/world/Explosion;Lnet/minecraft/world/World;IIILnet/minecraft/block/Block;)F"))
	private float etfuturum$waterloggedResistance(Entity exploder, Explosion explosion, World world, int x, int y, int z, Block block) {
		WaterlogFlow.noteExplosionHook('A');
		WaterlogFlow.noteExplosionSample(world, x, y, z, block);
		return WaterlogFlow.explosionResistance(world, x, y, z, block,
				exploder.func_145772_a(explosion, world, x, y, z, block));
	}

	@Redirect(method = "doExplosionA", at = @At(value = "INVOKE", remap = false,
			target = "Lnet/minecraft/block/Block;getExplosionResistance(Lnet/minecraft/entity/Entity;Lnet/minecraft/world/World;IIIDDD)F"))
	private float etfuturum$waterloggedResistanceDirect(Block block, Entity exploder, World world, int x, int y, int z,
			double explosionX, double explosionY, double explosionZ) {
		WaterlogFlow.noteExplosionHook('B');
		WaterlogFlow.noteExplosionSample(world, x, y, z, block);
		return WaterlogFlow.explosionResistance(world, x, y, z, block,
				block.getExplosionResistance(exploder, world, x, y, z, explosionX, explosionY, explosionZ));
	}
}
