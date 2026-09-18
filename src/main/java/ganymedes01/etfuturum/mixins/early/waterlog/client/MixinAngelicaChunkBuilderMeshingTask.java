package ganymedes01.etfuturum.mixins.early.waterlog.client;

import com.gtnewhorizons.angelica.rendering.celeritas.AngelicaChunkBuildContext;
import com.gtnewhorizons.angelica.rendering.celeritas.iris.BlockRenderContext;
import ganymedes01.etfuturum.waterlog.VisualWaterlog;
import ganymedes01.etfuturum.waterlog.WaterlogFamily;
import net.coderbot.iris.block_rendering.NbtConditionalIdMap;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Celeritas decides each block's render pass via Block#canRenderInPass, which
 * has no position context, so the decision cannot be overridden per cell
 * there. Instead the pass argument of the per-block renderBlock call is
 * rewritten for a visually waterlogged member: 0 -&gt; 1, so the content goes
 * through the translucent terrain pass - the painted water then sorts
 * together with the surrounding real water and gets the shader pack's water
 * treatment. The water volume is painted right after
 * renderBlockByRenderType: later is useless, because renderBlock resets the
 * tessellator before returning and the copyRawBuffer arguments are evaluated
 * in between. Both the pass rewrite and the paint dispatch through the
 * VisualWaterlog registry, so every family shares this path.
 * <p>
 * Targets Angelica by name; registered only when Angelica is present, so the
 * compile-time Angelica types in the handler signature only ever load in an
 * environment that provides them.
 */
@Mixin(targets = "com.gtnewhorizons.angelica.rendering.celeritas.AngelicaChunkBuilderMeshingTask", remap = false)
public abstract class MixinAngelicaChunkBuilderMeshingTask {

	@ModifyArg(method = {"execute", "processDeferredBlocks", "lambda$processDeferredBlocks$0"},
			at = @At(value = "INVOKE",
					target = "Lcom/gtnewhorizons/angelica/rendering/celeritas/AngelicaChunkBuilderMeshingTask;renderBlock(Lnet/minecraft/block/Block;IIIIILnet/minecraft/client/renderer/Tessellator;Lnet/minecraft/client/renderer/RenderBlocks;Lorg/embeddedt/embeddium/impl/render/chunk/compile/ChunkBuildBuffers;Lcom/gtnewhorizons/angelica/rendering/celeritas/AngelicaChunkBuildContext;Lcom/gtnewhorizons/angelica/rendering/celeritas/iris/BlockRenderContext;IIILorg/embeddedt/embeddium/impl/render/chunk/terrain/material/Material;ZLnet/coderbot/iris/block_rendering/NbtConditionalIdMap;J)V"),
			index = 5, remap = false)
	private int etfuturum$waterloggedBlocksUseTranslucentPass(Block block, int metadata, int x, int y, int z, int pass,
			Tessellator tessellator, RenderBlocks renderBlocks, ChunkBuildBuffers buffers,
			AngelicaChunkBuildContext buildContext, BlockRenderContext blockRenderContext,
			int originX, int originY, int originZ, Material materialOverride, boolean isShaderPackOverride,
			NbtConditionalIdMap<Block> teMap, long currentTick) {
		if (pass == 0 && VisualWaterlog.isWaterlogged(renderBlocks.blockAccess, x, y, z)) {
			return 1;
		}
		return pass;
	}

	@Inject(method = "renderBlock", remap = false, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/RenderBlocks;renderBlockByRenderType(Lnet/minecraft/block/Block;III)Z",
			shift = At.Shift.AFTER, remap = true))
	private void etfuturum$afterRenderByType(Block block, int metadata, int x, int y, int z, int pass,
			Tessellator tessellator, RenderBlocks renderBlocks, ChunkBuildBuffers buffers,
			AngelicaChunkBuildContext buildContext, BlockRenderContext blockRenderContext,
			int originX, int originY, int originZ, Material materialOverride, boolean isShaderPackOverride,
			NbtConditionalIdMap<Block> teMap, long currentTick, CallbackInfo ci) {
		// The water volume must be added right after renderBlockByRenderType:
		// later is useless because the copyRawBuffer arguments are evaluated before
		// an INVOKE-before injection would run, and renderBlock resets the
		// tessellator before returning. The paint re-checks the stateless
		// waterlog condition itself.
		WaterlogFamily family = VisualWaterlog.familyOf(block);
		if (family != null && pass == 1) {
			family.paint(renderBlocks.blockAccess, x, y, z, tessellator);
		}
	}
}
