package work.crash.fallingalchemy.modsupport.crt;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.event.IEventCancelable;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import crafttweaker.api.world.IBlockPos;
import crafttweaker.api.world.IWorld;
import net.minecraft.item.ItemStack;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenMethod;
import work.crash.fallingalchemy.event.FallingConversionEvent;
import work.crash.fallingalchemy.item.ConsumedItem;

import java.util.List;
import java.util.stream.Collectors;

@ZenRegister
@ZenClass("mods.fallingalchemy.event.FallingConversionPreEvent")
public class FallingConversionPreEvent implements IEventCancelable, IFallingConversionEvent {
    private final FallingConversionEvent.Pre event;

    public FallingConversionPreEvent(FallingConversionEvent.Pre event) {
        this.event = event;
    }

    @Override
    public boolean isCanceled() {
        return event.isCanceled();
    }

    @Override
    public void setCanceled(boolean canceled) {
        event.setCanceled(canceled);
    }

    @Override
    public IWorld getWorld() {
        return CraftTweakerMC.getIWorld(event.getWorld());
    }

    @Override
    public IBlockPos getPosition() {
        return CraftTweakerMC.getIBlockPos(event.getPos());
    }

    @ZenGetter("inputs")
    public ConsumedItem[] getInputs() {
        return event.getRule().consumedItems.toArray(new ConsumedItem[0]);
    }

    @ZenGetter("fallingBlock")
    public IItemStack getFallingBlock() {
        return CraftTweakerMC.getIItemStack(new ItemStack(event.getRule().triggerBlock));
    }
    @Override
    @ZenGetter("id")
    public String getId() {
        return event.getRule().id == null ? "" : event.getRule().id;
    }

    @Override
    public List<IItemStack> getOutputs() {
        return event.getOutputs().stream().map(CraftTweakerMC::getIItemStack).collect(Collectors.toList());
    }

    @ZenMethod
    public void setOutputs(IItemStack[] outputs) {
        event.getOutputs().clear();
        for (IItemStack stack : outputs) {
            event.getOutputs().add(CraftTweakerMC.getItemStack(stack));
        }
    }

    @ZenMethod
    public void addOutput(IItemStack stack) {
        event.getOutputs().add(CraftTweakerMC.getItemStack(stack));
    }
}