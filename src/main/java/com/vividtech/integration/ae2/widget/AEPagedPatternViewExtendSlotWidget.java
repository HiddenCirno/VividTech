package com.vividtech.integration.ae2.widget;

import org.gtlcore.gtlcore.integration.ae2.widget.AEPatternViewExtendSlotWidget;

import com.lowdragmc.lowdraglib.side.item.IItemTransfer;

import java.util.function.IntSupplier;

/**
 * Pattern slot that only renders and accepts interaction while its page is selected.
 */
public class AEPagedPatternViewExtendSlotWidget extends AEPatternViewExtendSlotWidget {

    private final int pageIndex;
    private final IntSupplier currentPage;

    public AEPagedPatternViewExtendSlotWidget(IItemTransfer inventory, int slotIndex, int xPosition, int yPosition,
                                              int slotsPerPage, IntSupplier currentPage) {
        super(inventory, slotIndex, xPosition, yPosition);
        this.pageIndex = slotIndex / slotsPerPage;
        this.currentPage = currentPage;
    }

    @Override
    public boolean isVisible() {
        if (!super.isVisible()) return false;
        // The server must keep every slot available so manually inserted patterns on later
        // GUI pages can still reach their slot. Visibility paging is a client-side concern.
        return !isRemote() || currentPage.getAsInt() == pageIndex;
    }
}
