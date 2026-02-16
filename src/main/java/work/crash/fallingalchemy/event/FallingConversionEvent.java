package work.crash.fallingalchemy.event;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;
import work.crash.fallingalchemy.modsupport.FallingAlchemyTweaker;

import java.util.List;

public class FallingConversionEvent extends Event {
    private final World world;
    private final BlockPos pos;
    private final IBlockState state;
    private final List<ItemStack> outputs;
    private final FallingAlchemyTweaker.ConversionRule rule;

    public FallingConversionEvent(World world, BlockPos pos, IBlockState state, List<ItemStack> outputs,  FallingAlchemyTweaker.ConversionRule rule) {
        this.world = world;
        this.pos = pos;
        this.state = state;
        this.outputs = outputs;
        this.rule = rule;
    }

    public World getWorld() {
        return world;
    }

    public BlockPos getPos() {
        return pos;
    }
    // todo:也许可用于更换下落方块，大概
    public IBlockState getState() {
        return state;
    }

    public List<ItemStack> getOutputs() {
        return outputs;
    }
    public FallingAlchemyTweaker.ConversionRule getRule() {
        return rule;
    }

    @Cancelable
    public static class Pre extends FallingConversionEvent {
        public Pre(World world, BlockPos pos, IBlockState state, List<ItemStack> outputs, FallingAlchemyTweaker.ConversionRule rule) {
            super(world, pos, state, outputs, rule);
        }
    }

    public static class Post extends FallingConversionEvent {
        public Post(World world, BlockPos pos, IBlockState state, List<ItemStack> outputs, FallingAlchemyTweaker.ConversionRule rule) {
            super(world, pos, state, outputs, rule);
        }
    }
}