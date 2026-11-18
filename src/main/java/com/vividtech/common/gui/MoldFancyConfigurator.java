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
 * 熔铸输入总线的左侧配置页：从 GT 的全部铸模/挤出模中选择一个虚拟模具。
 */
public class MoldFancyConfigurator implements IFancyConfigurator {

    private static final int WIDTH = 174;
    private static final int HEIGHT = 150;
    private static final int GRID_X = 5;
    private static final int GRID_Y = 48;
    private static final int COLUMNS = 9;
    private static final int CELL = 18;

    private static final List<ItemStack> MOLDS = buildMolds();

    private final ItemStackTransfer slot;

    public MoldFancyConfigurator(ItemStackTransfer slot) {
        this.slot = slot;
    }

    private static List<ItemStack> buildMolds() {
        List<ItemStack> molds = new ArrayList<>();
        for (ItemEntry<Item> entry : GTItems.SHAPE_MOLDS) {
            if (entry != null) {
                molds.add(entry.asStack());
            }
        }
        for (ItemEntry<Item> entry : GTItems.SHAPE_EXTRUDERS) {
            if (entry != null) {
                molds.add(entry.asStack());
            }
        }
        return List.copyOf(molds);
    }

    @Override
    public Component getTitle() {
        return Component.translatable("vividtech.gui.casting_input_bus.mold");
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
        for (ItemStack mold : MOLDS) {
            int x = GRID_X + (index % COLUMNS) * CELL;
            int y = GRID_Y + (index / COLUMNS) * CELL;
            group.addWidget(new ButtonWidget(x, y, CELL, CELL,
                    new ItemStackTexture(mold),
                    click -> {
                        if (click.isRemote) {
                            return;
                        }
                        slot.setStackInSlot(0, mold.copy());
                        slot.onContentsChanged(0);
                    })
                    .setHoverTooltips(mold.getHoverName()));
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
                .setHoverTooltips(Component.translatable("vividtech.gui.casting_input_bus.mold.clear")));

        return group;
    }

    @Override
    public List<Component> getTooltips() {
        return List.of(Component.translatable("vividtech.gui.casting_input_bus.mold.tooltip"));
    }
}
