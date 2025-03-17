package work.crash.fallingalchemy.condition;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class HeightCondition implements ICondition {
    private final int minHeight;
    private final int maxHeight;

    public HeightCondition(int min, int max) {
        this.minHeight = min;
        this.maxHeight = max;
    }

    @Override
    public boolean test(World world, BlockPos pos) {
        int height = pos.getY();
        return height >= minHeight && height <= maxHeight;
    }
}
