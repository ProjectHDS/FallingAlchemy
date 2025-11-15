package work.crash.fallingalchemy.modsupport.jei;

import crafttweaker.api.minecraft.CraftTweakerMC;
import crafttweaker.api.item.IItemStack;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import work.crash.fallingalchemy.item.ConsumedItem;
import work.crash.fallingalchemy.modsupport.FallingAlchemyTweaker;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class ConversionRecipeWrapper implements IRecipeWrapper {

    private final FallingAlchemyTweaker.ConversionRule rule;

    private static final int ICON_SIZE = 16;
    private static final int LEFT_COLUMN_X = 20;
    private static final int RIGHT_COLUMN_X = 100;
    private static final int TOP_ROW_Y = 50;
    private static final int BOTTOM_ROW_Y = 66;

    private static final ResourceLocation ICON_SUCCESS =
            new ResourceLocation("fallingalchemy", "textures/gui/icon_success.png");
    private static final ResourceLocation ICON_RADIUS =
            new ResourceLocation("fallingalchemy", "textures/gui/icon_radius.png");
    private static final ResourceLocation ICON_KEEP =
            new ResourceLocation("fallingalchemy", "textures/gui/icon_keep.png");
    private static final ResourceLocation ICON_DISPLACEMENT =
            new ResourceLocation("fallingalchemy", "textures/gui/icon_displacement.png");


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

        ingredients.setInputLists(VanillaTypes.ITEM, inputs);
        ingredients.setOutputs(VanillaTypes.ITEM, rule.outputs);
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

        minecraft.getTextureManager().bindTexture(ICON_SUCCESS);
        drawIcon(LEFT_COLUMN_X, TOP_ROW_Y);

        minecraft.getTextureManager().bindTexture(ICON_RADIUS);
        drawIcon(RIGHT_COLUMN_X, TOP_ROW_Y);

        if (rule.keepBlockChance > 0) {
            minecraft.getTextureManager().bindTexture(ICON_KEEP);
            drawIcon(LEFT_COLUMN_X, BOTTOM_ROW_Y);
        }

        if (rule.displacement > 0) {
            minecraft.getTextureManager().bindTexture(ICON_DISPLACEMENT);
            drawIcon(RIGHT_COLUMN_X, BOTTOM_ROW_Y);
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
            fontRenderer.drawString(conditionsText, 5, 5, 0xFF8B4513);
        }
    }

    private void drawIcon(int x, int y) {
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
    }


    @Override
    public List<String> getTooltipStrings(int mouseX, int mouseY) {
        List<String> tooltip = new ArrayList<>();

        if (isInArea(mouseX, mouseY, LEFT_COLUMN_X, TOP_ROW_Y)) {
            tooltip.add(I18n.format("jei.fallingalchemy.success_chance"));
            tooltip.add("§7" + String.format("%.1f%%", rule.successChance * 100));
            return tooltip;
        }

        if (isInArea(mouseX, mouseY, RIGHT_COLUMN_X, TOP_ROW_Y)) {
            tooltip.add(I18n.format("jei.fallingalchemy.radius"));
            tooltip.add("§7" + String.format("%.1f", rule.radius));
            return tooltip;
        }

        if (rule.keepBlockChance > 0 && isInArea(mouseX, mouseY, LEFT_COLUMN_X, BOTTOM_ROW_Y)) {
            tooltip.add(I18n.format("jei.fallingalchemy.keep_chance", ""));
            tooltip.add("§7" + String.format("%.1f%%", rule.keepBlockChance * 100));
            return tooltip;
        }

        if (rule.displacement > 0 && isInArea(mouseX, mouseY, RIGHT_COLUMN_X, BOTTOM_ROW_Y)) {
            tooltip.add(I18n.format("jei.fallingalchemy.displacement"));
            tooltip.add("§7" + String.format("%.1f", rule.displacement));
            if (rule.additionalProducts) {
                tooltip.add("");
                tooltip.add("§a" + I18n.format("jei.fallingalchemy.bonus_products"));
                tooltip.add("§7" + I18n.format("jei.fallingalchemy.bonus_products.tooltip"));
            }
            return tooltip;
        }

        if (mouseX >= 5 && mouseX <= 140 && mouseY >= 5 && mouseY <= 15) {
            List<String> allConditions = new ArrayList<>();

            for (int i = 0; i < rule.conditionInfos.size(); i++) {
                String condDesc = formatConditionDescription(rule.conditionInfos.get(i));
                allConditions.add("§7• " + condDesc);
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

        return tooltip.isEmpty() ? null : tooltip;
    }

    private boolean isInArea(int mouseX, int mouseY, int x, int y) {
        return mouseX >= x && mouseX < x +ICON_SIZE && mouseY >= y && mouseY < y + ICON_SIZE;
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