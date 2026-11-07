package com.vividtech.common.machine.multiblock.steam;

import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineHost;
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineModule;
import org.gtlcore.gtlcore.common.machine.multiblock.part.LargeSteamHatchPartMachine;
import org.gtlcore.gtlcore.common.machine.multiblock.steam.LargeSteamParallelMultiblockMachine;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.trait.IRecipeHandlerTrait;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.common.machine.multiblock.part.SteamHatchPartMachine;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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

    private static final int[] MODULE_SLOTS = { 6, 12, 15, 18, 24 };
    private static final int[] RAIL_PARALLELS = { 32, 64, 96, 128, 256 };
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

    private final Set<IModularMachineModule<DreamElevatorMachine, ?>> modules = new ReferenceOpenHashSet<>();
    private final Map<SteamProcessingModuleMachine, Integer> parallelReservations = new HashMap<>();
    private List<BlockPos> moduleSlotPositions = List.of();
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
        collectSteamTanks();
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
        moduleShellPositions = Set.of();
        steamTanks = List.of();
        railTier = 0;
        stopHostSubscription();
        super.onStructureInvalid();
    }

    @Override
    public @NotNull Set<IModularMachineModule<DreamElevatorMachine, ?>> getModuleSet() {
        return modules;
    }

    @Override
    public BlockPos[] getModuleScanPositions() {
        return moduleSlotPositions.toArray(BlockPos[]::new);
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
            return;
        }
        List<BlockPos> positions = new ArrayList<>(slots.size());
        for (LongIterator iterator = slots.iterator(); iterator.hasNext();) {
            positions.add(BlockPos.of(iterator.nextLong()));
        }
        positions.sort(Comparator.comparingLong(BlockPos::asLong));
        moduleSlotPositions = List.copyOf(positions);
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

    private void collectSteamTanks() {
        List<NotifiableFluidTank> tanks = new ArrayList<>();
        for (IMultiPart part : getParts()) {
            for (IRecipeHandlerTrait<?> trait : part.getRecipeHandlers()) {
                if (trait instanceof NotifiableFluidTank tank) {
                    MetaMachine machine = tank.getMachine();
                    if ((machine instanceof SteamHatchPartMachine || machine instanceof LargeSteamHatchPartMachine) &&
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
            FluidStack stack = tank.drain(toDrain, false);
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
        Integer existing = parallelReservations.get(module);
        if (existing != null) {
            return existing >= amount;
        }
        if (!modules.contains(module)) {
            return false;
        }
        int used = 0;
        for (Map.Entry<SteamProcessingModuleMachine, Integer> entry : parallelReservations.entrySet()) {
            if (entry.getKey() != module) {
                used += entry.getValue();
            }
        }
        if (used + amount > getRailParallel()) {
            return false;
        }
        parallelReservations.put(module, amount);
        return true;
    }

    public void releaseParallel(SteamProcessingModuleMachine module) {
        parallelReservations.remove(module);
    }

    public int getRailParallel() {
        return railTier > 0 ? RAIL_PARALLELS[railTier - 1] : 0;
    }

    public int getRailTier() {
        return railTier;
    }

    public int getModuleSlots() {
        return railTier > 0 ? MODULE_SLOTS[railTier - 1] : 0;
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
        super.addDisplayText(textList);
        if (!isFormed()) return;
        textList.add(Component.translatable("vividtech.machine.dream_elevator.rail_tier", railTier));
        textList.add(Component.translatable("vividtech.machine.dream_elevator.module_slots", getModuleSlots()));
        textList.add(Component.translatable("vividtech.machine.dream_elevator.modules_online", modules.size(),
                getModuleSlots()));
        textList.add(Component.translatable("vividtech.machine.dream_elevator.parallel_pool", getRailParallel(),
                getReservedParallel()));
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
