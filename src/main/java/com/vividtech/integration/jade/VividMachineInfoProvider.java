package com.vividtech.integration.jade;

import com.vividtech.VividTech;
import com.vividtech.common.machine.multiblock.steam.AbstractSteamResourceModuleMachine;
import com.vividtech.common.machine.multiblock.steam.DreamElevatorMachine;
import com.vividtech.common.machine.multiblock.steam.SteamProcessingModuleMachine;
import com.vividtech.common.machine.multiblock.steam.SteamStratumDrillingModuleMachine;
import com.vividtech.common.machine.multiblock.steam.SteamStratumMiningModuleMachine;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.integration.jade.provider.CapabilityBlockProvider;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jetbrains.annotations.Nullable;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade 方块信息扩展：显示梦想电梯、加工模块和资源模块的内部工作状态。
 */
public final class VividMachineInfoProvider extends CapabilityBlockProvider<MetaMachine> {

    private static final String KIND = "kind";

    public VividMachineInfoProvider() {
        super(VividTech.id("machine_info"));
    }

    @Override
    protected @Nullable MetaMachine getCapability(Level level, BlockPos pos, @Nullable Direction side) {
        return MetaMachine.getMachine(level, pos);
    }

    @Override
    protected boolean allowDisplaying(MetaMachine machine) {
        return machine instanceof DreamElevatorMachine ||
                machine instanceof SteamProcessingModuleMachine ||
                machine instanceof AbstractSteamResourceModuleMachine;
    }

    @Override
    protected void write(CompoundTag data, MetaMachine machine) {
        if (machine instanceof DreamElevatorMachine elevator) {
            data.putString(KIND, "dream_elevator");
            data.putInt("rail_tier", elevator.getRailTier());
            data.putInt("speed", (int) Math.round(elevator.getProductionSpeedMultiplier() * 100.0D));
            data.putInt("parallel_pool", elevator.getParallelPool());
            data.putInt("modules_online", elevator.getModuleSet().size());
            data.putInt("module_slots", elevator.getModuleSlots());
            data.putInt("max_recipe_tier", elevator.getMaxRecipeTier());
            data.putInt("pressurization", elevator.getPressurizationTierBonus());
        } else if (machine instanceof SteamProcessingModuleMachine module) {
            data.putString(KIND, "processing_module");
            data.putInt("module_tier", module.getModuleTier());
            data.putInt("overclock", module.getOverclockLevel());
            data.putInt("effective_overclock",
                    module.getEffectiveOverclock(module.getRecipeLogic().getLastOriginRecipe()));
            data.putInt("effective_parallel",
                    module.getEffectiveParallel(module.getRecipeLogic().getLastOriginRecipe()));
            data.putBoolean("connected", module.getHost() != null);
        } else if (machine instanceof AbstractSteamResourceModuleMachine module) {
            data.putString(KIND, "resource_module");
            data.putInt("module_tier", module.getModuleTier());
            data.putInt("overclock", module.getOverclockLevel());
            data.putBoolean("connected", module.getHost() != null);
            if (module instanceof SteamStratumMiningModuleMachine mining) {
                data.putBoolean("whitelist", mining.isWhitelistMode());
            } else if (module instanceof SteamStratumDrillingModuleMachine drilling) {
                Level level = drilling.getLevel();
                if (level != null && level.dimension().equals(Level.OVERWORLD)) {
                    data.putString("output", drilling.isSaltWaterMode() ? "salt_water" : "oil");
                } else if (level != null && level.dimension().equals(Level.NETHER) &&
                        drilling.getModuleTier() >= 2) {
                    data.putString("output", "natural_gas");
                } else {
                    data.putString("output", "none");
                }
            }
        }
    }

