package com.vividtech.common.data;

import com.vividtech.VividTech;
import com.vividtech.client.renderer.machine.CreativeSteamInputHatchRenderer;
import com.vividtech.client.renderer.machine.DreamElevatorRenderer;
import com.vividtech.client.renderer.machine.SubsystemHatchRenderer;
import com.vividtech.common.data.machines.structure.DreamElevatorStructure;
import com.vividtech.common.data.machines.structure.EyeOfWoodStructure;
import com.vividtech.common.data.machines.structure.SteamProcessingModuleStructure;
import com.vividtech.common.machine.generator.CompressedMagicEnergyMachine;
import com.vividtech.common.machine.multiblock.noenergy.EyeOfWoodMachine;
import com.vividtech.common.machine.multiblock.part.CastingInputHatchPartMachine;
import com.vividtech.common.machine.multiblock.part.CreativeSteamInputHatchMachine;
import com.vividtech.common.machine.multiblock.part.DreamElevatorCleanroomSubsystemMachine;
import com.vividtech.common.machine.multiblock.part.DreamElevatorGravitySubsystemMachine;
import com.vividtech.common.machine.multiblock.part.ForgingInputBusPartMachine;
import com.vividtech.common.machine.multiblock.electric.AtmosphericCollectionUnitMachine;
import com.vividtech.common.machine.multiblock.electric.BasicIntegratedOreProcessorMachine;
import com.vividtech.common.machine.multiblock.electric.CrystalAggregationFactoryMachine;
import com.vividtech.common.machine.multiblock.steam.DreamElevatorMachine;
import com.vividtech.common.machine.multiblock.steam.PressurizationModuleMachine;
import com.vividtech.common.machine.multiblock.steam.SteamProcessingModuleMachine;
import com.vividtech.common.machine.multiblock.steam.SteamStratumDrillingModuleMachine;
import com.vividtech.common.machine.multiblock.steam.SteamStratumMiningModuleMachine;

import org.gtlcore.gtlcore.api.pattern.GTLPredicates;
import org.gtlcore.gtlcore.common.data.GTLMachines;
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes;
import org.gtlcore.gtlcore.common.data.machines.structure.MultiBlockMachineAStructureA;
import org.gtlcore.gtlcore.common.machine.generator.MagicEnergyMachine;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.client.renderer.machine.SimpleGeneratorMachineRenderer;
import com.gregtechceu.gtceu.common.data.GTCreativeModeTabs;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import com.lowdragmc.lowdraglib.utils.BlockInfo;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import com.tterrag.registrate.util.entry.RegistryEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static com.gregtechceu.gtceu.api.pattern.Predicates.blocks;
import static com.gregtechceu.gtceu.common.registry.GTRegistration.REGISTRATE;

public class VividMachines {

    public static void init() {}

    /** 添加者 Tooltip 的渐变配色：FF1493 → FF00FF → FF69B4。 */
    private static final int[] ADDON_GRADIENT_COLORS = { 0xFF1493, 0xFF00FF, 0xFF69B4 };

    public static final BiConsumer<ItemStack, List<Component>> VIVID_ADD = (stack, components) -> components
            .add(gradientText("vividtech.registry.add", ADDON_GRADIENT_COLORS));

