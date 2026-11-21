package com.vividtech.common.command;

import appeng.api.inventories.InternalInventory;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.networking.security.IActionHost;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.AECraftingPattern;
import appeng.crafting.pattern.AEProcessingPattern;
import appeng.crafting.pattern.CraftingPatternItem;
import appeng.crafting.pattern.ProcessingPatternItem;
import appeng.helpers.patternprovider.PatternContainer;
import appeng.helpers.patternprovider.PatternProviderLogicHost;

import com.vividtech.common.menu.PatternReplacerMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkHooks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 样板原料替换器的服务端扫描与应用逻辑。界面部分见 {@link PatternReplacerMenu}。
 *
 * <p>右击网络组件后扫描指定类型的样板（处理样板或合成样板），把物品原料列表交给客户端；
 * 客户端完成“原料 → 标签 → 替代品 → 确认”的选择后，服务端重新扫描并原地替换。</p>
 */
public final class PatternReplacerService {

    public static final Component TITLE = Component.translatable("vividtech.pattern_replacer.gui.title");
    public static final Component CRAFTING_TITLE =
            Component.translatable("vividtech.pattern_replacer.gui.crafting_title");

    private PatternReplacerService() {}

    public record ScanResult(int patternCount, List<AEItemKey> ingredients,
                             List<ItemStack> stacks, List<Integer> counts) {}

