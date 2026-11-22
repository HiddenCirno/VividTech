package com.vividtech.common.machine.multiblock.electric;

import com.gregtechceu.gtceu.api.capability.IParallelHatch;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.logic.OCParams;
import com.gregtechceu.gtceu.api.recipe.logic.OCResult;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;

import org.jetbrains.annotations.Nullable;

/**
 * 原子重构器。
 *
 * <p>标准 EV 多方块：并行控制仓提供并行，随后进行无损超频（不损失能效）。</p>
 */
public class AtomicReconstructorMachine extends WorkableElectricMultiblockMachine {

    public AtomicReconstructorMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    /** 当前并行控制仓提供的并行数；没有并行仓时返回 1。 */
    public int getParallelHatchParallel() {
        if (!isFormed()) {
            return 1;
        }
        int parallel = 1;
        for (IMultiPart part : getParts()) {
            if (part instanceof IParallelHatch hatch) {
                parallel = Math.max(parallel, hatch.getCurrentParallel());
            }
        }
        return parallel;
    }

    @Nullable
    public static GTRecipe modifyRecipe(MetaMachine machine, GTRecipe recipe, OCParams params, OCResult result) {
        if (!(machine instanceof AtomicReconstructorMachine reconstructor)) {
            return recipe;
        }

        result.reset();

        int maxParallel = reconstructor.getParallelHatchParallel();
        var parallelResult = GTRecipeModifiers.accurateParallel(reconstructor, recipe, maxParallel, false);
        GTRecipe modified = parallelResult.getFirst();
        int parallel = parallelResult.getSecond();
        if (modified == null || parallel <= 0) {
            return null;
        }
        if (modified == recipe) {
            modified = recipe.copy();
        }

        // 无损超频：完美超频不损失能效；低于 1t 的最后一档由全局 Mixin 取整到 1t。
        RecipeHelper.applyOverclock(OverclockingLogic.PERFECT_OVERCLOCK, modified,
                reconstructor.getOverclockVoltage(), params, result);
        return modified;
    }
}
