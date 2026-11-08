package com.vividtech.common.machine.multiblock.steam;

import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineModule;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;

import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 梦想电梯蒸汽加压模块。
 *
 * <p>
 * 本身不加工任何配方，只提供全局加压加成：安装 4 个时配方电压上限 +1、工作速度 ×2；
 * 安装 16 个时配方电压上限 +2、工作速度 ×4。数量由梦想电梯实时统计。
 * </p>
 */
public class PressurizationModuleMachine extends MetaMachine
        implements IModularMachineModule<DreamElevatorMachine, PressurizationModuleMachine>, IMachineLife {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            PressurizationModuleMachine.class, MetaMachine.MANAGED_FIELD_HOLDER);

    private static final int CONNECTION_CHECK_INTERVAL_TICKS = 20;

    @Persisted
    @Nullable
    @Getter
    @Setter
    private BlockPos hostPosition;

    @Nullable
    @Getter
    @Setter
    private DreamElevatorMachine host;

    @Nullable
    private TickableSubscription connectionSubscription;

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
    public boolean isFormed() {
        return !isInValid();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote() && connectionSubscription == null) {
            connectionSubscription = subscribeServerTick(this::connectionTick);
        }
    }

    @Override
    public void onUnload() {
        if (connectionSubscription != null) {
            connectionSubscription.unsubscribe();
            connectionSubscription = null;
        }
        super.onUnload();
    }

    @Override
    public void onMachineRemoved() {
        removeFromHost(getHost());
    }

    private void connectionTick() {
        if (isRemote()) {
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
}
