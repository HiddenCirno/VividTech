package com.vividtech.common.data;

import com.vividtech.VividTech;
import com.vividtech.client.renderer.machine.DreamElevatorRenderer;
import com.vividtech.common.data.machines.structure.DreamElevatorStructure;
import com.vividtech.common.data.machines.structure.EyeOfWoodStructure;
import com.vividtech.common.data.machines.structure.SteamIndustrialProcessingPlantStructure;
import com.vividtech.common.machine.multiblock.noenergy.EyeOfWoodMachine;
import com.vividtech.common.machine.multiblock.steam.DreamElevatorMachine;
import com.vividtech.common.machine.multiblock.steam.SteamIndustrialProcessingPlantMachine;

import org.gtlcore.gtlcore.api.pattern.GTLPredicates;
import org.gtlcore.gtlcore.common.data.GTLMachines;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.common.data.GTCreativeModeTabs;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import com.lowdragmc.lowdraglib.utils.BlockInfo;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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

    public static final BiConsumer<ItemStack, List<Component>> VIVID_ADD = (stack, components) -> components
            .add(Component.translatable("vividtech.registry.add").withStyle(ChatFormatting.AQUA));

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

    private static final GTRecipeType[] STEAM_PROCESSING_RECIPES = {
            com.gregtechceu.gtceu.common.data.GTRecipeTypes.FORGE_HAMMER_RECIPES,
            com.gregtechceu.gtceu.common.data.GTRecipeTypes.COMPRESSOR_RECIPES,
            com.gregtechceu.gtceu.common.data.GTRecipeTypes.MACERATOR_RECIPES,
            com.gregtechceu.gtceu.common.data.GTRecipeTypes.ALLOY_SMELTER_RECIPES,
            com.gregtechceu.gtceu.common.data.GTRecipeTypes.MIXER_RECIPES,
            com.gregtechceu.gtceu.common.data.GTRecipeTypes.CENTRIFUGE_RECIPES,
            com.gregtechceu.gtceu.common.data.GTRecipeTypes.THERMAL_CENTRIFUGE_RECIPES,
            com.gregtechceu.gtceu.common.data.GTRecipeTypes.ELECTROLYZER_RECIPES,
            com.gregtechceu.gtceu.common.data.GTRecipeTypes.ORE_WASHER_RECIPES,
            com.gregtechceu.gtceu.common.data.GTRecipeTypes.CHEMICAL_BATH_RECIPES,
            com.gregtechceu.gtceu.common.data.GTRecipeTypes.EXTRACTOR_RECIPES,
            com.gregtechceu.gtceu.common.data.GTRecipeTypes.FURNACE_RECIPES,
            com.gregtechceu.gtceu.common.data.GTRecipeTypes.ROCK_BREAKER_RECIPES
    };

    private static ItemStack steamTabIcon() {
        return EYE_OF_WOOD.asStack();
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
                Stream.of(GTLMachines.LARGE_STEAM_HATCH.getBlock()))
                .distinct()
                .toArray(Block[]::new);
        return blocks(GTBlocks.CASING_BRONZE_BRICKS.get())
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
        List<Block> allowed = List.of(
                GTBlocks.CASING_BRONZE_BRICKS.get(),
                STEAM_INDUSTRIAL_PROCESSING_PLANT_T1.getBlock(),
                STEAM_INDUSTRIAL_PROCESSING_PLANT_T2.getBlock(),
                STEAM_INDUSTRIAL_PROCESSING_PLANT_T3.getBlock());
        Block[] allowedArray = allowed.toArray(Block[]::new);
        BlockInfo[] candidates = allowed.stream()
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

    private static MultiblockMachineDefinition registerSteamProcessingPlant(String name, String langValue, int tier) {
        return REGISTRATE.multiblock(name, holder -> new SteamIndustrialProcessingPlantMachine(holder, tier))
                .langValue(langValue)
                .rotationState(RotationState.ALL)
                .allowExtendedFacing(false)
                .recipeTypes(STEAM_PROCESSING_RECIPES)
                .appearanceBlock(GTBlocks.CASING_BRONZE_BRICKS)
                .tooltips(Component.translatable("vividtech.machine.steam_industrial_processing_plant.tooltip.0"))
                .tooltips(Component.translatable("vividtech.machine.steam_industrial_processing_plant.tooltip.1"))
                .tooltipBuilder(VIVID_ADD)
                .pattern(definition -> SteamIndustrialProcessingPlantStructure.MODULE
                        .where("~", Predicates.controller(Predicates.blocks(definition.get())))
                        .where("H", blocks(GTBlocks.CASING_BRONZE_BRICKS.get())
                                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(2))
                                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2))
                                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(2))
                                .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMaxGlobalLimited(2)))
                        .build())
                .recipeModifier(SteamIndustrialProcessingPlantMachine::recipeModifier)
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

    public final static MultiblockMachineDefinition STEAM_INDUSTRIAL_PROCESSING_PLANT_T1 = registerSteamProcessingPlant(
            "steam_industrial_processing_plant_t1", "Steam Industrial Processing Plant T1", 1);

    public final static MultiblockMachineDefinition STEAM_INDUSTRIAL_PROCESSING_PLANT_T2 = registerSteamProcessingPlant(
            "steam_industrial_processing_plant_t2", "Steam Industrial Processing Plant T2", 2);

    public final static MultiblockMachineDefinition STEAM_INDUSTRIAL_PROCESSING_PLANT_T3 = registerSteamProcessingPlant(
            "steam_industrial_processing_plant_t3", "Steam Industrial Processing Plant T3", 3);

    public final static MultiblockMachineDefinition DREAM_ELEVATOR = REGISTRATE
            .multiblock("dream_elevator", DreamElevatorMachine::new)
            .langValue("Dream Elevator")
            .rotationState(RotationState.ALL)
            .recipeType(VividRecipeTypes.DREAM_ELEVATOR_RECIPES)
            .appearanceBlock(GTBlocks.CASING_BRONZE_BRICKS)
            .tooltips(Component.translatable("vividtech.machine.dream_elevator.tooltip.0"))
            .tooltips(Component.translatable("vividtech.machine.dream_elevator.tooltip.1"))
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
