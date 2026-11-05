package ganymedes01.etfuturum.blocks;

import java.util.Random;

import ganymedes01.etfuturum.client.sound.ModSounds;
import ganymedes01.etfuturum.waterlog.WaterlogFlow;
import ganymedes01.etfuturum.waterlog.WaterlogRuntime;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * 26.2 {@code CopperGrateBlock extends WaterloggedTransparentBlock implements
 * SimpleWaterloggedBlock}（WATERLOGGED 完整支持）→ 含水家族成员（见
 * {@link ganymedes01.etfuturum.waterlog.WaterlogFamilies} javadoc）。
 * EtFR 自有方块 → 全部源码级改动无 mixin。
 */
public class BlockCopperGrate extends BlockCopper {

	public BlockCopperGrate() {
		super("copper_grate", "exposed_copper_grate", "weathered_copper_grate", "oxidized_copper_grate",
				"waxed_copper_grate", "waxed_exposed_copper_grate", "waxed_weathered_copper_grate", "waxed_oxidized_copper_grate");
		setBlockSound(ModSounds.soundCopperGrate);
	}

	/**
	 * 含水剥位（家族铁律：裸 meta 比较审计）：{@code meta > 3} 裸判下含水位
	 * world meta 16-23 → 铜 meta 20-27，氧化阶段判定（i &lt; 7 / i % 4）与打蜡
	 * 分支（meta &lt; 8）全部失真，且含水格栅永不氧化。剥位后干格行为不变。
	 */
	@Override
	public int getCopperMeta(int meta) {
		meta &= 15;
		return meta + (meta > 3 ? 4 : 0);
	}

	/**
	 * 入参为 copper meta 域（tryDegrade 的 i+1 / waxMeta / meta-1），理论不含
	 * bit16，按家族铁律防御性剥位（对 0-15 有效输入为恒等变换）。
	 */
	@Override
	public int getFinalCopperMeta(IBlockAccess world, int x, int y, int z, int meta, int worldMeta) {
		meta &= 15;
		return meta - (meta > 3 ? 4 : 0);
	}

	/**
	 * 氧化/打蜡写回保留含水位：26.2 氧化（{@code tryDegrade}）与打蜡/去蜡
	 * （{@code tryWaxOnWaxOff}）走 {@code setBlock(state.setValue(AGE...))}
	 * 的属性保留语义（WATERLOGGED 原样），1.7.10 重写 meta 会清 bit16 →
	 * 含水格栅一氧化/打蜡就"排水"。仅同类转换保留（本方块氧化/打蜡从不跨
	 * 方块，{@code getCopperBlockFromMeta} 恒返回自身）。
	 */
	@Override
	public void setCopperBlock(Block newBlock, int newMeta, World world, int x, int y, int z) {
		if (newBlock == this && WaterlogRuntime.isWaterlogged(world, x, y, z)) {
			newMeta |= WaterlogRuntime.WATERLOGGED_BIT;
		}
		super.setCopperBlock(newBlock, newMeta, world, x, y, z);
	}

	// ---- 含水家族生命周期（源码覆写，锁链 mixin 四件套同构）----

	@Override
	public void updateTick(World world, int x, int y, int z, Random rand) {
		WaterlogFlow.handleFamilyTick(world, x, y, z, this);
		super.updateTick(world, x, y, z, rand); // BlockCopper: tickDegradation
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
		if (!world.isRemote && WaterlogRuntime.isWaterlogged(world, x, y, z)) {
			WaterlogFlow.scheduleSpread(world, x, y, z, this);
		}
	}

	@Override
	public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest) {
		return WaterlogFlow.leaveSourceWater(world, x, y, z) || super.removedByPlayer(world, player, x, y, z, willHarvest);
	}

	@Override
	public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z) {
		return WaterlogFlow.leaveSourceWater(world, x, y, z) || super.removedByPlayer(world, player, x, y, z);
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}
}
