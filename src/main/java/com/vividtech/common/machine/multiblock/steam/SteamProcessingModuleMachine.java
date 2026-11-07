package com.vividtech.common.machine.multiblock.steam;

import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineModule;

import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IDisplayUIMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.logic.OCParams;
import com.gregtechceu.gtceu.api.recipe.logic.OCResult;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 蒸汽加工模块。
 *
 * <p>
 * 模块本身是独立的 1×5×2 蒸汽多方块，拥有自己的物品仓；蒸汽直接从梦想电梯结构内的输入仓抽取。
 * 模块的并行、速度与蒸汽乘区在 {@link #recipeModifier} 中处理。
 * </p>
 */
public class SteamProcessingModuleMachine extends WorkableMultiblockMachine
                                                   implements IModularMachineModule<DreamElevatorMachine, SteamProcessingModuleMachine>,
                                                   IMachineLife, IDisplayUIMachine {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            SteamProcessingModuleMachine.class, WorkableMultiblockMachine.MANAGED_FIELD_HOLDER);

    private static final int CONNECTION_CHECK_INTERVAL_TICKS = 20;
    private static final int[] MODULE_PARALLELS = { 8, 16, 32 };

    private final int moduleTier;

    @Nullable
    private TickableSubscription connectionSubscription;

    @Persisted
    @Nullable
    @Getter
    @Setter
    private BlockPos hostPosition;

    @Nullable
    @Getter
    @Setter
    private DreamElevatorMachine host;

    @Persisted
    @DescSynced
    private boolean moduleEnabled = true;

    @DescSynced
    private boolean hostWorkingEnabled = true;

    @DescSynced
    private boolean connectedToHost;
    @DescSynced
    private int hostRailTier;

    private int reservedParallel;
    private double appliedConversion = -1.0D;

    public SteamProcessingModuleMachine(IMachineBlockEntity holder, int moduleTier) {
        super(holder);
        this.moduleTier = moduleTier;
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public @NotNull Class<DreamElevatorMachine> getHostType() {
        return DreamElevatorMachine.class;
    }

    @Override
    public BlockPos[] getHostScanPositions() {
        // 主机主动扫描并连接模块，模块侧不需要自己找主机。
        return new BlockPos[0];
    }

    public int getModuleTier() {
        return moduleTier;
    }

    public int getModuleParallel() {
        return MODULE_PARALLELS[moduleTier - 1];
    }

    @Override
    public void onConnected(@NotNull DreamElevatorMachine host) {
        setConnectionState(true, host.getRailTier());
        updateHostWorkingState(host);
        updateConnectionSubscription();
        refreshSteamHandler();
        recipeLogic.updateTickSubscription();
    }

    @Override
    public void onDisconnected() {
        setConnectionState(false, 0);
        hostWorkingEnabled = true;
        applyWorkingState();
        releaseReservedParallel();
        refreshSteamHandler();
    }

    @Override
    public boolean isWorkingEnabled() {
        return moduleEnabled;
    }

    @Override
    public void setWorkingEnabled(boolean enabled) {
        moduleEnabled = enabled;
        markDirty();
        notifyBlockUpdate();
        applyWorkingState();
    }

    /** 主机被软锤禁用时，模块同步暂停；主机恢复后按模块自己的开关恢复。 */
    private void applyWorkingState() {
        getRecipeLogic().setWorkingEnabled(moduleEnabled && hostWorkingEnabled);
    }

    private void updateHostWorkingState(DreamElevatorMachine currentHost) {
        boolean enabled = currentHost == null || !currentHost.isFormed() || currentHost.isWorkingEnabled();
        if (hostWorkingEnabled != enabled) {
            hostWorkingEnabled = enabled;
            markDirty();
            notifyBlockUpdate();
            applyWorkingState();
        }
    }

    private void setConnectionState(boolean connected, int railTier) {
        if (isRemote()) return;
        if (connectedToHost == connected && hostRailTier == railTier) return;
        connectedToHost = connected;
        hostRailTier = connected ? railTier : 0;
        markDirty();
        notifyBlockUpdate();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        updateHostWorkingState(getHost());
        applyWorkingState();
        updateConnectionSubscription();
    }

    @Override
    public void onUnload() {
        stopConnectionSubscription();
        super.onUnload();
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        updateHostWorkingState(getHost());
        applyWorkingState();
        findAndConnectToHost();
        DreamElevatorMachine currentHost = getHost();
        setConnectionState(currentHost != null && currentHost.isFormed(),
                currentHost != null ? currentHost.getRailTier() : 0);
        updateConnectionSubscription();
        refreshSteamHandler();
    }

    @Override
    public void onStructureInvalid() {
        removeFromHost(getHost());
        stopConnectionSubscription();
        super.onStructureInvalid();
    }

    @Override
    public void onPartUnload() {
        super.onPartUnload();
        removeFromHost(getHost());
        stopConnectionSubscription();
    }

    @Override
    public void onMachineRemoved() {
        removeFromHost(getHost());
        stopConnectionSubscription();
    }

    private void updateConnectionSubscription() {
        if (isFormed() && connectionSubscription == null) {
            connectionSubscription = subscribeServerTick(this::connectionTick);
        }
    }

    private void stopConnectionSubscription() {
        if (connectionSubscription != null) {
            connectionSubscription.unsubscribe();
            connectionSubscription = null;
        }
    }

    private void connectionTick() {
        if (!isFormed()) {
            stopConnectionSubscription();
            return;
        }
        updateHostWorkingState(getHost());
        if (getOffsetTimer() % CONNECTION_CHECK_INTERVAL_TICKS == 0) {
            DreamElevatorMachine currentHost = getHost();
            if (currentHost != null && !currentHost.isFormed()) {
                removeFromHost(currentHost);
                currentHost = null;
            }
            if (currentHost == null) {
                findAndConnectToHost();
            } else {
                setConnectionState(true, currentHost.getRailTier());
            }
        }
    }

    @Override
    public boolean beforeWorking(@Nullable GTRecipe recipe) {
        DreamElevatorMachine currentHost = getHost();
        if (currentHost == null || !currentHost.isFormed()) {
            return false;
        }
        int requestedParallel = currentHost.getMaxModuleParallel(this);
        if (reservedParallel < requestedParallel) {
            if (!currentHost.tryReserveParallel(this, requestedParallel)) {
                return false;
            }
            reservedParallel = requestedParallel;
        }
        boolean result = super.beforeWorking(recipe);
        if (!result) {
            releaseReservedParallel();
        }
        return result;
    }

    @Override
    public void afterWorking() {
        super.afterWorking();
        releaseReservedParallel();
    }

    @Override
    public boolean onWorking() {
        if (getOffsetTimer() % CONNECTION_CHECK_INTERVAL_TICKS == 0) {
            DreamElevatorMachine currentHost = getHost();
            if (currentHost == null || !currentHost.isFormed()) {
                if (currentHost != null) {
                    removeFromHost(currentHost);
                }
                releaseReservedParallel();
                getRecipeLogic().interruptRecipe();
                return false;
            }
            setConnectionState(true, currentHost.getRailTier());
            refreshSteamHandler();
        }
        return super.onWorking();
    }

    private void releaseReservedParallel() {
        DreamElevatorMachine currentHost = getHost();
        if (currentHost != null && reservedParallel > 0) {
            currentHost.releaseParallel(this);
        }
        reservedParallel = 0;
    }

    private void refreshSteamHandler() {
        DreamElevatorMachine currentHost = getHost();
        double conversion = currentHost != null ? currentHost.getSteamConversionRate() : 0.5D;
        if (Math.abs(conversion - appliedConversion) < 1.0e-6D &&
                capabilitiesProxy.contains(IO.IN, EURecipeCapability.CAP)) {
            return;
        }
        appliedConversion = conversion;
        capabilitiesProxy.put(IO.IN, EURecipeCapability.CAP,
                List.of(new SharedSteamEnergyRecipeHandler(() -> {
                    DreamElevatorMachine host = getHost();
                    return host != null ? host.getSteamTanks() : List.of();
                }, conversion)));
    }

    public double getSteamMultiplier(int recipeVoltageTier) {
        DreamElevatorMachine currentHost = getHost();
        if (currentHost == null) return 1.0D;
        int powerTier = currentHost.getPowerTier();
        if (powerTier <= 0) return 1.0D;
        // 配方电压等级：ULV = 0.5，LV = 1，MV = 2，HV = 3……
        // 机器不会主动超频：这里始终使用配方自身的电压等级，而不是动力模块解锁的上限。
        double voltageTier = recipeVoltageTier <= 0 ? 0.5D : recipeVoltageTier;
        // (0.8 / 动力模块等级) * 电压等级 + 生产模块等级 / 5
        double multiplier = (0.8D / powerTier) * voltageTier + moduleTier / 5.0D;
        return Math.max(0.05D, Math.min(multiplier, 1000.0D));
    }

    @Nullable
    public static GTRecipe recipeModifier(MetaMachine machine, GTRecipe recipe, OCParams params, OCResult result) {
        if (!(machine instanceof SteamProcessingModuleMachine module)) {
            return null;
        }
        DreamElevatorMachine currentHost = module.getHost();
        if (currentHost == null || !currentHost.isFormed()) {
            return null;
        }
        if (RecipeHelper.getRecipeEUtTier(recipe) > currentHost.getMaxRecipeTier()) {
            return null;
        }
        var parallelResult = GTRecipeModifiers.accurateParallel(machine, recipe,
                currentHost.getMaxModuleParallel(module), false);
        int parallel = parallelResult.getSecond();
        GTRecipe parallelRecipe = parallelResult.getFirst();
        if (parallel <= 0 || parallelRecipe == null) {
            return null;
        }
        GTRecipe modified = parallelRecipe.copy();
        long parallelEUt = RecipeHelper.getInputEUt(parallelRecipe);
        int recipeVoltageTier = RecipeHelper.getRecipeEUtTier(recipe);
        long modifiedEUt = Math.max(1L, Math.round(parallelEUt * module.getSteamMultiplier(recipeVoltageTier)));
        RecipeHelper.setInputEUt(modified, modifiedEUt);
        double speedMultiplier = currentHost.getProductionSpeedMultiplier();
        if (speedMultiplier != 1.0D) {
            modified.duration = (int) Math.max(1L, Math.round(modified.duration / speedMultiplier));
        }
        return modified;
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        IDisplayUIMachine.super.addDisplayText(textList);
        if (!isFormed()) return;
        textList.add(Component.translatable("vividtech.machine.steam_processing_module.tier", moduleTier));
        textList.add(Component.translatable("vividtech.machine.steam_processing_module.parallel",
                getModuleParallel()));
        DreamElevatorMachine currentHost = getHost();
        if (currentHost != null) {
            textList.add(Component.translatable("vividtech.machine.steam_processing_module.effective_parallel",
                    currentHost.getMaxModuleParallel(this)));
        }
        textList.add(Component.translatable(connectedToHost ?
                "vividtech.machine.steam_processing_module.connected" :
                "vividtech.machine.steam_processing_module.disconnected"));
        if (connectedToHost && currentHost != null) {
            textList.add(Component.translatable("vividtech.machine.steam_processing_module.steam",
                    currentHost.getStoredSteam() / 1000L, hostRailTier));
        }
    }
}
