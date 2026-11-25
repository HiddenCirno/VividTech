package com.vividtech.common.machine.multiblock.part;

import com.vividtech.integration.ae2.widget.AEPagedPatternViewExtendSlotWidget;

import org.gtlcore.gtlcore.api.gui.MEPatternCatalystUIManager;
import org.gtlcore.gtlcore.common.machine.multiblock.part.ae.MEPatternBufferPartMachine;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.integration.ae2.gui.widget.AETextInputButtonWidget;

import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import appeng.crafting.pattern.EncodedPatternItem;
import org.jetbrains.annotations.NotNull;

public class MEUltimateExtendedPatternBufferPartMachine extends MEPatternBufferPartMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            MEUltimateExtendedPatternBufferPartMachine.class, MEPatternBufferPartMachine.MANAGED_FIELD_HOLDER);

    public static final int PATTERN_SLOTS = 576;
    public static final int SLOTS_PER_PAGE = 72;
    public static final int SHARED_CATALYST_SLOTS = 36;

    private int currentPage = 0;

    public MEUltimateExtendedPatternBufferPartMachine(IMachineBlockEntity holder) {
        super(holder, PATTERN_SLOTS, IO.BOTH);
    }

    @Override
    public @NotNull ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    public int getPageCount() {
        return Math.max(1, (maxPatternCount + SLOTS_PER_PAGE - 1) / SLOTS_PER_PAGE);
    }

    public int getCurrentPage() {
        return Mth.clamp(currentPage, 0, getPageCount() - 1);
    }

    public void setCurrentPage(int page) {
        currentPage = Mth.clamp(page, 0, getPageCount() - 1);
    }

    @Override
    public @NotNull Widget createUIWidget() {
        int rowSize = 9;
        int colSize = Math.min(8, (maxPatternCount + rowSize - 1) / rowSize);
        var group = new WidgetGroup(0, 0, 18 * rowSize + 16, 18 * colSize + 16) {

            @Override
            public boolean mouseWheelMove(double mouseX, double mouseY, double wheelDelta) {
                if (getPageCount() > 1 && isMouseOverElement(mouseX, mouseY) && wheelDelta != 0) {
                    setCurrentPage(getCurrentPage() + (wheelDelta > 0 ? -1 : 1));
                    return true;
                }
                return super.mouseWheelMove(mouseX, mouseY, wheelDelta);
            }
        };

        group.addWidget(new LabelWidget(8, 2,
                () -> isOnline() ? "gtceu.gui.me_network.online" : "gtceu.gui.me_network.offline"));

        if (getPageCount() > 1) {
            int[] pageLayout = getPageHeaderLayout(rowSize);
            int previousX = pageLayout[0];
            int pageX = pageLayout[1];
            int nextX = pageLayout[2];
            int arrowWidth = pageLayout[3];

            group.addWidget(createPageTextButton(previousX, "<", arrowWidth,
                    () -> setCurrentPage(getCurrentPage() - 1)));

            // The page state is client-only, so the label must re-evaluate its supplier on the client.
            // A regular (server-synced) LabelWidget only refreshes through the server's lastTextValue.
            group.addWidget(new LabelWidget(pageX, 2,
                    () -> (getCurrentPage() + 1) + "/" + getPageCount()).setClientSideWidget());

            group.addWidget(createPageTextButton(nextX, ">", arrowWidth,
                    () -> setCurrentPage(getCurrentPage() + 1)));
        }

        group.addWidget(new AETextInputButtonWidget(18 * rowSize + 8 - 70, 2, 70, 10)
                .setText(customName)
                .setOnConfirm(this::setCustomName)
                .setButtonTooltips(Component.translatable("gui.gtceu.rename.desc")));

        final var catalystUIManager = new MEPatternCatalystUIManager(group.getSizeWidth() + 4, catalystItems,
                catalystFluids, cacheRecipeCount, this::removeSlotFromGTRecipeCache);
        group.waitToAdded(catalystUIManager);

        for (int index = 0; index < maxPatternCount; index++) {
            int finalI = index;
            int local = index % SLOTS_PER_PAGE;
            int x = local % rowSize;
            int y = local / rowSize;

            var slot = new AEPagedPatternViewExtendSlotWidget(getPatternInventory(), index,
                    x * 18 + 8, y * 18 + 14, SLOTS_PER_PAGE, this::getCurrentPage)
                    .setOnMiddleClick(() -> catalystUIManager.toggleFor(finalI))
                    .setOnPatternSlotChanged(() -> this.onPatternChange(finalI))
                    .setOccupiedTexture(GuiTextures.SLOT)
                    .setItemHook(stack -> {
                        if (!stack.isEmpty() && stack.getItem() instanceof EncodedPatternItem iep) {
                            final ItemStack out = iep.getOutput(stack);
                            if (!out.isEmpty()) return out;
                        }
                        return stack;
                    })
                    .setOnAddedTooltips((s, l) -> {
                        if (cacheRecipe[finalI])
                            l.add(Component.translatable("gtceu.machine.pattern.recipe.cache"));
                    })
                    .setBackground(GuiTextures.SLOT, GuiTextures.PATTERN_OVERLAY);
            group.addWidget(slot);
        }
        return group;
    }

    /**
     * Centres the page number and its text buttons in the free space between the network status
     * label and the rename input.
     */
    private int[] getPageHeaderLayout(int rowSize) {
        int renameX = 18 * rowSize + 8 - 70;
        int networkEnd = 8 + measureTextWidth(
                Component.translatable("gtceu.gui.me_network.online").getString(), 55);
        int gapLeft = networkEnd + 2;
        int gapRight = renameX - 2;

        String pageText = (getCurrentPage() + 1) + "/" + getPageCount();
        int pageWidth = measureTextWidth(pageText, 14);
        int arrowWidth = Math.max(4, measureTextWidth("<", 5));
        int totalWidth = arrowWidth * 2 + pageWidth + 4;
        int start = (gapLeft + gapRight - totalWidth) / 2;
        if (start < gapLeft) start = gapLeft;
        if (start + totalWidth > gapRight) start = Math.max(gapLeft, gapRight - totalWidth);

        int pageX = start + arrowWidth + 2;
        return new int[] { start, pageX, pageX + pageWidth + 2, arrowWidth };
    }

    private int measureTextWidth(String text, int fallback) {
        return isRemote() ? Minecraft.getInstance().font.width(text) : fallback;
    }

    private Widget createPageTextButton(int x, String text, int width, Runnable action) {
        return new ButtonWidget(x, 2, width, 10, new TextTexture(text), clickData -> action.run());
    }
}
