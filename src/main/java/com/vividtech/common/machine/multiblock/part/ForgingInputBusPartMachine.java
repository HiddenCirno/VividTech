package com.vividtech.common.machine.multiblock.part;

import com.vividtech.common.gui.ShapeFancyConfigurator;

import com.gregtechceu.gtceu.api.capability.IControllable;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfiguratorButton;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IDistinctPart;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.machine.multiblock.part.ItemBusPartMachine;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.tterrag.registrate.util.entry.ItemEntry;

import java.util.List;

/**
 * 锻压输入总线：与原版同级输入总线容量一致，但隐藏槽从虚拟编程电路改为虚拟模头。
 */
public class ForgingInputBusPartMachine extends ItemBusPartMachine implements IDistinctPart {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            ForgingInputBusPartMachine.class, ItemBusPartMachine.MANAGED_FIELD_HOLDER);

    public ForgingInputBusPartMachine(IMachineBlockEntity holder, int tier) {
        super(holder, tier, IO.IN);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    protected NotifiableItemStackHandler createCircuitItemHandler(Object... args) {
        return new NotifiableItemStackHandler(this, 1, IO.IN, IO.NONE)
                .setFilter(ForgingInputBusPartMachine::isExtruder);
    }

    @Override
    public void attachConfigurators(ConfiguratorPanel panel) {
        // 手动保留电源/总线隔离按钮，但不调用 ItemBusPartMachine 的默认实现，
        // 否则左侧会额外出现编程电路配置页。
        IControllable controllable = this;
        panel.attachConfigurators(new IFancyConfiguratorButton.Toggle(
                GuiTextures.BUTTON_POWER.getSubTexture(0, 0, 1, 0.5),
                GuiTextures.BUTTON_POWER.getSubTexture(0, 0.5, 1, 0.5),
                controllable::isWorkingEnabled,
                (click, value) -> controllable.setWorkingEnabled(value))
                .setTooltipsSupplier(pressed -> List.of(Component.translatable(pressed ?
                        "behaviour.soft_hammer.enabled" : "behaviour.soft_hammer.disabled"))));

        // 与 IDistinctPart 默认实现一致：上半张是“已隔离”，下半张是“未隔离”。
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
                ShapeFancyConfigurator.Kind.EXTRUDER));
    }

    @Override
    public void onMachineRemoved() {
        super.onMachineRemoved();
        getCircuitInventory().storage.setStackInSlot(0, ItemStack.EMPTY);
    }

    public static boolean isExtruder(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        Item item = stack.getItem();
        for (ItemEntry<Item> entry : GTItems.SHAPE_EXTRUDERS) {
            if (entry != null && entry.get() == item) {
                return true;
            }
        }
        return false;
    }
}