    @Override
    protected void addTooltip(CompoundTag data, ITooltip tooltip, Player player, BlockAccessor block,
                              BlockEntity blockEntity, IPluginConfig config) {
        String kind = data.getString(KIND);
        if (kind.isEmpty()) {
            return;
        }
        switch (kind) {
            case "dream_elevator" -> addDreamElevatorTooltip(data, tooltip);
            case "processing_module" -> addProcessingModuleTooltip(data, tooltip);
            case "resource_module" -> addResourceModuleTooltip(data, tooltip);
            default -> {}
        }
    }

    private static void addDreamElevatorTooltip(CompoundTag data, ITooltip tooltip) {
        tooltip.add(Component.translatable("vividtech.jade.rail_tier",
                Component.literal(roman(data.getInt("rail_tier"))).withStyle(ChatFormatting.AQUA)));
        tooltip.add(Component.translatable("vividtech.jade.speed", data.getInt("speed")));
        tooltip.add(Component.translatable("vividtech.jade.parallel_pool", data.getInt("parallel_pool")));
        tooltip.add(Component.translatable("vividtech.jade.modules",
                data.getInt("modules_online"), data.getInt("module_slots")));
        tooltip.add(Component.translatable("vividtech.jade.max_recipe_tier",
                GTValues.VNF[data.getInt("max_recipe_tier")]));
        tooltip.add(Component.translatable("vividtech.jade.pressurization", data.getInt("pressurization")));
    }

    private static void addProcessingModuleTooltip(CompoundTag data, ITooltip tooltip) {
        tooltip.add(Component.translatable("vividtech.jade.module_tier",
                Component.literal("MK-" + VividTech.toRoman(data.getInt("module_tier")))));
        tooltip.add(connectedLine(data.getBoolean("connected")));
        tooltip.add(Component.translatable("vividtech.jade.effective_parallel", data.getInt("effective_parallel")));
        tooltip.add(Component.translatable("vividtech.jade.overclock",
                coloredOverclock(data.getInt("effective_overclock"))));
        tooltip.add(Component.translatable("vividtech.jade.overclock_limit",
                coloredOverclock(data.getInt("overclock"))));
    }

    private static void addResourceModuleTooltip(CompoundTag data, ITooltip tooltip) {
        tooltip.add(Component.translatable("vividtech.jade.module_tier",
                Component.literal("MK-" + VividTech.toRoman(data.getInt("module_tier")))));
        tooltip.add(connectedLine(data.getBoolean("connected")));
        tooltip.add(Component.translatable("vividtech.jade.overclock",
                coloredOverclock(data.getInt("overclock"))));
        if (data.contains("whitelist")) {
            tooltip.add(Component.translatable("vividtech.jade.work_mode",
                    Component.translatable(data.getBoolean("whitelist") ?
                            "vividtech.machine.stratum_mining.work_mode.whitelist" :
                            "vividtech.machine.stratum_mining.work_mode.blacklist")));
        }
        if (data.contains("output")) {
            tooltip.add(Component.translatable("vividtech.jade.output_mode",
                    Component.translatable("vividtech.jade.output." + data.getString("output"))));
        }
    }

    private static Component connectedLine(boolean connected) {
        return Component.translatable(connected ?
                "vividtech.jade.connected" : "vividtech.jade.disconnected");
    }

    private static Component coloredOverclock(int level) {
        return Component.literal(Integer.toString(level)).withStyle(overclockColor(level));
    }

    private static ChatFormatting overclockColor(int level) {
        return switch (level) {
            case 0 -> ChatFormatting.GRAY;
            case 1 -> ChatFormatting.WHITE;
            case 2 -> ChatFormatting.GREEN;
            case 3 -> ChatFormatting.BLUE;
            case 4 -> ChatFormatting.LIGHT_PURPLE;
            case 5 -> ChatFormatting.YELLOW;
            case 6 -> ChatFormatting.RED;
            default -> ChatFormatting.GRAY;
        };
    }

    private static String roman(int tier) {
        return switch (tier) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> "-";
        };
    }
}
