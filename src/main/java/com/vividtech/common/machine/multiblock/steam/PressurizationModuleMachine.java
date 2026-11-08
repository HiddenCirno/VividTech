package com.vividtech.common.machine.multiblock.steam;

import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineModule;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IDisplayUIMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;

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
 * 梦想电梯蒸汽加压模块。
 *
 * <p>
 * 与普通蒸汽模块一样是 2×1×5 的多方块结构，可以被软锤/界面启停，但没有配方、没有输入输出。
 * 只有已成型、已连接且处于启用状态的加压模块才会计入主机加成：
 * 4 个时配方电压上限 +1、工作速度 ×2；16 个时 +2、×4。
 * </p>
 */
public class PressurizationModuleMachine extends WorkableMultiblockMachine
        implements IModularMachineModule<DreamElevatorMachine, PressurizationModuleMachine>,
        IMachineLife, IDisplayUIMachine {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            PressurizationModuleMachine.class, WorkableMultiblockMachine.MANAGED_FIELD_HOLDER);

    private static final int CONNECTION_CHECK_INTERVAL_TICKS = 20;

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

    public PressurizationModuleMachine(IMachineBlockEntity holder) {
        super(holder);
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
        return new BlockPos[0];
    }

    @Override
    public void onConnected(@NotNull DreamElevatorMachine host) {
        updateConnectionSubscription();
        recipeLogic.updateTickSubscription();
    }

    @Override
    public void onDisconnected() {
        updateConnectionSubscription();
        recipeLogic.updateTickSubscription();
    }

    @Override
    public void onLoad() {
        super.onLoad();
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
        findAndConnectToHost();
        updateConnectionSubscription();
        recipeLogic.updateTickSubscription();
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
        if (getOffsetTimer() % CONNECTION_CHECK_INTERVAL_TICKS != 0) {
            return;
        }
        DreamElevatorMachine currentHost = getHost();
        if (currentHost != null && !currentHost.isFormed()) {
            removeFromHost(currentHost);
            currentHost = null;
        }
        if (currentHost == null) {
            findAndConnectToHost();
        }
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        if (!isFormed()) {
            return;
        }
        DreamElevatorMachine currentHost = getHost();
        boolean connected = currentHost != null && currentHost.isFormed();
        int active = connected ? currentHost.getPressurizationModuleCount() : 0;
        int total = connected ? currentHost.getPressurizationModuleTotalCount() : 0;
        textList.add(Component.translatable("vividtech.machine.pressurization_module.active_count",
                active, total));
        textList.add(Component.translatable(connected ?
                "vividtech.machine.pressurization_module.connected" :
                "vividtech.machine.pressurization_module.disconnected"));
    }
}
