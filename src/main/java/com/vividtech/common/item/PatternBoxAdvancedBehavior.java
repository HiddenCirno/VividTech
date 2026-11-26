package com.vividtech.common.item;

import org.gtlcore.gtlcore.common.item.PatternBoxBehavior;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.IntInputWidget;
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour;

import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.SwitchWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;


import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Pattern box with the pattern buffer's batch circuit writer.
 */
public class PatternBoxAdvancedBehavior extends PatternBoxBehavior {

    public static final PatternBoxAdvancedBehavior INSTANCE = new PatternBoxAdvancedBehavior();

    private static final int COLS = 9;
    private static final int SLOTS_PER_PAGE = 36;
    private static final int ROWS = SLOTS_PER_PAGE / COLS;
    private static final int SLOT_SIZE = 18;
    private static final int LEFT = 7;
    private static final int PATTERN_TOP = 42;
    private static final int INV_TOP = PATTERN_TOP + ROWS * SLOT_SIZE + 8;
    private static final int WIDTH = 176;
    private static final int HEIGHT = INV_TOP + 82;
    private static final int SIDE_WIDTH = 132;
    private static final int SIDE_PANEL_WIDTH = SIDE_WIDTH - 4;
    private static final int UI_WIDTH = WIDTH + SIDE_WIDTH;
    private static final int PAGE_BUTTON_Y = 24;
    private static final int PREVIOUS_PAGE_BUTTON_X = 136;
    private static final int PAGE_LABEL_X = 104;
    private static final int PAGE_LABEL_Y = 29;
    private static final int NEXT_PAGE_BUTTON_X = 154;
    private static final int PAGE_BUTTON_SIZE = 16;

