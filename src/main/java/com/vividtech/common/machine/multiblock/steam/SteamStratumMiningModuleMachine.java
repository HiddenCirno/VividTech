package com.vividtech.common.machine.multiblock.steam;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeHandler;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.UnificationEntry;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 蒸汽岩层开采模块：每个周期从当前维度的矿脉权重表抽取一种原矿并产出 64 个。
 */
public class SteamStratumMiningModuleMachine extends AbstractSteamResourceModuleMachine {

    public SteamStratumMiningModuleMachine(IMachineBlockEntity holder, int moduleTier) {
        super(holder, moduleTier);
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
        ItemStack output = ResourceOreTable.roll(level, 64, level.random, getFilteredMaterials());
        if (output.isEmpty()) {
            return null;
        }
        return baseRecipe().outputItems(output).buildRawRecipe();
    }

    /**
     * 输入总线里的物品作为矿物屏蔽列表：物品对应的材料会被排除在抽取池外。
     * 只读取内容，不消耗物品。
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
}
