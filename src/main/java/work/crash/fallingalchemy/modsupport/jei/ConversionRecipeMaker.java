package work.crash.fallingalchemy.modsupport.jei;

import work.crash.fallingalchemy.modsupport.FallingAlchemyTweaker;

import java.util.ArrayList;
import java.util.List;

public class ConversionRecipeMaker {
    public static List<ConversionRecipeWrapper> getConversionRecipes() {
        List<ConversionRecipeWrapper> recipes = new ArrayList<>();
        for (List<FallingAlchemyTweaker.ConversionRule> rules : FallingAlchemyTweaker.RULES.values()) {
            for (FallingAlchemyTweaker.ConversionRule rule : rules) {
                recipes.add(new ConversionRecipeWrapper(rule));
            }
        }
        return recipes;
    }
}