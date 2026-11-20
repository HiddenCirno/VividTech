package com.vividtech.common.machine.multiblock.electric;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.logic.OCParams;
import com.gregtechceu.gtceu.api.recipe.logic.OCResult;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;

import org.jetbrains.annotations.Nullable;

/**
 * 基础集成矿物处理厂。
 *
 * <p>固定最大并行数为 16，不依赖并行控制仓；并行结算后再进行普通无损超频。</p>
 */
public class BasicIntegratedOreProcessorMachine extends WorkableElectricMultiblockMachine {

    public static final int MAX_PARALLEL = 16;

    public BasicIntegratedOreProcessorMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Nullable
    public static GTRecipe modifyRecipe(MetaMachine machine, GTRecipe recipe, OCParams params, OCResult result) {
        if (!(machine instanceof BasicIntegratedOreProcessorMachine processor)) {
            return recipe;
        }

        result.reset();

        var parallelResult = GTRecipeModifiers.accurateParallel(processor, recipe, MAX_PARALLEL, false);
        GTRecipe modified = parallelResult.getFirst();
        int parallel = parallelResult.getSecond();
        if (modified == null || parallel <= 0) {
            return null;
        }
        if (modified == recipe) {
            modified = recipe.copy();
        }

        // 普通无损超频；低于 1t 的最后一档由全局 Mixin 取整到 1t。
        RecipeHelper.applyOverclock(OverclockingLogic.PERFECT_OVERCLOCK, modified,
                processor.getOverclockVoltage(), params, result);
        return modified;
    }
}
