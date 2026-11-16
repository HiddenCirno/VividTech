package com.vividtech.common.machine.multiblock.steam;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeHandler;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.UnificationEntry;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 蒸汽岩层开采模块：每个周期从当前维度的矿脉权重表抽取一种原矿并产出 64 个。
 */
public class SteamStratumMiningModuleMachine extends AbstractSteamResourceModuleMachine {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            SteamStratumMiningModuleMachine.class, AbstractSteamResourceModuleMachine.MANAGED_FIELD_HOLDER);

    /**
     * false = 黑名单：输入总线中的材料不产出。
     * true  = 白名单：只产出输入总线中标记的材料；输入总线为空时正常产出。
     */
    @Persisted
    @DescSynced
    private boolean whitelistMode;

    public SteamStratumMiningModuleMachine(IMachineBlockEntity holder, int moduleTier) {
        super(holder, moduleTier);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    public boolean isWhitelistMode() {
        return whitelistMode;
    }

    @Override
    public boolean isDimensionAllowed(Level level) {
        if (level.dimension().equals(Level.OVERWORLD)) {
            return true;
        }
        if (level.dimension().equals(Level.NETHER)) {
            return getModuleTier() >= 2;
        }
        if (level.dimension().equals(Level.END)) {
            return getModuleTier() >= 3;
        }
        return false;
    }

    @Override
    public GTRecipe buildCycleRecipe(Level level) {
        Set<Material> filters = getFilteredMaterials();
        // 白名单模式下，输入总线为空才代表“不筛选”；总线非空但没有可识别的材料时，
        // 传入空集合，让 ResourceOreTable 知道当前没有任何允许产出的材料。
        if (whitelistMode && filters.isEmpty() && !hasInputBusItems()) {
            filters = null;
        }
        ItemStack output = ResourceOreTable.roll(level, 64, level.random, filters, whitelistMode);
        if (output.isEmpty()) {
            return null;
        }
        return baseRecipe().outputItems(output).buildRawRecipe();
    }

    @Override
    protected InteractionResult onScrewdriverClick(Player playerIn, InteractionHand hand, Direction gridSide,
                                                   BlockHitResult hitResult) {
        if (playerIn.isShiftKeyDown()) {
            if (!isRemote()) {
                whitelistMode = !whitelistMode;
                markDirty();
                notifyBlockUpdate();
                playerIn.displayClientMessage(modeText(), true);
            }
            return InteractionResult.SUCCESS;
        }
        return super.onScrewdriverClick(playerIn, hand, gridSide, hitResult);
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        super.addDisplayText(textList);
        if (!isFormed()) {
            return;
        }
        textList.add(modeText());
    }

    private Component modeText() {
        Component value = Component.translatable(whitelistMode ?
                "vividtech.machine.stratum_mining.work_mode.whitelist" :
                "vividtech.machine.stratum_mining.work_mode.blacklist")
                .withStyle(whitelistMode ? ChatFormatting.GREEN : ChatFormatting.RED);
        return Component.translatable("vividtech.machine.stratum_mining.work_mode", value);
    }

    /**
     * 输入总线里的物品作为筛选列表：黑名单模式下排除对应材料，白名单模式下只保留对应材料。
     * 只读取内容，不消耗物品。白名单模式下由调用方传入 null 表示“总线为空、不筛选”，
     * 传入空集合表示“总线非空但没有可识别的材料”。
     */
    public Set<Material> getFilteredMaterials() {
        Set<Material> filters = new HashSet<>();
        collectFilters(capabilitiesProxy.get(IO.IN, ItemRecipeCapability.CAP), filters);
        collectFilters(capabilitiesProxy.get(IO.BOTH, ItemRecipeCapability.CAP), filters);
        return filters;
    }

    private static void collectFilters(List<IRecipeHandler<?>> handlers, Set<Material> filters) {
        if (handlers == null) {
            return;
        }
        for (IRecipeHandler<?> handler : handlers) {
            List<Object> contents = handler.getContents();
            if (contents == null) {
                continue;
            }
            for (Object content : contents) {
                if (content instanceof ItemStack stack && !stack.isEmpty()) {
                    UnificationEntry entry = ChemicalHelper.getUnificationEntry(stack.getItem());
                    if (entry != null && entry.material != null) {
                        filters.add(entry.material);
                    }
                }
            }
        }
    }

    private boolean hasInputBusItems() {
        return hasAnyItem(capabilitiesProxy.get(IO.IN, ItemRecipeCapability.CAP)) ||
                hasAnyItem(capabilitiesProxy.get(IO.BOTH, ItemRecipeCapability.CAP));
    }

    private static boolean hasAnyItem(List<IRecipeHandler<?>> handlers) {
        if (handlers == null) {
            return false;
        }
        for (IRecipeHandler<?> handler : handlers) {
            List<Object> contents = handler.getContents();
            if (contents == null) {
                continue;
            }
            for (Object content : contents) {
                if (content instanceof ItemStack stack && !stack.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }
}
