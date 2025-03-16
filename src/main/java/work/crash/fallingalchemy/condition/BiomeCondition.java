package work.crash.fallingalchemy.condition;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

// 生物群系条件
public class BiomeCondition implements ICondition {
    private final ResourceLocation biome;

    public BiomeCondition(String biomeId) {
        this.biome = new ResourceLocation(biomeId);
    }

    @Override
    public boolean test(World world, BlockPos pos) {
        return world.getBiome(pos).getRegistryName().equals(biome);
    }
}
