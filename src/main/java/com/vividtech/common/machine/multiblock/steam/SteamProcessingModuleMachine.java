package com.vividtech.common.machine.multiblock.steam;

import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineModule;

import com.vividtech.VividTech;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeHandler;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.ICleanroomProvider;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IDisplayUIMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeCondition;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.logic.OCParams;
import com.gregtechceu.gtceu.api.recipe.logic.OCResult;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;
import com.gregtechceu.gtceu.common.recipe.condition.RockBreakerCondition;

import org.gtlcore.gtlcore.common.recipe.condition.GravityCondition;

import com.google.gson.JsonObject;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
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
    private static final int[] MODULE_PARALLELS = { 16, 32, 64 };
    /** LuV 上限 - ULV 配方 = 最多 6 次超频。 */
    private static final int MAX_OVERCLOCK = 6;

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

    /** 玩家为这个模块单独设置的超频等级；默认 0，不自动超频。 */
    @Persisted
    @DescSynced
    private int overclockLevel = 0;

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

    public int getOverclockLevel() {
        return Mth.clamp(overclockLevel, 0, MAX_OVERCLOCK);
    }

    /** 每个模块单独设置；需要玩家在模块 GUI 里手动加减。 */
    public void setOverclockLevel(int level) {
        int clamped = Mth.clamp(level, 0, MAX_OVERCLOCK);
        if (overclockLevel == clamped) {
            return;
        }
        overclockLevel = clamped;
        markDirty();
        notifyBlockUpdate();
        getRecipeLogic().markLastRecipeDirty();
    }

    /** 当前配方最多能吃到几次超频：上限等级 - 配方等级。 */
    public int getAvailableOverclock(@Nullable GTRecipe recipe) {
        if (recipe == null) {
            return 0;
        }
        DreamElevatorMachine currentHost = getHost();
        if (currentHost == null || !currentHost.isFormed()) {
            return 0;
        }
        return Math.max(0, currentHost.getMaxRecipeTier() - RecipeHelper.getRecipeEUtTier(recipe));
    }

    /** 基础并行 × 2^实际超频等级；超频部分不占用控制器并行池。 */
    public int getEffectiveParallel(@Nullable GTRecipe recipe) {
        DreamElevatorMachine currentHost = getHost();
        int base = currentHost != null ? currentHost.getMaxModuleParallel(this) : getModuleParallel();
        if (recipe == null) {
            return base;
        }
        int overclock = Math.min(getOverclockLevel(), getAvailableOverclock(recipe));
        return doubleParallel(base, overclock);
    }

    private static int doubleParallel(int base, int overclock) {
        if (overclock <= 0) {
            return base;
        }
        long boosted = (long) base << Math.min(overclock, 30);
        return (int) Math.min(Integer.MAX_VALUE, boosted);
    }

    @Override
    public void handleDisplayClick(String componentData, ClickData clickData) {
        if (clickData.isRemote) {
            return;
        }
        if ("overclock_add".equals(componentData)) {
            setOverclockLevel(getOverclockLevel() + 1);
        } else if ("overclock_sub".equals(componentData)) {
            setOverclockLevel(getOverclockLevel() - 1);
        }
    }

    /**
     * 模块自身没有超净间时，直接使用梦想电梯本体上安装的超净维护子系统。
     * 这样只需要在电梯本体上装一个子系统，所有连接模块都能执行超净配方。
     */
    @Override
    public @Nullable ICleanroomProvider getCleanroom() {
        ICleanroomProvider own = super.getCleanroom();
        if (own != null) {
            return own;
        }
        DreamElevatorMachine currentHost = getHost();
        return currentHost != null && currentHost.isFormed() ? currentHost.getCleanroom() : null;
    }

    public int getModuleParallel() {
        return MODULE_PARALLELS[moduleTier - 1];
    }

    public static int getParallelForTier(int tier) {
        return MODULE_PARALLELS[tier - 1];
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
        // (0.8 / 动力模块等级) * 电压等级 + 生产模块等级 / 10
        double multiplier = (0.8D / powerTier) * voltageTier + moduleTier / 10.0D;
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
        int recipeVoltageTier = RecipeHelper.getRecipeEUtTier(recipe);
        if (recipeVoltageTier > currentHost.getMaxRecipeTier()) {
            return null;
        }
        var parallelResult = GTRecipeModifiers.accurateParallel(machine, recipe,
                module.getEffectiveParallel(recipe), false);
        int parallel = parallelResult.getSecond();
        GTRecipe parallelRecipe = parallelResult.getFirst();
        if (parallel <= 0 || parallelRecipe == null) {
            return null;
        }
        GTRecipe modified = parallelRecipe.copy();
        if (!applyEnvironmentalConditions(module, modified)) {
            return null;
        }
        long parallelEUt = RecipeHelper.getInputEUt(parallelRecipe);
        long modifiedEUt = Math.max(1L, Math.round(parallelEUt *
                module.getSteamMultiplier(recipeVoltageTier) *
                currentHost.getPressurizationConsumptionMultiplier()));
        RecipeHelper.setInputEUt(modified, modifiedEUt);
        double speedMultiplier = currentHost.getProductionSpeedMultiplier();
        if (speedMultiplier != 1.0D) {
            modified.duration = (int) Math.max(1L, Math.round(modified.duration / speedMultiplier));
        }
        return modified;
    }

    /**
     * 把模块无法原生满足的环境条件改写成梦想电梯体系可以满足的形式：
     * <ul>
     *     <li>碎岩：条件改为检查模块自己的流体输入仓（而不是控制器旁边的流体方块）；</li>
     *     <li>重力：读取梦想电梯本体上重力控制子系统提供的重力值；</li>
     *     <li>超净：由模块的 {@link #getCleanroom()} 转发到梦想电梯的超净维护子系统。</li>
     * </ul>
     *
     * @return false 表示当前条件不满足，配方不能开始
     */
    private static boolean applyEnvironmentalConditions(SteamProcessingModuleMachine module, GTRecipe recipe) {
        if (recipe.conditions.isEmpty()) {
            return true;
        }

        DreamElevatorMachine currentHost = module.getHost();
        Integer installedGravity = currentHost != null && currentHost.isFormed() ? currentHost.getInstalledGravity() : null;

        boolean hasRockBreaker = recipe.conditions.stream().anyMatch(RockBreakerCondition.class::isInstance);
        boolean rockBreakerFluids = hasRockBreaker && module.hasRockBreakerFluids(recipe);

        for (RecipeCondition condition : new ArrayList<>(recipe.conditions)) {
            if (condition instanceof RockBreakerCondition) {
                // 只认输入仓，不再使用 GT 默认的“控制器水平邻接流体方块”判定。
                boolean passes = rockBreakerFluids != condition.isReverse();
                if (!passes) {
                    return false;
                }
                recipe.conditions.remove(condition);
            } else if (condition instanceof GravityCondition gravityCondition && installedGravity != null) {
                JsonObject data = gravityCondition.serialize();
                if (!data.has("gravity")) {
                    continue;
                }
                boolean zero = data.get("gravity").getAsBoolean();
                boolean available = installedGravity == (zero ? 0 : 100);
                boolean passes = available != gravityCondition.isReverse();
                if (!passes) {
                    return false;
                }
                recipe.conditions.remove(condition);
            }
        }
        return true;
    }

    /**
     * 检查模块自己的流体输入仓/输入总成中是否同时存在配方要求的两种岩破流体。
     * 岩破配方把流体写在 data 的 fluidA / fluidB 里，且不消耗它们。
     */
    private boolean hasRockBreakerFluids(GTRecipe recipe) {
        ResourceLocation fluidAId = ResourceLocation.tryParse(recipe.data.getString("fluidA"));
        ResourceLocation fluidBId = ResourceLocation.tryParse(recipe.data.getString("fluidB"));
        if (fluidAId == null || fluidBId == null) {
            return false;
        }
        Fluid fluidA = BuiltInRegistries.FLUID.get(fluidAId);
        Fluid fluidB = BuiltInRegistries.FLUID.get(fluidBId);
        if (fluidA == Fluids.EMPTY || fluidB == Fluids.EMPTY) {
            return false;
        }

        boolean hasA = false;
        boolean hasB = false;
        for (IO io : List.of(IO.IN, IO.BOTH)) {
            List<IRecipeHandler<?>> handlers = getCapabilitiesProxy().get(io, FluidRecipeCapability.CAP);
            if (handlers == null) {
                continue;
            }
            for (IRecipeHandler<?> handler : handlers) {
                if (!(handler instanceof NotifiableFluidTank tank)) {
                    continue;
                }
                for (int i = 0; i < tank.getTanks(); i++) {
                    FluidStack stack = tank.getFluidInTank(i);
                    if (stack.isEmpty()) {
                        continue;
                    }
                    if (stack.getFluid() == fluidA) {
                        hasA = true;
                    }
                    if (stack.getFluid() == fluidB) {
                        hasB = true;
                    }
                }
            }
        }
        return hasA && hasB;
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        IDisplayUIMachine.super.addDisplayText(textList);
        if (!isFormed()) return;
        textList.add(Component.translatable("vividtech.machine.steam_processing_module.tier", VividTech.toRoman(moduleTier)));
        DreamElevatorMachine voltageHost = getHost();
        if (voltageHost != null && voltageHost.isFormed()) {
            textList.add(Component.translatable("vividtech.machine.steam_processing_module.max_recipe_voltage",
                    Component.literal(GTValues.VNF[voltageHost.getMaxRecipeTier()])));
        }
        textList.add(Component.translatable("vividtech.machine.steam_processing_module.parallel",
                getModuleParallel()));
        DreamElevatorMachine currentHost = getHost();
        GTRecipe displayRecipe = getRecipeLogic().getLastOriginRecipe();
        if (currentHost != null) {
            int availableOverclock = getAvailableOverclock(displayRecipe);
            textList.add(Component.translatable("vividtech.machine.steam_processing_module.overclock",
                            getOverclockLevel(), availableOverclock)
                    .append(ComponentPanelWidget.withButton(Component.literal(" [-]"), "overclock_sub"))
                    .append(ComponentPanelWidget.withButton(Component.literal("[+]"), "overclock_add")));
            textList.add(Component.translatable(
                    "vividtech.machine.steam_processing_module.overclock_hint"));
            textList.add(Component.translatable("vividtech.machine.steam_processing_module.effective_parallel",
                    getEffectiveParallel(displayRecipe)));
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
