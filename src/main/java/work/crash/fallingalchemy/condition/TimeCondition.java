package work.crash.fallingalchemy.condition;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

// 时间条件（0-24000）
public class TimeCondition implements ICondition {
    private final int minTime;
    private final int maxTime;

    public TimeCondition(int min, int max) {
        this.minTime = min;
        this.maxTime = max;
    }

    @Override
    public boolean test(World world, BlockPos pos) {
        long time = world.getWorldTime() % 24000;
        return time >= minTime && time <= maxTime;
    }
}
