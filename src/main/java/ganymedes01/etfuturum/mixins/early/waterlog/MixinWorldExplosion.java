package ganymedes01.etfuturum.mixins.early.waterlog;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ganymedes01.etfuturum.waterlog.WaterlogFlow;
import net.minecraft.entity.Entity;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

/**
 * 探针：一切原版路径爆炸的创建入口（{@code World.newExplosion}——TNT/
 * 苦力怕/mod 爆炸只要走原版 World API 都经此）。HEAD 注入，用于核对
 * "看得见爆炸但采样探针不响"时爆炸是否真的走 vanilla Explosion。
 */
@Mixin(World.class)
public abstract class MixinWorldExplosion {

	@Inject(method = "newExplosion(Lnet/minecraft/entity/Entity;DDDFZZ)Lnet/minecraft/world/Explosion;",
			at = @At("HEAD"))
	private void etfuturum$logExplosionCreation(Entity entity, double x, double y, double z, float size,
			boolean isFlaming, boolean isSmoking, CallbackInfoReturnable<Explosion> cir) {
		WaterlogFlow.noteExplosionCreate(entity, x, y, z, size, isSmoking);
	}
}
