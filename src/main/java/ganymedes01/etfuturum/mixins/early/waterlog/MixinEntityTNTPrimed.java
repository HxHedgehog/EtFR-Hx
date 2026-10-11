package ganymedes01.etfuturum.mixins.early.waterlog;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ganymedes01.etfuturum.waterlog.WaterlogFlow;
import net.minecraft.entity.item.EntityTNTPrimed;

/**
 * 探针：TNT 生成（引信开始）与引信走完（explode → World.createExplosion）。
 * 与 MixinExplosion 的采样探针分离，用于判别"TNT 爆了但采样探针没响"时断点
 * 在哪一环：SPAWN 响而 BOOM 不响 = 引信中途被移除/熄灭；SPAWN 都不响 =
 * 原版 TNT 实体根本没生成（点燃方式走的非原版实体）。
 */
@Mixin(EntityTNTPrimed.class)
public abstract class MixinEntityTNTPrimed {

	@Inject(method = "<init>(Lnet/minecraft/world/World;DDDLnet/minecraft/entity/EntityLivingBase;)V", at = @At("RETURN"))
	private void etfuturum$logTntSpawn(CallbackInfo ci) {
		WaterlogFlow.noteTntSpawn((Object) this);
	}

	@Inject(method = "explode()V", at = @At("HEAD"))
	private void etfuturum$logTntBoom(CallbackInfo ci) {
		WaterlogFlow.noteTntBoom((Object) this);
	}
}
