package com.vividtech.common.machine.multiblock.steam;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.Map;

/**
 * 资源采集模块的周期逻辑：不查找静态配方，而是每次周期开始时生成一个采集配方。
 */
public class ResourceModuleRecipeLogic extends RecipeLogic {

    public ResourceModuleRecipeLogic(IRecipeLogicMachine machine) {
        super(machine);
    }

    @Override
    public AbstractSteamResourceModuleMachine getMachine() {
        return (AbstractSteamResourceModuleMachine) super.getMachine();
    }

    @Override
    public void findAndHandleRecipe() {
        AbstractSteamResourceModuleMachine machine = getMachine();
        if (!machine.isFormed()) {
            return;
        }
        DreamElevatorMachine host = machine.getHost();
        if (host == null || !host.isFormed()) {
            return;
        }
        Level level = machine.getLevel();
        if (level == null) {
            return;
        }
        if (!machine.isDimensionAllowed(level)) {
            setWaiting(Component.translatable("vividtech.machine.resource_module.wrong_dimension"));
            return;
        }
        if (machine.getOffsetTimer() % 20 != 0) {
            return;
        }
        GTRecipe recipe = machine.buildCycleRecipe(level);
        if (recipe == null) {
            setWaiting(Component.translatable("vividtech.machine.resource_module.no_output"));
            return;
        }
        setupRecipe(recipe);
    }

    @Override
    public void onRecipeFinish() {
        getMachine().afterWorking();
        getMachine().onCycleCompleted();
        if (lastRecipe != null) {
            lastRecipe.postWorking(getMachine());
            boolean handled = handleRecipeIO(lastRecipe, IO.OUT);
            if (!handled) {
                DreamElevatorMachine host = getMachine().getHost();
                if (host != null) {
                    lastRecipe.handleRecipeIO(IO.OUT, host, Map.of());
                }
            }
        }
        setStatus(Status.IDLE);
        progress = 0;
        duration = 0;
    }
}
