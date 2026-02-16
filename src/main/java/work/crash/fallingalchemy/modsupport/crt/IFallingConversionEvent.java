package work.crash.fallingalchemy.modsupport.crt;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.event.IEventPositionable;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.world.IWorld;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;

import java.util.List;

@ZenRegister
@ZenClass("mods.fallingalchemy.event.IFallingConversionEvent")
public interface IFallingConversionEvent extends IEventPositionable {
    @ZenGetter("outputs")
    List<IItemStack> getOutputs();

    @ZenGetter("world")
    IWorld getWorld();
    @ZenGetter("id")
    String getId();
}