package com.vividtech.common.machine.multiblock.electric;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.logic.OCParams;
import com.gregtechceu.gtceu.api.recipe.logic.OCResult;

import org.jetbrains.annotations.Nullable;

/**
 * 晶体重铸工厂。
 *
 * <p>单晶硅类配方的数据时长已为高炉配方的 1/10；宝石处理使用自身基础时长。
 * 两类配方都会受线圈温度耗时/耗能修正，以及能源仓高于 MV 后的电压加速。</p>
 */
public class CrystalAggregationFactoryMachine extends CoilWorkableElectricMultiblockMachine {

    public CrystalAggregationFactoryMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    /**
     * 只修改本机器自己的配方：单晶硅类与宝石处理都使用数据中的基础时长。
     */
    @Nullable
    public static GTRecipe modifyRecipe(MetaMachine machine, GTRecipe recipe, OCParams params, OCResult result) {
        if (!(machine instanceof CrystalAggregationFactoryMachine factory)) {
            return recipe;
        }
        boolean boule = recipe.data.getBoolean("vividtech_crystal_boule");
        boolean gem = recipe.data.getBoolean("vividtech_gem_aggregation");
        if (!boule && !gem) {
            return recipe;
        }

        int recipeTemperature = recipe.data.getInt("ebf_temp");
        int coilTemperature = factory.getCoilType().getCoilTemperature();
        if (recipeTemperature > 0 && coilTemperature < recipeTemperature) {
            return null;
        }

        long eut = RecipeHelper.getInputEUt(recipe);
        if (recipeTemperature > 0 && coilTemperature > recipeTemperature) {
            long steps = (coilTemperature - recipeTemperature) / 900L;
            if (steps > 0) {
                eut = Math.max(1L, Math.round(eut * Math.pow(0.9D, steps)));
            }
        }

        double durationMultiplier = 1.0D;
        if (recipeTemperature > 0) {
            durationMultiplier *= Math.max(0.5D, (double) recipeTemperature / coilTemperature);
        }

        int machineTier = factory.getTier();
        if (machineTier > GTValues.MV) {
            durationMultiplier /= Math.pow(1.5D, machineTier - GTValues.MV);
        }

        GTRecipe modified = recipe.copy();
        modified.duration = Math.max(1, (int) Math.round(recipe.duration * durationMultiplier));
        RecipeHelper.setInputEUt(modified, eut);
        return modified;
    }
}
