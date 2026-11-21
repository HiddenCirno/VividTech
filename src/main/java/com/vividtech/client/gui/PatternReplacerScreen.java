package com.vividtech.client.gui;

import com.vividtech.common.menu.PatternReplacerData;
import com.vividtech.common.menu.PatternReplacerMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * 虚拟箱子式样板原料替换界面。
 *
 * <p>菜单没有任何真实槽位，物品格全部自绘，所以界面天然无法被玩家拿取/放入物品。
 * 页面顺序：原料 → 标签 → 标签内物品 → 确认。</p>
 */
public class PatternReplacerScreen extends AbstractContainerScreen<PatternReplacerMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("textures/gui/container/generic_54.png");
    private static final int GRID_X = 8;
    private static final int GRID_Y = 18;
    private static final int COLS = 9;
    private static final int ROWS = 6;
    private static final int PER_PAGE = COLS * ROWS;

    private static final int PAGE_INGREDIENTS = 0;
    private static final int PAGE_TAGS = 1;
    private static final int PAGE_ITEMS = 2;
    private static final int PAGE_CONFIRM = 3;

    /** 确认页在网格中的位置：第一行 原料 / 更改为 / 替代品，第二行 确认 / 取消。 */
    private static final int CONFIRM_ITEM_ROW = 0;
    private static final int SOURCE_COL = 3;
    private static final int CHANGE_TO_COL = 4;
    private static final int REPLACEMENT_COL = 5;
    private static final int CONFIRM_ACTION_ROW = 1;
    private static final int CONFIRM_COL = 3;
    private static final int CANCEL_COL = 5;

    private final ItemStack backStack = named(new ItemStack(Items.RED_DYE),
            "vividtech.pattern_replacer.gui.back_item");
    private final ItemStack changeToStack = named(new ItemStack(Items.NAME_TAG),
            "vividtech.pattern_replacer.gui.change_to");
    private final ItemStack confirmStack = named(new ItemStack(Items.LIME_DYE),
            "vividtech.pattern_replacer.gui.confirm");
    private final ItemStack cancelStack = named(new ItemStack(Items.RED_DYE),
            "vividtech.pattern_replacer.gui.cancel");

    private int page = PAGE_INGREDIENTS;
    private int pageOffset;

    private int selectedIngredient = -1;
    private int selectedTag = -1;
    private int selectedItem = -1;

    private List<TagKey<Item>> tags = List.of();
    private List<ItemStack> tagIcons = List.of();
    private List<Integer> tagSizes = List.of();
    private List<ItemStack> items = List.of();
    /** 与 items 一一对应：当前 ME 网络能否合成该物品。 */
    private boolean[] craftableFlags = new boolean[0];

    public PatternReplacerScreen(PatternReplacerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
    }

    private static ItemStack named(ItemStack stack, String translationKey) {
        stack.setHoverName(Component.translatable(translationKey));
        return stack;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, pageTitle(), 8, 6, 0x404040, false);
        Component patternCount = Component.translatable(
                "vividtech.pattern_replacer.gui.pattern_count", this.menu.getPatternCount());
        graphics.drawString(this.font, patternCount,
                this.imageWidth - 8 - this.font.width(patternCount), 6, 0x707070, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderFooter(graphics);
        renderPageContents(graphics, mouseX, mouseY);
        renderHoverTooltip(graphics, mouseX, mouseY);
    }

    private Component pageTitle() {
        return Component.translatable(switch (this.page) {
            case PAGE_TAGS -> "vividtech.pattern_replacer.gui.page.tags";
            case PAGE_ITEMS -> "vividtech.pattern_replacer.gui.page.items";
            case PAGE_CONFIRM -> "vividtech.pattern_replacer.gui.page.confirm";
            default -> "vividtech.pattern_replacer.gui.page.ingredients";
        });
    }

    /** 当前页面的实际数据列表（不含末尾的返回项）。 */
    private List<ItemStack> currentList() {
        return switch (this.page) {
            case PAGE_TAGS -> this.tagIcons;
            case PAGE_ITEMS -> this.items;
            case PAGE_CONFIRM -> List.of();
            default -> this.menu.getIngredientStacks();
        };
    }

    /** 列表页把“返回上一级”实体物品追加在最末端，参与翻页。 */
    private List<ItemStack> displayList() {
        List<ItemStack> base = currentList();
        if (this.page != PAGE_TAGS && this.page != PAGE_ITEMS) {
            return base;
        }
        List<ItemStack> list = new ArrayList<>(base.size() + 1);
        list.addAll(base);
        list.add(this.backStack);
        return list;
    }

    private int pageCount(int size) {
        return Math.max(1, (size + PER_PAGE - 1) / PER_PAGE);
    }

    private void renderFooter(GuiGraphics graphics) {
        if (this.page == PAGE_CONFIRM) {
            return;
        }
        int pages = pageCount(displayList().size());
        String pageText = (Math.min(this.pageOffset, pages - 1) + 1) + " / " + pages;
        graphics.drawString(this.font, pageText,
                this.leftPos + (this.imageWidth - this.font.width(pageText)) / 2,
                this.topPos + this.imageHeight - 14, 0x404040, false);
    }

    private void renderPageContents(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.page == PAGE_CONFIRM) {
            renderConfirmPage(graphics, mouseX, mouseY);
            return;
        }

        List<ItemStack> list = displayList();
        if (list.isEmpty()) {
            return;
        }

        int actualSize = currentList().size();
        int selected = selectedIndexForPage();
        int hovered = hoveredIndex(mouseX, mouseY);
        int start = this.pageOffset * PER_PAGE;

        for (int i = 0; i < PER_PAGE; i++) {
            int index = start + i;
            if (index >= list.size()) {
                break;
            }
            int col = i % COLS;
            int row = i / COLS;
            int x = this.leftPos + GRID_X + col * 18;
            int y = this.topPos + GRID_Y + row * 18;
            graphics.renderItem(list.get(index), x, y);

            // 数字/标签必须画在物品之上，否则会被物品模型盖住。
            if (this.page == PAGE_INGREDIENTS && index < actualSize) {
                String count = "x" + this.menu.getIngredientCounts().get(index);
                drawOverlayText(graphics, count,
                        x + 17 - this.font.width(count), y + 9, 0xFFFFFF);
            } else if (this.page == PAGE_TAGS && index < actualSize) {
                drawOverlayText(graphics, "#", x + 1, y + 1, 0xFFFF55);
                String count = Integer.toString(this.tagSizes.get(index));
                drawOverlayText(graphics, count,
                        x + 17 - this.font.width(count), y + 9, 0xFFFFFF);
            } else if (this.page == PAGE_ITEMS && index < actualSize &&
                    index < this.craftableFlags.length && this.craftableFlags[index]) {
                drawOverlayText(graphics, "+",
                        x + 17 - this.font.width("+"), y + 1, 0x55FF55);
            }

            if (index == selected || index == hovered) {
                AbstractContainerScreen.renderSlotHighlight(graphics, x, y, 0);
            }
        }

        if (this.page == PAGE_TAGS && actualSize == 0) {
            Component empty = Component.translatable("vividtech.pattern_replacer.gui.no_tags");
            graphics.drawCenteredString(this.font, empty,
                    this.leftPos + this.imageWidth / 2, this.topPos + 140, 0xAA0000);
        }
    }

    private void drawOverlayText(GuiGraphics graphics, String text, int x, int y, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 200.0F);
        graphics.drawString(this.font, text, x, y, color, true);
        graphics.pose().popPose();
    }

    private void renderConfirmPage(GuiGraphics graphics, int mouseX, int mouseY) {
        ItemStack source = this.selectedIngredient >= 0 &&
                this.selectedIngredient < this.menu.getIngredientStacks().size() ?
                this.menu.getIngredientStacks().get(this.selectedIngredient) : ItemStack.EMPTY;
        ItemStack replacement = this.selectedItem >= 0 && this.selectedItem < this.items.size() ?
                this.items.get(this.selectedItem) : ItemStack.EMPTY;

        renderGridCell(graphics, SOURCE_COL, CONFIRM_ITEM_ROW, source, mouseX, mouseY);
        renderGridCell(graphics, CHANGE_TO_COL, CONFIRM_ITEM_ROW, this.changeToStack, mouseX, mouseY);
        renderGridCell(graphics, REPLACEMENT_COL, CONFIRM_ITEM_ROW, replacement, mouseX, mouseY);
        renderGridCell(graphics, CONFIRM_COL, CONFIRM_ACTION_ROW, this.confirmStack, mouseX, mouseY);
        renderGridCell(graphics, CANCEL_COL, CONFIRM_ACTION_ROW, this.cancelStack, mouseX, mouseY);
    }

    private void renderGridCell(GuiGraphics graphics, int col, int row, ItemStack stack,
                                int mouseX, int mouseY) {
        if (stack.isEmpty()) {
            return;
        }
        int x = this.leftPos + GRID_X + col * 18;
        int y = this.topPos + GRID_Y + row * 18;
        graphics.renderItem(stack, x, y);
        if (cellHovered(mouseX, mouseY, col, row)) {
            AbstractContainerScreen.renderSlotHighlight(graphics, x, y, 0);
        }
    }

    private void renderHoverTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.page == PAGE_CONFIRM) {
            ItemStack stack = confirmStackAt(mouseX, mouseY);
            if (!stack.isEmpty()) {
                graphics.renderTooltip(this.font, stack, mouseX, mouseY);
            }
            return;
        }

        int hovered = hoveredIndex(mouseX, mouseY);
        List<ItemStack> list = displayList();
        if (hovered < 0 || hovered >= list.size()) {
            return;
        }

        int actualSize = currentList().size();
        if (this.page == PAGE_TAGS && hovered < actualSize) {
            TagKey<Item> tag = this.tags.get(hovered);
            graphics.renderComponentTooltip(this.font, List.of(
                    Component.literal("#" + tag.location()),
                    Component.translatable("vividtech.pattern_replacer.gui.tag.items",
                            this.tagSizes.get(hovered))), mouseX, mouseY);
        } else {
            graphics.renderTooltip(this.font, list.get(hovered), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (this.page == PAGE_CONFIRM) {
                if (cellHovered(mouseX, mouseY, CONFIRM_COL, CONFIRM_ACTION_ROW)) {
                    sendButton(PatternReplacerMenu.BTN_CONFIRM);
                    return true;
                }
                if (cellHovered(mouseX, mouseY, CANCEL_COL, CONFIRM_ACTION_ROW)) {
                    this.page = PAGE_ITEMS;
                    this.pageOffset = Math.max(0, this.selectedItem) / PER_PAGE;
                    this.selectedItem = -1;
                    return true;
                }
                return super.mouseClicked(mouseX, mouseY, button);
            }

            int hovered = hoveredIndex(mouseX, mouseY);
            List<ItemStack> list = displayList();
            if (hovered >= 0 && hovered < list.size()) {
                int actualSize = currentList().size();

                // 列表最后一项是返回物品。
                if ((this.page == PAGE_TAGS || this.page == PAGE_ITEMS) && hovered == actualSize) {
                    if (this.page == PAGE_ITEMS) {
                        this.page = PAGE_TAGS;
                        this.pageOffset = Math.max(0, this.selectedTag) / PER_PAGE;
                        this.selectedTag = -1;
                        this.selectedItem = -1;
                    } else {
                        this.page = PAGE_INGREDIENTS;
                        this.pageOffset = Math.max(0, this.selectedIngredient) / PER_PAGE;
                        this.selectedIngredient = -1;
                        this.selectedTag = -1;
                        this.selectedItem = -1;
                        this.craftableFlags = new boolean[0];
                    }
                    return true;
                }

                if (hovered < actualSize) {
                    if (this.page == PAGE_INGREDIENTS) {
                        this.selectedIngredient = hovered;
                        this.selectedTag = -1;
                        this.selectedItem = -1;
                        this.craftableFlags = new boolean[0];
                        sendButton(PatternReplacerMenu.BTN_INGREDIENT + hovered);
                        buildTags();
                        this.page = PAGE_TAGS;
                        this.pageOffset = 0;
                    } else if (this.page == PAGE_TAGS) {
                        this.selectedTag = hovered;
                        this.selectedItem = -1;
                        this.craftableFlags = new boolean[0];
                        sendButton(PatternReplacerMenu.BTN_TAG + hovered);
                        buildItems();
                        this.page = PAGE_ITEMS;
                        this.pageOffset = 0;
                    } else if (this.page == PAGE_ITEMS) {
                        this.selectedItem = hovered;
                        sendButton(PatternReplacerMenu.BTN_ITEM + hovered);
                        this.page = PAGE_CONFIRM;
                        this.pageOffset = 0;
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.page != PAGE_CONFIRM) {
            int pages = pageCount(displayList().size());
            if (delta > 0) {
                this.pageOffset = Math.max(0, this.pageOffset - 1);
            } else if (delta < 0) {
                this.pageOffset = Math.min(pages - 1, this.pageOffset + 1);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void buildTags() {
        if (this.selectedIngredient < 0 ||
                this.selectedIngredient >= this.menu.getIngredientStacks().size()) {
            this.tags = List.of();
            this.tagIcons = List.of();
            this.tagSizes = List.of();
            return;
        }
        ItemStack source = this.menu.getIngredientStacks().get(this.selectedIngredient);
        this.tags = PatternReplacerData.tagsFor(source);
        List<ItemStack> icons = new ArrayList<>(this.tags.size());
        List<Integer> sizes = new ArrayList<>(this.tags.size());
        for (TagKey<Item> tag : this.tags) {
            List<ItemStack> tagItems = PatternReplacerData.itemsFor(tag);
            icons.add(tagItems.isEmpty() ? ItemStack.EMPTY : tagItems.get(0));
            sizes.add(tagItems.size());
        }
        this.tagIcons = List.copyOf(icons);
        this.tagSizes = List.copyOf(sizes);
    }

    private void buildItems() {
        if (this.selectedTag < 0 || this.selectedTag >= this.tags.size()) {
            this.items = List.of();
            return;
        }
        this.items = PatternReplacerData.itemsFor(this.tags.get(this.selectedTag));
    }

    /** 服务端返回某个标签内物品的网络可合成状态。 */
    public void applyCraftability(ResourceLocation tagId, boolean[] flags) {
        if (this.selectedTag < 0 || this.selectedTag >= this.tags.size()) {
            return;
        }
        if (!this.tags.get(this.selectedTag).location().equals(tagId)) {
            return;
        }
        if (flags == null || flags.length != this.items.size()) {
            return;
        }
        this.craftableFlags = flags;
    }

    private int selectedIndexForPage() {
        return switch (this.page) {
            case PAGE_TAGS -> this.selectedTag;
            case PAGE_ITEMS -> this.selectedItem;
            case PAGE_INGREDIENTS -> this.selectedIngredient;
            default -> -1;
        };
    }

    private int hoveredIndex(double mouseX, double mouseY) {
        int relX = (int) mouseX - this.leftPos - GRID_X;
        int relY = (int) mouseY - this.topPos - GRID_Y;
        if (relX < 0 || relY < 0) {
            return -1;
        }
        int col = relX / 18;
        int row = relY / 18;
        if (col >= COLS || row >= ROWS || relX % 18 >= 16 || relY % 18 >= 16) {
            return -1;
        }
        return this.pageOffset * PER_PAGE + row * COLS + col;
    }

    private boolean cellHovered(double mouseX, double mouseY, int col, int row) {
        int x = this.leftPos + GRID_X + col * 18;
        int y = this.topPos + GRID_Y + row * 18;
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
    }

    private ItemStack confirmStackAt(double mouseX, double mouseY) {
        if (cellHovered(mouseX, mouseY, SOURCE_COL, CONFIRM_ITEM_ROW) &&
                this.selectedIngredient >= 0 &&
                this.selectedIngredient < this.menu.getIngredientStacks().size()) {
            return this.menu.getIngredientStacks().get(this.selectedIngredient);
        }
        if (cellHovered(mouseX, mouseY, CHANGE_TO_COL, CONFIRM_ITEM_ROW)) {
            return this.changeToStack;
        }
        if (cellHovered(mouseX, mouseY, REPLACEMENT_COL, CONFIRM_ITEM_ROW) &&
                this.selectedItem >= 0 && this.selectedItem < this.items.size()) {
            return this.items.get(this.selectedItem);
        }
        if (cellHovered(mouseX, mouseY, CONFIRM_COL, CONFIRM_ACTION_ROW)) {
            return this.confirmStack;
        }
        if (cellHovered(mouseX, mouseY, CANCEL_COL, CONFIRM_ACTION_ROW)) {
            return this.cancelStack;
        }
        return ItemStack.EMPTY;
    }

    private void sendButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }
}
