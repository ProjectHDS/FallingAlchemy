package work.crash.fallingalchemy.utils;

import net.minecraft.util.math.BlockPos;

public class Math {
    public static double positionDistance(BlockPos pos1, BlockPos pos2) {
        double dx = pos2.getX() - pos1.getX();
        double dy = pos2.getY() - pos1.getY();
        double dz = pos2.getZ() - pos1.getZ();

        return java.lang.Math.sqrt(dx*dx + dy*dy + dz*dz);
    }
}
