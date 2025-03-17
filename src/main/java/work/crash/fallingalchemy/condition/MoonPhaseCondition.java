package work.crash.fallingalchemy.condition;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class MoonPhaseCondition implements ICondition{
    private final int moonCondition;

    public MoonPhaseCondition(int moonCondition) {
        this.moonCondition = moonCondition;
    }

    @Override
    public boolean test(World world, BlockPos pos) {
        return moonCondition == world.getMoonPhase();
    }
}
