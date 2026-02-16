package work.crash.fallingalchemy.modsupport.crt;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import crafttweaker.api.world.IBlockPos;
import crafttweaker.api.world.IWorld;
import net.minecraft.item.ItemStack;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import work.crash.fallingalchemy.event.FallingConversionEvent;
import work.crash.fallingalchemy.item.ConsumedItem;

import java.util.List;
import java.util.stream.Collectors;

@ZenRegister
@ZenClass("mods.fallingalchemy.event.FallingConversionPostEvent")
public class FallingConversionPostEvent implements IFallingConversionEvent {
    private final FallingConversionEvent.Post event;

    public FallingConversionPostEvent(FallingConversionEvent.Post event) {
        this.event = event;
    }

    @Override
    public IWorld getWorld() {
        return CraftTweakerMC.getIWorld(event.getWorld());
    }

    @Override
    public IBlockPos getPosition() {
        return CraftTweakerMC.getIBlockPos(event.getPos());
    }

    @Override
    public List<IItemStack> getOutputs() {
        return event.getOutputs().stream().map(CraftTweakerMC::getIItemStack).collect(Collectors.toList());
    }
    @Override
    @ZenGetter("id")
    public String getId() {
        return event.getRule().id == null ? "" : event.getRule().id;
    }
    @ZenGetter("inputs")
    public ConsumedItem[] getInputs() {
        return event.getRule().consumedItems.toArray(new ConsumedItem[0]);
    }
    @ZenGetter("fallingBlock")
    public IItemStack getFallingBlock() {
        return CraftTweakerMC.getIItemStack(new ItemStack(event.getRule().triggerBlock));
    }
}