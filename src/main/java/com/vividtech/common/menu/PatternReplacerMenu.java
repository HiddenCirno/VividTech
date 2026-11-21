package com.vividtech.common.menu;

import appeng.api.stacks.AEItemKey;

import com.vividtech.common.command.PatternKind;
import com.vividtech.common.command.PatternReplacerService;
import com.vividtech.common.network.TagCraftabilityPacket;
import com.vividtech.common.network.VividNetwork;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * 样板原料替换器的虚拟箱子菜单：没有任何真实槽位，所有物品格都由客户端界面绘制。
 *
 * <p>材料列表由服务端扫描后通过打开菜单时的缓冲区下发；标签与标签内物品由两端
 * 用同一套 {@link PatternReplacerData} 按注册名排序计算，因此客户端点击的索引
 * 可以在服务端还原成同一个物品。</p>
 */
public class PatternReplacerMenu extends AbstractContainerMenu {

    public static final int BTN_INGREDIENT = 100_000;
    public static final int BTN_TAG = 200_000;
    public static final int BTN_ITEM = 300_000;
    public static final int BTN_CONFIRM = 400_000;

    private final ServerLevel level;
    private final BlockPos pos;
    private final Direction side;
    private final int patternCount;
    private final PatternKind kind;
    /** 仅服务端持有；客户端只使用下面两个列表。 */
    private final List<AEItemKey> ingredients;
    private final List<ItemStack> ingredientStacks;
    private final List<Integer> ingredientCounts;

    private AEItemKey source;
    private TagKey<Item> selectedTag;
    private AEItemKey replacement;

    public PatternReplacerMenu(int id, Inventory inventory, ServerLevel level, BlockPos pos,
                               Direction side, PatternReplacerService.ScanResult result, PatternKind kind) {
        super(VividMenus.PATTERN_REPLACER.get(), id);
        this.level = level;
        this.pos = pos;
        this.side = side;
        this.patternCount = result.patternCount();
        this.kind = kind;
        this.ingredients = result.ingredients();
        this.ingredientStacks = result.stacks();
        this.ingredientCounts = result.counts();
    }

    public PatternReplacerMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
        super(VividMenus.PATTERN_REPLACER.get(), id);
        this.level = null;
        this.pos = buf.readBlockPos();
        this.side = buf.readEnum(Direction.class);
        this.patternCount = buf.readVarInt();
        this.kind = PatternKind.PROCESSING;
        this.ingredients = List.of();
        int size = buf.readVarInt();
        List<ItemStack> stacks = new ArrayList<>(size);
        List<Integer> counts = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            stacks.add(buf.readItem());
            counts.add(buf.readVarInt());
        }
        this.ingredientStacks = List.copyOf(stacks);
        this.ingredientCounts = List.copyOf(counts);
    }

    public static void writeExtraData(FriendlyByteBuf buf, BlockPos pos, Direction side,
                                      PatternReplacerService.ScanResult result) {
        buf.writeBlockPos(pos);
        buf.writeEnum(side);
        buf.writeVarInt(result.patternCount());
        List<ItemStack> stacks = result.stacks();
        buf.writeVarInt(stacks.size());
        for (int i = 0; i < stacks.size(); i++) {
            buf.writeItem(stacks.get(i));
            buf.writeVarInt(result.counts().get(i));
        }
    }

    public int getPatternCount() {
        return patternCount;
    }

    public List<ItemStack> getIngredientStacks() {
        return ingredientStacks;
    }

    public List<Integer> getIngredientCounts() {
        return ingredientCounts;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // 虚拟箱子没有任何真实槽位，也永远不参与玩家物品栏转移。
        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (level == null || player.level().isClientSide) {
            return false;
        }

        if (id >= BTN_INGREDIENT && id < BTN_TAG) {
            int index = id - BTN_INGREDIENT;
            if (index < 0 || index >= ingredients.size()) {
                return false;
            }
            source = ingredients.get(index);
            selectedTag = null;
            replacement = null;
            return true;
        }

        if (id >= BTN_TAG && id < BTN_ITEM) {
            if (source == null) {
                return false;
            }
            int index = id - BTN_TAG;
            List<TagKey<Item>> tags = PatternReplacerData.tagsFor(source.toStack());
            if (index < 0 || index >= tags.size()) {
                return false;
            }
            selectedTag = tags.get(index);
            replacement = null;
            if (player instanceof ServerPlayer serverPlayer) {
                List<ItemStack> tagItems = PatternReplacerData.itemsFor(selectedTag);
                boolean[] flags = PatternReplacerService.craftableFlags(level, pos, side, tagItems);
                VividNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new TagCraftabilityPacket(selectedTag.location(), flags));
            }
            return true;
        }

        if (id >= BTN_ITEM && id < BTN_CONFIRM) {
            if (selectedTag == null) {
                return false;
            }
            int index = id - BTN_ITEM;
            List<ItemStack> items = PatternReplacerData.itemsFor(selectedTag);
            if (index < 0 || index >= items.size()) {
                return false;
            }
            replacement = AEItemKey.of(items.get(index));
            return true;
        }

        if (id == BTN_CONFIRM) {
            if (source == null || replacement == null) {
                player.sendSystemMessage(Component.translatable("vividtech.pattern_replacer.incomplete"));
                return false;
            }
            int changed = PatternReplacerService.apply(level, pos, side, source, replacement, kind);
            player.closeContainer();
            if (changed > 0) {
                player.sendSystemMessage(Component.translatable(kind == PatternKind.CRAFTING ?
                        "vividtech.pattern_replacer.saved.crafting" :
                        "vividtech.pattern_replacer.saved", changed).withStyle(ChatFormatting.GREEN));
            } else {
                player.sendSystemMessage(Component.translatable(kind == PatternKind.CRAFTING ?
                        "vividtech.pattern_replacer.not_found.crafting" :
                        "vividtech.pattern_replacer.not_found").withStyle(ChatFormatting.RED));
            }
            return true;
        }
        return false;
    }
}
