package com.vividtech.common.machine.multiblock.steam;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeHandler;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

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
        AbstractSteamResourceModuleMachine machine = getMachine();
        machine.afterWorking();
        machine.onCycleCompleted();
        if (lastRecipe != null) {
            lastRecipe.postWorking(machine);
            // 注意：GTL 的 RecipeLogicMixin 把 handleRecipeIO 整个覆盖了，OUT 也会被当成
            // 输入处理，所以不能再用 handleRecipeIO(..., IO.OUT)。GTL 自己的机器是在
            // onRecipeFinish 里调用 handleRecipeOutput 的，这里必须同样处理。
            boolean handled = RecipeRunnerHelper.handleRecipeOutput(machine, lastRecipe);
            if (!handled) {
                handled = outputProductsDirectly(machine);
            }
            if (!handled) {
                com.gregtechceu.gtceu.GTCEu.LOGGER.warn(
                        "[VividTech] resource module at {} could not output {} (proxyOUT={}, parts={})",
                        machine.getPos(), lastRecipe.outputs.keySet(),
                        machine.getCapabilitiesProxy().row(IO.OUT).keySet(), machine.getParts().size());
            }
        }
        setStatus(Status.IDLE);
        progress = 0;
        duration = 0;
    }

    /**
     * 采集配方是我们自己生成的、产物必定 100% 产出，不需要经过 ChanceLogic。
     * 这里直接把输出交给模块自己的输出仓/输出总成，避免 GT 的概率包装在某些情况下
     * 静默丢弃产物（表现为周期完成但仓里是空的）。
     */
    private boolean outputProductsDirectly(AbstractSteamResourceModuleMachine machine) {
        var proxy = machine.getCapabilitiesProxy();
        for (var entry : lastRecipe.outputs.entrySet()) {
            RecipeCapability<?> capability = entry.getKey();
            List<Object> outputs = new ArrayList<>(entry.getValue().size());
            for (var content : entry.getValue()) {
                outputs.add(content.content);
            }
            if (outputs.isEmpty()) {
                continue;
            }

            List<IRecipeHandler<?>> handlers = new ArrayList<>();
            var out = proxy.get(IO.OUT, capability);
            if (out != null) {
                handlers.addAll(out);
            }
            var both = proxy.get(IO.BOTH, capability);
            if (both != null) {
                handlers.addAll(both);
            }

            List<?> left = outputs;
            for (IRecipeHandler<?> handler : handlers) {
                if (left == null || left.isEmpty()) {
                    break;
                }
                left = handler.handleRecipe(IO.OUT, lastRecipe, left, null, false);
            }
            if (left != null && !left.isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
