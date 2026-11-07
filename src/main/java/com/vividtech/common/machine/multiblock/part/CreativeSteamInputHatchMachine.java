package com.vividtech.common.machine.multiblock.part;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 创造模式蒸汽输入仓。
 *
 * <p>
 * 容量 2,147,483,647 mB，仅接受蒸汽，不提供 GUI，也不提供任何超频能力。
 * </p>
 */
public class CreativeSteamInputHatchMachine extends FluidHatchPartMachine {

    /** Integer.MAX_VALUE mB。 */
    public static final long CAPACITY = Integer.MAX_VALUE;

    public CreativeSteamInputHatchMachine(IMachineBlockEntity holder) {
        super(holder, 0, IO.IN, CAPACITY, 1);
    }

    @Override
    protected NotifiableFluidTank createTank(long initialCapacity, int slots, Object... args) {
        return super.createTank(initialCapacity, slots)
                .setFilter(fluidStack -> fluidStack.getFluid().is(GTMaterials.Steam.getFluidTag()));
    }

    @Override
    public boolean shouldOpenUI(Player player, InteractionHand hand, BlockHitResult hit) {
        return false;
    }
}