    public static void open(ServerPlayer player, ServerLevel level, BlockPos pos, Direction side,
                            PatternKind kind) {
        IGrid grid = findGrid(level, pos, side);
        if (grid == null) {
            player.sendSystemMessage(Component.translatable("vividtech.pattern_replacer.no_grid"));
            return;
        }

        ScanResult result = scan(grid, level, kind);
        if (result.ingredients().isEmpty()) {
            player.sendSystemMessage(Component.translatable(kind == PatternKind.CRAFTING ?
                    "vividtech.pattern_replacer.no_patterns.crafting" :
                    "vividtech.pattern_replacer.no_patterns"));
            return;
        }

        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (id, inventory, p) ->
                        new PatternReplacerMenu(id, inventory, level, pos, side, result, kind),
                kind == PatternKind.CRAFTING ? CRAFTING_TITLE : TITLE),
                buf -> PatternReplacerMenu.writeExtraData(buf, pos, side, result));
    }

    /**
     * 当前 ME 网络上能否为这些物品发起合成；返回数组与传入列表一一对应。
     */
    public static boolean[] craftableFlags(ServerLevel level, BlockPos pos, Direction side,
                                           List<ItemStack> stacks) {
        boolean[] flags = new boolean[stacks.size()];
        IGrid grid = findGrid(level, pos, side);
        if (grid == null) {
            return flags;
        }
        ICraftingService crafting = grid.getCraftingService();
        for (int i = 0; i < stacks.size(); i++) {
            AEItemKey key = AEItemKey.of(stacks.get(i));
            flags[i] = key != null && crafting.isCraftable(key);
        }
        return flags;
    }

    public static int apply(ServerLevel level, BlockPos pos, Direction side,
                            AEItemKey source, AEItemKey replacement, PatternKind kind) {
        // 网络可能在打开界面后合流/分裂，应用时按原坐标重新取一次当前网格。
        IGrid grid = findGrid(level, pos, side);
        if (grid == null) {
            return 0;
        }
        return kind == PatternKind.CRAFTING ?
                applyCrafting(grid, level, source, replacement) :
                applyProcessing(grid, level, source, replacement);
    }

    private static int applyProcessing(IGrid grid, ServerLevel level,
                                       AEItemKey source, AEItemKey replacement) {
        int changed = 0;
        for (PatternContainer container : patternContainers(grid)) {
            InternalInventory inventory = container.getTerminalPatternInventory();
            for (int slot = 0; slot < inventory.size(); slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (!(stack.getItem() instanceof ProcessingPatternItem patternItem)) {
                    continue;
                }
                AEProcessingPattern pattern = patternItem.decode(stack, level, false);
                if (pattern == null) {
                    continue;
                }
                // getSparseInputs() 直接返回内部数组，必须克隆后再改。
                GenericStack[] sparseInputs = pattern.getSparseInputs().clone();
                boolean modified = false;
                for (int i = 0; i < sparseInputs.length; i++) {
                    GenericStack input = sparseInputs[i];
                    if (input != null && input.what().equals(source)) {
                        sparseInputs[i] = new GenericStack(replacement, input.amount());
                        modified = true;
                    }
                }
                if (!modified) {
                    continue;
                }
                try {
                    ItemStack encoded = patternItem.encode(sparseInputs, pattern.getSparseOutputs());
                    inventory.setItemDirect(slot, encoded);
                    if (container instanceof PatternProviderLogicHost host) {
                        host.saveChanges();
                    }
                    changed++;
                } catch (RuntimeException ignored) {
                    // 自定义/损坏样板：跳过，不影响其他样板。
                }
            }
        }
        return changed;
    }

    /**
     * 合成样板存的是具体物品 + 配方 id。这里只改 NBT 里的输入格，并重新解码验证：
     * 如果替换后的输入不再满足原配方（例如配方只接受某一种具体物品），就跳过该样板。
     */
    private static int applyCrafting(IGrid grid, ServerLevel level,
                                     AEItemKey source, AEItemKey replacement) {
        int changed = 0;
        for (PatternContainer container : patternContainers(grid)) {
            InternalInventory inventory = container.getTerminalPatternInventory();
            for (int slot = 0; slot < inventory.size(); slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (!(stack.getItem() instanceof CraftingPatternItem patternItem)) {
                    continue;
                }
                CompoundTag tag = stack.getTag();
                if (tag == null || !tag.contains("in", Tag.TAG_LIST)) {
                    continue;
                }
                ListTag inputs = tag.getList("in", Tag.TAG_COMPOUND);
                if (inputs.isEmpty()) {
                    continue;
                }

                ListTag updatedInputs = new ListTag();
                boolean modified = false;
                for (int i = 0; i < inputs.size(); i++) {
                    CompoundTag entry = inputs.getCompound(i);
                    if (entry.isEmpty()) {
                        updatedInputs.add(new CompoundTag());
                        continue;
                    }
                    ItemStack input = ItemStack.of(entry);
                    AEItemKey inputKey = input.isEmpty() ? null : AEItemKey.of(input);
                    if (inputKey != null && inputKey.equals(source)) {
                        ItemStack replaced = replacement.toStack(input.getCount());
                        updatedInputs.add(replaced.save(new CompoundTag()));
                        modified = true;
                    } else {
                        updatedInputs.add(entry.copy());
                    }
                }
                if (!modified) {
                    continue;
                }

                ItemStack updated = stack.copy();
                updated.getOrCreateTag().put("in", updatedInputs);
                // AECraftingPattern 会用配方校验输入格；不匹配说明替换后样板失效，保持原样。
                if (patternItem.decode(updated, level, false) == null) {
                    continue;
                }

                inventory.setItemDirect(slot, updated);
                if (container instanceof PatternProviderLogicHost host) {
                    host.saveChanges();
                }
                changed++;
            }
        }
        return changed;
    }

    private static ScanResult scan(IGrid grid, ServerLevel level, PatternKind kind) {
        Map<AEItemKey, Integer> counts = new LinkedHashMap<>();
        int patternCount = 0;
        for (PatternContainer container : patternContainers(grid)) {
            InternalInventory inventory = container.getTerminalPatternInventory();
            for (int slot = 0; slot < inventory.size(); slot++) {
                GenericStack[] sparseInputs = decodeInputs(inventory.getStackInSlot(slot), level, kind);
                if (sparseInputs == null) {
                    continue;
                }
                patternCount++;
                Set<AEItemKey> seen = new HashSet<>();
                for (GenericStack input : sparseInputs) {
                    if (input != null && input.what() instanceof AEItemKey itemKey && seen.add(itemKey)) {
                        counts.merge(itemKey, 1, Integer::sum);
                    }
                }
            }
        }

        // 索引要在客户端/服务端保持一致，所以按注册名排序，不能按本地化显示名。
        List<AEItemKey> ingredients = new ArrayList<>(counts.keySet());
        ingredients.sort(Comparator.comparing(key -> key.getId().toString()));
        List<ItemStack> stacks = new ArrayList<>(ingredients.size());
        List<Integer> countsList = new ArrayList<>(ingredients.size());
        for (AEItemKey key : ingredients) {
            stacks.add(key.toStack());
            countsList.add(counts.getOrDefault(key, 0));
        }
        return new ScanResult(patternCount, List.copyOf(ingredients),
                List.copyOf(stacks), List.copyOf(countsList));
    }

    private static GenericStack[] decodeInputs(ItemStack stack, ServerLevel level, PatternKind kind) {
        if (kind == PatternKind.PROCESSING) {
            if (!(stack.getItem() instanceof ProcessingPatternItem patternItem)) {
                return null;
            }
            AEProcessingPattern pattern = patternItem.decode(stack, level, false);
            return pattern == null ? null : pattern.getSparseInputs();
        }
        if (!(stack.getItem() instanceof CraftingPatternItem patternItem)) {
            return null;
        }
        AECraftingPattern pattern = patternItem.decode(stack, level, false);
        return pattern == null ? null : pattern.getSparseInputs();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static List<PatternContainer> patternContainers(IGrid grid) {
        List<PatternContainer> result = new ArrayList<>();
        for (Class<?> machineClass : grid.getMachineClasses()) {
            if (!PatternContainer.class.isAssignableFrom(machineClass)) {
                continue;
            }
            for (Object machine : grid.getActiveMachines((Class) machineClass)) {
                result.add((PatternContainer) machine);
            }
        }
        return result;
    }

    private static IGrid findGrid(ServerLevel level, BlockPos pos, Direction side) {
        IGridNode node = GridHelper.getExposedNode(level, pos, side);
        if (node == null) {
            for (Direction direction : Direction.values()) {
                node = GridHelper.getExposedNode(level, pos, direction);
                if (node != null) break;
            }
        }
        if (node == null) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof IActionHost actionHost) {
                node = actionHost.getActionableNode();
            }
        }
        return node != null && node.isActive() ? node.getGrid() : null;
    }
}
