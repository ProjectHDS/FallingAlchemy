package work.crash.fallingalchemy.modsupport.jei;

import crafttweaker.api.minecraft.CraftTweakerMC;
import crafttweaker.api.item.IItemStack;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import work.crash.fallingalchemy.item.ConsumedItem;
import work.crash.fallingalchemy.modsupport.FallingAlchemyTweaker;
import work.crash.fallingalchemy.condition.ICondition;

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
        List<List<ItemStack>> inputs = new ArrayList<>();
        List<ItemStack> triggerInputs = new ArrayList<>();
        triggerInputs.add(new ItemStack(rule.triggerBlock));
        inputs.add(triggerInputs);

        for (ConsumedItem consumedItem : rule.consumedItems) {
            List<ItemStack> matchingStacks = getMatchingStacks(consumedItem);
            inputs.add(matchingStacks);
        }

        ingredients.setInputLists(ItemStack.class, inputs);
        ingredients.setOutputs(ItemStack.class, rule.outputs);
    }

    private List<ItemStack> getMatchingStacks(ConsumedItem consumedItem) {
        List<ItemStack> result = new ArrayList<>();

        List<IItemStack> ctStacks = consumedItem.ingredient.getItems();
        for (IItemStack ctStack : ctStacks) {
            ItemStack mcStack = CraftTweakerMC.getItemStack(ctStack);
            mcStack.setCount(consumedItem.requiredCount);
            result.add(mcStack);
        }

        return result;
    }

    @Override
    public void drawInfo(@Nonnull Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        FontRenderer fontRenderer = minecraft.fontRenderer;
        int yOffset = 20;

        String successText = I18n.format("jei.fallingalchemy.success_chance") + " " + String.format("%.1f%%", rule.successChance * 100);
        fontRenderer.drawString(successText, 45, yOffset, 0xFF404040);
        yOffset += 10;

        if (rule.keepBlockChance > 0) {
            String keepText = I18n.format("jei.fallingalchemy.keep_chance", String.format("%.1f%%", rule.keepBlockChance * 100));
            fontRenderer.drawString(keepText, 45, yOffset, 0xFF404040);
            yOffset += 10;
        }

        String radiusText = I18n.format("jei.fallingalchemy.radius") + " " + String.format("%.1f", rule.radius);
        fontRenderer.drawString(radiusText, 45, yOffset, 0xFF404040);
        yOffset += 10;

        if (rule.displacement > 0) {
            String displacementText = I18n.format("jei.fallingalchemy.displacement") + " " + String.format("%.1f", rule.displacement);
            fontRenderer.drawString(displacementText, 45, yOffset, 0xFF404040);
            yOffset += 10;

            if (rule.additionalProducts) {
                String bonusText = I18n.format("jei.fallingalchemy.bonus_products");
                fontRenderer.drawString(bonusText, 45, yOffset, 0xFF00AA00);
                yOffset += 10;
            }
        }

        int totalConditions = rule.conditions.size();
        boolean hasNBTRequirement = false;
        for (ConsumedItem item : rule.consumedItems) {
            if (item.matchNBT) {
                hasNBTRequirement = true;
                totalConditions++;
                break;
            }
        }

        if (!rule.conditions.isEmpty() || hasNBTRequirement) {
            String conditionsText = I18n.format("jei.fallingalchemy.conditions") + " " + totalConditions;
            fontRenderer.drawString(conditionsText, 5, 65, 0xFF8B4513);
        }
    }

    @Override
    public List<String> getTooltipStrings(int mouseX, int mouseY) {
        List<String> tooltip = new ArrayList<>();

        if (mouseX >= 5 && mouseX <= 140 && mouseY >= 65 && mouseY <= 75) {
            List<String> allConditions = new ArrayList<>();

            for (int i = 0; i < rule.conditionInfos.size(); i++) {
                String condDesc = formatConditionDescription(rule.conditionInfos.get(i));
                if (condDesc != null) {
                    allConditions.add("§7• " + condDesc);
                }
            }

            for (ConsumedItem item : rule.consumedItems) {
                if (item.matchNBT) {
                    if (item.fuzzyNBT) {
                        allConditions.add("§7• " + I18n.format("jei.fallingalchemy.condition.nbt.fuzzy"));
                    } else {
                        allConditions.add("§7• " + I18n.format("jei.fallingalchemy.condition.nbt.exact"));
                    }
                    break;
                }
            }

            if (!allConditions.isEmpty()) {
                tooltip.add(I18n.format("jei.fallingalchemy.conditions.title"));
                tooltip.add("");
                tooltip.addAll(allConditions);
            }
        }

        if (mouseX >= 50 && mouseX <= 140 && mouseY >= 40 && mouseY <= 60 && rule.displacement > 0) {
            tooltip.add(I18n.format("jei.fallingalchemy.displacement.tooltip"));
            if (rule.additionalProducts) {
                tooltip.add(I18n.format("jei.fallingalchemy.bonus_products.tooltip"));
            }
        }

        return tooltip.isEmpty() ? null : tooltip;
    }

    private String formatConditionDescription(FallingAlchemyTweaker.ConditionInfo info) {
        switch (info.type) {
            case "biome":
                return I18n.format("jei.fallingalchemy.condition.biome.format", info.description);

            case "time":
                if (info.description.startsWith("custom:")) {
                    String timeRange = info.description.substring(7);
                    return I18n.format("jei.fallingalchemy.condition.time.format", timeRange);
                } else {
                    return I18n.format("jei.fallingalchemy.condition.time." + info.description);
                }

            case "height":
                return I18n.format("jei.fallingalchemy.condition.height.format", info.description);

            case "weather":
                return I18n.format("jei.fallingalchemy.condition.weather." + info.description);

            case "moon":
                try {
                    int moonPhase = Integer.parseInt(info.description);
                    if (moonPhase >= 0 && moonPhase <= 7) {
                        return I18n.format("jei.fallingalchemy.condition.moon.phase" + moonPhase);
                    }
                } catch (NumberFormatException e) {
                }
                return I18n.format("jei.fallingalchemy.condition.moon.format", info.description);

            default:
                return I18n.format("jei.fallingalchemy.condition.custom");
        }
    }


    public List<FallingAlchemyTweaker.ConditionInfo> getConditionInfos() {
        return rule.conditionInfos;
    }

    public FallingAlchemyTweaker.ConversionRule getRule() {
        return rule;
    }

    @Override
    public boolean handleClick(@Nonnull Minecraft minecraft, int mouseX, int mouseY, int mouseButton) {
        return false;
    }
}