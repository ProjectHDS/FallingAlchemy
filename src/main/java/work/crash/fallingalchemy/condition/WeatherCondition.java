package work.crash.fallingalchemy.condition;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

// 天气条件
public class WeatherCondition implements ICondition {
    private final boolean raining;
    private final boolean thundering;

    public WeatherCondition(boolean rain, boolean thunder) {
        this.raining = rain;
        this.thundering = thunder;
    }

    @Override
    public boolean test(World world, BlockPos pos) {
        return world.isRaining() == raining && world.isThundering() == thundering;
    }
}