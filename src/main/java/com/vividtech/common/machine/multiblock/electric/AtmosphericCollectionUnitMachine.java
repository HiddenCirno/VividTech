package com.vividtech.common.machine.multiblock.electric;

import org.gtlcore.gtlcore.common.data.GTLMaterials;

import com.gregtechceu.gtceu.api.capability.IParallelHatch;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.logic.OCParams;
import com.gregtechceu.gtceu.api.recipe.logic.OCResult;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 大气捕集单元。
 *
 * <p>机器本身不写死某种维度，而是由 {@link #sourceFor(Level)} 判断当前维度，
 * 再从数据配方中挑出对应来源的配方。这样 JEI/EMI 里仍然能看到四条独立配方，
 * 运行时也不会被无序的配方查找结果干扰。</p>
 */
public class AtmosphericCollectionUnitMachine extends WorkableElectricMultiblockMachine {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            AtmosphericCollectionUnitMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    /** 配方数据中标记大气来源的键。 */
    public static final String SOURCE_KEY = "vividtech_atmosphere_source";
    /** 配方数据中标记工作模式的键。 */
    public static final String MODE_KEY = "vividtech_atmosphere_mode";
    public static final String MODE_CONDENSE = "condense";
    public static final String MODE_COLLECT = "collect";
    public static final String MODE_ANY = "any";

    public static final String SOURCE_OVERWORLD = "overworld";
    public static final String SOURCE_ANCIENT_WORLD = "ancient_world";
    public static final String SOURCE_PERSONAL = "personal";
    public static final String SOURCE_NETHER = "nether";
    public static final String SOURCE_END = "end";
    public static final String SOURCE_BARNARDA = "barnarda";

    private static final ResourceLocation OVERWORLD = new ResourceLocation("minecraft", "overworld");
    private static final ResourceLocation NETHER = new ResourceLocation("minecraft", "the_nether");
    private static final ResourceLocation END = new ResourceLocation("minecraft", "the_end");
    private static final ResourceLocation ANCIENT_WORLD = new ResourceLocation("kubejs", "ancient_world");
    private static final ResourceLocation BARNARDA = new ResourceLocation("kubejs", "barnarda");

    /** 运行时配方。数据配方只负责 EMI 展示，实际运行不依赖配方索引。 */
    private static final Map<String, GTRecipe> RUNTIME_RECIPES = new HashMap<>();

    /** true = 自动冷凝（液态空气），false = 普通集气（气态空气）。 */
    @Persisted
    @DescSynced
    private boolean condenseAir = true;

    public AtmosphericCollectionUnitMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    protected RecipeLogic createRecipeLogic(Object... args) {
        return new AtmosphericCollectionRecipeLogic(this);
    }

    public boolean isCondenseAir() {
        return condenseAir;
    }

    /**
     * 潜行 + 螺丝刀右击切换工作模式：自动冷凝 ↔ 普通集气。
     */
    @Override
    protected InteractionResult onScrewdriverClick(Player playerIn, InteractionHand hand, Direction gridSide,
                                                   BlockHitResult hitResult) {
        if (isRemote() || !playerIn.isShiftKeyDown()) {
            return super.onScrewdriverClick(playerIn, hand, gridSide, hitResult);
        }

        condenseAir = !condenseAir;
        markDirty();
        notifyBlockUpdate();
        getRecipeLogic().resetRecipeLogic();
        playerIn.displayClientMessage(Component.translatable(condenseAir ?
                "vividtech.machine.atmospheric_collection.mode.changed.condense" :
                "vividtech.machine.atmospheric_collection.mode.changed.collect"), true);
        playerIn.swing(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void addDisplayText(@NotNull List<Component> textList) {
        super.addDisplayText(textList);
        if (!isFormed()) {
            return;
        }
        textList.add(Component.translatable("vividtech.machine.atmospheric_collection.mode",
                Component.translatable(condenseAir ?
                        "vividtech.machine.atmospheric_collection.mode.condense" :
                        "vividtech.machine.atmospheric_collection.mode.collect")));
    }

    /**
     * 当前维度对应的大气来源；未支持的维度返回 null，机器会保持待机。
     */
    @Nullable
    public static String sourceFor(@Nullable Level level) {
        if (level == null) {
            return null;
        }
        ResourceLocation dimension = level.dimension().location();
        if (NETHER.equals(dimension)) {
            return SOURCE_NETHER;
        }
        if (END.equals(dimension)) {
            return SOURCE_END;
        }
        if (BARNARDA.equals(dimension)) {
            return SOURCE_BARNARDA;
        }
        if (isAvaritiaPersonalDimension(dimension)) {
            return SOURCE_PERSONAL;
        }
        if (ANCIENT_WORLD.equals(dimension)) {
            return SOURCE_ANCIENT_WORLD;
        }
        if (OVERWORLD.equals(dimension)) {
            return SOURCE_OVERWORLD;
        }
        return null;
    }

    /**
     * 无尽贪婪的私人维度使用 {@code avaritia:personal_<uuid>} 的命名规则。
     */
    public static boolean isAvaritiaPersonalDimension(ResourceLocation dimension) {
        return "avaritia".equals(dimension.getNamespace()) && dimension.getPath().startsWith("personal_");
    }

    /**
     * 当前并行控制仓提供的并行数；没有并行仓时返回 1。
     */
    public int getParallelHatchParallel() {
        if (!isFormed()) {
            return 1;
        }
        int parallel = 1;
        for (IMultiPart part : getParts()) {
            if (part instanceof IParallelHatch hatch) {
                parallel = Math.max(parallel, hatch.getCurrentParallel());
            }
        }
        return parallel;
    }

    /**
     * 并行修正。
     *
     * <p>不能直接用 {@code GTRecipeModifiers.PARALLEL_HATCH}：它在计算并行上限时
     * 会遍历配方输入，而本机器的配方没有物品/流体输入，空集合会被算成 0 并行。
     * 这里改用并行仓提供的数值，并用输出仓空间做上限。</p>
     */
    @Nullable
    public static GTRecipe modifyRecipe(MetaMachine machine, GTRecipe recipe, OCParams params, OCResult result) {
        if (!(machine instanceof AtmosphericCollectionUnitMachine unit)) {
            return recipe;
        }

        result.reset();

        GTRecipe modified = recipe.copy();
        int parallel = unit.getParallelHatchParallel();
        if (parallel > 1) {
            int limited = ParallelLogic.limitByOutputMerging(recipe, unit, parallel, capability -> true);
            if (limited > 1) {
                modified = recipe.copy(ContentModifier.multiplier(limited), false);
                modified.parallels = limited;
            }
        }

        // 普通无损超频交给全局 Mixin 处理：低于 1t 的最后一档会取整到 1t。
        RecipeHelper.applyOverclock(OverclockingLogic.PERFECT_OVERCLOCK, modified,
                unit.getOverclockVoltage(), params, result);
        return modified;
    }

    /**
     * 优先使用 EMI/JEI 展示的数据配方，保证实际运行与配方界面是同一份定义。
     * 找不到时再退回运行时构造，避免配方表异常导致机器彻底不工作。
     */
    @Nullable
    private static GTRecipe findDataRecipe(Level level, GTRecipeType type, String source, boolean condense) {
        String wantedMode = condense ? MODE_CONDENSE : MODE_COLLECT;
        for (GTRecipe recipe : level.getRecipeManager().getAllRecipesFor(type)) {
            if (!source.equals(recipe.data.getString(SOURCE_KEY))) {
                continue;
            }
            String mode = recipe.data.getString(MODE_KEY);
            if (wantedMode.equals(mode) || MODE_ANY.equals(mode)) {
                return recipe;
            }
        }
        return null;
    }

    private static GTRecipe runtimeRecipe(GTRecipeType type, String source, boolean condense) {
        String key = source + (condense ? ":condense" : ":collect");
        return RUNTIME_RECIPES.computeIfAbsent(key, ignored -> GTRecipeBuilder
                .of(new ResourceLocation("vividtech", "atmospheric_collection_runtime_" +
                        source + (condense ? "_condense" : "_collect")), type)
                .EUt(120)
                .duration(200)
                .outputFluids(outputFor(source, condense))
                .addData(SOURCE_KEY, source)
                .addData(MODE_KEY, condense ? MODE_CONDENSE : MODE_COLLECT)
                .buildRawRecipe());
    }

    private static FluidStack outputFor(String source, boolean condense) {
        return switch (source) {
            case SOURCE_OVERWORLD, SOURCE_ANCIENT_WORLD, SOURCE_PERSONAL -> (condense ?
                    GTMaterials.LiquidAir : GTMaterials.Air).getFluid(10_000);
            case SOURCE_NETHER -> (condense ?
                    GTMaterials.LiquidNetherAir : GTMaterials.NetherAir).getFluid(10_000);
            case SOURCE_END -> (condense ?
                    GTMaterials.LiquidEnderAir : GTMaterials.EnderAir).getFluid(10_000);
            // BarnardaAir 只有气态材料，两种模式都输出同一种气体。
            case SOURCE_BARNARDA -> GTLMaterials.BarnardaAir.getFluid(10_000);
            default -> FluidStack.empty();
        };
    }

    /**
     * 直接使用运行时配方，避免无输入配方在 GT 配方索引中查不到。
     * 数据配方仍然保留给 EMI 展示维度条件。
     */
    private static final class AtmosphericCollectionRecipeLogic extends RecipeLogic {

        private AtmosphericCollectionRecipeLogic(IRecipeLogicMachine machine) {
            super(machine);
        }

        /**
         * GTLCore 重写了 {@code findAndHandleRecipe}，不会调用普通的 {@code searchRecipe}。
         * 这里直接接管查找流程，只使用当前维度对应的运行时配方。
         */
        @Override
        public void findAndHandleRecipe() {
            this.lastRecipe = null;
            Level level = this.machine.self().getLevel();
            String source = sourceFor(level);
            GTRecipeType type = this.machine.getRecipeType();
            if (level == null || source == null || type == null) {
                this.lastOriginRecipe = null;
                setStatus(Status.IDLE);
                this.recipeDirty = false;
                return;
            }

            boolean condense = !(this.machine instanceof AtmosphericCollectionUnitMachine unit) ||
                    unit.isCondenseAir();
            // 数据配方按来源+工作模式区分；找不到时退回运行时配方。
            GTRecipe origin = findDataRecipe(level, type, source, condense);
            if (origin == null) {
                origin = runtimeRecipe(type, source, condense);
            }
            GTRecipe modified = this.machine.fullModifyRecipe(origin.copy(), this.ocParams, this.ocResult);
            if (modified != null &&
                    modified.matchRecipe(this.machine).isSuccess() &&
                    modified.matchTickRecipe(this.machine).isSuccess() &&
                    modified.checkConditions(this).isSuccess()) {
                setupRecipe(modified);
                if (this.lastRecipe != null && getStatus() == Status.WORKING) {
                    this.lastOriginRecipe = origin;
                    this.recipeDirty = false;
                    return;
                }
            }

            this.lastOriginRecipe = null;
            setStatus(Status.IDLE);
            this.recipeDirty = false;
        }
    }
}
