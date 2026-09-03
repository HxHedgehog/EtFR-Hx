package ganymedes01.etfuturum.mixins.early.creativetabs;

import net.minecraft.creativetab.CreativeTabs;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Makes the creative tab layout position (page / column / row) and the tab index
 * derive from the tab's position inside {@link CreativeTabs#creativeTabArray}
 * instead of the (final) {@code tabIndex} field.
 * <p>
 * 1.7.10's creative GUI relies on the invariant {@code creativeTabArray[pos] == tab
 * AND tab.getTabIndex() == pos}: {@code selectedTabIndex} is used directly as an
 * array subscript, and the Forge-patched layout methods
 * ({@code getTabPage/getTabColumn/isTabInFirstRow}) compute position from the
 * {@code tabIndex} field.
 * <p>
 * {@code tabIndex} is {@code final} and is set from {@code getNextID()}, so a
 * late-registered tab (like UNCLASSIFIED) keeps a stale index that does not match
 * its array position. Reflection cannot be relied on to fix the field: in the
 * reobfuscated runtime the field is {@code field_78033_n}, so a lookup by MCP
 * name ({@code "tabIndex"}) throws {@code NoSuchFieldException}. UNCLASSIFIED
 * therefore kept its late index and rendered as the *second* tab of page 2.
 * <p>
 * By reading the array position instead, the layout stays consistent no matter
 * what index {@code tabIndex} holds. UNCLASSIFIED is placed at array index 12
 * (first tab of page 2) in {@code replaceCreativeTabArray()}; mod tabs follow at
 * 13+. Their original {@code tabIndex} values are irrelevant now.
 */
@Mixin(CreativeTabs.class)
public abstract class MixinCreativeTabs {

	/** The raw, final index stored on the tab; used only as a fallback when a tab is not in the array. */
	@Shadow
	private int tabIndex;

	/**
	 * Returns the tab's position inside {@code creativeTabArray}, or its stored
	 * {@code tabIndex} as a fallback when it is not present in the array.
	 */
	private int etfuturum$arrayIndexOf(CreativeTabs tab) {
		CreativeTabs[] array = CreativeTabs.creativeTabArray;
		for (int i = 0; i < array.length; i++) {
			if (array[i] == tab) {
				return i;
			}
		}
		return this.tabIndex;
	}

	@Redirect(method = "getTabIndex",
			at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
					target = "Lnet/minecraft/creativetab/CreativeTabs;tabIndex:I"))
	private int etfuturum$getTabIndex(CreativeTabs receiver) {
		return etfuturum$arrayIndexOf(receiver);
	}

	@Redirect(method = "getTabPage",
			at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
					target = "Lnet/minecraft/creativetab/CreativeTabs;tabIndex:I"))
	private int etfuturum$getTabPage(CreativeTabs receiver) {
		return etfuturum$arrayIndexOf(receiver);
	}

	@Redirect(method = "getTabColumn",
			at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
					target = "Lnet/minecraft/creativetab/CreativeTabs;tabIndex:I"))
	private int etfuturum$getTabColumn(CreativeTabs receiver) {
		return etfuturum$arrayIndexOf(receiver);
	}

	@Redirect(method = "isTabInFirstRow",
			at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
					target = "Lnet/minecraft/creativetab/CreativeTabs;tabIndex:I"))
	private int etfuturum$isTabInFirstRow(CreativeTabs receiver) {
		return etfuturum$arrayIndexOf(receiver);
	}
}
