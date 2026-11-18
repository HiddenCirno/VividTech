package com.vividtech.common.cover;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.ITieredMachine;

import com.hepdd.gtmthings.api.misc.WirelessEnergyManager;

import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * 无线电网发射器覆盖板：从所贴机器侧面抽取能量，充入放置者所在的无线电网。
 */
public class WirelessGridTransmitCover extends CoverBehavior {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            WirelessGridTransmitCover.class, CoverBehavior.MANAGED_FIELD_HOLDER);

    @Persisted
    private UUID uuid;

    private final int tier;
    private final int amperage;
    private TickableSubscription subscription;

    public WirelessGridTransmitCover(CoverDefinition definition, ICoverable coverHolder,
                                     Direction attachedSide, int tier, int amperage) {
        super(definition, coverHolder, attachedSide);
        this.tier = tier;
        this.amperage = amperage;
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public boolean canAttach() {
        MetaMachine machine = MetaMachine.getMachine(coverHolder.getLevel(), coverHolder.getPos());
        if (!(machine instanceof ITieredMachine tieredMachine) || tieredMachine.getTier() < tier) {
            return false;
        }

        IEnergyContainer container = GTCapabilityHelper.getEnergyContainer(
                coverHolder.getLevel(), coverHolder.getPos(), attachedSide);
        if (container == null || !container.outputsEnergy(attachedSide)) {
            return false;
        }

        // 与接收器一致：每台机器只允许一个无线电网覆盖板。
        for (CoverBehavior cover : coverHolder.getCovers()) {
            if (cover instanceof WirelessGridTransmitCover) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onAttached(ItemStack stack, ServerPlayer player) {
        super.onAttached(stack, player);
        this.uuid = player.getUUID();
        updateCoverSub();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        updateCoverSub();
    }

    @Override
    public void onRemoved() {
        super.onRemoved();
        if (subscription != null) {
            subscription.unsubscribe();
            subscription = null;
        }
    }

    private void updateCoverSub() {
        if (uuid != null) {
            subscription = coverHolder.subscribeServerTick(subscription, this::updateEnergy);
        } else if (subscription != null) {
            subscription.unsubscribe();
            subscription = null;
        }
    }

    private void updateEnergy() {
        if (uuid == null) {
            return;
        }

        IEnergyContainer container = GTCapabilityHelper.getEnergyContainer(
                coverHolder.getLevel(), coverHolder.getPos(), attachedSide);
        if (container == null || !container.outputsEnergy(attachedSide)) {
            return;
        }

        long stored = container.getEnergyStored();
        if (stored <= 0) {
            return;
        }

        long requested = Math.min(stored, GTValues.V[tier] * (long) amperage);
        if (requested <= 0) {
            return;
        }

        // 先扣机器，再充入电网，避免出现“电网加了、机器没扣”的能量复制。
        long removed = container.removeEnergy(requested);
        if (removed <= 0) {
            return;
        }

        MetaMachine machine = MetaMachine.getMachine(coverHolder.getLevel(), coverHolder.getPos());
        if (!WirelessEnergyManager.addEUToGlobalEnergyMap(uuid, removed, machine)) {
            // 理论上正数入账不会失败；保留回滚以防万一。
            container.addEnergy(removed);
        }
    }
}
