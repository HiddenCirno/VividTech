package com.vividtech.common.machine.generator;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableEnergyContainer;

import org.gtlcore.gtlcore.common.machine.generator.MagicEnergyMachine;

/**
 * 压缩魔法能源吸收器：输出电压与普通版本相同，但电流和缓存均为 16 倍。
 */
public class CompressedMagicEnergyMachine extends MagicEnergyMachine {

    public CompressedMagicEnergyMachine(IMachineBlockEntity holder, int tier, Object... args) {
        super(holder, tier, args);
    }

    @Override
    public long getMaxInputOutputAmperage() {
        return super.getMaxInputOutputAmperage() * 16L;
    }

    @Override
    protected NotifiableEnergyContainer createEnergyContainer(Object... args) {
        long tierVoltage = GTValues.V[getTier()];
        return NotifiableEnergyContainer.emitterContainer(this,
                tierVoltage * 512L * 16L, tierVoltage, getMaxInputOutputAmperage());
    }
}
