package work.crash.fallingalchemy.condition;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

// 条件接口
public interface ICondition {
    boolean test(World world, BlockPos pos);
}
