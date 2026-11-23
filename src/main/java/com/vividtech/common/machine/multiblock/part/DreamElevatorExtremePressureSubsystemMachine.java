package com.vividtech.common.machine.multiblock.part;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredPartMachine;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * 梦想电梯极限加压子系统。
 *
 * <p>安装在梦想电梯本体的 S/X 仓位上。主机检测到它后，会在主机 GUI 中解锁极限超频选项；
 * 模块原本的手动超频将被主机的有损超频覆盖。</p>
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DreamElevatorExtremePressureSubsystemMachine extends TieredPartMachine {

    public DreamElevatorExtremePressureSubsystemMachine(IMachineBlockEntity holder) {
        super(holder, GTValues.EV);
    }
}
