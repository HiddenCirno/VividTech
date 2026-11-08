package com.vividtech.common.machine.multiblock.part;

import org.gtlcore.gtlcore.api.machine.multiblock.GTLCleanroomType;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.ICleanroomReceiver;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.ICleanroomProvider;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.CleanroomType;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredPartMachine;

import net.minecraft.MethodsReturnNonnullByDefault;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * 梦想电梯超净维护子系统。
 *
 * <p>
 * 安装在梦想电梯本体上的仓室，固定提供 100% 洁净度，没有预热、没有洁净度衰减。
 * 它自身就是 {@link ICleanroomProvider}，成型时把 provider 挂到梦想电梯主机上；
 * 各蒸汽模块通过主机读取该 provider，因此不需要每个模块单独造超净间。
 * </p>
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DreamElevatorCleanroomSubsystemMachine extends TieredPartMachine implements ICleanroomProvider {

    private static final Set<CleanroomType> TYPES = Set.of(
            CleanroomType.CLEANROOM,
            CleanroomType.STERILE_CLEANROOM,
            GTLCleanroomType.LAW_CLEANROOM);

    public DreamElevatorCleanroomSubsystemMachine(IMachineBlockEntity holder) {
        super(holder, GTValues.IV);
    }

    @Override
    public Set<CleanroomType> getTypes() {
        return TYPES;
    }

    @Override
    public boolean isClean() {
        return true;
    }

    @Override
    public void addedToController(@NotNull IMultiController controller) {
        super.addedToController(controller);
        if (controller instanceof ICleanroomReceiver receiver) {
            receiver.setCleanroom(this);
        }
    }

    @Override
    public void removedFromController(@NotNull IMultiController controller) {
        if (controller instanceof ICleanroomReceiver receiver && receiver.getCleanroom() == this) {
            receiver.setCleanroom(null);
        }
        super.removedFromController(controller);
    }
}
