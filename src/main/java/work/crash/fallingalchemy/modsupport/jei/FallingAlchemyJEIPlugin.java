package work.crash.fallingalchemy.modsupport.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import work.crash.fallingalchemy.Tags;
import work.crash.fallingalchemy.modsupport.FallingAlchemyTweaker;

import javax.annotation.Nonnull;
import java.util.List;

@JEIPlugin
public class FallingAlchemyJEIPlugin implements IModPlugin {

    public static final String CONVERSION_CATEGORY_UID = Tags.MOD_ID + ".conversion";

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        registry.addRecipeCategories(new ConversionRecipeCategory(registry.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void register(@Nonnull IModRegistry registry) {
        registry.handleRecipes(ConversionRecipeWrapper.class, recipe -> recipe, CONVERSION_CATEGORY_UID);

        List<ConversionRecipeWrapper> recipes = ConversionRecipeMaker.getConversionRecipes();
        registry.addRecipes(recipes, CONVERSION_CATEGORY_UID);

        if (!FallingAlchemyTweaker.RULES.isEmpty()) {
            Block firstBlock = FallingAlchemyTweaker.RULES.keySet().iterator().next();
            registry.addRecipeCategoryCraftingItem(new ItemStack(firstBlock), CONVERSION_CATEGORY_UID);
        }
    }
}