    /**
     * 按字符把整行文字映射到一组颜色带上。
     */
    private static Component gradientText(String translationKey, int... colors) {
        String text = Component.translatable(translationKey).getString();
        MutableComponent result = Component.empty();
        int length = text.length();
        for (int i = 0; i < length; i++) {
            float progress = length <= 1 ? 0.0F : (float) i / (float) (length - 1);
            result.append(Component.literal(String.valueOf(text.charAt(i)))
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(interpolateColor(progress, colors)))));
        }
        return result;
    }

    private static int interpolateColor(float progress, int[] colors) {
        if (colors.length == 0) return 0xFFFFFF;
        if (colors.length == 1) return colors[0];
        float clamped = Math.max(0.0F, Math.min(1.0F, progress));
        int segments = colors.length - 1;
        float scaled = clamped * segments;
        int index = Math.min((int) scaled, segments - 1);
        float local = scaled - index;
        int from = colors[index];
        int to = colors[index + 1];
        int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * local);
        int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * local);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * local);
        return (r << 16) | (g << 8) | b;
    }

    public static final RegistryEntry<CreativeModeTab> STEAM_TAB = REGISTRATE.defaultCreativeTab("steam_power",
            builder -> builder
                    .displayItems(new GTCreativeModeTabs.RegistrateDisplayItemsGenerator("steam_power", REGISTRATE))
                    .icon(VividMachines::steamTabIcon)
                    .title(Component.translatable("itemGroup.vividtech.steam_power"))
                    .build())
            .register();

    static {
        // 让之后注册的内容进入绚乱科技的创造标签页。
        REGISTRATE.creativeModeTab(() -> STEAM_TAB);
    }

    public static final RegistryEntry<CreativeModeTab> POTENTIAL_TAB = REGISTRATE.defaultCreativeTab("infinite_potential",
            builder -> builder
                    .displayItems(new GTCreativeModeTabs.RegistrateDisplayItemsGenerator("infinite_potential", REGISTRATE))
                    .icon(VividMachines::potentialTabIcon)
                    .title(Component.translatable("itemGroup.vividtech.infinite_potential"))
                    .build())
            .register();

    static {
        REGISTRATE.creativeModeTab(() -> POTENTIAL_TAB);
    }

    /**
     * 锻压输入总线 / 熔铸输入仓：LV 起，容量与原版同级输入部件一致。
     * ULV 没有同级电力活塞，因此不注册。
     */
    public static final int[] MOLD_INPUT_PART_TIERS = Arrays.stream(GTMachines.ALL_TIERS)
            .filter(tier -> tier >= GTValues.LV && tier <= GTValues.OpV)
            .toArray();

    /** 按电压等级索引，未注册的等级为 null。 */
    public static final MachineDefinition[] FORGING_INPUT_BUS = registerForgingInputBuses();
    public static final MachineDefinition[] CASTING_INPUT_HATCH = registerCastingInputHatches();

    private static MachineDefinition[] registerForgingInputBuses() {
        REGISTRATE.creativeModeTab(() -> STEAM_TAB);
        try {
            MachineDefinition[] registered = GTMachines.registerTieredMachines(
                    // 保留旧注册 ID，避免已放置的方块在更新后消失；显示名与行为已改为锻压输入总线。
                    "casting_input_bus",
                    ForgingInputBusPartMachine::new,
                    (tier, builder) -> builder
                            .langValue(GTValues.VNF[tier] + " Forging Input Bus")
                            .rotationState(RotationState.ALL)
                            .abilities(PartAbility.IMPORT_ITEMS)
                            .overlayTieredHullRenderer("item_bus.import")
                            .tooltips(Component.translatable("gtceu.machine.item_bus.import.tooltip"))
                            .tooltips(Component.translatable("vividtech.machine.forging_input_bus.tooltip.0"))
                            .tooltips(Component.translatable("gtceu.universal.tooltip.item_storage_capacity",
                                    (1 + Math.min(9, tier)) * (1 + Math.min(9, tier))))
                            .tooltipBuilder(VIVID_ADD)
                            .register(),
                    MOLD_INPUT_PART_TIERS);

            // GTMachines.registerTieredMachines 返回的数组已经按 tier 索引，
            // 不能按传入 tiers 的循环顺序重新映射，否则整条线会错位一级。
            return registered;
        } finally {
            REGISTRATE.creativeModeTab(() -> POTENTIAL_TAB);
        }
    }

    private static MachineDefinition[] registerCastingInputHatches() {
        REGISTRATE.creativeModeTab(() -> STEAM_TAB);
        try {
            MachineDefinition[] registered = GTMachines.registerTieredMachines(
                    "casting_input_hatch",
                    CastingInputHatchPartMachine::new,
                    (tier, builder) -> builder
                            .langValue(GTValues.VNF[tier] + " Casting Input Hatch")
                            .rotationState(RotationState.ALL)
                            .abilities(PartAbility.IMPORT_FLUIDS, PartAbility.IMPORT_FLUIDS_1X)
                            .overlayTieredHullRenderer("fluid_hatch.import")
                            .tooltips(Component.translatable("gtceu.machine.fluid_hatch.import.tooltip"))
                            .tooltips(Component.translatable("vividtech.machine.casting_input_hatch.tooltip.0"))
                            .tooltips(Component.translatable("gtceu.universal.tooltip.fluid_storage_capacity",
                                    FormattingUtil.formatNumbers(FluidHatchPartMachine.getTankCapacity(
                                            FluidHatchPartMachine.INITIAL_TANK_CAPACITY_1X, tier))))
                            .tooltipBuilder(VIVID_ADD)
                            .register(),
                    MOLD_INPUT_PART_TIERS);

            return registered;
        } finally {
            REGISTRATE.creativeModeTab(() -> POTENTIAL_TAB);
        }
    }

    /**
     * 魔法能源吸收器的 MV ~ LuV 版本。ULV/LV 由 GTLCore 注册，这里只补后续等级。
     * 发电量、容量和 16A 输出逻辑与 GTLCore 的版本保持一致。
     */
    public static final MachineDefinition[] PRIMITIVE_MAGIC_ENERGY = GTMachines.registerTieredMachines(
            "primitive_magic_energy",
            MagicEnergyMachine::new,
            (tier, builder) -> builder
                    .langValue(magicEnergyName(tier))
                    .rotationState(RotationState.NON_Y_AXIS)
                    .renderer(() -> new SimpleGeneratorMachineRenderer(tier,
                            GTCEu.id("block/generators/primitive_magic_energy")))
                    .tooltips(Component.translatable("gtceu.machine.primitive_magic_energy.tooltip.0"))
                    .tooltips(Component.translatable("gtceu.machine.primitive_magic_energy.tooltip.1"))
                    .tooltips(Component.translatable("gtceu.universal.tooltip.ampere_out", 16))
                    .tooltips(Component.translatable("gtceu.universal.tooltip.voltage_out",
                            FormattingUtil.formatNumbers(GTValues.V[tier]), GTValues.VNF[tier]))
                    .tooltips(Component.translatable("gtceu.universal.tooltip.energy_storage_capacity",
                            FormattingUtil.formatNumbers(GTValues.V[tier] * 512L)))
                    .tooltipBuilder(VIVID_ADD)
                    .register(),
            GTValues.MV, GTValues.HV, GTValues.EV, GTValues.IV, GTValues.LuV,
            GTValues.ZPM, GTValues.UV, GTValues.UHV, GTValues.UEV, GTValues.UIV);

    static {
        REGISTRATE.creativeModeTab(() -> STEAM_TAB);
    }

    private static String magicEnergyName(int tier) {
        return switch (tier) {
            case GTValues.MV -> "Novice Magic Energy Absorber";
            case GTValues.HV -> "Apprentice Magic Energy Absorber";
            case GTValues.EV -> "Master Magic Energy Absorber";
            case GTValues.IV -> "Grandmaster Magic Energy Absorber";
            case GTValues.LuV -> "Legendary Magic Energy Absorber";
            case GTValues.ZPM -> "Mythic Magic Energy Absorber";
            case GTValues.UV -> "Transcendent Magic Energy Absorber";
            case GTValues.UHV -> "Ultimate Magic Energy Absorber";
            case GTValues.UEV -> "Eternal Magic Energy Absorber";
            case GTValues.UIV -> "Paradoxical Magic Energy Absorber";
            default -> "Magic Energy Absorber";
        };
    }

    private static String compressedMagicEnergyName(int tier) {
        if (tier == GTValues.LV) {
            return "Compressed Low Voltage Primitive Magic Energy Absorber";
        }
        return "Compressed " + magicEnergyName(tier);
    }

    /** 一种蒸汽加工类型对应一组独立的模块方块。 */
    private record SteamModuleType(String id, String enName, String zhName, GTRecipeType recipeType) {}

    private static final List<SteamModuleType> STEAM_MODULE_TYPES = List.of(
            new SteamModuleType("circuit_assembly", "Circuit Assembler", "电路组装机",
                    GTRecipeTypes.CIRCUIT_ASSEMBLER_RECIPES),
            new SteamModuleType("forge_hammer", "Forge Hammer", "锻造锤", GTRecipeTypes.FORGE_HAMMER_RECIPES),
            new SteamModuleType("compressor", "Compressor", "压缩机", GTRecipeTypes.COMPRESSOR_RECIPES),
            new SteamModuleType("macerator", "Macerator", "研磨机", GTRecipeTypes.MACERATOR_RECIPES),
            new SteamModuleType("alloy_smelter", "Alloy Smelter", "合金冶炼", GTRecipeTypes.ALLOY_SMELTER_RECIPES),
            new SteamModuleType("mixer", "Mixer", "搅拌机", GTRecipeTypes.MIXER_RECIPES),
            new SteamModuleType("centrifuge", "Centrifuge", "离心机", GTRecipeTypes.CENTRIFUGE_RECIPES),
            new SteamModuleType("thermal_centrifuge", "Thermal Centrifuge", "热力离心机",
                    GTRecipeTypes.THERMAL_CENTRIFUGE_RECIPES),
            new SteamModuleType("ore_washer", "Ore Washer", "洗矿厂", GTRecipeTypes.ORE_WASHER_RECIPES),
            new SteamModuleType("chemical_bath", "Chemical Bath", "化学浸洗机", GTRecipeTypes.CHEMICAL_BATH_RECIPES),
            new SteamModuleType("extractor", "Extractor", "提取机", GTRecipeTypes.EXTRACTOR_RECIPES),
            new SteamModuleType("furnace", "Electric Furnace", "电炉", GTRecipeTypes.FURNACE_RECIPES),
            new SteamModuleType("rock_breaker", "Rock Breaker", "碎岩机", GTRecipeTypes.ROCK_BREAKER_RECIPES),
            new SteamModuleType("arc_furnace", "Arc Furnace", "电弧炉", GTRecipeTypes.ARC_FURNACE_RECIPES),
            new SteamModuleType("assembler", "Assembler", "组装机", GTRecipeTypes.ASSEMBLER_RECIPES),
            new SteamModuleType("autoclave", "Autoclave", "高压釜", GTRecipeTypes.AUTOCLAVE_RECIPES),
            new SteamModuleType("bender", "Bender", "卷板机", GTRecipeTypes.BENDER_RECIPES),
            new SteamModuleType("brewery", "Brewery", "酿造室", GTRecipeTypes.BREWING_RECIPES),
            new SteamModuleType("canner", "Canner", "装罐机", GTRecipeTypes.CANNER_RECIPES),
            new SteamModuleType("chemical_reactor", "Chemical Reactor", "化学反应釜", GTRecipeTypes.CHEMICAL_RECIPES),
            new SteamModuleType("cutter", "Cutter", "切割机", GTRecipeTypes.CUTTER_RECIPES),
            new SteamModuleType("distillery", "Distillery", "蒸馏室", GTRecipeTypes.DISTILLERY_RECIPES),
            new SteamModuleType("electrolyzer", "Electrolyzer", "电解机", GTRecipeTypes.ELECTROLYZER_RECIPES),
            new SteamModuleType("extruder", "Extruder", "压模器", GTRecipeTypes.EXTRUDER_RECIPES),
            new SteamModuleType("fermenter", "Fermenter", "发酵槽", GTRecipeTypes.FERMENTING_RECIPES),
            new SteamModuleType("fluid_heater", "Fluid Heater", "流体加热器", GTRecipeTypes.FLUID_HEATER_RECIPES),
            new SteamModuleType("fluid_solidifier", "Fluid Solidifier", "流体固化器", GTRecipeTypes.FLUID_SOLIDFICATION_RECIPES),
            new SteamModuleType("forming_press", "Forming Press", "冲压机床", GTRecipeTypes.FORMING_PRESS_RECIPES),
            new SteamModuleType("lathe", "Lathe", "车床", GTRecipeTypes.LATHE_RECIPES),
            new SteamModuleType("polarizer", "Polarizer", "两极磁化机", GTRecipeTypes.POLARIZER_RECIPES),
            new SteamModuleType("laser_engraver", "Laser Engraver", "激光蚀刻机", GTRecipeTypes.LASER_ENGRAVER_RECIPES),
            new SteamModuleType("sifter", "Sifter", "筛选机", GTRecipeTypes.SIFTER_RECIPES),
            new SteamModuleType("wiremill", "Wiremill", "线材轧机", GTRecipeTypes.WIREMILL_RECIPES),
            new SteamModuleType("gas_collector", "Gas Collector", "集气室", GTRecipeTypes.GAS_COLLECTOR_RECIPES),
            new SteamModuleType("dehydrator", "Dehydrator", "脱水机", GTLRecipeTypes.DEHYDRATOR_RECIPES),
            new SteamModuleType("lightning_processor", "Lightning Processor", "闪电处理机", GTLRecipeTypes.LIGHTNING_PROCESSOR_RECIPES));

    /** 所有已注册的蒸汽模块控制器，供梦想电梯模块槽位识别。 */
    public static final List<MultiblockMachineDefinition> STEAM_MODULES = new ArrayList<>();

    /** 按加工类型和模块等级获取已注册的模块方块。 */
    public static MultiblockMachineDefinition getSteamModule(String typeId, int tier) {
        String id = "dream_elevator_steam_" + typeId + "_module_mk" + tier;
        for (MultiblockMachineDefinition module : STEAM_MODULES) {
            if (module.getId().getPath().equals(id)) {
                return module;
            }
        }
        throw new IllegalStateException("Unknown steam module: " + id);
    }

    static {
        for (SteamModuleType type : STEAM_MODULE_TYPES) {
            for (int tier = 1; tier <= 3; tier++) {
                STEAM_MODULES.add(registerSteamProcessingModule(type, tier));
            }
        }
    }

    public static final List<MultiblockMachineDefinition> RESOURCE_MODULES = new ArrayList<>();

    static {
        for (int tier = 1; tier <= 3; tier++) {
            MultiblockMachineDefinition mining = registerStratumMiningModule(tier);
            MultiblockMachineDefinition drilling = registerStratumDrillingModule(tier);
            RESOURCE_MODULES.add(mining);
            RESOURCE_MODULES.add(drilling);
        }
        STEAM_MODULES.addAll(RESOURCE_MODULES);
    }

    public static final MachineDefinition CREATIVE_STEAM_INPUT_HATCH = REGISTRATE
            .machine("creative_steam_input_hatch", CreativeSteamInputHatchMachine::new)
            .rotationState(RotationState.ALL)
            .abilities(PartAbility.STEAM)
            .renderer(CreativeSteamInputHatchRenderer::new)
            .tooltips(Component.translatable("vividtech.machine.creative_steam_input_hatch.tooltip.0"))
            .tooltipBuilder(VIVID_ADD)
            .register();

    public static final MultiblockMachineDefinition PRESSURIZATION_MODULE = REGISTRATE
            .multiblock("dream_elevator_steam_pressurization_module", PressurizationModuleMachine::new)
            .langValue("Dream Elevator Steam Pressurization Module")
            .rotationState(RotationState.ALL)
            .allowExtendedFacing(false)
            .recipeType(VividRecipeTypes.STEAM_STRATUM_MINING_RECIPES)
            .appearanceBlock(GTBlocks.CASING_BRONZE_BRICKS)
            .tooltips(pressurizationTooltips())
            .tooltipBuilder(VIVID_ADD)
            .pattern(definition -> SteamProcessingModuleStructure.MODULE
                    .where("~", Predicates.controller(Predicates.blocks(definition.get())))
                    .where("H", blocks(GTBlocks.CASING_BRONZE_BRICKS.get())
                            .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(9))
                            .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(9))
                            .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(9))
                            .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(9)))
                    .build())
            .workableCasingRenderer(GTCEu.id("block/casings/solid/machine_casing_bronze_plated_bricks"),
                    GTCEu.id("block/multiblock/fusion_reactor"))
            .register();

    static {
        STEAM_MODULES.add(PRESSURIZATION_MODULE);
    }

    /**
     * 梦想电梯超净维护子系统：安装在电梯本体上的非模块仓室。
     * 正面沿用 GT/GTL 的 cleaning_gravity_configuration_maintenance_hatch 外观。
     */
    public static final MachineDefinition CLEANROOM_SUBSYSTEM = REGISTRATE
            .machine("dream_elevator_cleanroom_subsystem", DreamElevatorCleanroomSubsystemMachine::new)
            .langValue("Dream Elevator Cleanroom Maintenance Subsystem")
            .rotationState(RotationState.ALL)
            .renderer(() -> new SubsystemHatchRenderer(
                    GTCEu.id("block/machine/part/maintenance.cleaning")))
            .tooltips(cleanroomSubsystemTooltips())
            .tooltipBuilder(VIVID_ADD)
            .register();

    /**
     * 梦想电梯重力控制子系统：安装在电梯本体上的非模块仓室。
     * 正面沿用 GT/GTL 的 gravity_hatch 外观。
     */
    public static final MachineDefinition GRAVITY_SUBSYSTEM = REGISTRATE
            .machine("dream_elevator_gravity_subsystem", DreamElevatorGravitySubsystemMachine::new)
            .langValue("Dream Elevator Gravity Control Subsystem")
            .rotationState(RotationState.ALL)
            .renderer(() -> new SubsystemHatchRenderer(
                    GTCEu.id("block/machine/part/maintenance.full_auto")))
            .tooltips(gravitySubsystemTooltips())
            .tooltipBuilder(VIVID_ADD)
            .register();

    private static Component[] cleanroomSubsystemTooltips() {
        Component[] tooltips = new Component[5];
        for (int i = 0; i < tooltips.length; i++) {
            tooltips[i] = Component.translatable("vividtech.machine.cleanroom_subsystem.tooltip." + i);
        }
        return tooltips;
    }

    private static Component[] gravitySubsystemTooltips() {
        Component[] tooltips = new Component[4];
        for (int i = 0; i < tooltips.length; i++) {
            tooltips[i] = Component.translatable("vividtech.machine.gravity_subsystem.tooltip." + i);
        }
        return tooltips;
    }

    private static Component[] pressurizationTooltips() {
        Component[] tooltips = new Component[7];
        for (int i = 0; i < tooltips.length; i++) {
            tooltips[i] = Component.translatable("vividtech.machine.pressurization_module.tooltip." + i);
        }
        return tooltips;
    }

    private static ItemStack steamTabIcon() {
        return EYE_OF_WOOD.asStack();
    }

    private static ItemStack potentialTabIcon() {
        return BASIC_INTEGRATED_ORE_PROCESSOR.asStack();
    }

    private static Component[] dreamElevatorTooltips() {
        Component[] tooltips = new Component[20];
        for (int i = 0; i < tooltips.length; i++) {
            tooltips[i] = Component.translatable("vividtech.machine.dream_elevator.tooltip." + i);
        }
        return tooltips;
    }

    private static Component[] steamModuleTooltips(SteamModuleType type, int tier) {
        return new Component[] {
                Component.translatable("vividtech.machine.steam_processing_module.tooltip.0." + type.id()),
                Component.translatable("vividtech.machine.steam_processing_module.tooltip.1"),
                Component.translatable("vividtech.machine.steam_processing_module.tooltip.2"),
                Component.translatable("vividtech.machine.steam_processing_module.tooltip.3"),
                Component.translatable("vividtech.machine.steam_processing_module.tooltip.4"),
                Component.translatable("vividtech.machine.steam_processing_module.tooltip.5"),
                Component.translatable("vividtech.machine.steam_processing_module.tooltip.6"),
                Component.translatable("vividtech.machine.steam_processing_module.tooltip.7.mk" + tier)
        };
    }

    private static Component[] stratumMiningTooltips(int tier) {
        return new Component[] {
                Component.translatable("vividtech.machine.stratum_mining.tooltip.0"),
                Component.translatable("vividtech.machine.stratum_mining.tooltip.1"),
                Component.translatable("vividtech.machine.stratum_mining.tooltip.2.mk" + tier),
                Component.translatable("vividtech.machine.stratum_mining.tooltip.3.mk" + tier),
                Component.translatable("vividtech.machine.stratum_mining.tooltip.4"),
                Component.translatable("vividtech.machine.stratum_mining.tooltip.5"),
                Component.translatable("vividtech.machine.stratum_mining.tooltip.6"),
                Component.translatable("vividtech.machine.stratum_mining.tooltip.7")
        };
    }

    private static Component[] stratumDrillingTooltips(int tier) {
        return new Component[] {
                Component.translatable("vividtech.machine.stratum_drilling.tooltip.0"),
                Component.translatable("vividtech.machine.stratum_drilling.tooltip.1"),
                Component.translatable("vividtech.machine.stratum_drilling.tooltip.2.mk" + tier),
                Component.translatable("vividtech.machine.stratum_drilling.tooltip.3"),
                Component.translatable("vividtech.machine.stratum_drilling.tooltip.4"),
                Component.translatable("vividtech.machine.stratum_drilling.tooltip.5"),
                Component.translatable("vividtech.machine.stratum_drilling.tooltip.6"),
                Component.translatable("vividtech.machine.stratum_drilling.tooltip.7")
        };
    }

    private static MultiblockMachineDefinition registerStratumMiningModule(int tier) {
        String id = "dream_elevator_steam_stratum_mining_module_mk" + tier;
        return REGISTRATE.multiblock(id, holder -> new SteamStratumMiningModuleMachine(holder, tier))
                .langValue("Dream Elevator Steam Stratum Mining Module MK-" + VividTech.toRoman(tier))
                .rotationState(RotationState.ALL)
                .allowExtendedFacing(false)
                .recipeType(VividRecipeTypes.STEAM_STRATUM_MINING_RECIPES)
                .appearanceBlock(GTBlocks.CASING_BRONZE_BRICKS)
                .tooltips(stratumMiningTooltips(tier))
                .tooltipBuilder(VIVID_ADD)
                .pattern(definition -> SteamProcessingModuleStructure.MODULE
                        .where("~", Predicates.controller(Predicates.blocks(definition.get())))
                        .where("H", blocks(GTBlocks.CASING_BRONZE_BRICKS.get())
                                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(9))
                                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(9))
                                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(9))
                                .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(9)))
                        .build())
                .workableCasingRenderer(GTCEu.id("block/casings/solid/machine_casing_bronze_plated_bricks"),
                        GTCEu.id("block/multiblock/fusion_reactor"))
                .register();
    }

    private static MultiblockMachineDefinition registerStratumDrillingModule(int tier) {
        String id = "dream_elevator_steam_stratum_drilling_module_mk" + tier;
        return REGISTRATE.multiblock(id, holder -> new SteamStratumDrillingModuleMachine(holder, tier))
                .langValue("Dream Elevator Steam Stratum Drilling Module MK-" + VividTech.toRoman(tier))
                .rotationState(RotationState.ALL)
                .allowExtendedFacing(false)
                .recipeType(VividRecipeTypes.STEAM_STRATUM_DRILLING_RECIPES)
                .appearanceBlock(GTBlocks.CASING_BRONZE_BRICKS)
                .tooltips(stratumDrillingTooltips(tier))
                .tooltipBuilder(VIVID_ADD)
                .pattern(definition -> SteamProcessingModuleStructure.MODULE
                        .where("~", Predicates.controller(Predicates.blocks(definition.get())))
                        .where("H", blocks(GTBlocks.CASING_BRONZE_BRICKS.get())
                                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(9))
                                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(9))
                                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(9))
                                .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(9)))
                        .build())
                .workableCasingRenderer(GTCEu.id("block/casings/solid/machine_casing_bronze_plated_bricks"),
                        GTCEu.id("block/multiblock/fusion_reactor"))
                .register();
    }

    private static Component[] eyeOfWoodTooltips() {
        Component[] tooltips = new Component[19];
        for (int i = 0; i < tooltips.length; i++) {
            tooltips[i] = Component.translatable("vividtech.machine.eye_of_wood.tooltip." + i);
        }
        return tooltips;
    }

    private static TraceabilityPredicate dreamSteamCasing() {
        // PartAbility.STEAM 的 getAllBlocks() 是 memoize 的，GTL 的大型蒸汽输入仓注册较晚，
        // 可能不在缓存里，因此这里显式补上它的方块。
        Block[] steamParts = Stream.concat(
                PartAbility.STEAM.getAllBlocks().stream(),
                Stream.of(GTLMachines.LARGE_STEAM_HATCH.getBlock(),
                        CREATIVE_STEAM_INPUT_HATCH.getBlock()))
                .distinct()
                .toArray(Block[]::new);
        return blocks(GTBlocks.CASING_BRONZE_BRICKS.get())
                // 优先用能力匹配，保证 GTL 稍后注册的大型蒸汽输入仓也能被识别。
                .or(Predicates.abilities(PartAbility.STEAM).setMaxGlobalLimited(4))
                .or(Predicates.blocks(steamParts).setMaxGlobalLimited(4))
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(4))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(4))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(4))
                .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(4))
                // 两个非模块子系统仓室：超净维护 / 重力控制。
                .or(Predicates.blocks(CLEANROOM_SUBSYSTEM.getBlock(), GRAVITY_SUBSYSTEM.getBlock())
                        .setMaxGlobalLimited(2));
    }

    private static TraceabilityPredicate dreamModuleShell() {
        List<Block> allowed = new ArrayList<>();
        allowed.add(GTBlocks.CASING_BRONZE_BRICKS.get());
        allowed.addAll(PartAbility.IMPORT_ITEMS.getAllBlocks());
        allowed.addAll(PartAbility.EXPORT_ITEMS.getAllBlocks());
        allowed.addAll(PartAbility.IMPORT_FLUIDS.getAllBlocks());
        allowed.addAll(PartAbility.EXPORT_FLUIDS.getAllBlocks());
        // PartAbility 的方块列表可能被缓存；显式补上锻压输入总线与熔铸输入仓。
        for (MachineDefinition[] definitions : new MachineDefinition[][] { FORGING_INPUT_BUS, CASTING_INPUT_HATCH }) {
            for (MachineDefinition definition : definitions) {
                if (definition != null) {
                    allowed.add(definition.getBlock());
                }
            }
        }
        Block[] allowedArray = allowed.stream().distinct().toArray(Block[]::new);
        BlockInfo[] candidates = Stream.of(allowedArray)
                .map(block -> BlockInfo.fromBlockState(block.defaultBlockState()))
                .toArray(BlockInfo[]::new);
        return new TraceabilityPredicate(state -> {
            Block block = state.getBlockState().getBlock();
            for (Block candidate : allowedArray) {
                if (block == candidate) {
                    state.getMatchContext().getOrCreate(DreamElevatorMachine.MODULE_SHELL_KEY,
                            LongOpenHashSet::new).add(state.getPos().asLong());
                    return true;
                }
            }
            return false;
        }, () -> candidates);
    }

    private static TraceabilityPredicate dreamModuleSlot() {
        List<Block> allowed = new ArrayList<>();
        allowed.add(GTBlocks.CASING_BRONZE_BRICKS.get());
        for (MultiblockMachineDefinition module : STEAM_MODULES) {
            allowed.add(module.getBlock());
        }
        Block[] allowedArray = allowed.stream().distinct().toArray(Block[]::new);
        BlockInfo[] candidates = allowed.stream().distinct()
                .map(block -> BlockInfo.fromBlockState(block.defaultBlockState()))
                .toArray(BlockInfo[]::new);
        return new TraceabilityPredicate(state -> {
            Block block = state.getBlockState().getBlock();
            for (Block candidate : allowedArray) {
                if (block == candidate) {
                    state.getMatchContext().getOrCreate(DreamElevatorMachine.MODULE_SLOT_KEY,
                            LongOpenHashSet::new).add(state.getPos().asLong());
                    return true;
                }
            }
            return false;
        }, () -> candidates);
    }

    private static MultiblockMachineDefinition registerSteamProcessingModule(SteamModuleType type, int tier) {
        String id = "dream_elevator_steam_" + type.id() + "_module_mk" + tier;
        String langValue = "Dream Elevator Steam " + type.enName() + " Module MK-" + VividTech.toRoman(tier);
        return REGISTRATE.multiblock(id, holder -> new SteamProcessingModuleMachine(holder, tier))
                .langValue(langValue)
                .rotationState(RotationState.ALL)
                .allowExtendedFacing(false)
                .recipeTypes(type.recipeType())
                .appearanceBlock(GTBlocks.CASING_BRONZE_BRICKS)
                .tooltips(steamModuleTooltips(type, tier))
                .tooltipBuilder(VIVID_ADD)
                .pattern(definition -> SteamProcessingModuleStructure.MODULE
                        .where("~", Predicates.controller(Predicates.blocks(definition.get())))
                        .where("H", blocks(GTBlocks.CASING_BRONZE_BRICKS.get())
                                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(9))
                                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(9))
                                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(9))
                                .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(9)))
                        .build())
                .recipeModifier(SteamProcessingModuleMachine::recipeModifier)
                .workableCasingRenderer(GTCEu.id("block/casings/solid/machine_casing_bronze_plated_bricks"),
                        GTCEu.id("block/multiblock/fusion_reactor"))
                .register();
    }

    public final static MultiblockMachineDefinition EYE_OF_WOOD = REGISTRATE
            .multiblock("eye_of_wood", EyeOfWoodMachine::new)
            .langValue("Eye of Wood")
            .rotationState(RotationState.NON_Y_AXIS)
            .allowExtendedFacing(false)
            .recipeType(VividRecipeTypes.EYE_OF_WOOD_RECIPES)
            .appearanceBlock(GTBlocks.CASING_BRONZE_BRICKS)
            .tooltips(eyeOfWoodTooltips())
            .tooltips(Component.translatable("tooltip.gtlcore.structure.source", "Twist Space Technology"))
            .tooltipBuilder(VIVID_ADD)
            .pattern(definition -> EyeOfWoodStructure.EYE_OF_HARMONY
                    .where('~', Predicates.controller(Predicates.blocks(definition.get())))
                    .where('A', Predicates.blocks(Blocks.BRICKS))
                    .where('B', Predicates.blocks(GTBlocks.CASING_BRONZE_BRICKS.get())
                            .or(Predicates.abilities(PartAbility.IMPORT_ITEMS))
                            .or(Predicates.abilities(PartAbility.EXPORT_ITEMS))
                            .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS))
                            .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS)))
                    .where('D', Predicates.blockTag(BlockTags.PLANKS))
                    .where('E', Predicates.blocks(Blocks.LAPIS_BLOCK))
                    .where('F', Predicates.blocks(Blocks.BOOKSHELF))
                    .where('G', Predicates.blocks(Blocks.CHISELED_STONE_BRICKS))
                    .where(" ", Predicates.any())
                    .build())
            .workableCasingRenderer(GTCEu.id("block/casings/solid/machine_casing_bronze_plated_bricks"),
                    GTCEu.id("block/multiblock/fusion_reactor"))
            .register();

    static {
        REGISTRATE.creativeModeTab(() -> POTENTIAL_TAB);
    }

    public final static MultiblockMachineDefinition BASIC_INTEGRATED_ORE_PROCESSOR = REGISTRATE
            .multiblock("basic_integrated_ore_processor", BasicIntegratedOreProcessorMachine::new)
            .langValue("Basic Integrated Ore Processing Plant")
            .rotationState(RotationState.NON_Y_AXIS)
            .allowExtendedFacing(false)
            .recipeType(GTLRecipeTypes.INTEGRATED_ORE_PROCESSOR)
            .recipeModifier(BasicIntegratedOreProcessorMachine::modifyRecipe)
            .appearanceBlock(GTBlocks.CASING_STEEL_SOLID)
            .tooltips(Component.translatable("gtceu.machine.integrated_ore_processor.tooltip.0"))
            .tooltips(Component.translatable("gtceu.machine.integrated_ore_processor.tooltip.1"))
            .tooltips(Component.translatable("gtceu.machine.integrated_ore_processor.tooltip.2"))
            .tooltips(Component.translatable("gtceu.machine.integrated_ore_processor.tooltip.3"))
            .tooltips(Component.translatable("gtceu.machine.integrated_ore_processor.tooltip.4"))
            .tooltips(Component.translatable("gtceu.machine.integrated_ore_processor.tooltip.5"))
            .tooltips(Component.translatable("gtceu.machine.integrated_ore_processor.tooltip.6"))
            .tooltips(Component.translatable("gtceu.machine.integrated_ore_processor.tooltip.7"))
            .tooltips(Component.translatable("vividtech.machine.basic_integrated_ore_processor.tooltip.0"))
            .tooltips(Component.translatable("vividtech.machine.basic_integrated_ore_processor.tooltip.1"))
            .tooltipBuilder(VIVID_ADD)
            .pattern(definition -> MultiBlockMachineAStructureA.INTEGRATED_ORE_PROCESSOR
                    .where("~", Predicates.controller(Predicates.blocks(definition.get())))
                    .where("a", Predicates.blocks(GTBlocks.CASING_INVAR_HEATPROOF.get()))
                    .where("c", Predicates.blocks(GTBlocks.CASING_STEEL_SOLID.get())
                            .setMinGlobalLimited(60)
                            .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                            .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1)))
                    .where("b", Predicates.blocks(GTBlocks.CASING_TEMPERED_GLASS.get()))
                    .where("d", Predicates.blocks(ChemicalHelper.getBlock(TagPrefix.frameGt, GTMaterials.Steel)))
                    .where("e", Predicates.blocks(GTBlocks.CASING_STEEL_GEARBOX.get()))
                    .where("f", Predicates.blocks(GTBlocks.CASING_STEEL_PIPE.get()))
                    .where("g", Predicates.abilities(PartAbility.MUFFLER))
                    .where(" ", Predicates.any())
                    .build())
            .workableCasingRenderer(GTCEu.id("block/casings/solid/machine_casing_solid_steel"),
                    GTCEu.id("block/multiblock/gcym/large_maceration_tower"))
            .register();

    /**
     * 晶体重铸工厂：单晶硅与宝石粉的高温重铸设备。
     */
    public static final MultiblockMachineDefinition CRYSTAL_AGGREGATION_FACTORY = REGISTRATE
            .multiblock("crystal_aggregation_factory", CrystalAggregationFactoryMachine::new)
            .langValue("Crystal Recasting Factory")
            .rotationState(RotationState.ALL)
            .allowExtendedFacing(false)
            .recipeType(VividRecipeTypes.CRYSTAL_AGGREGATION_RECIPES)
            .recipeModifier(CrystalAggregationFactoryMachine::modifyRecipe)
            .appearanceBlock(GTBlocks.CASING_ALUMINIUM_FROSTPROOF)
            .tooltips(
                    Component.translatable("vividtech.machine.crystal_aggregation_factory.tooltip.0"),
                    Component.translatable("vividtech.machine.crystal_aggregation_factory.tooltip.1"),
                    Component.translatable("vividtech.machine.crystal_aggregation_factory.tooltip.2"),
                    Component.translatable("vividtech.machine.crystal_aggregation_factory.tooltip.3"),
                    Component.translatable("vividtech.machine.crystal_aggregation_factory.tooltip.4"),
                    Component.translatable("vividtech.machine.crystal_aggregation_factory.tooltip.5"))
            .tooltipBuilder(VIVID_ADD)
            .pattern(definition -> FactoryBlockPattern.start(RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.RIGHT)
                    .aisle("         ", "  AAAAA  ", " AABBBAA ", " ABBBBBA ", " ABBBBBA ", " ABBBBBA ", " AABBBAA ", "  AAAAA  ", "         ")
                    .aisle("  AAAAA  ", " ACDDDCA ", "AC     CA", "AD     DA", "AD     DA", "AD     DA", "AC     CA", " ACDDDCA ", "  AAAAA  ")
                    .aisle(" AABBBAA ", "AC     CA", "A C   C A", "B       B", "B       B", "B       B", "A C   C A", "AC     CA", " AABBBAA ")
                    .aisle(" ABBBBBA ", "AD     DA", "B       B", "B  CBC  B", "B  BBB  B", "B  CBC  B", "B       B", "AD     DA", " ABBBBBA ")
                    .aisle(" ABBBBBA ", "AD     DE", "B       B", "B  BBB  B", "B  BBB  B", "B  BBB  B", "B       B", "AD     DA", " ABBBBBA ")
                    .aisle(" ABBBBBA ", "AD     DA", "B       B", "B  CBC  B", "B  BBB  B", "B  CBC  B", "B       B", "AD     DA", " ABBBBBA ")
                    .aisle(" AABBBAA ", "AC     CA", "A C   C A", "B       B", "B       B", "B       B", "A C   C A", "AC     CA", " AABBBAA ")
                    .aisle("  AAAAA  ", " ACDDDCA ", "AC     CA", "AD     DA", "AD     DA", "AD     DA", "AC     CA", " ACDDDCA ", "  AAAAA  ")
                    .aisle("         ", "  AAAAA  ", " AABBBAA ", " ABBBBBA ", " ABBBBBA ", " ABBBBBA ", " AABBBAA ", "  AAAAA  ", "         ")
                    .where("E", Predicates.controller(Predicates.blocks(definition.get())))
                    .where("D", Predicates.heatingCoils())
                    .where("C", Predicates.blocks(ChemicalHelper.getBlock(TagPrefix.frameGt, GTMaterials.Aluminium)))
                    .where("B", Predicates.blocks(GTBlocks.CASING_TEMPERED_GLASS.get()))
                    .where("A", crystalAggregationFactoryCasing())
                    .build())
            .workableCasingRenderer(GTCEu.id("block/casings/solid/machine_casing_frost_proof"),
                    GTCEu.id("block/multiblock/fusion_reactor"))
            .register();

    /**
     * 大气捕集单元。
     *
     * <p>结构来自导出图，控制器位于最后一层底部中央；在默认朝向（主机面朝西）下，
     * 结构向控制器后方展开。主机与方块的渲染沿用晶体重铸工厂。</p>
     */
    public static final MultiblockMachineDefinition ATMOSPHERIC_COLLECTION_UNIT = REGISTRATE
            .multiblock("atmospheric_collection_unit", AtmosphericCollectionUnitMachine::new)
            .langValue("Atmospheric Collection Unit")
            .tier(GTValues.MV)
            .rotationState(RotationState.ALL)
            .allowExtendedFacing(false)
            .recipeType(VividRecipeTypes.ATMOSPHERIC_COLLECTION_RECIPES)
            .recipeModifier(AtmosphericCollectionUnitMachine::modifyRecipe)
            .appearanceBlock(GTBlocks.CASING_ALUMINIUM_FROSTPROOF)
            .tooltips(atmosphericCollectionTooltips())
            .tooltipBuilder(VIVID_ADD)
            .pattern(definition -> FactoryBlockPattern.start()
                    .aisle("  AAA  ", "  AAA  ", "  AAA  ", "  AAA  ", "  AAA  ", "  AAA  ", "  AAA  ", "  AAA  ", "  AAA  ")
                    .aisle(" AAAAA ", " ACCCA ", " A   A ", " A D A ", " A   A ", " A D A ", " A   A ", " A D A ", " AEEEA ")
                    .aisle("AAAAAAA", "ACFFFCA", "A CCC A", "A  D  A", "A     A", "A  D  A", "A     A", "A  D  A", "AEEEEEA")
                    .aisle("AAAAAAA", "ACFFFCA", "A CFC A", "ADDGDDA", "A  G  A", "ADDGDDA", "A  G  A", "ADDGDDA", "AEEEEEA")
                    .aisle("AAAAAAA", "ACFFFCA", "A CCC A", "A  D  A", "A     A", "A  D  A", "A     A", "A  D  A", "AEEEEEA")
                    .aisle(" AAAAA ", " ACCCA ", " A   A ", " A D A ", " A   A ", " A D A ", " A   A ", " A D A ", " AEEEA ")
                    .aisle("  ABA  ", "  AAA  ", "  AAA  ", "  AAA  ", "  AAA  ", "  AAA  ", "  AAA  ", "  AAA  ", "  AAA  ")
                    .where("B", Predicates.controller(Predicates.blocks(definition.get())))
                    .where("A", atmosphericCollectionCasing())
                    .where("C", Predicates.blocks(GTBlocks.CASING_STAINLESS_CLEAN.get()))
                    .where("D", Predicates.blocks(ChemicalHelper.getBlock(TagPrefix.frameGt, GTMaterials.StainlessSteel)))
                    .where("E", Predicates.blocks(GTBlocks.FILTER_CASING.get()))
                    .where("F", Predicates.blocks(GTBlocks.CASING_STEEL_PIPE.get()))
                    .where("G", Predicates.blocks(GTBlocks.CASING_STAINLESS_STEEL_GEARBOX.get()))
                    .build())
            .workableCasingRenderer(GTCEu.id("block/casings/solid/machine_casing_frost_proof"),
                    GTCEu.id("block/multiblock/fusion_reactor"))
            .register();

    /**
     * A 位：防冻机械方块，或能源/物品/流体/维护/并行仓（无消声仓）。
     */
    private static TraceabilityPredicate atmosphericCollectionCasing() {
        return Predicates.blocks(GTBlocks.CASING_ALUMINIUM_FROSTPROOF.get())
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY)
                        .setMaxGlobalLimited(8).setPreviewCount(1))
                .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(8).setPreviewCount(1))
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setPreviewCount(1))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setPreviewCount(1))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setPreviewCount(1))
                .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setPreviewCount(1))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1));
    }

    private static Component[] atmosphericCollectionTooltips() {
        Component[] tooltips = new Component[3];
        for (int i = 0; i < tooltips.length; i++) {
            tooltips[i] = Component.translatable("vividtech.machine.atmospheric_collection_unit.tooltip." + i);
        }
        return tooltips;
    }

    /**
     * A 位：防冻机械方块，或能源/物品/流体/维护/ME 仓室。
     */
    private static TraceabilityPredicate crystalAggregationFactoryCasing() {
        return Predicates.blocks(GTBlocks.CASING_ALUMINIUM_FROSTPROOF.get())
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY)
                        .setMinGlobalLimited(1).setMaxGlobalLimited(8).setPreviewCount(1))
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setPreviewCount(1))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setPreviewCount(1))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setPreviewCount(1))
                .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setPreviewCount(1))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1));
    }

    public static final MachineDefinition[] COMPRESSED_PRIMITIVE_MAGIC_ENERGY = GTMachines.registerTieredMachines(
            "compressed_primitive_magic_energy",
            CompressedMagicEnergyMachine::new,
            (tier, builder) -> builder
                    .langValue(compressedMagicEnergyName(tier))
                    .rotationState(RotationState.NON_Y_AXIS)
                    .renderer(() -> new SimpleGeneratorMachineRenderer(tier,
                            GTCEu.id("block/generators/primitive_magic_energy")))
                    .tooltips(Component.translatable("gtceu.machine.primitive_magic_energy.tooltip.0"))
                    .tooltips(Component.translatable("gtceu.machine.primitive_magic_energy.tooltip.1"))
                    .tooltips(Component.translatable("gtceu.universal.tooltip.ampere_out", 256))
                    .tooltips(Component.translatable("gtceu.universal.tooltip.voltage_out",
                            FormattingUtil.formatNumbers(GTValues.V[tier]), GTValues.VNF[tier]))
                    .tooltips(Component.translatable("gtceu.universal.tooltip.energy_storage_capacity",
                            FormattingUtil.formatNumbers(GTValues.V[tier] * 512L * 16L)))
                    .tooltipBuilder(VIVID_ADD)
                    .register(),
            GTValues.LV, GTValues.MV, GTValues.HV, GTValues.EV, GTValues.IV, GTValues.LuV,
            GTValues.ZPM, GTValues.UV, GTValues.UHV, GTValues.UEV, GTValues.UIV);

    static {
        REGISTRATE.creativeModeTab(() -> STEAM_TAB);
    }

    public final static MultiblockMachineDefinition DREAM_ELEVATOR = REGISTRATE
            .multiblock("dream_elevator", DreamElevatorMachine::new)
            .langValue("Dream Elevator")
            .rotationState(RotationState.ALL)
            .recipeType(VividRecipeTypes.DREAM_ELEVATOR_RECIPES)
            .appearanceBlock(GTBlocks.CASING_BRONZE_BRICKS)
            .tooltips(dreamElevatorTooltips())
            .tooltipBuilder(VIVID_ADD)
            .pattern(definition -> DreamElevatorStructure.DREAM_ELEVATOR
                    .where("~", Predicates.controller(Predicates.blocks(definition.get())))
                    .where("A", Predicates.blocks(Blocks.STONE_BRICKS))
                    .where("B", Predicates.blocks(Blocks.BRICKS))
                    .where("P", Predicates.blocks(Blocks.POLISHED_BASALT))
                    .where("W", Predicates.blocks(
                            ChemicalHelper.getBlock(TagPrefix.frameGt, GTMaterials.TreatedWood)))
                    .where("R", Predicates.blocks(Blocks.REDSTONE_BLOCK))
                    .where("C", GTLPredicates.tierCasings(DreamElevatorMachine.RAIL_TIER_BLOCKS,
                            DreamElevatorMachine.RAIL_TIER_KEY))
                    .where("S", dreamSteamCasing())
                    .where("X", dreamSteamCasing())
                    .where("G", dreamModuleShell())
                    .where("I", dreamModuleSlot())
                    .where("-", Predicates.air())
                    .where(" ", Predicates.any())
                    .build())
            .renderer(DreamElevatorRenderer::new)
            .hasTESR(true)
            .register();

    static {
        // 恢复 GTCEu 的默认标签页，避免影响之后加载的其他附属模组。
        REGISTRATE.creativeModeTab(() -> GTCreativeModeTabs.MACHINE);
    }
}
