package com.vividtech.common.machine.multiblock.steam;

import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeHandler;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 让多个模块共享梦想电梯结构内所有蒸汽输入仓的 EU 处理器。
 *
 * <p>
 * 与 GTCEu 的 {@code SteamEnergyRecipeHandler} 不同，这里不绑定单个罐子，
 * 而是每次处理配方时按顺序从结构内的全部蒸汽仓抽取，因此模块不需要自己的缓存。
 * </p>
 */
public class SharedSteamEnergyRecipeHandler implements IRecipeHandler<Long> {

    private final Supplier<List<NotifiableFluidTank>> tanks;
    private final double conversionRate;

    public SharedSteamEnergyRecipeHandler(Supplier<List<NotifiableFluidTank>> tanks, double conversionRate) {
        this.tanks = tanks;
        this.conversionRate = conversionRate;
    }

    @Override
    public List<Long> handleRecipeInner(IO io, GTRecipe recipe, List<Long> left, @Nullable String slotName,
                                        boolean simulate) {
        if (io != IO.IN) {
            return left;
        }
        long totalEUt = left.stream().mapToLong(Long::longValue).sum();
        if (totalEUt <= 0) {
            return left;
        }
        long steamNeeded = (long) Math.ceil(totalEUt * conversionRate);
        long remainingSteam = steamNeeded;
        for (NotifiableFluidTank tank : tanks.get()) {
            if (remainingSteam <= 0) {
                break;
            }
            long stored = tank.getFluidInTank(0).getAmount();
            if (stored <= 0) {
                continue;
            }
            long toDrain = Math.min(remainingSteam, stored);
            if (!simulate) {
                FluidStack drained = tank.drainInternal(toDrain, false);
                toDrain = drained.getAmount();
            }
            remainingSteam -= toDrain;
        }
        if (remainingSteam >= steamNeeded) {
            return left;
        }
        if (remainingSteam <= 0) {
            return null;
        }
        long satisfiedEUt = (long) ((steamNeeded - remainingSteam) / conversionRate);
        long leftover = totalEUt - satisfiedEUt;
        return leftover > 0 ? List.of(leftover) : null;
    }

    @Override
    public List<Object> getContents() {
        List<Object> contents = new ArrayList<>();
        for (NotifiableFluidTank tank : tanks.get()) {
            for (int i = 0; i < tank.getTanks(); i++) {
                FluidStack stack = tank.getFluidInTank(i);
                if (!stack.isEmpty()) {
                    contents.add(stack);
                }
            }
        }
        return contents;
    }

    @Override
    public double getTotalContentAmount() {
        long amount = 0L;
        for (NotifiableFluidTank tank : tanks.get()) {
            for (int i = 0; i < tank.getTanks(); i++) {
                amount += tank.getFluidInTank(i).getAmount();
            }
        }
        return amount;
    }

    @Override
    public RecipeCapability<Long> getCapability() {
        return EURecipeCapability.CAP;
    }
}
