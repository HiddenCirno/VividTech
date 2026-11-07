package com.vividtech.common.machine.multiblock.part;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;

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

    private TickableSubscription fillSubscription;

    public CreativeSteamInputHatchMachine(IMachineBlockEntity holder) {
        super(holder, 0, IO.IN, CAPACITY, 1);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) {
            fillSteam();
            fillSubscription = subscribeServerTick(this::fillSteam);
        }
    }

    @Override
    public void onUnload() {
        if (fillSubscription != null) {
            fillSubscription.unsubscribe();
            fillSubscription = null;
        }
        super.onUnload();
    }

    /** 创造模式仓自行补满蒸汽。 */
    private void fillSteam() {
        long capacity = tank.getTankCapacity(0);
        long stored = tank.getFluidInTank(0).getAmount();
        if (stored < capacity) {
            tank.fillInternal(FluidStack.create(GTMaterials.Steam.getFluid(), capacity - stored), false);
        }
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
