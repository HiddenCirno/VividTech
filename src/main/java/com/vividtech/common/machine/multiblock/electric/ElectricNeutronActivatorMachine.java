package com.vividtech.common.machine.multiblock.electric;

import org.gtlcore.gtlcore.api.recipe.IGTRecipe;
import org.gtlcore.gtlcore.common.data.GTLRecipeModifiers;

import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.logic.OCParams;
import com.gregtechceu.gtceu.api.recipe.logic.OCResult;

import java.util.List;

/**
 * 电力中子活化器。
 *
 * <p>行为与中子漩涡一致：忽略 {@code ev_min}/{@code ev_max} 与中子动能池，
 * 直接把配方数据中的 {@code evt}（KeV/t）按 2 EU = 1 eV 换算成每 tick 的 EU 输入。</p>
 */
public class ElectricNeutronActivatorMachine extends WorkableElectricMultiblockMachine {

    public ElectricNeutronActivatorMachine(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
    }

    public static GTRecipe modifyRecipe(MetaMachine machine, GTRecipe recipe, OCParams params, OCResult result) {
        long eu = recipe.data.getInt("evt") * 2000L;
        recipe.tickInputs.put(EURecipeCapability.CAP,
                List.of(new Content(eu, 10000, 10000, 0, null, null)));
        ((IGTRecipe) recipe).setHasTick(true);
        int parallel = GTLRecipeModifiers.getHatchParallel(machine);
        result.init(eu, recipe.duration, parallel, params.getOcAmount());
        return recipe;
    }
}
