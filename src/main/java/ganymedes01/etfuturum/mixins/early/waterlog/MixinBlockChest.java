package ganymedes01.etfuturum.mixins.early.waterlog;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ganymedes01.etfuturum.waterlog.WaterlogFlow;
import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

/**
 * 箱子扩族（26.2 ChestBlock：WATERLOGGED 完整支持，getStateForPlacement L238
 * 按 replacedFluidState 判定、updateShape 蔓延 L186/L253 scheduleTick）。
 * 与 MixinBlockStairs 同构的基类通用解——mod 木箱子类（含陷阱箱
 * Blocks.trappedChest = BlockChest(1) 独立实例）自动继承；直接继承
 * BlockContainer 的容器（铁箱扩展等）不在家族，26.2 同样不含它们。
 *
 * <p>1710 箱子 meta = 朝向 2-5（陷阱箱不占 meta 位），无掩码读取负担之外的
 * 位操作。bit16 落点审计：
 * <ul>
 * <li>onBlockPlacedBy（放置朝向重写，最多 3 处写：自身 + 双箱合并时的邻居格）
 * 与 func_149954_e（onBlockAdded 的双箱朝向修正）：整格 setBlockMetadataWithNotify
 * 重写 → @Redirect 写点并入目标格当前 meta 的 bit16（纯保留式，区别于楼梯的
 * isPlacementSource 并位——箱子会写邻居格，邻居可能自带含水，不能用放置捕获
 * 条件；同 ServerEventHandler.onPlaceBlock 活板门修复约定）；</li>
 * <li>func_149954_e 读邻居 meta 裸比较 j1 == 2 / j1 == 4（双箱朝向匹配）→
 * getBlockMetadata 调用点 & 15 剥离（同梯子渲染掩码约定）；</li>
 * <li>damageDropped 未声明（基类默认 0）→ 物品侧天然干净；breakBlock 掉落
 * 物品由 Forge setBlock→breakBlock 链路保证照常执行（leaveSourceWater 的
 * setBlock 触发）。</li>
 * </ul>
 *
 * <p>onNeighborBlockChange 是 vanilla 声明方法（super + TE 缓存失效）→
 * @Inject HEAD 保留原体 + 蔓延调度；updateTick/removedByPlayer 未声明 →
 * extends + @Override。
 */
@Mixin(BlockChest.class)
public abstract class MixinBlockChest extends Block {

	protected MixinBlockChest(Material material) {
		super(material);
	}

	/**
	 * 挖掘含水方块留下水源格——双载全覆写（6 参 = 服务端挖掘路径；5 参 = GTNH
	 * 客户端挖掘预测，默认 setBlockToAir），共用 {@link WaterlogFlow#leaveSourceWater}。
	 */
	@Override
	public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest) {
		return WaterlogFlow.leaveSourceWater(world, x, y, z) || super.removedByPlayer(world, player, x, y, z, willHarvest);
	}

	@Override
	public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z) {
		return WaterlogFlow.leaveSourceWater(world, x, y, z) || super.removedByPlayer(world, player, x, y, z);
	}

	@Override
	public void updateTick(World world, int x, int y, int z, Random random) {
		WaterlogFlow.handleFamilyTick(world, x, y, z, (Block) (Object) this);
	}

	/**
	 * 蔓延调度：vanilla 体（super + 邻居 TE 缓存失效）照常执行（@Inject 不取消）。
	 */
	@Inject(method = "onNeighborBlockChange(Lnet/minecraft/world/World;IIILnet/minecraft/block/Block;)V", at = @At("HEAD"))
	private void etfuturum$scheduleSpreadOnNeighbor(World world, int x, int y, int z, Block neighbor, CallbackInfo ci) {
		if (!world.isRemote && WaterlogRuntime.isWaterlogged(world, x, y, z)) {
			WaterlogFlow.scheduleSpread(world, x, y, z, (Block) (Object) this);
		}
	}

	/**
	 * bit16 纯保留式写点：onBlockPlacedBy 的全部 setBlockMetadataWithNotify
	 * （自身朝向 + 双箱合并的邻居格），目标格已有 bit16 则保留。门控关闭时
	 * bit16 恒为 0 → vanilla 行为逐位一致。
	 */
	@Redirect(method = "onBlockPlacedBy(Lnet/minecraft/world/World;IIILnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockMetadataWithNotify(IIIII)Z"))
	private boolean etfuturum$keepWaterlogOnPlacedBy(World world, int x, int y, int z, int meta, int flags) {
		return world.setBlockMetadataWithNotify(x, y, z, meta | (world.getBlockMetadata(x, y, z) & WaterlogRuntime.WATERLOGGED_BIT), flags);
	}

	/**
	 * 同上，func_149954_e 的双箱朝向修正写入（onBlockAdded 对自身与邻居各调
	 * 一次本方法，写点唯一）。
	 */
	@Redirect(method = "func_149954_e(Lnet/minecraft/world/World;III)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockMetadataWithNotify(IIIII)Z"))
	private boolean etfuturum$keepWaterlogOnChestMerge(World world, int x, int y, int z, int meta, int flags) {
		return world.setBlockMetadataWithNotify(x, y, z, meta | (world.getBlockMetadata(x, y, z) & WaterlogRuntime.WATERLOGGED_BIT), flags);
	}

	/**
	 * func_149954_e 的邻居 meta 裸比较（j1 == 2 / j1 == 4 双箱朝向匹配）：
	 * 含水邻居 meta 18/20 裸判失配 → 朝向退化默认值。调用点 & 15 剥离。
	 */
	@Redirect(method = "func_149954_e(Lnet/minecraft/world/World;III)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockMetadata(III)I"))
	private int etfuturum$maskMergeReadMeta(World world, int x, int y, int z) {
		return world.getBlockMetadata(x, y, z) & 15;
	}
}
