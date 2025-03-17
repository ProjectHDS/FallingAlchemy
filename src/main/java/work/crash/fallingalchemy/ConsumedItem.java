package work.crash.fallingalchemy;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IIngredient;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;
import stanhebben.zenscript.annotations.ZenClass;

import java.util.List;

@ZenRegister
@ZenClass("mods.fallingalchemy.ConsumedItem")
public class ConsumedItem {
    final IIngredient ingredient;
    final int requiredCount;
    final boolean matchNBT;
    final boolean fuzzyNBT; // 新增模糊匹配标志

    public ConsumedItem(IIngredient ingredient, int count, boolean matchNBT, boolean fuzzyNBT) {
        this.ingredient = ingredient;
        this.requiredCount = Math.max(count, 1);
        this.matchNBT = matchNBT;
        this.fuzzyNBT = fuzzyNBT;
    }

    public boolean matches(ItemStack targetStack) {
        List<IItemStack> ctStacks = ingredient.getItems();
        if (ctStacks.isEmpty()) return false;

        for (IItemStack ctStack : ctStacks) {
            ItemStack matchStack = CraftTweakerMC.getItemStack(ctStack);

            // 基础物品匹配
            if (!OreDictionary.itemMatches(targetStack, matchStack, false))
                continue;

            // NBT处理逻辑
            if (matchNBT) {
                if (fuzzyNBT) {
                    // 模糊匹配：检查目标是否包含所有指定NBT标签
                    if (!fuzzyMatchNBT(matchStack.getTagCompound(), targetStack.getTagCompound())) {
                        continue;
                    }
                } else {
                    // 精确匹配：完全一致
                    if (!ItemStack.areItemStackTagsEqual(targetStack, matchStack)) {
                        continue;
                    }
                }
            }
            return true;
        }
        return false;
    }

    private boolean fuzzyMatchNBT(NBTTagCompound required, NBTTagCompound target) {
        if (required == null) return true; // 无NBT要求视为匹配
        if (target == null) return false;

        for (String key : required.getKeySet()) {
            NBTBase requiredTag = required.getTag(key);
            NBTBase actualTag = target.getTag(key);

            if (actualTag == null || !requiredTag.equals(actualTag)) {
                return false;
            }
        }
        return true;
    }
}