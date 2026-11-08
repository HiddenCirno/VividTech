package com.vividtech.common.machine.multiblock.steam;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

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
        ItemStack output = ResourceOreTable.roll(level, 64, level.random);
        if (output.isEmpty()) {
            return null;
        }
        return baseRecipe().outputItems(output).buildRawRecipe();
    }
}
