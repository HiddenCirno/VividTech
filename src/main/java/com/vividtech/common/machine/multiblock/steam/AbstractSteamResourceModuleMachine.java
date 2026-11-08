package com.vividtech.common.machine.multiblock.steam;

import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineModule;

import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IDisplayUIMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 梦想电梯资源采集模块基类：负责主机连接、暂停状态、共享蒸汽处理器与 GUI 显示。
 */
public abstract class AbstractSteamResourceModuleMachine extends WorkableMultiblockMachine
                                                        implements IModularMachineModule<DreamElevatorMachine, AbstractSteamResourceModuleMachine>,
                                                        IMachineLife, IDisplayUIMachine {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            AbstractSteamResourceModuleMachine.class, WorkableMultiblockMachine.MANAGED_FIELD_HOLDER);

    private static final int CONNECTION_CHECK_INTERVAL_TICKS = 20;
    private static final int[] CYCLE_DURATIONS = { 2400, 1200, 600 };

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

    @DescSynced
    private boolean resourceAvailable = true;

    @Persisted
    @DescSynced
    private int completedCycles;

    private boolean steamHandlerAttached;

    public AbstractSteamResourceModuleMachine(IMachineBlockEntity holder, int moduleTier) {
        super(holder);
        this.moduleTier = moduleTier;
    }

    @Override
    protected RecipeLogic createRecipeLogic(Object... args) {
        return new ResourceModuleRecipeLogic(this);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    public int getModuleTier() {
        return moduleTier;
    }

    public int getCycleDuration() {
        return CYCLE_DURATIONS[moduleTier - 1];
    }

    /** 受梦想电梯轨道速度加成后的实际周期。 */
    public int getEffectiveCycleDuration() {
        DreamElevatorMachine currentHost = getHost();
        double speed = currentHost != null ? currentHost.getProductionSpeedMultiplier() : 1.0D;
        return Math.max(1, (int) Math.round(getCycleDuration() / speed));
    }

    /** 模块等级 × 200 mB/t × [(0.8/轨道等级)×模块等级 + 模块等级/10]。 */
    public double getSteamPerTick() {
        DreamElevatorMachine currentHost = getHost();
        if (currentHost == null || currentHost.getRailTier() <= 0) {
            return 0.0D;
        }
        double multiplier = (0.8D / currentHost.getRailTier()) * moduleTier + moduleTier / 10.0D;
        return moduleTier * 200.0D * multiplier;
    }

    public long getRecipeEUt() {
        return Math.max(1L, Math.round(getSteamPerTick() / 0.5D));
    }

    public abstract boolean isDimensionAllowed(Level level);

    public abstract GTRecipe buildCycleRecipe(Level level);

    protected GTRecipeBuilder baseRecipe() {
        return GTRecipeBuilder.ofRaw()
                .duration(getEffectiveCycleDuration())
                .EUt(getRecipeEUt());
    }

    @Override
    public @NotNull Class<DreamElevatorMachine> getHostType() {
        return DreamElevatorMachine.class;
    }

    @Override
    public BlockPos[] getHostScanPositions() {
        return new BlockPos[0];
    }

    @Override
    public boolean keepSubscribing() {
        // WorkableMultiblockMachine 默认返回 false，资源模块没有静态配方，
        // 空闲时会被 RecipeLogic 取消订阅，导致 findAndHandleRecipe 不再被调用。
        return true;
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

    private void applyWorkingState() {
        getRecipeLogic().setWorkingEnabled(moduleEnabled && hostWorkingEnabled && resourceAvailable);
    }

    protected void setResourceAvailable(boolean available) {
        if (resourceAvailable == available) {
            return;
        }
        resourceAvailable = available;
        if (!isRemote()) {
            markDirty();
            notifyBlockUpdate();
            applyWorkingState();
        }
    }

    public boolean isResourceAvailable() {
        return resourceAvailable;
    }

    public int getCompletedCycles() {
        return completedCycles;
    }

    /** 每个周期完成（产出已尝试进入输出仓）后累加，用于界面诊断。 */
    public void onCycleCompleted() {
        if (isRemote()) {
            return;
        }
        completedCycles++;
        markDirty();
    }

    /** 每 tick 由模块自身检查/消耗资源；默认不消耗。 */
    protected void tickResource() {}

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
        resourceAvailable = true;
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
        resourceAvailable = true;
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
        tickResource();
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
    public boolean onWorking() {
        if (getOffsetTimer() % CONNECTION_CHECK_INTERVAL_TICKS == 0) {
            DreamElevatorMachine currentHost = getHost();
            if (currentHost == null || !currentHost.isFormed()) {
                if (currentHost != null) {
                    removeFromHost(currentHost);
                }
                getRecipeLogic().interruptRecipe();
                return false;
            }
            setConnectionState(true, currentHost.getRailTier());
            refreshSteamHandler();
        }
        return super.onWorking();
    }

    private void refreshSteamHandler() {
        if (steamHandlerAttached) {
            return;
        }
        steamHandlerAttached = true;
        capabilitiesProxy.put(IO.IN, EURecipeCapability.CAP,
                List.of(new SharedSteamEnergyRecipeHandler(() -> {
                    DreamElevatorMachine currentHost = getHost();
                    return currentHost != null ? currentHost.getSteamTanks() : List.of();
                }, 0.5D)));
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        IDisplayUIMachine.super.addDisplayText(textList);
        if (!isFormed()) {
            return;
        }
        textList.add(Component.translatable("vividtech.machine.resource_module.tier", moduleTier));
        textList.add(Component.translatable("vividtech.machine.resource_module.cycle",
                getEffectiveCycleDuration() / 20));
        textList.add(Component.translatable(connectedToHost ?
                "vividtech.machine.resource_module.connected" :
                "vividtech.machine.resource_module.disconnected"));
        if (connectedToHost) {
            textList.add(Component.translatable("vividtech.machine.resource_module.steam_tick",
                    String.format("%.1f", getSteamPerTick())));
        }
        int itemOutputs = capabilitiesProxy.get(IO.OUT, ItemRecipeCapability.CAP) == null ? 0 :
                capabilitiesProxy.get(IO.OUT, ItemRecipeCapability.CAP).size();
        int fluidOutputs = capabilitiesProxy.get(IO.OUT, FluidRecipeCapability.CAP) == null ? 0 :
                capabilitiesProxy.get(IO.OUT, FluidRecipeCapability.CAP).size();
        textList.add(Component.translatable("vividtech.machine.resource_module.output_ports",
                itemOutputs, fluidOutputs));
        textList.add(Component.translatable("vividtech.machine.resource_module.completed_cycles",
                completedCycles));
        if (getLevel() != null && !isDimensionAllowed(getLevel())) {
            textList.add(Component.translatable("vividtech.machine.resource_module.wrong_dimension"));
        }
    }
}
