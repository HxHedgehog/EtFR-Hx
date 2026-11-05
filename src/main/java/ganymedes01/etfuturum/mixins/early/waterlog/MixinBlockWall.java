package ganymedes01.etfuturum.mixins.early.waterlog;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ganymedes01.etfuturum.waterlog.WaterlogFlow;
import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import net.minecraft.block.Block;
import net.minecraft.block.BlockWall;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

/**
 * 墙扩族（26.2 WallBlock implements SimpleWaterloggedBlock——WATERLOGGED 完整
 * 支持：getStateForPlacement L133 按 replacedFluidState 判定、updateShape
 * L139-141 含水 scheduleTick 蔓延）。与 MixinBlockFence 同构——1.7.10 BlockWall
 * 不声明 onNeighborBlockChange/updateTick/removedByPlayer（基类空实现，墙无
 * 支撑判定），按 {@code extends + @Override} 模式添加覆写（隐式重映射，生产
 * SRG 环境虚分派命中）。
 *
 * <p>1.7.10 BlockWall 连接状态靠 canConnectWallTo 邻居查询不存 meta，meta 只有
 * 变体位（bit0 normal/mossy）→ 含水位 bit16 的唯一裸比较污染点是
 * {@code damageDropped}（原样返回 meta，bit16 直通物品侧掉落/中键取块）——
 * @Inject HEAD 条件剥离；{@code getIcon} 带 {@code @SideOnly(CLIENT)}（专用
 * 服务器被 SideTransformer 剥离成员），剥位放 client 包的
 * {@link ganymedes01.etfuturum.mixins.early.waterlog.client.MixinBlockWall}。
 * EtFR BaseWall/BlockStoneWall/BlockBlackstoneWall 子类经 instanceof 自动继承，
 * 其取模/映射键的剥位在源码内完成。
 */
@Mixin(BlockWall.class)
public abstract class MixinBlockWall extends Block {

	protected MixinBlockWall(Block source) {
		super(source.blockMaterial);
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
	public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
		if (!world.isRemote && WaterlogRuntime.isWaterlogged(world, x, y, z)) {
			WaterlogFlow.scheduleSpread(world, x, y, z, (Block) (Object) this);
		}
	}

	@Override
	public void updateTick(World world, int x, int y, int z, Random random) {
		WaterlogRuntime.debug("WALL-TICK " + x + "," + y + "," + z + " " + (world.isRemote ? "client" : "server")
				+ " meta=" + world.getBlockMetadata(x, y, z)
				+ " wet=" + WaterlogRuntime.isWaterlogged(world, x, y, z));
		WaterlogFlow.handleFamilyTick(world, x, y, z, (Block) (Object) this);
	}

	/**
	 * 物品侧含水位剥离（栅栏天然干净、墙此方法直通 meta）：掉落与中键取块的
	 * 物品 damage = damageDropped(世界 meta)，bit16 直通 → 剥离（26.2 参照：
	 * getCloneItemStack 不带任何 state）。vanilla 体 = {@code return meta;}，
	 * 条件命中时以 {@code meta & 15} 等价复刻，未命中（干墙）原体放行。
	 */
	@Inject(method = "damageDropped(I)I", at = @At("HEAD"), cancellable = true)
	public void etfuturum$waterlogStripDropMeta(int meta, CallbackInfoReturnable<Integer> cir) {
		if ((meta & 0x10) != 0) {
			cir.setReturnValue(meta & 15);
		}
	}
}
