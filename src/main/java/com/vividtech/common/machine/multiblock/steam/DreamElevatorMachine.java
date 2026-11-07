package com.vividtech.common.machine.multiblock.steam;

import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineHost;
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineModule;
import org.gtlcore.gtlcore.common.machine.multiblock.part.LargeSteamHatchPartMachine;
import org.gtlcore.gtlcore.common.machine.multiblock.steam.LargeSteamParallelMultiblockMachine;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.trait.IRecipeHandlerTrait;
import com.gregtechceu.gtceu.api.machine.steam.SteamEnergyRecipeHandler;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.common.machine.multiblock.part.SteamHatchPartMachine;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 梦想电梯主机。
 *
 * <p>
 * 主机负责两件事：识别加速轨道等级，以及给已连接的蒸汽加工模块均分蒸汽。
 * 模块本身是独立多方块，仓和配方进度完全隔离。
 * </p>
 */
public class DreamElevatorMachine extends LargeSteamParallelMultiblockMachine
                                  implements IModularMachineHost<DreamElevatorMachine>, IMachineLife {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            DreamElevatorMachine.class, LargeSteamParallelMultiblockMachine.MANAGED_FIELD_HOLDER);

    /** 动力模块等级提供的全机并行池。 */
    private static final int[] POWER_PARALLELS = { 256, 384, 512, 768, 1200 };

    /** 动力模块等级提供的生产速度倍率。 */
    private static final double[] POWER_SPEED = { 1.0D, 1.5D, 2.0D, 2.5D, 4.0D };

    /** 动力模块等级解锁的模块槽位数量。 */
    private static final int[] MODULE_SLOTS = { 6, 12, 15, 18, 24 };

    /** 内环槽位固定 12 个，按到结构中心的距离识别。 */
    private static final int INNER_MODULE_SLOTS = 12;

    private static final int HOST_CHECK_INTERVAL_TICKS = 20;
    private static final int STEAM_DISTRIBUTION_INTERVAL_TICKS = 5;

    /**
     * 加速轨道等级。与 GTL 的 SEPM 外壳检测相同：所有轨道位共用同一个 match context key，
     * 第一个轨道方块决定等级，后续任意一块不一致都会让整座结构匹配失败。
     */
    public static final String RAIL_TIER_KEY = "DreamElevatorRailTier";

    /** 主机成型时记录的模块基座位置。 */
    public static final String MODULE_SLOT_KEY = "DreamElevatorModuleSlots";

    /** 主机成型时记录的模块外壳位置；这些位置上的仓不能被主机登记。 */
    public static final String MODULE_SHELL_KEY = "DreamElevatorModuleShells";

    public static final Int2ObjectMap<Supplier<?>> RAIL_TIER_BLOCKS = new Int2ObjectOpenHashMap<>();

    static {
        RAIL_TIER_BLOCKS.put(1, () -> Blocks.COPPER_BLOCK);
        RAIL_TIER_BLOCKS.put(2, () -> Blocks.LAPIS_BLOCK);
        RAIL_TIER_BLOCKS.put(3, () -> Blocks.DIAMOND_BLOCK);
        RAIL_TIER_BLOCKS.put(4, () -> Blocks.EMERALD_BLOCK);
        RAIL_TIER_BLOCKS.put(5, () -> Blocks.NETHERITE_BLOCK);
    }

    @Persisted
    @DescSynced
    private int railTier;

    @Persisted
    @DescSynced
    private boolean animationEnabled = true;

    @DescSynced
    private boolean hasLargeSteamHatch;

    private final Set<IModularMachineModule<DreamElevatorMachine, ?>> modules = new ReferenceOpenHashSet<>();
    private final Map<SteamProcessingModuleMachine, Integer> parallelReservations = new HashMap<>();
    private List<BlockPos> moduleSlotPositions = List.of();
    private List<BlockPos> moduleOrderedSlots = List.of();
    private Set<BlockPos> moduleInnerSlots = Set.of();
    private Set<BlockPos> moduleShellPositions = Set.of();
    private List<NotifiableFluidTank> steamTanks = List.of();

    private TickableSubscription hostTickSubscription;

    public DreamElevatorMachine(IMachineBlockEntity holder) {
        super(holder, 8);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        updateHostSubscription();
    }

    @Override
    public void onUnload() {
        stopHostSubscription();
        super.onUnload();
    }

    @Override
    public void onMachineRemoved() {
        safeClearModules();
        stopHostSubscription();
    }

    @Override
    public void onStructureFormed() {
        // 必须在 super 之前收集模块位置：super 会通过 shouldAddPartToController 过滤模块仓。
        collectModuleSlots();
        collectModuleShells();
        super.onStructureFormed();
        Integer tier = getMultiblockState().getMatchContext().get(RAIL_TIER_KEY);
        railTier = tier == null ? 0 : tier;
        hasLargeSteamHatch = detectLargeSteamHatch();
        markDirty();
        notifyBlockUpdate();
        collectSteamTanks();
        disableLargeSteamOverclock();
        safeClearModules();
        scanAndConnectModules();
        updateHostSubscription();
    }

    @Override
    public boolean shouldAddPartToController(IMultiPart part) {
        return !moduleShellPositions.contains(part.self().getPos());
    }

    @Override
    public void onStructureInvalid() {
        safeClearModules();
        parallelReservations.clear();
        moduleSlotPositions = List.of();
        moduleOrderedSlots = List.of();
        moduleInnerSlots = Set.of();
        moduleShellPositions = Set.of();
        steamTanks = List.of();
        railTier = 0;
        hasLargeSteamHatch = false;
        stopHostSubscription();
        super.onStructureInvalid();
    }

    @Override
    public @NotNull Set<IModularMachineModule<DreamElevatorMachine, ?>> getModuleSet() {
        return modules;
    }

    @Override
    public BlockPos[] getModuleScanPositions() {
        int slots = getModuleSlots();
        if (slots <= 0) {
            return new BlockPos[0];
        }
        int limit = Math.min(slots, moduleOrderedSlots.size());
        return moduleOrderedSlots.subList(0, limit).toArray(BlockPos[]::new);
    }

    @Override
    public int getMaxModuleCount() {
        return getModuleSlots();
    }

    @Override
    public boolean isValidModule(MetaMachine machine) {
        if (!IModularMachineHost.super.isValidModule(machine)) {
            return false;
        }
        if (machine instanceof IModularMachineModule<?, ?> module && modules.contains(module)) {
            return true;
        }
        return getFormedModuleCount() < getMaxModuleCount();
    }

    private void updateHostSubscription() {
        if (isFormed() && hostTickSubscription == null) {
            hostTickSubscription = subscribeServerTick(this::hostTick);
        }
    }

    private void stopHostSubscription() {
        if (hostTickSubscription != null) {
            hostTickSubscription.unsubscribe();
            hostTickSubscription = null;
        }
    }

    private void hostTick() {
        if (!isFormed()) {
            stopHostSubscription();
            return;
        }
        long timer = getOffsetTimer();
        if (timer % HOST_CHECK_INTERVAL_TICKS == 0) {
            pruneParallelReservations();
            if (modules.size() < getModuleScanPositions().length) {
                scanAndConnectModules();
            }
        }
        if (timer % STEAM_DISTRIBUTION_INTERVAL_TICKS == 0) {
            distributeSteam();
        }
    }

    private void collectModuleSlots() {
        Object raw = getMultiblockState().getMatchContext().get(MODULE_SLOT_KEY);
        if (!(raw instanceof LongSet slots)) {
            moduleSlotPositions = List.of();
            moduleOrderedSlots = List.of();
            moduleInnerSlots = Set.of();
            return;
        }
        List<BlockPos> positions = new ArrayList<>(slots.size());
        for (LongIterator iterator = slots.iterator(); iterator.hasNext();) {
            positions.add(BlockPos.of(iterator.nextLong()));
        }
        positions.sort(Comparator.comparingLong(BlockPos::asLong));
        moduleSlotPositions = List.copyOf(positions);
        moduleInnerSlots = computeInnerSlots(positions);
        // 内环优先解锁，随后才是外环。
        List<BlockPos> ordered = new ArrayList<>(positions.size());
        for (BlockPos pos : positions) {
            if (moduleInnerSlots.contains(pos)) {
                ordered.add(pos);
            }
        }
        for (BlockPos pos : positions) {
            if (!moduleInnerSlots.contains(pos)) {
                ordered.add(pos);
            }
        }
        moduleOrderedSlots = List.copyOf(ordered);
    }

    /**
     * 内环模块离结构中心最近。用全部槽位的质心做距离排序，方向无关，
     * 取最近的 12 个作为内环槽位。
     */
    private Set<BlockPos> computeInnerSlots(List<BlockPos> positions) {
        if (positions.size() <= INNER_MODULE_SLOTS) {
            return Set.copyOf(positions);
        }
        double centerX = 0.0D;
        double centerY = 0.0D;
        double centerZ = 0.0D;
        for (BlockPos pos : positions) {
            centerX += pos.getX();
            centerY += pos.getY();
            centerZ += pos.getZ();
        }
        int count = positions.size();
        centerX /= count;
        centerY /= count;
        centerZ /= count;
        List<BlockPos> sorted = new ArrayList<>(positions);
        double fx = centerX;
        double fy = centerY;
        double fz = centerZ;
        sorted.sort(Comparator.comparingDouble(pos -> {
            double dx = pos.getX() - fx;
            double dy = pos.getY() - fy;
            double dz = pos.getZ() - fz;
            return dx * dx + dy * dy + dz * dz;
        }));
        return Set.copyOf(sorted.subList(0, INNER_MODULE_SLOTS));
    }

    private boolean detectLargeSteamHatch() {
        for (IMultiPart part : getParts()) {
            if (part.self() instanceof LargeSteamHatchPartMachine) {
                return true;
            }
        }
        return false;
    }

    /**
     * 梦想电梯单独禁用 GTL 大型蒸汽输入仓的超频：蒸汽转换率恒定为 0.5。
     */
    private void disableLargeSteamOverclock() {
        if (steamTanks.isEmpty()) {
            return;
        }
        capabilitiesProxy.put(IO.IN, EURecipeCapability.CAP,
                List.of(new SteamEnergyRecipeHandler(steamTanks.get(0), 0.5D)));
    }

    @Override
    public double getSteamConversionRate() {
        return 0.5D;
    }

    @Override
    public long getMaxRecipeEUt() {
        if (railTier >= 5) return 512L;
        if (railTier >= 3) return 128L;
        return 32L;
    }

    @Override
    public boolean isUsingLargeSteamHatch() {
        return hasLargeSteamHatch;
    }

    @Override
    public double getConversionRate() {
        return getSteamConversionRate();
    }

    /** 全机允许执行的最高配方电压等级：T1/T2 = LV，T3/T4 = MV，T5 = HV。 */
    public int getMaxRecipeTier() {
        if (railTier >= 5) return GTValues.HV;
        if (railTier >= 3) return GTValues.MV;
        return GTValues.LV;
    }

    public double getProductionSpeedMultiplier() {
        return railTier > 0 ? POWER_SPEED[railTier - 1] : 1.0D;
    }

    public boolean isInnerModule(BlockPos pos) {
        return moduleInnerSlots.contains(pos);
    }

    /** 模块实际可申请的并行：T5 动力模块让内环模块翻倍。 */
    public int getMaxModuleParallel(SteamProcessingModuleMachine module) {
        int base = module.getModuleParallel();
        if (railTier >= 5 && isInnerModule(module.getPos())) {
            return base * 2;
        }
        return base;
    }

    private void collectModuleShells() {
        Object raw = getMultiblockState().getMatchContext().get(MODULE_SHELL_KEY);
        if (!(raw instanceof LongSet shells)) {
            moduleShellPositions = Set.of();
            return;
        }
        Set<BlockPos> positions = new HashSet<>();
        for (LongIterator iterator = shells.iterator(); iterator.hasNext();) {
            positions.add(BlockPos.of(iterator.nextLong()));
        }
        moduleShellPositions = Set.copyOf(positions);
    }

    private boolean isSteamHatchTank(NotifiableFluidTank tank) {
        MetaMachine machine = tank.getMachine();
        return machine instanceof SteamHatchPartMachine || machine instanceof LargeSteamHatchPartMachine;
    }

    private void collectSteamTanks() {
        List<NotifiableFluidTank> tanks = new ArrayList<>();
        for (IMultiPart part : getParts()) {
            for (IRecipeHandlerTrait<?> trait : part.getRecipeHandlers()) {
                if (trait instanceof NotifiableFluidTank tank) {
                    MetaMachine machine = tank.getMachine();
                    if ((machine instanceof SteamHatchPartMachine ||
                            machine instanceof LargeSteamHatchPartMachine) &&
                            !tanks.contains(tank)) {
                        tanks.add(tank);
                    }
                }
            }
        }
        steamTanks = tanks;
    }

    private void distributeSteam() {
        if (steamTanks.isEmpty()) return;
        Set<SteamProcessingModuleMachine> steamModules = getModulesOfType(
                SteamProcessingModuleMachine.class);
        steamModules.removeIf(module -> !module.isFormed());
        if (steamModules.isEmpty()) return;

        long available = 0;
        for (NotifiableFluidTank tank : steamTanks) {
            available += tank.getFluidInTank(0).getAmount();
        }
        if (available <= 0) return;

        long share = available / steamModules.size();
        if (share <= 0) return;

        for (SteamProcessingModuleMachine module : steamModules) {
            long amount = Math.min(share, module.getSteamSpace());
            if (amount <= 0) continue;
            long drained = drainSteam(amount);
            if (drained > 0) {
                module.addSteam(drained);
            }
        }
    }

    private long drainSteam(long amount) {
        long drained = 0;
        for (NotifiableFluidTank tank : steamTanks) {
            if (drained >= amount) break;
            long stored = tank.getFluidInTank(0).getAmount();
            if (stored <= 0) continue;
            long toDrain = Math.min(amount - drained, stored);
            // 蒸汽输入仓的 capabilityIO 是 IN，外部 drain() 会被拒绝，必须走内部接口。
            FluidStack stack = tank.drainInternal(toDrain, false);
            drained += stack.getAmount();
        }
        return drained;
    }

    private void pruneParallelReservations() {
        parallelReservations.keySet().removeIf(module -> !modules.contains(module) || !module.isFormed() ||
                !module.getRecipeLogic().isWorking());
    }

    public boolean tryReserveParallel(SteamProcessingModuleMachine module, int amount) {
        if (isRemote() || amount <= 0) return false;
        if (!modules.contains(module)) {
            return false;
        }
        Integer existing = parallelReservations.get(module);
        int current = existing == null ? 0 : existing;
        if (current >= amount) {
            return true;
        }
        int used = 0;
        for (Map.Entry<SteamProcessingModuleMachine, Integer> entry : parallelReservations.entrySet()) {
            if (entry.getKey() != module) {
                used += entry.getValue();
            }
        }
        if (used + amount > getParallelPool()) {
            return false;
        }
        parallelReservations.put(module, amount);
        return true;
    }

    public void releaseParallel(SteamProcessingModuleMachine module) {
        parallelReservations.remove(module);
    }

    /** 全机并行池。 */
    public int getParallelPool() {
        return railTier > 0 ? POWER_PARALLELS[railTier - 1] : 0;
    }

    public int getRailTier() {
        return railTier;
    }

    public int getModuleSlots() {
        return railTier > 0 ? MODULE_SLOTS[railTier - 1] : 0;
    }

    public int getPowerTier() {
        return railTier;
    }

    public boolean isAnimationEnabled() {
        return animationEnabled;
    }

    @Override
    protected InteractionResult onScrewdriverClick(Player playerIn, InteractionHand hand, Direction gridSide,
                                                   BlockHitResult hitResult) {
        if (playerIn.isShiftKeyDown()) {
            if (!isRemote()) {
                animationEnabled = !animationEnabled;
                markDirty();
                notifyBlockUpdate();
                playerIn.displayClientMessage(Component.translatable(animationEnabled ?
                        "vividtech.machine.dream_elevator.animation.enabled" :
                        "vividtech.machine.dream_elevator.animation.disabled"), true);
            }
            return InteractionResult.SUCCESS;
        }
        return super.onScrewdriverClick(playerIn, hand, gridSide, hitResult);
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        // 不调用 GTL 父类的显示：它会为大型蒸汽输入仓显示超频按钮，而梦想电梯已禁用该超频。
        for (IMultiPart part : getParts()) {
            part.addMultiText(textList);
        }
        if (!isFormed()) return;

        long steamStored = 0L;
        long steamCapacity = 0L;
        for (IMultiPart part : getParts()) {
            for (IRecipeHandlerTrait<?> trait : part.getRecipeHandlers()) {
                if (trait instanceof NotifiableFluidTank tank && isSteamHatchTank(tank)) {
                    steamStored += tank.getFluidInTank(0).getAmount();
                    steamCapacity += tank.getTankCapacity(0);
                }
            }
        }
        if (steamCapacity > 0) {
            textList.add(Component.translatable("gtceu.multiblock.steam.steam_stored", steamStored, steamCapacity));
        }
        if (!isWorkingEnabled()) {
            textList.add(Component.translatable("gtceu.multiblock.work_paused"));
        } else if (isActive()) {
            textList.add(Component.translatable("gtceu.multiblock.running"));
            textList.add(Component.translatable("gtceu.multiblock.progress",
                    (int) (recipeLogic.getProgressPercent() * 100)));
        } else {
            textList.add(Component.translatable("gtceu.multiblock.idling"));
        }
        if (recipeLogic.isWaiting()) {
            textList.add(Component.translatable("gtceu.multiblock.steam.low_steam")
                    .setStyle(Style.EMPTY.withColor(ChatFormatting.RED)));
        }

        textList.add(Component.translatable("vividtech.machine.dream_elevator.power_tier", railTier));
        textList.add(Component.translatable("vividtech.machine.dream_elevator.speed",
                String.format("%.0f", getProductionSpeedMultiplier() * 100.0D)));
        textList.add(Component.translatable("vividtech.machine.dream_elevator.module_slots", getModuleSlots()));
        textList.add(Component.translatable("vividtech.machine.dream_elevator.modules_online", modules.size(),
                getModuleSlots()));
        textList.add(Component.translatable("vividtech.machine.dream_elevator.parallel_pool", getParallelPool(),
                getReservedParallel()));
        textList.add(Component.translatable(hasLargeSteamHatch ?
                "vividtech.machine.dream_elevator.large_steam_hatch.enabled" :
                "vividtech.machine.dream_elevator.large_steam_hatch.disabled"));
        textList.add(Component.translatable(animationEnabled ?
                "vividtech.machine.dream_elevator.animation.status.enabled" :
                "vividtech.machine.dream_elevator.animation.status.disabled"));
    }

    private int getReservedParallel() {
        int used = 0;
        for (int amount : parallelReservations.values()) {
            used += amount;
        }
        return used;
    }
}
