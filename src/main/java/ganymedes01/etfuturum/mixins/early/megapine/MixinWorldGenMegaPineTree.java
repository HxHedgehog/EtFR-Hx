package ganymedes01.etfuturum.mixins.early.megapine;

import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraft.world.gen.feature.WorldGenMegaPineTree;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

/**
 * 大型云杉（2x2）长成后将下方土系方块转化为灰化土。
 * <p>
 * 官方 26.2 MEGA_SPRUCE / MEGA_PINE 的 AlterGroundDecorator 是树配置的一部分，
 * 树无论怎么生成都会放置灰化土。但 1.7.10 原版只在世界生成（BiomeDecorator/BiomeGenForest）
 * 里调用 WorldGenMegaPineTree.func_150524_b，玩家用 2x2 云杉树苗催熟长成的树从未调用过该方法。
 * 因此在此处补上：generate 成功（返回 true）后调用 func_150524_b 放置灰化土。
 */
@Mixin(WorldGenMegaPineTree.class)
public abstract class MixinWorldGenMegaPineTree extends WorldGenAbstractTree {

	protected MixinWorldGenMegaPineTree(boolean doBlockNotify) {
		super(doBlockNotify);
	}

	@Inject(method = "generate", at = @At(value = "RETURN"))
	private void placePodzolWhenGrown(World world, Random rand, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValue()) {
			// 官方 AlterGroundDecorator 的 1.7.10 等效实现（原版 func_150524_b）
			this.func_150524_b(world, rand, x, y, z);
		}
	}
}
