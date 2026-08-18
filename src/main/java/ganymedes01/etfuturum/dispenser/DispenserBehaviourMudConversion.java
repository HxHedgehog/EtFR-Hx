package ganymedes01.etfuturum.dispenser;

import ganymedes01.etfuturum.ModBlocks;
import net.minecraft.block.BlockDispenser;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

/**
 * 发射器发射水瓶时，将前方 CONVERTABLE_TO_MUD（泥土、粗泥、缠根泥土）转化为泥巴，返回玻璃瓶。
 * 官方对应：PotionItem.useOn() 中 CONVERTABLE_TO_MUD + 水瓶。
 */
public class DispenserBehaviourMudConversion extends BehaviorDefaultDispenseItem {

	@Override
	protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
		// 只处理水瓶（damage 0 且无药水效果）
		if (stack.getItem() != Items.potionitem || stack.getItemDamage() != 0 || Items.potionitem.getEffects(stack) != null) {
			return super.dispenseStack(source, stack);
		}

		EnumFacing facing = BlockDispenser.func_149937_b(source.getBlockMetadata());
		int x = source.getXInt() + facing.getFrontOffsetX();
		int y = source.getYInt() + facing.getFrontOffsetY();
		int z = source.getZInt() + facing.getFrontOffsetZ();

		net.minecraft.block.Block targetBlock = source.getWorld().getBlock(x, y, z);
		int targetMeta = source.getWorld().getBlockMetadata(x, y, z);

		if (ModBlocks.MUD.isEnabled() && isConvertableToMud(targetBlock, targetMeta)) {
			source.getWorld().setBlock(x, y, z, ModBlocks.MUD.get(), 0, 3);
			// 返回玻璃瓶
			return new ItemStack(Items.glass_bottle);
		}

		return super.dispenseStack(source, stack);
	}

	/**
	 * 官方 CONVERTABLE_TO_MUD：dirt、coarse_dirt（meta 1）、rooted_dirt。
	 * 1.7.10 中 dirt 的 meta 0/1/2 分别对应 dirt/coarse_dirt/podzol。
	 */
	private static boolean isConvertableToMud(net.minecraft.block.Block block, int meta) {
		if (block == Blocks.dirt) {
			return meta == 0 || meta == 1; // dirt 或 coarse_dirt
		}
		return block == ModBlocks.ROOTED_DIRT.get();
	}
}