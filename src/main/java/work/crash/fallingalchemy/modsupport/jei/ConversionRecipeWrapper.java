package work.crash.fallingalchemy.modsupport.jei;

import crafttweaker.api.minecraft.CraftTweakerMC;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import work.crash.fallingalchemy.item.ConsumedItem;
import work.crash.fallingalchemy.modsupport.FallingAlchemyTweaker;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class ConversionRecipeWrapper implements IRecipeWrapper {

    private final FallingAlchemyTweaker.ConversionRule rule;

    public ConversionRecipeWrapper(FallingAlchemyTweaker.ConversionRule rule) {
        this.rule = rule;
    }

    @Override
    public void getIngredients(@Nonnull IIngredients ingredients) {
        List<ItemStack> triggerInputs = new ArrayList<>();
        triggerInputs.add(new ItemStack(rule.triggerBlock));
        ingredients.setInputs(ItemStack.class, triggerInputs);

        for (ConsumedItem consumedItem : rule.consumedItems) {
            List<ItemStack> matchingStacks = getMatchingStacks(consumedItem);
            ingredients.setInputs(ItemStack.class, matchingStacks);
        }

        ingredients.setOutputs(ItemStack.class, rule.outputs);
    }

    private List<ItemStack> getMatchingStacks(ConsumedItem consumedItem) {
        List<ItemStack> result = new ArrayList<>();
        result.add(CraftTweakerMC.getItemStack(consumedItem.ingredient));
        return result;
    }

    @Override
    public void drawInfo(@Nonnull Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        FontRenderer fontRenderer = minecraft.fontRenderer;

        String successText = String.format("%.1f%%", rule.successChance * 100);
        fontRenderer.drawString(successText, 50, 30, 0xFF404040);

        if (rule.keepBlockChance > 0) {
            String keepText = I18n.format("jei.fallingalchemy.keep_chance", String.format("%.1f%%", rule.keepBlockChance * 100));
            fontRenderer.drawString(keepText, 50, 40, 0xFF404040);
        }

        String radiusText = I18n.format("jei.fallingalchemy.radius", rule.radius);
        fontRenderer.drawString(radiusText, 50, 50, 0xFF404040);

        if (!rule.conditions.isEmpty()) {
            String conditionsText = I18n.format("jei.fallingalchemy.conditions", rule.conditions.size());
            fontRenderer.drawString(conditionsText, 50, 60, 0xFF404040);
        }
    }

    @Override
    public List<String> getTooltipStrings(int mouseX, int mouseY) {
        List<String> tooltip = new ArrayList<>();

        //TODO: hover tooltips?

        return tooltip.isEmpty() ? null : tooltip;
    }

    @Override
    public boolean handleClick(@Nonnull Minecraft minecraft, int mouseX, int mouseY, int mouseButton) {
        return false;
    }
}