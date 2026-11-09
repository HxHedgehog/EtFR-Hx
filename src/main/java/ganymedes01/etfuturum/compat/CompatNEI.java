package ganymedes01.etfuturum.compat;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import codechicken.nei.api.API;
import codechicken.nei.api.IHighlightHandler;
import codechicken.nei.api.ItemInfo;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.blocks.BaseCaveVines;

/**
 * NEI highlight compatibility. NEI resolves the displayed stack through block.getPickBlock(),
 * which BaseCaveVines intentionally overrides to return glow berries (mimicking the modern
 * getCloneItemStack behaviour), so the highlight would show "Glow Berries" instead of the
 * block name. Same fix as the Waila stack provider in CompatWaila.
 */
public class CompatNEI {

    public static void register() {
        // registerHighlightHandler only feeds tooltip text (ItemInfo#highlightHandlers); the
        // displayed stack is resolved through highlightIdentifiers, which requires this API
        API.registerHighlightIdentifier(null, new CaveVinesHighlightHandler());
    }

    public static class CaveVinesHighlightHandler implements IHighlightHandler {

        @Override
        public ItemStack identifyHighlight(World world, EntityPlayer player, MovingObjectPosition mop) {
            if (world.getBlock(mop.blockX, mop.blockY, mop.blockZ) instanceof BaseCaveVines) {
                return ((BaseCaveVines) world.getBlock(mop.blockX, mop.blockY, mop.blockZ))
                        .getHighlightStack(world.getBlockMetadata(mop.blockX, mop.blockY, mop.blockZ));
            }

            return null;
        }

        @Override
        public List<String> handleTextData(ItemStack stack, World world, EntityPlayer player, MovingObjectPosition mop,
                List<String> currenttip, ItemInfo.Layout layout) {
            return currenttip;
        }
    }
}
