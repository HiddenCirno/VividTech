package com.vividtech.common.machine.multiblock.steam;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.data.DimensionMarker;
import com.gregtechceu.gtceu.api.data.worldgen.bedrockfluid.BedrockFluidDefinition;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 蒸汽地层钻探模块：每个周期产出 64 桶石油/天然气，并持续消耗钻井液。
 */
public class SteamStratumDrillingModuleMachine extends AbstractSteamResourceModuleMachine {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            SteamStratumDrillingModuleMachine.class, AbstractSteamResourceModuleMachine.MANAGED_FIELD_HOLDER);

    private static final long OIL_OUTPUT = 64_000L;

    /** 当前选择的流体注册名；为空或不在当前维度列表时使用列表第一项。 */
    @Persisted
    @DescSynced
    private String selectedFluidId = "";

    private boolean waitingForDrillingFluid;

    public SteamStratumDrillingModuleMachine(IMachineBlockEntity holder, int moduleTier) {
        super(holder, moduleTier);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    /**
     * 当前维度可开采的流体：原有石油/盐水/天然气预设，加上该维度的 GT 基岩流体。
     * 顺序固定，供螺丝刀轮询使用。
     */
    public List<Fluid> getAvailableFluids(Level level) {
        if (level == null) {
            return List.of();
        }
        ResourceKey<Level> dimension = ResourceDimensionCompat.effectiveDimension(level.dimension());
        LinkedHashSet<Fluid> fluids = new LinkedHashSet<>();

        if (ResourceDimensionCompat.isDrillingOverworldLike(level.dimension())) {
            addFluid(fluids, GTMaterials.Oil.getFluid());
            addFluid(fluids, GTMaterials.SaltWater.getFluid());
        }
        if (level.dimension().equals(Level.NETHER) && getModuleTier() >= 2) {
            addFluid(fluids, GTMaterials.NaturalGas.getFluid());
        }
        if (dimension.equals(Level.END)) {
            // 末地没有 GT 的基岩流体定义，补一个末地空气作为 MK-III 的保底产出。
            addFluid(fluids, GTMaterials.LiquidEnderAir.getFluid());
        }

        List<BedrockFluidDefinition> definitions =
                new ArrayList<>(GTRegistries.BEDROCK_FLUID_DEFINITIONS.values());
        definitions.sort(Comparator.comparing(definition -> {
            ResourceLocation key = GTRegistries.BEDROCK_FLUID_DEFINITIONS.getKey(definition);
            return key == null ? "" : key.toString();
        }));
        for (BedrockFluidDefinition definition : definitions) {
            if (definition == null) {
                continue;
            }
            Set<ResourceKey<Level>> filter = definition.getDimensionFilter();
            if (filter == null || !filter.contains(dimension)) {
                continue;
            }
            addFluid(fluids, definition.getStoredFluid().get());
        }
        return List.copyOf(fluids);
    }

    /** 当前选中的流体；存档中的选择不在当前列表时自动退回第一项。 */
    public Fluid getSelectedFluid(Level level) {
        List<Fluid> fluids = getAvailableFluids(level);
        if (fluids.isEmpty()) {
            return Fluids.EMPTY;
        }
        ResourceLocation selected = ResourceLocation.tryParse(selectedFluidId);
        if (selected != null) {
            Fluid fluid = BuiltInRegistries.FLUID.get(selected);
            if (fluid != Fluids.EMPTY && fluids.contains(fluid)) {
                return fluid;
            }
        }
        return fluids.get(0);
    }

    private static void addFluid(Set<Fluid> fluids, Fluid fluid) {
        if (fluid != null && fluid != Fluids.EMPTY) {
            fluids.add(fluid);
        }
    }

    /** 维度等级来自 GT 的 DimensionMarker；未登记的维度视为最高等级（仅 MK-III 可进）。 */
    private static int dimensionTier(Level level) {
        DimensionMarker marker = GTRegistries.DIMENSION_MARKERS.get(level.dimension().location());
        return marker != null ? marker.getTier() : Integer.MAX_VALUE;
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
                if (level == null || !isDimensionAllowed(level)) {
                    playerIn.displayClientMessage(Component.translatable(
                            "vividtech.machine.resource_module.wrong_dimension"), true);
                } else {
                    List<Fluid> fluids = getAvailableFluids(level);
                    if (fluids.isEmpty()) {
                        playerIn.displayClientMessage(Component.translatable(
                                "vividtech.machine.stratum_drilling.no_fluid"), true);
                    } else {
                        Fluid current = getSelectedFluid(level);
                        int index = fluids.indexOf(current);
                        Fluid next = fluids.get((index + 1) % fluids.size());
                        ResourceLocation id = BuiltInRegistries.FLUID.getKey(next);
                        selectedFluidId = id == null ? "" : id.toString();
                        markDirty();
                        notifyBlockUpdate();
                        playerIn.displayClientMessage(outputModeText(next), true);
                    }
                }
            }
            return InteractionResult.SUCCESS;
        }
        return super.onScrewdriverClick(playerIn, hand, gridSide, hitResult);
    }

    private Component outputModeText(Fluid fluid) {
        return Component.translatable("vividtech.machine.stratum_drilling.output_mode",
                new net.minecraftforge.fluids.FluidStack(fluid, 1).getDisplayName());
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
        // MK-III 覆盖全部维度；MK-II 保留原有的下界开采权限。
        if (getModuleTier() >= 3) {
            return true;
        }
        if (ResourceDimensionCompat.isDrillingOverworldLike(level.dimension())) {
            return true;
        }
        if (level.dimension().equals(Level.NETHER)) {
            return getModuleTier() >= 2;
        }
        // T1 模块 -> T0/T1 维度，T2 模块 -> T0/T1/T2 维度。
        return dimensionTier(level) <= getModuleTier();
    }

    @Override
    public GTRecipe buildCycleRecipe(Level level) {
        Fluid output = getSelectedFluid(level);
        if (output == Fluids.EMPTY) {
            return null;
        }
        return baseRecipe()
                .outputFluids(FluidStack.create(output, OIL_OUTPUT))
                .buildRawRecipe();
    }
}
