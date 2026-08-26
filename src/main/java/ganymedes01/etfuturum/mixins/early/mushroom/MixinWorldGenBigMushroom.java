package ganymedes01.etfuturum.mixins.early.mushroom;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.gen.feature.WorldGenBigMushroom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 让红/棕蘑菇树在骨粉催熟生长时，可以把末地传送门框架当作可替换方块覆盖（破坏），
 * 用于还原高版本的刷沙机。
 * <p>
 * 不对空间检查做任何干预（含对传送门的检查），原版空间检查本身就存在漏洞，
 * 蘑菇在特定位置也能长出来；这里只保证树冠/菌柄放置阶段能替换掉末地传送门框架。
 */
@Mixin(WorldGenBigMushroom.class)
public abstract class MixinWorldGenBigMushroom {

	/**
	 * 放置树冠/菌柄前的可替换检查。框架虽然 {@code isOpaqueCube() == false}，
	 * 但 {@code canBeReplacedByLeaves} 实际调用的是返回 {@code this.opaque} 的
	 * {@code func_149730_j()}（未被框架覆盖，恒为 true），导致框架不可被蘑菇块替换。
	 * 这里对末地传送门框架放行，使其被树冠/菌柄覆盖；末地传送门方块保持不可替换。
	 */
	@WrapOperation(method = "generate",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;canBeReplacedByLeaves(Lnet/minecraft/world/IBlockAccess;III)Z", remap = false))
	private boolean etfuturum$canBeReplacedByLeaves(Block block, IBlockAccess world, int x, int y, int z, Operation<Boolean> original) {
		if (block == Blocks.end_portal_frame) {
			return true;
		}
		return original.call(block, world, x, y, z);
	}
}