    @Override
    public ModularUI createUI(HeldItemUIFactory.HeldItemHolder holder, Player player) {
        ItemStack pouch = holder.getHeld();
        ItemStackTransfer inventory = getInventory(pouch);
        inventory.setOnContentsChanged(() -> {
            saveInventory(pouch, inventory);
            holder.markAsDirty();
        });

        WidgetGroup group = new WidgetGroup(0, 0, UI_WIDTH, HEIGHT);
        group.addWidget(new LabelWidget(LEFT, 6,
                () -> Component.translatable("item.vividtech.pattern_box_advanced").getString()));

        // One page of pattern slots: 4 rows x 9 columns.
        int[] page = { 0 };
        WidgetGroup patternPage = new WidgetGroup(0, 0, WIDTH, PATTERN_TOP + ROWS * SLOT_SIZE);
        rebuildPatternPage(patternPage, inventory, page);
        group.addWidget(patternPage);

        Inventory playerInv = player.getInventory();
        int heldSlot = holder.getHand() == InteractionHand.MAIN_HAND ? playerInv.selected : -1;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < COLS; col++) {
                int index = col + (row + 1) * COLS;
                group.addWidget(new SlotWidget(playerInv, index,
                        LEFT + col * SLOT_SIZE, INV_TOP + row * SLOT_SIZE, true, true)
                        .setLocationInfo(true, false)
                        .setBackgroundTexture(GuiTextures.SLOT));
            }
        }
        int hotbarY = INV_TOP + 3 * SLOT_SIZE + 4;
        for (int col = 0; col < COLS; col++) {
            boolean locked = col == heldSlot;
            group.addWidget(new SlotWidget(playerInv, col,
                    LEFT + col * SLOT_SIZE, hotbarY, !locked, !locked)
                    .setLocationInfo(true, true)
                    .setBackgroundTexture(GuiTextures.SLOT));
        }

        addCircuitWidgets(group, inventory, player);

        return new ModularUI(UI_WIDTH, HEIGHT, holder, player)
                .widget(group)
                .background(GuiTextures.BACKGROUND);
    }

    private void addCircuitWidgets(WidgetGroup group, ItemStackTransfer inventory, Player player) {
        WidgetGroup circuitPanel = new WidgetGroup(WIDTH + 2, 0, SIDE_PANEL_WIDTH, HEIGHT);
        circuitPanel.setBackground(GuiTextures.BACKGROUND);
        group.addWidget(circuitPanel);
        circuitPanel.setVisible(false).setActive(false);

        int[] circuitConfig = { 1 };
        boolean[] skipExisting = { true };

        circuitPanel.addWidget(new LabelWidget(4, 28,
                "gui.vividtech.pattern_box_advanced_circuit_title").setTextColor(0xFAF9F6));
        circuitPanel.addWidget(new LabelWidget(4, 48,
                "gui.vividtech.pattern_box_advanced_circuit_number"));
        circuitPanel.addWidget(new IntInputWidget(4, 60, SIDE_PANEL_WIDTH - 8, 14,
                () -> circuitConfig[0], value -> circuitConfig[0] = value)
                .setMin(0)
                .setMax(IntCircuitBehaviour.CIRCUIT_MAX)
                .setHoverTooltips(Component.translatable("tooltip.vividtech.pattern_box_advanced_circuit_number",
                        IntCircuitBehaviour.CIRCUIT_MAX)));

        circuitPanel.addWidget(new LabelWidget(4, 82,
                "gui.vividtech.pattern_box_advanced_skip_existing"));
        circuitPanel.addWidget(new SwitchWidget(SIDE_PANEL_WIDTH - 40, 79, 36, 14,
                (clickData, pressed) -> skipExisting[0] = pressed)
                .setSupplier(() -> skipExisting[0])
                .setPressed(skipExisting[0])
                .setTexture(
                        new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("OFF")),
                        new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("ON")))
                .setHoverTooltips(Component.translatable("tooltip.vividtech.pattern_box_advanced_skip_existing")));

        circuitPanel.addWidget(new ButtonWidget(4, 104, SIDE_PANEL_WIDTH / 2 - 6, 18,
                new GuiTextureGroup(GuiTextures.BUTTON,
                        new TextTexture(() -> Component.translatable("gui.vividtech.pattern_box_advanced_apply_circuit").getString())),
                clickData -> applyCircuit(inventory, circuitConfig[0], !skipExisting[0], player))
                .setHoverTooltips(Component.translatable("tooltip.vividtech.pattern_box_advanced_apply_circuit")));
        circuitPanel.addWidget(new ButtonWidget(SIDE_PANEL_WIDTH / 2 + 2, 104, SIDE_PANEL_WIDTH / 2 - 6, 18,
                new GuiTextureGroup(GuiTextures.BUTTON,
                        new TextTexture(() -> Component.translatable("gui.vividtech.pattern_box_advanced_clear_circuits").getString())),
                clickData -> clearCircuits(inventory, player))
                .setHoverTooltips(Component.translatable("tooltip.vividtech.pattern_box_advanced_clear_circuits")));

        group.addWidget(new ButtonWidget(WIDTH + 2, 6, 18, 18,
                new GuiTextureGroup(GuiTextures.BUTTON, GuiTextures.INT_CIRCUIT_OVERLAY),
                clickData -> {
                    boolean show = !circuitPanel.isVisible();
                    circuitPanel.setVisible(show).setActive(show);
                })
                .setHoverTooltips(Component.translatable("tooltip.vividtech.pattern_box_advanced_open_circuit")));
    }

    private static void applyCircuit(ItemStackTransfer inventory, int circuitConfig, boolean replaceExisting,
                                     Player player) {
        int safeCircuitConfig = Math.max(0, Math.min(IntCircuitBehaviour.CIRCUIT_MAX, circuitConfig));
        int changed = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) continue;

            ItemStack after = PatternCircuitHelper.applyCircuit(stack, safeCircuitConfig, replaceExisting,
                    player.level());
            if (!after.isEmpty() && !ItemStack.matches(stack, after)) {
                inventory.setStackInSlot(slot, after);
                changed++;
            }
        }
        if (changed > 0) {
            inventory.onContentsChanged();
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.vividtech.pattern_box_advanced_circuit_applied", changed, safeCircuitConfig), true);
        }
    }

    private static void clearCircuits(ItemStackTransfer inventory, Player player) {
        int changed = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) continue;

            ItemStack after = PatternCircuitHelper.clearCircuit(stack, player.level());
            if (!after.isEmpty() && !ItemStack.matches(stack, after)) {
                inventory.setStackInSlot(slot, after);
                changed++;
            }
        }
        if (changed > 0) {
            inventory.onContentsChanged();
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.vividtech.pattern_box_advanced_circuits_cleared", changed), true);
        }
    }

    private static void rebuildPatternPage(WidgetGroup patternPage, ItemStackTransfer inventory, int[] page) {
        patternPage.clearAllWidgets();
        int pageCount = getPageCount();
        page[0] = Math.min(page[0], pageCount - 1);
        int startSlot = page[0] * SLOTS_PER_PAGE;
        int endSlot = Math.min(startSlot + SLOTS_PER_PAGE, inventory.getSlots());
        for (int i = startSlot; i < endSlot; i++) {
            int slotInPage = i - startSlot;
            int row = slotInPage / COLS;
            int col = slotInPage % COLS;
            patternPage.addWidget(new SlotWidget(inventory, i,
                    LEFT + col * SLOT_SIZE, PATTERN_TOP + row * SLOT_SIZE, true, true)
                    .setBackgroundTexture(GuiTextures.SLOT)
                    .setIngredientIO(IngredientIO.INPUT));
        }
        patternPage.addWidget(new ButtonWidget(PREVIOUS_PAGE_BUTTON_X, PAGE_BUTTON_Y, PAGE_BUTTON_SIZE,
                PAGE_BUTTON_SIZE, new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("<<")), clickData -> {
                    if (page[0] > 0) {
                        page[0]--;
                        rebuildPatternPage(patternPage, inventory, page);
                    }
                }));
        patternPage.addWidget(new LabelWidget(PAGE_LABEL_X, PAGE_LABEL_Y, (page[0] + 1) + " / " + pageCount));
        patternPage.addWidget(new ButtonWidget(NEXT_PAGE_BUTTON_X, PAGE_BUTTON_Y, PAGE_BUTTON_SIZE,
                PAGE_BUTTON_SIZE, new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture(">>")), clickData -> {
                    if (page[0] < pageCount - 1) {
                        page[0]++;
                        rebuildPatternPage(patternPage, inventory, page);
                    }
                }));
    }


}
