package com.vividtech.common.machine;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IInteractedMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.common.data.GTItems;

import com.hepdd.gtmthings.api.misc.WirelessEnergyManager;

import org.gtlcore.gtlcore.utils.datastructure.Int128;

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
 * 创造模式无线电网能量井。
 *
 * <p>每 tick 把放置者无线电网的能量维持在 GTLCore 的 Int128 能量上限。GTMThings 的
 * 无线电网余额本身是 BigInteger，没有硬编码上限；这里按本包的 Int128 最大能量值作为
 * 电网上限。只在实际低于上限时写入，因此全局只放一个时开销可以忽略。</p>
 */
public class CreativeWirelessGridEnergyWellMachine extends MetaMachine
        implements IMachineLife, IInteractedMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            CreativeWirelessGridEnergyWellMachine.class, MetaMachine.MANAGED_FIELD_HOLDER);

    public static final BigInteger GRID_MAX_ENERGY = Int128.MAX_VALUE.toBigInteger();

    @Persisted
    @DescSynced
    private UUID owner;

    private TickableSubscription fillSubscription;

    public CreativeWirelessGridEnergyWellMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) {
            fillSubscription = subscribeServerTick(this::fillNetwork);
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

    private void fillNetwork() {
        if (owner == null) {
            return;
        }
        BigInteger current = WirelessEnergyManager.getUserEU(owner);
        if (current == null || current.compareTo(GRID_MAX_ENERGY) < 0) {
            WirelessEnergyManager.setUserEU(owner, GRID_MAX_ENERGY);
        }
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
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(GTItems.TOOL_DATA_STICK.get())) {
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
