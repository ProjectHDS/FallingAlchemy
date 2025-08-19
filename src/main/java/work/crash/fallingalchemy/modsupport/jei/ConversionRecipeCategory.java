package work.crash.fallingalchemy.modsupport.jei;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import work.crash.fallingalchemy.Tags;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ConversionRecipeCategory implements IRecipeCategory<ConversionRecipeWrapper> {

    public static final int WIDTH = 140;
    public static final int HEIGHT = 80;

    private final IDrawable background;
    private final IDrawable icon;
    private final String localizedName;

    public ConversionRecipeCategory(IGuiHelper guiHelper) {
        ResourceLocation location = new ResourceLocation("fallingalchemy", "textures/gui/conversion_jei.png");
        background = guiHelper.createDrawable(location, 0, 0, WIDTH, HEIGHT);
        localizedName = I18n.format("jei.category.fallingalchemy.conversion");
        icon = guiHelper.createDrawable(location, 140, 0, 16, 16);
    }

    @Nonnull
    @Override
    public String getUid() {
        return FallingAlchemyJEIPlugin.CONVERSION_CATEGORY_UID;
    }

    @Nonnull
    @Override
    public String getTitle() {
        return localizedName;
    }

    @Nonnull
    @Override
    public String getModName() {
        return Tags.MOD_NAME;
    }

    @Nonnull
    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Nullable
    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(@Nonnull IRecipeLayout recipeLayout, @Nonnull ConversionRecipeWrapper recipeWrapper, @Nonnull IIngredients ingredients) {
        IGuiItemStackGroup guiItemStacks = recipeLayout.getItemStacks();

        List<List<ItemStack>> inputs = ingredients.getInputs(ItemStack.class);
        guiItemStacks.init(0, true, 20, 20);
        guiItemStacks.set(0, inputs.get(0));

        for (int i = 1; i < inputs.size(); i++) {
            guiItemStacks.init(i, true, 20 + (i-1)*18, 40);
            guiItemStacks.set(i, inputs.get(i));
        }

        List<List<ItemStack>> outputs = ingredients.getOutputs(ItemStack.class);
        int outputStartX = 100;
        int outputStartY = 20;
        int outputsPerRow = 2;

        for (int i = 0; i < outputs.size(); i++) {
            int row = i / outputsPerRow;
            int col = i % outputsPerRow;
            guiItemStacks.init(inputs.size() + i, false, outputStartX + col * 18, outputStartY + row * 18);
            guiItemStacks.set(inputs.size() + i, outputs.get(i));
        }
    }

    @Override
    public void drawExtras(@Nonnull Minecraft minecraft) {
        String chanceText = I18n.format("jei.fallingalchemy.success_chance");
        minecraft.fontRenderer.drawString(chanceText, 50, 20, 0xFF404040);

        //TODO: Condition icons?
    }
}