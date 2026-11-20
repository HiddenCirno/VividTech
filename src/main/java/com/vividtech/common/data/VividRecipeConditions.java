package com.vividtech.common.data;

import com.vividtech.common.recipe.condition.AvaritiaPersonalDimensionCondition;

import com.gregtechceu.gtceu.api.recipe.condition.RecipeConditionType;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

/**
 * VividTech 自定义配方条件。
 */
public final class VividRecipeConditions {

    private VividRecipeConditions() {}

    public static void init() {}

    public static final RecipeConditionType<AvaritiaPersonalDimensionCondition> AVARITIA_PERSONAL =
            GTRegistries.RECIPE_CONDITIONS.register("avaritia_personal",
                    new RecipeConditionType<>(AvaritiaPersonalDimensionCondition::new,
                            AvaritiaPersonalDimensionCondition.CODEC));
}
