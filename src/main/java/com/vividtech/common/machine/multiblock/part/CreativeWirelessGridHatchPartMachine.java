package com.vividtech.common.machine.multiblock.part;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IInteractedMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredIOPartMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableEnergyContainer;
import com.gregtechceu.gtceu.common.data.GTItems;

import com.hepdd.gtmthings.api.misc.WirelessEnergyManager;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.math.BigInteger;
import java.util.UUID;

/**
 * 创造模式无线电网仓。
 *
 * <p>从放置者的无线电网拉取能量。GT 的能量容器电流 API 是 {@code long}；
 * 这里使用 33,554,432A（2^25），配合 MAX 电压时单 tick 需求为 2^56 EU，
 * 乘 16 作为缓存仍在 {@code long} 安全范围内，并与 GTLCore 的自动能源仓 Tooltip 对齐。</p>
 */
public class CreativeWirelessGridHatchPartMachine extends TieredIOPartMachine
        implements IMachineLife, IInteractedMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            CreativeWirelessGridHatchPartMachine.class, TieredIOPartMachine.MANAGED_FIELD_HOLDER);

    public static final long MAX_VOLTAGE = GTValues.V[GTValues.MAX];
    public static final long AMPERAGE = 33_554_432L;
    public static final long ENERGY_PER_TICK = Math.multiplyExact(MAX_VOLTAGE, AMPERAGE);
    public static final long CAPACITY = Math.multiplyExact(ENERGY_PER_TICK, 16L);

    public final NotifiableEnergyContainer energyContainer;

    @Persisted
    @DescSynced
    private UUID owner;

    private TickableSubscription energySubscription;

    public CreativeWirelessGridHatchPartMachine(IMachineBlockEntity holder) {
        super(holder, GTValues.MAX, IO.IN);
        this.energyContainer = NotifiableEnergyContainer.receiverContainer(
                this, CAPACITY, MAX_VOLTAGE, AMPERAGE);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) {
            energySubscription = subscribeServerTick(this::pullEnergy);
        }
    }

    @Override
    public void onUnload() {
        if (energySubscription != null) {
            energySubscription.unsubscribe();
            energySubscription = null;
        }
        super.onUnload();
    }

    private void pullEnergy() {
        if (owner == null) {
            return;
        }
        long stored = energyContainer.getEnergyStored();
        long missing = energyContainer.getEnergyCapacity() - stored;
        if (missing <= 0) {
            return;
        }
        long request = Math.min(missing, ENERGY_PER_TICK);
        if (request <= 0) {
            return;
        }
        if (!WirelessEnergyManager.addEUToGlobalEnergyMap(owner, BigInteger.valueOf(-request), this)) {
            return;
        }
        energyContainer.setEnergyStored(stored + request);
    }

    @Override
    public void onMachinePlaced(LivingEntity player, ItemStack stack) {
        if (player != null) {
            owner = player.getUUID();
            markDirty();
        }
    }

    @Override
    public InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player,
                                   InteractionHand hand, BlockHitResult hit) {
        if (!player.getItemInHand(hand).is(GTItems.TOOL_DATA_STICK.get())) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        owner = player.getUUID();
        markDirty();
        player.displayClientMessage(Component.translatable("vividtech.wireless_grid.bound"), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean onLeftClick(Player player, Level level, InteractionHand hand, BlockPos pos, Direction direction) {
        if (!player.getItemInHand(hand).is(GTItems.TOOL_DATA_STICK.get())) {
            return false;
        }
        if (level.isClientSide) {
            return true;
        }
        owner = null;
        markDirty();
        player.displayClientMessage(Component.translatable("vividtech.wireless_grid.unbound"), true);
        return true;
    }
}
