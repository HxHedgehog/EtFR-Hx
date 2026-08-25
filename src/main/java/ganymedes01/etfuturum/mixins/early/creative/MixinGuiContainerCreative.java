package ganymedes01.etfuturum.mixins.early.creative;

import net.minecraft.client.gui.inventory.GuiContainerCreative;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/**
 * Fixes the creative tab scroll wheel step count so a category whose item list
 * exactly fills whole rows (a multiple of 9) does not gain an extra blank row.
 * <p>
 * Vanilla computes {@code itemList.size() / 9 - 5 + 1}, which assumes a trailing
 * partial row and overshoots by one when size is an exact multiple of 9. Returning
 * {@code size - 1} turns it into {@code ceil(size / 9) - 5}, the correct number of
 * scrollable rows (5 rows are visible at once).
 */
@Mixin(GuiContainerCreative.class)
public abstract class MixinGuiContainerCreative {

	@Redirect(method = "handleMouseInput",
			at = @At(value = "INVOKE", target = "Ljava/util/List;size()I"))
	private int etfuturum$mouseWheelRowCount(List<?> itemList) {
		return itemList.size() - 1;
	}
}
