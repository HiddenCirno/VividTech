package com.vividtech.common.gui;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfigurator;
import com.gregtechceu.gtceu.common.data.GTItems;

import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.tterrag.registrate.util.entry.ItemEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * 虚拟模具/模头选择页：锻压输入总线只会看到模头，熔铸输入仓只会看到模具。
 */
public class ShapeFancyConfigurator implements IFancyConfigurator {

    public enum Kind {

        MOLD("vividtech.gui.casting_input_hatch.mold",
                "vividtech.gui.casting_input_hatch.mold.tooltip"),
        EXTRUDER("vividtech.gui.forging_input_bus.extruder",
                "vividtech.gui.forging_input_bus.extruder.tooltip");

        private final String titleKey;
        private final String tooltipKey;

        Kind(String titleKey, String tooltipKey) {
            this.titleKey = titleKey;
            this.tooltipKey = tooltipKey;
        }
    }

    private static final int WIDTH = 174;
    private static final int HEIGHT = 150;
    private static final int GRID_X = 5;
    private static final int GRID_Y = 48;
    private static final int COLUMNS = 9;
    private static final int CELL = 18;

    private static final List<ItemStack> MOLDS = buildItems(GTItems.SHAPE_MOLDS);
    private static final List<ItemStack> EXTRUDERS = buildItems(GTItems.SHAPE_EXTRUDERS);

    private final ItemStackTransfer slot;
    private final Kind kind;

    public ShapeFancyConfigurator(ItemStackTransfer slot, Kind kind) {
        this.slot = slot;
        this.kind = kind;
    }

    private static List<ItemStack> buildItems(ItemEntry<Item>[] entries) {
        List<ItemStack> items = new ArrayList<>();
        for (ItemEntry<Item> entry : entries) {
            if (entry != null) {
                items.add(entry.asStack());
            }
        }
        return List.copyOf(items);
    }

    @Override
    public Component getTitle() {
        return Component.translatable(kind.titleKey);
    }

    @Override
    public IGuiTexture getIcon() {
        ItemStack selected = slot.getStackInSlot(0);
        return new ItemStackTexture(selected.isEmpty() ? GTItems.SHAPE_EMPTY.asStack() : selected);
    }

    @Override
    public Widget createConfigurator() {
        WidgetGroup group = new WidgetGroup(0, 0, WIDTH, HEIGHT);
        group.addWidget(new LabelWidget(9, 8, getTitle().getString()));
        group.addWidget(new SlotWidget(slot, 0, (WIDTH - CELL) / 2, 22, false, false)
                .setBackgroundTexture(new GuiTextureGroup(GuiTextures.SLOT, GuiTextures.MOLD_OVERLAY)));

        int index = 0;
        for (ItemStack item : kind == Kind.MOLD ? MOLDS : EXTRUDERS) {
            int x = GRID_X + (index % COLUMNS) * CELL;
            int y = GRID_Y + (index / COLUMNS) * CELL;
            group.addWidget(new ButtonWidget(x, y, CELL, CELL,
                    new ItemStackTexture(item),
                    click -> {
                        if (click.isRemote) {
                            return;
                        }
                        slot.setStackInSlot(0, item.copy());
                        slot.onContentsChanged(0);
                    })
                    .setHoverTooltips(item.getHoverName()));
            index++;
        }

        int clearX = GRID_X + (index % COLUMNS) * CELL;
        int clearY = GRID_Y + (index / COLUMNS) * CELL;
        group.addWidget(new ButtonWidget(clearX, clearY, CELL, CELL,
                new ItemStackTexture(new ItemStack(Items.BARRIER)),
                click -> {
                    if (click.isRemote) {
                        return;
                    }
                    slot.setStackInSlot(0, ItemStack.EMPTY);
                    slot.onContentsChanged(0);
                })
                .setHoverTooltips(Component.translatable("vividtech.gui.mold_selection.clear")));

        return group;
    }

    @Override
    public List<Component> getTooltips() {
        return List.of(Component.translatable(kind.tooltipKey));
    }
}
