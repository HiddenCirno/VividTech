package com.vividtech.common.data;

import com.vividtech.VividTech;
import com.vividtech.client.renderer.machine.CreativeSteamInputHatchRenderer;
import com.vividtech.client.renderer.machine.DreamElevatorRenderer;
import com.vividtech.common.data.machines.structure.DreamElevatorStructure;
import com.vividtech.common.data.machines.structure.EyeOfWoodStructure;
import com.vividtech.common.data.machines.structure.SteamProcessingModuleStructure;
import com.vividtech.common.machine.multiblock.noenergy.EyeOfWoodMachine;
import com.vividtech.common.machine.multiblock.part.CreativeSteamInputHatchMachine;
import com.vividtech.common.machine.multiblock.steam.DreamElevatorMachine;
import com.vividtech.common.machine.multiblock.steam.SteamProcessingModuleMachine;

import org.gtlcore.gtlcore.api.pattern.GTLPredicates;
import org.gtlcore.gtlcore.common.data.GTLMachines;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.common.data.GTCreativeModeTabs;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;

import com.lowdragmc.lowdraglib.utils.BlockInfo;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.ChatFormatting;
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

    /** 一种蒸汽加工类型对应一组独立的模块方块。 */
    private record SteamModuleType(String id, String enName, String zhName, GTRecipeType recipeType) {}

    private static final List<SteamModuleType> STEAM_MODULE_TYPES = List.of(
            new SteamModuleType("circuit_assembly", "Circuit Assembly", "电路组装",
                    GTRecipeTypes.CIRCUIT_ASSEMBLER_RECIPES),
            new SteamModuleType("forge_hammer", "Forge Hammer", "锻造", GTRecipeTypes.FORGE_HAMMER_RECIPES),
            new SteamModuleType("compressor", "Compressor", "压缩", GTRecipeTypes.COMPRESSOR_RECIPES),
            new SteamModuleType("macerator", "Macerator", "研磨", GTRecipeTypes.MACERATOR_RECIPES),
            new SteamModuleType("alloy_smelter", "Alloy Smelter", "合金冶炼", GTRecipeTypes.ALLOY_SMELTER_RECIPES),
            new SteamModuleType("mixer", "Mixer", "混合", GTRecipeTypes.MIXER_RECIPES),
            new SteamModuleType("centrifuge", "Centrifuge", "离心", GTRecipeTypes.CENTRIFUGE_RECIPES),
            new SteamModuleType("thermal_centrifuge", "Thermal Centrifuge", "热力离心",
                    GTRecipeTypes.THERMAL_CENTRIFUGE_RECIPES),
            new SteamModuleType("ore_washer", "Ore Washer", "洗矿", GTRecipeTypes.ORE_WASHER_RECIPES),
            new SteamModuleType("chemical_bath", "Chemical Bath", "化学浸洗", GTRecipeTypes.CHEMICAL_BATH_RECIPES),
            new SteamModuleType("extractor", "Extractor", "提取", GTRecipeTypes.EXTRACTOR_RECIPES),
            new SteamModuleType("furnace", "Furnace", "熔炼", GTRecipeTypes.FURNACE_RECIPES),
            new SteamModuleType("rock_breaker", "Rock Breaker", "碎岩", GTRecipeTypes.ROCK_BREAKER_RECIPES));

    /** 所有已注册的蒸汽模块控制器，供梦想电梯模块槽位识别。 */
    public static final List<MultiblockMachineDefinition> STEAM_MODULES = new ArrayList<>();

    static {
        for (SteamModuleType type : STEAM_MODULE_TYPES) {
            for (int tier = 1; tier <= 3; tier++) {
                STEAM_MODULES.add(registerSteamProcessingModule(type, tier));
            }
        }
    }

    public static final MachineDefinition CREATIVE_STEAM_INPUT_HATCH = REGISTRATE
            .machine("creative_steam_input_hatch", CreativeSteamInputHatchMachine::new)
            .rotationState(RotationState.ALL)
            .abilities(PartAbility.STEAM)
            .renderer(CreativeSteamInputHatchRenderer::new)
            .tooltips(Component.translatable("vividtech.machine.creative_steam_input_hatch.tooltip.0"))
            .tooltipBuilder(VIVID_ADD)
            .register();

    private static ItemStack steamTabIcon() {
        return EYE_OF_WOOD.asStack();
    }

    private static Component[] dreamElevatorTooltips() {
        Component[] tooltips = new Component[18];
        for (int i = 0; i < tooltips.length; i++) {
            tooltips[i] = Component.translatable("vividtech.machine.dream_elevator.tooltip." + i);
        }
        return tooltips;
    }

    private static Component[] steamModuleTooltips(SteamModuleType type, int tier) {
        Component function = Component.translatable(
                "vividtech.machine.steam_processing_module.function." + type.id())
                .withStyle(ChatFormatting.YELLOW);
        return new Component[] {
                Component.translatable("vividtech.machine.steam_processing_module.tooltip.0", function),
                Component.translatable("vividtech.machine.steam_processing_module.tooltip.1"),
                Component.translatable("vividtech.machine.steam_processing_module.tooltip.2"),
                Component.translatable("vividtech.machine.steam_processing_module.tooltip.3"),
                Component.translatable("vividtech.machine.steam_processing_module.tooltip.4",
                        Component.literal(Integer.toString(SteamProcessingModuleMachine.getParallelForTier(tier)))
                                .withStyle(ChatFormatting.YELLOW))
        };
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
                .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(4));
    }

    private static TraceabilityPredicate dreamModuleShell() {
        List<Block> allowed = new ArrayList<>();
        allowed.add(GTBlocks.CASING_BRONZE_BRICKS.get());
        allowed.addAll(PartAbility.IMPORT_ITEMS.getAllBlocks());
        allowed.addAll(PartAbility.EXPORT_ITEMS.getAllBlocks());
        allowed.addAll(PartAbility.IMPORT_FLUIDS.getAllBlocks());
        allowed.addAll(PartAbility.EXPORT_FLUIDS.getAllBlocks());
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
        String langValue = "Dream Elevator Steam " + type.enName() + " Module MK " + tier;
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
                                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2))
                                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2))
                                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(2))
                                .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(2)))
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
