package com.vividtech.common.machine.multiblock.steam;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/**
 * 蒸汽地层钻探模块：每个周期产出 64 桶石油/天然气，并持续消耗钻井液。
 */
public class SteamStratumDrillingModuleMachine extends AbstractSteamResourceModuleMachine {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            SteamStratumDrillingModuleMachine.class, AbstractSteamResourceModuleMachine.MANAGED_FIELD_HOLDER);

    private static final long OIL_OUTPUT = 64_000L;

    /** false = 石油；true = 盐水。仅主世界有效。 */
    @Persisted
    @DescSynced
    private boolean saltWaterMode;

    private boolean waitingForDrillingFluid;

    public SteamStratumDrillingModuleMachine(IMachineBlockEntity holder, int moduleTier) {
        super(holder, moduleTier);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    public boolean isSaltWaterMode() {
        return saltWaterMode;
    }

    @Override
    protected void tickResource() {
        RecipeLogic logic = getRecipeLogic();
        boolean working = logic.isWorking();
        if (!working && !waitingForDrillingFluid) {
            return;
        }
        boolean drained = drainDrillingFluid(getModuleTier());
        waitingForDrillingFluid = !drained;
        setResourceAvailable(drained);
    }

    /** 从模块的流体输入仓按 tick 抽取钻井液。 */
    private boolean drainDrillingFluid(long amount) {
        var handlers = capabilitiesProxy.get(IO.IN, FluidRecipeCapability.CAP);
        if (handlers == null) {
            return false;
        }
        long remaining = amount;
        Fluid drillingFluid = GTMaterials.DrillingFluid.getFluid();
        for (var handler : handlers) {
            if (!(handler instanceof NotifiableFluidTank tank)) {
                continue;
            }
            FluidStack stack = tank.getFluidInTank(0);
            if (stack.isEmpty() || stack.getFluid() != drillingFluid) {
                continue;
            }
            long toDrain = Math.min(remaining, stack.getAmount());
            if (toDrain <= 0) {
                continue;
            }
            FluidStack drained = tank.drainInternal(toDrain, false);
            remaining -= drained.getAmount();
            if (remaining <= 0) {
                return true;
            }
        }
        return remaining <= 0;
    }

    @Override
    protected InteractionResult onScrewdriverClick(Player playerIn, InteractionHand hand, Direction gridSide,
                                                   BlockHitResult hitResult) {
        if (playerIn.isShiftKeyDown()) {
            if (!isRemote()) {
                Level level = getLevel();
                if (level != null && level.dimension().equals(Level.OVERWORLD)) {
                    saltWaterMode = !saltWaterMode;
                    markDirty();
                    notifyBlockUpdate();
                    playerIn.displayClientMessage(outputModeText(), true);
                } else {
                    playerIn.displayClientMessage(
                            Component.translatable("vividtech.machine.stratum_drilling.mode_overworld_only"), true);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return super.onScrewdriverClick(playerIn, hand, gridSide, hitResult);
    }

    private Component outputModeText() {
        return Component.translatable("vividtech.machine.stratum_drilling.output_mode",
                Component.translatable(saltWaterMode ?
                        "vividtech.machine.stratum_drilling.output_mode.salt_water" :
                        "vividtech.machine.stratum_drilling.output_mode.oil"));
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        super.addDisplayText(textList);
        if (!isResourceAvailable()) {
            textList.add(Component.translatable("vividtech.machine.resource_module.missing_drilling_fluid"));
        }
    }

    @Override
    public boolean isDimensionAllowed(Level level) {
        return getOutputFluid(level) != null;
    }

    private FluidStack getOutputFluid(Level level) {
        if (level.dimension().equals(Level.OVERWORLD)) {
            Fluid output = saltWaterMode ? GTMaterials.SaltWater.getFluid() : GTMaterials.Oil.getFluid();
            return FluidStack.create(output, OIL_OUTPUT);
        }
        if (level.dimension().equals(Level.NETHER) && getModuleTier() >= 2) {
            return FluidStack.create(GTMaterials.NaturalGas.getFluid(), OIL_OUTPUT);
        }
        return null;
    }

    @Override
    public GTRecipe buildCycleRecipe(Level level) {
        FluidStack output = getOutputFluid(level);
        if (output == null) {
            return null;
        }
        return baseRecipe()
                .outputFluids(output)
                .buildRawRecipe();
    }
}
