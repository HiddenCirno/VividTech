package com.vividtech.common.machine.multiblock.part;

import org.gtlcore.gtlcore.common.machine.multiblock.part.maintenance.IGravityPartMachine;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.gui.widget.IntInputWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredPartMachine;

import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * 梦想电梯重力控制子系统。
 *
 * <p>
 * 安装在梦想电梯本体上的仓室，向梦想电梯主机提供重力值。默认 0（零重力），
 * 可以在仓室界面里调整为 0–100，以兼容要求普通重力的配方。
 * 模块在 {@code recipeModifier} 中读取梦想电梯当前的重力，并据此处理重力条件。
 * </p>
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DreamElevatorGravitySubsystemMachine extends TieredPartMachine implements IGravityPartMachine {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            DreamElevatorGravitySubsystemMachine.class, TieredPartMachine.MANAGED_FIELD_HOLDER);

    @Persisted
    @DescSynced
    private int gravity = 0;

    public DreamElevatorGravitySubsystemMachine(IMachineBlockEntity holder) {
        super(holder, GTValues.IV);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public int getCurrentGravity() {
        return gravity;
    }

    @Override
    public void setCurrentGravity(int gravity) {
        this.gravity = Mth.clamp(gravity, 0, 100);
        markDirty();
    }

    @Override
    public Widget createUIWidget() {
        WidgetGroup group = new WidgetGroup(0, 0, 100, 20);
        group.addWidget(new IntInputWidget(this::getCurrentGravity, this::setCurrentGravity).setMin(0).setMax(100));
        return group;
    }
}
