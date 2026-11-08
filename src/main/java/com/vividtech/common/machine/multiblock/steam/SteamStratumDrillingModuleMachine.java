package com.vividtech.common.machine.multiblock.steam;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

import java.util.List;

/**
 * 蒸汽地层钻探模块：每个周期产出 64 桶石油/天然气，并持续消耗钻井液。
 */
public class SteamStratumDrillingModuleMachine extends AbstractSteamResourceModuleMachine {

    private static final long OIL_OUTPUT = 64_000L;

    private boolean waitingForDrillingFluid;

    public SteamStratumDrillingModuleMachine(IMachineBlockEntity holder, int moduleTier) {
        super(holder, moduleTier);
    }

    @Override
    protected void tickResource() {
        RecipeLogic logic = getRecipeLogic();
        boolean working = logic.isWorking();
        if (!working && !waitingForDrillingFluid) {
            return;
        }
        boolean drained = drainDrillingFluid(1L);
        waitingForDrillingFluid = !drained;
        setResourceAvailable(drained);
    }

    /** 从模块的流体输入仓按 tick 抽取钻井液。 */
    private boolean drainDrillingFluid(long amount) {
        var handlers = capabilitiesProxy.get(IO.IN, FluidRecipeCapability.CAP);
        if (handlers == null) {
            return false;
        }
        long remaining = amount;
        Fluid drillingFluid = GTMaterials.DrillingFluid.getFluid();
        for (var handler : handlers) {
            if (!(handler instanceof NotifiableFluidTank tank)) {
                continue;
            }
            FluidStack stack = tank.getFluidInTank(0);
            if (stack.isEmpty() || stack.getFluid() != drillingFluid) {
                continue;
            }
            long toDrain = Math.min(remaining, stack.getAmount());
            if (toDrain <= 0) {
                continue;
            }
            FluidStack drained = tank.drainInternal(toDrain, false);
            remaining -= drained.getAmount();
            if (remaining <= 0) {
                return true;
            }
        }
        return remaining <= 0;
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        super.addDisplayText(textList);
        if (!isResourceAvailable()) {
            textList.add(Component.translatable("vividtech.machine.resource_module.missing_drilling_fluid"));
        }
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
        return baseRecipe()
                .outputFluids(output)
                .buildRawRecipe();
    }
}
