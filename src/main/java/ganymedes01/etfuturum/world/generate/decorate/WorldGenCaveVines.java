package ganymedes01.etfuturum.world.generate.decorate;


import ganymedes01.etfuturum.blocks.BlockCaveVines;
import ganymedes01.etfuturum.blocks.BlockCaveVinesPlant;
import ganymedes01.etfuturum.tileentities.TileEntityCaveVines;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Random;

import static net.minecraft.world.EnumSkyBlock.Sky;
public class WorldGenCaveVines extends WorldGenerator {

    private final Block caveVines;

    public WorldGenCaveVines(Block caveVines) {
        this.caveVines = caveVines;
    }

    @Override
    public boolean generate(World world, Random rand, int x, int y, int z) {
        if (world.isAirBlock(x, y, z) && world.getSavedLightValue(Sky, x, y, z) <= 3 && world.getBlock(x, y + 1, z).isSideSolid(world, x, y, z, ForgeDirection.DOWN)) {

            world.setBlock(x, y, z, caveVines);

            int tipY = y;
            for (int i = 1; i < rand.nextInt(6) + 1; i++)
            {
                if (world.isAirBlock(x, y - i, z) && world.getBlock(x, y - i + 1, z) instanceof BlockCaveVines vine)
                {
                    vine.growVine(world, x, y - i + 1, z, false);
                    tipY = y - i;
                }
            }

            // 官方自然生成 head 的 AGE 为 UniformInt(23,25)（满 25 停止生长），剩余生长量仅 0~2 格：
            // 将尖端的 maxLength 收敛为当前总长 + 0~2，自然藤蔓接近被修剪状态，不会持续疯长；
            // 玩家种植的藤蔓不受影响，仍按随机上限正常生长
            TileEntity te = world.getTileEntity(x, tipY, z);
            if (te instanceof TileEntityCaveVines)
            {
                int length = 1;
                while (world.getBlock(x, tipY + length, z) instanceof BlockCaveVinesPlant)
                {
                    length++;
                }
                ((TileEntityCaveVines) te).setMaxLength(length + rand.nextInt(3));
            }
            return true;
        }
        return false;
    }
}
