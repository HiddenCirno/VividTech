package com.vividtech.common.machine.multiblock.part;

import com.vividtech.common.gui.ShapeFancyConfigurator;

import org.gtlcore.gtlcore.api.machine.trait.IRecipeCapabilityMachine;

import com.gregtechceu.gtceu.api.capability.IControllable;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfiguratorButton;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IDistinctPart;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.tterrag.registrate.util.entry.ItemEntry;

import java.util.List;

/**
 * 熔铸输入仓：与原版同级输入仓容量一致，并提供一个隐藏的虚拟模具槽。
 */
public class CastingInputHatchPartMachine extends FluidHatchPartMachine implements IDistinctPart {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            CastingInputHatchPartMachine.class, FluidHatchPartMachine.MANAGED_FIELD_HOLDER);

    /** 默认隔离，避免多个熔铸输入仓的虚拟模具槽被合并匹配。 */
    @Persisted
    @DescSynced
    private boolean distinct = true;

    public CastingInputHatchPartMachine(IMachineBlockEntity holder, int tier) {
        super(holder, tier, IO.IN, INITIAL_TANK_CAPACITY_1X, 1);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    protected NotifiableItemStackHandler createCircuitItemHandler(Object... args) {
        return new NotifiableItemStackHandler(this, 1, IO.IN, IO.NONE)
                .setFilter(CastingInputHatchPartMachine::isMold);
    }

    @Override
    public void attachConfigurators(ConfiguratorPanel panel) {
        // 保留输入仓的电源按钮，但不调用 FluidHatchPartMachine 的默认实现，
        // 否则左侧会额外出现编程电路配置页。
        IControllable controllable = this;
        panel.attachConfigurators(new IFancyConfiguratorButton.Toggle(
                GuiTextures.BUTTON_POWER.getSubTexture(0, 0, 1, 0.5),
                GuiTextures.BUTTON_POWER.getSubTexture(0, 0.5, 1, 0.5),
                controllable::isWorkingEnabled,
                (click, value) -> controllable.setWorkingEnabled(value))
                .setTooltipsSupplier(pressed -> List.of(Component.translatable(pressed ?
                        "behaviour.soft_hammer.enabled" : "behaviour.soft_hammer.disabled"))));

        panel.attachConfigurators(new IFancyConfiguratorButton.Toggle(
                GuiTextures.BUTTON_DISTINCT_BUSES.getSubTexture(0, 0.5, 1, 0.5),
                GuiTextures.BUTTON_DISTINCT_BUSES.getSubTexture(0, 0, 1, 0.5),
                this::isDistinct,
                (click, value) -> setDistinct(value))
                .setTooltipsSupplier(pressed -> List.of(Component.translatable("gtceu.multiblock.universal.distinct")
                        .withStyle(ChatFormatting.YELLOW)
                        .append(Component.translatable(pressed ?
                                "gtceu.multiblock.universal.distinct.yes" :
                                "gtceu.multiblock.universal.distinct.no")))));

        panel.attachConfigurators(new ShapeFancyConfigurator(getCircuitInventory().storage,
                ShapeFancyConfigurator.Kind.MOLD));
    }

    @Override
    public boolean isDistinct() {
        return distinct;
    }

    @Override
    public void setDistinct(boolean distinct) {
        if (this.distinct == distinct) {
            return;
        }
        this.distinct = distinct;
        markDirty();
    }

    @Override
    public void addedToController(IMultiController controller) {
        super.addedToController(controller);
        // GT 的部件级 IDistinctPart 只隔离物品侧；虚拟模具与流体必须让控制器开启
        // 整体 distinct 模式，才能把每个熔铸仓的流体和模具放进同一个独立配方组。
        var controllerMachine = controller.self();
        if (!controllerMachine.isRemote() &&
                controller instanceof IRecipeCapabilityMachine capabilityMachine &&
                !capabilityMachine.isDistinct()) {
            capabilityMachine.setDistinct(true);
            controllerMachine.markDirty();
        }
    }

    @Override
    public void onMachineRemoved() {
        super.onMachineRemoved();
        getCircuitInventory().storage.setStackInSlot(0, ItemStack.EMPTY);
    }

    public static boolean isMold(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        Item item = stack.getItem();
        for (ItemEntry<Item> entry : GTItems.SHAPE_MOLDS) {
            if (entry != null && entry.get() == item) {
                return true;
            }
        }
        return false;
    }
}
