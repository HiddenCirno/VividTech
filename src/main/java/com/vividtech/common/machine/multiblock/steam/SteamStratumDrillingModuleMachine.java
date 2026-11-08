package com.vividtech.common.machine.multiblock.steam;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;

import net.minecraft.world.level.Level;

/**
 * 蒸汽地层钻探模块：每个周期产出 64 桶石油/天然气，并持续消耗钻井液。
 */
public class SteamStratumDrillingModuleMachine extends AbstractSteamResourceModuleMachine {

    private static final long OIL_OUTPUT = 64_000L;

    public SteamStratumDrillingModuleMachine(IMachineBlockEntity holder, int moduleTier) {
        super(holder, moduleTier);
    }

    @Override
    public boolean isDimensionAllowed(Level level) {
        return getOutputFluid(level) != null;
    }

    private FluidStack getOutputFluid(Level level) {
        if (level.dimension().equals(Level.OVERWORLD)) {
            return FluidStack.create(GTMaterials.Oil.getFluid(), OIL_OUTPUT);
        }
        if (level.dimension().equals(Level.NETHER) && getModuleTier() >= 2) {
            return FluidStack.create(GTMaterials.NaturalGas.getFluid(), OIL_OUTPUT);
        }
        return null;
    }

    @Override
    public GTRecipe buildCycleRecipe(Level level) {
        FluidStack output = getOutputFluid(level);
        if (output == null) {
            return null;
        }
        // 1 mB/t 钻井液，按整个周期一次性计入配方输入。
        return baseRecipe()
                .inputFluids(FluidStack.create(GTMaterials.DrillingFluid.getFluid(), getCycleDuration()))
                .outputFluids(output)
                .buildRawRecipe();
    }
}
