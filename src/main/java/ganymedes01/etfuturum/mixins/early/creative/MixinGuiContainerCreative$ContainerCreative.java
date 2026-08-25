package ganymedes01.etfuturum.mixins.early.creative;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/**
 * Fixes the creative tab row offset so a category whose item list exactly fills
 * whole rows (a multiple of 9) does not show an extra blank row at the bottom.
 * <p>
 * Vanilla computes {@code itemList.size() / 9 - 5 + 1} as the scrollable step
 * count, which overshoots by one when size is an exact multiple of 9, letting the
 * scroll reach a trailing row where every slot is empty. Only the first
 * {@code List.size()} call in scrollTo (the step count) is redirected; the second
 * occurrence (the {@code i1 < itemList.size()} bounds check) must stay untouched,
 * so it is selected with {@code ordinal = 0}.
 */
@Mixin(targets = "net.minecraft.client.gui.inventory.GuiContainerCreative$ContainerCreative")
public abstract class MixinGuiContainerCreative$ContainerCreative {

	@Redirect(method = "scrollTo",
			at = @At(value = "INVOKE", target = "Ljava/util/List;size()I", ordinal = 0))
	private int etfuturum$scrollRowCount(List<?> itemList) {
		return itemList.size() - 1;
	}
}
