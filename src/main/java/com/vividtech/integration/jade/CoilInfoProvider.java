package com.vividtech.integration.jade;

import com.gregtechceu.gtceu.api.block.ICoilType;
import com.gregtechceu.gtceu.common.block.CoilBlock;

import com.vividtech.VividTech;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * 在 Jade 中显示 GT 加热线圈的物品 Tooltip 数据。
 * 线圈数据完全由方块自身决定，因此不需要服务端数据同步。
 */
public enum CoilInfoProvider implements IBlockComponentProvider {

    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlock() instanceof CoilBlock coilBlock)) {
            return;
        }
        ICoilType coil = coilBlock.coilType;
        int coilTier = coil.getTier();

        tooltip.add(Component.translatable("block.gtceu.wire_coil.tooltip_heat", coil.getCoilTemperature()));
        tooltip.add(Component.translatable("block.gtceu.wire_coil.tooltip_smelter"));
        tooltip.add(Component.translatable("block.gtceu.wire_coil.tooltip_parallel_smelter",
                coil.getLevel() * 32));
        tooltip.add(Component.translatable("block.gtceu.wire_coil.tooltip_energy_smelter",
                Math.max(1, 16 / coil.getEnergyDiscount())));
        tooltip.add(Component.translatable("block.gtceu.wire_coil.tooltip_pyro"));
        tooltip.add(Component.translatable("block.gtceu.wire_coil.tooltip_speed_pyro",
                coilTier == 0 ? 75 : 50 * (coilTier + 1)));
        tooltip.add(Component.translatable("block.gtceu.wire_coil.tooltip_cracking"));
        tooltip.add(Component.translatable("block.gtceu.wire_coil.tooltip_energy_cracking",
                100 - 10 * coilTier));
    }

    @Override
    public ResourceLocation getUid() {
        return VividTech.id("coil_info");
    }
}
