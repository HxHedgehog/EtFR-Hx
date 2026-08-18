package ganymedes01.etfuturum.mixins.early.mushroom;

import ganymedes01.etfuturum.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHugeMushroom;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

/**
 * 给原版 1.7.10 {@link BlockHugeMushroom} 注入精准采集支持、中键选取修正、
 * 纹理修正和翻译区分。
 */
@Mixin(BlockHugeMushroom.class)
public abstract class MixinBlockHugeMushroom extends Block {

	protected MixinBlockHugeMushroom(Material materialIn) {
		super(materialIn);
	}

	/** mushroomType：0=棕，1=红 */
	@Shadow
	private int field_149792_b;

	/**
	 * 覆盖默认的 unlocalized name，区分棕色和红色蘑菇方块。
	 */
	@Override
	public String getUnlocalizedName() {
		return field_149792_b == 0 ? "tile.brown_mushroom_block" : "tile.red_mushroom_block";
	}

	/**
	 * 开启精准采集。
	 */
	@Override
	protected boolean canSilkHarvest() {
		return true;
	}

	/**
	 * 修复中键选取（pick block）：原版返回小蘑菇，改为返回方块物品自身。
	 */
	@Override
	public Item getItem(World worldIn, int x, int y, int z) {
		return Item.getItemFromBlock(this);
	}

	/**
	 * 物品栏只显示一个全帽纹理变体（meta=14）。
	 */
	@Override
	public void getSubBlocks(Item item, CreativeTabs tab, List<ItemStack> list) {
		list.add(new ItemStack(item, 1, 14));
	}

	/**
	 * 修复物品/放置方块的纹理：meta 0（物品栏默认 / createStackedBlock 默认）
	 * → 14（6面全帽纹理）。自然生成的蘑菇块使用 meta 1-9/10，不受影响。
	 * <p>
	 * 放在 {@code getIcon} 而非 {@code createStackedBlock} 中，因为：
	 * <ul>
	 *   <li>{@code @Override createStackedBlock} 对仅继承自父类、未被目标类
	 *       自身声明的方法可能无法正确 remap</li>
	 *   <li>{@code getIcon} 是 BlockHugeMushroom 自身声明且渲染的最终入口，
	 *       在此处修正确保所有渲染路径都正确显示</li>
	 * </ul>
	 */
	@ModifyVariable(method = "getIcon", at = @At("HEAD"), argsOnly = true, ordinal = 1)
	private int etfuturum$adjustMetaForRendering(int meta) {
		// meta 0 = 物品栏默认 / 掉落默认 → 14（全帽纹理）
		// meta 1-9 = 自然生成定向帽 → 保持原样（定向纹理）
		// meta 10 = 自然生成茎 → 原版逻辑（侧面茎+底面断面）
		// meta 14 = 全帽 → 保持原样
		// meta 15 = 全茎 → 保持原样
		if (meta == 0) {
			return 14;
		}
		return meta;
	}

	/**
	 * 精准采集时尝试返回正确 meta，作为辅助防御。
	 */
	@Override
	protected ItemStack createStackedBlock(int meta) {
		if (meta == 10 || meta == 15) {
			return new ItemStack(ModBlocks.MUSHROOM_STEM.get(), 1, 0);
		}
		return new ItemStack(this, 1, 14);
	}
}
