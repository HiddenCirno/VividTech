package com.vividtech.common.data;

import com.vividtech.VividTech;

import org.gtlcore.gtlcore.common.data.GTLMachines;
import org.gtlcore.gtlcore.common.data.GTLMaterials;
import org.gtlcore.gtlcore.common.data.machines.MultiBlockMachineA;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.addon.GTAddon;
import com.gregtechceu.gtceu.api.addon.IGTAddon;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.data.recipe.CustomTags;
import com.gregtechceu.gtceu.data.recipe.VanillaRecipeHelper;

import com.tterrag.registrate.util.entry.ItemEntry;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.LV;
import static com.gregtechceu.gtceu.api.GTValues.VA;
import static com.gregtechceu.gtceu.common.registry.GTRegistration.REGISTRATE;

@GTAddon
public class VividGTAddon implements IGTAddon {

    @Override
    public String addonModId() {
        return VividTech.MOD_ID;
    }

    @Override
    public GTRegistrate getRegistrate() {
        return REGISTRATE;
    }

    @Override
    public void initializeAddon() {
        // VividMachines is initialised from the mod constructor, before GTCEu freezes its registries.
    }

    @Override
    public void addRecipes(Consumer<FinishedRecipe> provider) {
        GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder("eye_of_wood")
                .inputItems(new ItemStack(Blocks.BRICKS, 32))
                .inputItems(GTBlocks.CASING_BRONZE_BRICKS.asStack(32))
                .inputItems(new ItemStack(Blocks.LAPIS_BLOCK, 16))
                .inputItems(new ItemStack(Blocks.BOOKSHELF, 16))
                .inputItems(new ItemStack(Blocks.CHISELED_STONE_BRICKS, 8))
                .inputItems(GTItems.EMITTER_LV, 64)
                .inputItems(GTItems.FIELD_GENERATOR_LV, 64)
                .inputItems(CustomTags.LV_CIRCUITS, 64)
                .inputItems(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE))
                .circuitMeta(17)
                .outputItems(VividMachines.EYE_OF_WOOD.asStack())
                .EUt(VA[LV]).duration(20 * 114)
                .save(provider);

        TagKey<Item> copperStorageBlock = TagKey.create(Registries.ITEM,
                new ResourceLocation("forge", "storage_blocks/copper"));
        GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder("dream_elevator")
                .inputItems(new ItemStack(Blocks.BRICKS, 64))
                .inputItems(new ItemStack(Blocks.STONE_BRICKS, 64))
                .inputItems(GTBlocks.CASING_BRONZE_BRICKS.asStack(64))
                .inputItems(TagPrefix.frameGt, GTMaterials.TreatedWood, 64)
                .inputItems(copperStorageBlock, 64)
                .inputItems(CustomTags.LV_CIRCUITS, 64)
                .inputItems(GTItems.EMITTER_LV, 64)
                .inputItems(GTItems.ELECTRIC_MOTOR_LV, 64)
                .inputItems(GTItems.FIELD_GENERATOR_LV, 64)
                .outputItems(VividMachines.DREAM_ELEVATOR.asStack())
                .EUt(30).duration(20 * 600)
                .save(provider);

        GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder("basic_integrated_ore_processor")
                .inputItems(GTMachines.MACERATOR[GTValues.LV], 1)
                .inputItems(GTMachines.CENTRIFUGE[GTValues.LV], 1)
                .inputItems(GTMachines.SIFTER[GTValues.LV], 1)
                .inputItems(GTMachines.CHEMICAL_BATH[GTValues.LV], 1)
                .inputItems(GTItems.ROBOT_ARM_LV, 8)
                .inputItems(GTItems.ELECTRIC_PUMP_LV, 8)
                .inputItems(GTItems.CONVEYOR_MODULE_LV, 8)
                .inputItems(CustomTags.LV_CIRCUITS, 4)
                .inputItems(TagPrefix.plateDouble, GTMaterials.WroughtIron, 16)
                .inputFluids(GTMaterials.TinAlloy.getFluid(2880))
                .outputItems(VividMachines.BASIC_INTEGRATED_ORE_PROCESSOR.asStack())
                .EUt(30).duration(20 * 120)
                .save(provider);

        GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder("dream_elevator_steam_pressurization_module")
                .inputItems(TagPrefix.frameGt, GTMaterials.StainlessSteel, 8)
                .inputItems(TagPrefix.plateDouble, GTMaterials.StainlessSteel, 32)
                .inputItems(TagPrefix.screw, GTMaterials.StainlessSteel, 16)
                .inputItems(GTItems.ELECTRIC_PUMP_HV, 16)
                .inputItems(CustomTags.HV_CIRCUITS, 16)
                .outputItems(VividMachines.PRESSURIZATION_MODULE.asStack())
                .EUt(480).duration(20 * 30)
                .save(provider);

        VanillaRecipeHelper.addShapelessRecipe(provider, "dream_elevator_cleanroom_subsystem",
                VividMachines.CLEANROOM_SUBSYSTEM.asStack(),
                GTLMachines.CLEANING_CONFIGURATION_MAINTENANCE_HATCH.asStack());

        VanillaRecipeHelper.addShapedRecipe(provider, "dream_elevator_gravity_subsystem",
                VividMachines.GRAVITY_SUBSYSTEM.asStack(),
                "ABA", "CDC", "EFE",
                'A', GTItems.ROBOT_ARM_LuV,
                'B', GTItems.FIELD_GENERATOR_LuV,
                'C', GTItems.GRAVI_STAR,
                'D', GTBlocks.MACHINE_CASING_LuV,
                'E', GTItems.GRAVITATION_ENGINE,
                'F', GTItems.FIELD_GENERATOR_LuV);

        addSteamModuleRecipes(provider);
        addPrimitiveMagicEnergyRecipes(provider);
        addCompressedMagicEnergyRecipes(provider);
        addForgingInputBusRecipes(provider);
        addCastingInputHatchRecipes(provider);
        addAutoclaveRecipes(provider);
    }

    private static void addPrimitiveMagicEnergyRecipes(Consumer<FinishedRecipe> provider) {
        VanillaRecipeHelper.addShapedRecipe(provider, true, "primitive_magic_energy_lv",
                GTLMachines.PRIMITIVE_MAGIC_ENERGY[GTValues.LV].asStack(),
                "ABC", "DED", "CFA",
                'A', GTItems.EMITTER_LV,
                'B', Blocks.EMERALD_BLOCK,
                'C', GTItems.SENSOR_LV,
                'D', Items.ENDER_PEARL,
                'E', GTMachines.HULL[GTValues.LV].asStack(),
                'F', Blocks.DIAMOND_BLOCK);

        VanillaRecipeHelper.addShapedRecipe(provider, true, "primitive_magic_energy_mv",
                VividMachines.PRIMITIVE_MAGIC_ENERGY[GTValues.MV].asStack(),
                "ABC", "DED", "CFA",
                'A', GTItems.EMITTER_MV,
                'B', Blocks.AMETHYST_CLUSTER,
                'C', GTItems.SENSOR_MV,
                'D', GTItems.FIELD_GENERATOR_LV,
                'E', GTMachines.HULL[GTValues.MV].asStack(),
                'F', GTLMachines.PRIMITIVE_MAGIC_ENERGY[GTValues.LV].asStack());

        VanillaRecipeHelper.addShapedRecipe(provider, true, "primitive_magic_energy_hv",
                VividMachines.PRIMITIVE_MAGIC_ENERGY[GTValues.HV].asStack(),
                "ABC", "DED", "CFA",
                'A', GTItems.EMITTER_HV,
                'B', Items.END_CRYSTAL,
                'C', GTItems.SENSOR_HV,
                'D', GTItems.FIELD_GENERATOR_MV,
                'E', GTMachines.HULL[GTValues.HV].asStack(),
                'F', VividMachines.PRIMITIVE_MAGIC_ENERGY[GTValues.MV].asStack());

        VanillaRecipeHelper.addShapedRecipe(provider, true, "primitive_magic_energy_ev",
                VividMachines.PRIMITIVE_MAGIC_ENERGY[GTValues.EV].asStack(),
                "ABC", "DED", "CFA",
                'A', GTItems.EMITTER_EV,
                'B', Items.HEART_OF_THE_SEA,
                'C', GTItems.SENSOR_EV,
                'D', GTItems.FIELD_GENERATOR_HV,
                'E', GTMachines.HULL[GTValues.EV].asStack(),
                'F', VividMachines.PRIMITIVE_MAGIC_ENERGY[GTValues.HV].asStack());

        VanillaRecipeHelper.addShapedRecipe(provider, true, "primitive_magic_energy_iv",
                VividMachines.PRIMITIVE_MAGIC_ENERGY[GTValues.IV].asStack(),
                "ABC", "DED", "CFA",
                'A', GTItems.EMITTER_IV,
                'B', Items.NETHER_STAR,
                'C', GTItems.SENSOR_IV,
                'D', GTItems.FIELD_GENERATOR_EV,
                'E', GTMachines.HULL[GTValues.IV].asStack(),
                'F', VividMachines.PRIMITIVE_MAGIC_ENERGY[GTValues.EV].asStack());

        VanillaRecipeHelper.addShapedRecipe(provider, true, "primitive_magic_energy_luv",
                VividMachines.PRIMITIVE_MAGIC_ENERGY[GTValues.LuV].asStack(),
                "ABC", "DED", "CFA",
                'A', GTItems.EMITTER_LuV,
                'B', Blocks.NETHERITE_BLOCK,
                'C', GTItems.SENSOR_LuV,
                'D', GTItems.FIELD_GENERATOR_IV,
                'E', GTMachines.HULL[GTValues.LuV].asStack(),
                'F', VividMachines.PRIMITIVE_MAGIC_ENERGY[GTValues.IV].asStack());

        VanillaRecipeHelper.addShapedRecipe(provider, true, "primitive_magic_energy_zpm",
                VividMachines.PRIMITIVE_MAGIC_ENERGY[GTValues.ZPM].asStack(),
                "ABC", "DED", "CFA",
                'A', GTItems.EMITTER_ZPM,
                'B', ChemicalHelper.getBlock(TagPrefix.block, GTMaterials.Naquadria),
                'C', GTItems.SENSOR_ZPM,
                'D', GTItems.FIELD_GENERATOR_LuV,
                'E', GTMachines.HULL[GTValues.ZPM].asStack(),
                'F', VividMachines.PRIMITIVE_MAGIC_ENERGY[GTValues.LuV].asStack());

        VanillaRecipeHelper.addShapedRecipe(provider, true, "primitive_magic_energy_uv",
                VividMachines.PRIMITIVE_MAGIC_ENERGY[GTValues.UV].asStack(),
                "ABC", "DED", "CFA",
                'A', GTItems.EMITTER_UV,
                'B', ChemicalHelper.getBlock(TagPrefix.block, GTLMaterials.Mithril),
                'C', GTItems.SENSOR_UV,
                'D', GTItems.FIELD_GENERATOR_ZPM,
                'E', GTMachines.HULL[GTValues.UV].asStack(),
                'F', VividMachines.PRIMITIVE_MAGIC_ENERGY[GTValues.ZPM].asStack());

        VanillaRecipeHelper.addShapedRecipe(provider, true, "primitive_magic_energy_uhv",
                VividMachines.PRIMITIVE_MAGIC_ENERGY[GTValues.UHV].asStack(),
                "ABC", "DED", "CFA",
                'A', GTItems.EMITTER_UHV,
                'B', ChemicalHelper.getBlock(TagPrefix.block, GTLMaterials.Quantanium),
                'C', GTItems.SENSOR_UHV,
                'D', GTItems.FIELD_GENERATOR_UV,
                'E', GTMachines.HULL[GTValues.UHV].asStack(),
                'F', VividMachines.PRIMITIVE_MAGIC_ENERGY[GTValues.UV].asStack());

        VanillaRecipeHelper.addShapedRecipe(provider, true, "primitive_magic_energy_uev",
                VividMachines.PRIMITIVE_MAGIC_ENERGY[GTValues.UEV].asStack(),
                "ABC", "DED", "CFA",
                'A', GTItems.EMITTER_UEV,
                'B', ChemicalHelper.getBlock(TagPrefix.block, GTLMaterials.Adamantium),
                'C', GTItems.SENSOR_UEV,
                'D', GTItems.FIELD_GENERATOR_UHV,
                'E', GTMachines.HULL[GTValues.UEV].asStack(),
                'F', VividMachines.PRIMITIVE_MAGIC_ENERGY[GTValues.UHV].asStack());

        VanillaRecipeHelper.addShapedRecipe(provider, true, "primitive_magic_energy_uiv",
                VividMachines.PRIMITIVE_MAGIC_ENERGY[GTValues.UIV].asStack(),
                "ABC", "DED", "CFA",
                'A', GTItems.EMITTER_UIV,
                'B', ChemicalHelper.getBlock(TagPrefix.block, GTLMaterials.HeavyQuarkDegenerateMatter),
                'C', GTItems.SENSOR_UIV,
                'D', GTItems.FIELD_GENERATOR_UEV,
                'E', GTMachines.HULL[GTValues.UIV].asStack(),
                'F', VividMachines.PRIMITIVE_MAGIC_ENERGY[GTValues.UEV].asStack());
    }

    private static void addCompressedMagicEnergyRecipes(Consumer<FinishedRecipe> provider) {
        int[] tiers = { GTValues.LV, GTValues.MV, GTValues.HV, GTValues.EV, GTValues.IV, GTValues.LuV,
                GTValues.ZPM, GTValues.UV, GTValues.UHV, GTValues.UEV, GTValues.UIV };
        for (int tier : tiers) {
            MachineDefinition input = tier == GTValues.LV ?
                    GTLMachines.PRIMITIVE_MAGIC_ENERGY[GTValues.LV] :
                    VividMachines.PRIMITIVE_MAGIC_ENERGY[tier];
            GTRecipeTypes.COMPRESSOR_RECIPES.recipeBuilder(
                            "compressed_primitive_magic_energy_" + GTValues.VN[tier].toLowerCase(Locale.ROOT))
                    .inputItems(input, 16)
                    .outputItems(VividMachines.COMPRESSED_PRIMITIVE_MAGIC_ENERGY[tier].asStack())
                    .EUt(30).duration(20 * 60)
                    .save(provider);
        }
    }

    private static void addSteamModuleRecipes(Consumer<FinishedRecipe> provider) {
        Map<String, MachineDefinition> workBlocks = new LinkedHashMap<>();
        workBlocks.put("circuit_assembly", MultiBlockMachineA.LARGE_STEAM_CIRCUIT_ASSEMBLER);
        workBlocks.put("forge_hammer", MultiBlockMachineA.STEAM_PISTON_HAMMER);
        workBlocks.put("compressor", MultiBlockMachineA.STEAM_PRESSOR);
        workBlocks.put("macerator", GTMachines.STEAM_GRINDER);
        workBlocks.put("alloy_smelter", MultiBlockMachineA.STEAM_FOUNDRY);
        workBlocks.put("mixer", MultiBlockMachineA.STEAM_MIXER);
        workBlocks.put("centrifuge", MultiBlockMachineA.LARGE_STEAM_CENTRIFUGE);
        workBlocks.put("thermal_centrifuge", MultiBlockMachineA.LARGE_STEAM_THERMAL_CENTRIFUGE);
        workBlocks.put("ore_washer", MultiBlockMachineA.LARGE_STEAM_ORE_WASHER);
        workBlocks.put("chemical_bath", MultiBlockMachineA.STEAM_BATH);
        workBlocks.put("extractor", GTMachines.STEAM_EXTRACTOR.first());
        workBlocks.put("furnace", GTMachines.STEAM_OVEN);
        workBlocks.put("rock_breaker", GTMachines.STEAM_ROCK_CRUSHER.first());
        workBlocks.put("arc_furnace", GTMachines.ARC_FURNACE[GTValues.LV]);
        workBlocks.put("assembler", GTMachines.ASSEMBLER[GTValues.LV]);
        workBlocks.put("autoclave", GTMachines.AUTOCLAVE[GTValues.LV]);
        workBlocks.put("bender", GTMachines.BENDER[GTValues.LV]);
        workBlocks.put("brewery", GTMachines.BREWERY[GTValues.LV]);
        workBlocks.put("canner", GTMachines.CANNER[GTValues.LV]);
        workBlocks.put("chemical_reactor", GTMachines.CHEMICAL_REACTOR[GTValues.LV]);
        workBlocks.put("cutter", GTMachines.CUTTER[GTValues.LV]);
        workBlocks.put("distillery", GTMachines.DISTILLERY[GTValues.LV]);
        workBlocks.put("electrolyzer", GTMachines.ELECTROLYZER[GTValues.LV]);
        workBlocks.put("extruder", GTMachines.EXTRUDER[GTValues.LV]);
        workBlocks.put("fermenter", GTMachines.FERMENTER[GTValues.LV]);
        workBlocks.put("fluid_heater", GTMachines.FLUID_HEATER[GTValues.LV]);
        workBlocks.put("fluid_solidifier", GTMachines.FLUID_SOLIDIFIER[GTValues.LV]);
        workBlocks.put("forming_press", GTMachines.FORMING_PRESS[GTValues.LV]);
        workBlocks.put("lathe", GTMachines.LATHE[GTValues.LV]);
        workBlocks.put("polarizer", GTMachines.POLARIZER[GTValues.LV]);
        workBlocks.put("laser_engraver", GTMachines.LASER_ENGRAVER[GTValues.LV]);
        workBlocks.put("sifter", GTMachines.SIFTER[GTValues.LV]);
        workBlocks.put("wiremill", GTMachines.WIREMILL[GTValues.LV]);
        workBlocks.put("gas_collector", GTMachines.GAS_COLLECTOR[GTValues.LV]);
        workBlocks.put("dehydrator", GTLMachines.DEHYDRATOR[GTValues.LV]);
        workBlocks.put("lightning_processor", GTLMachines.LIGHTNING_PROCESSOR[GTValues.LV]);

        for (Map.Entry<String, MachineDefinition> entry : workBlocks.entrySet()) {
            String type = entry.getKey();
            MachineDefinition workBlock = entry.getValue();

            GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder("steam_module_" + type + "_mk1")
                    .inputItems(workBlock, 4)
                    .inputItems(TagPrefix.frameGt, GTMaterials.TreatedWood, 4)
                    .inputItems(TagPrefix.screw, GTMaterials.WroughtIron, 8)
                    .inputItems(TagPrefix.plate, GTMaterials.WroughtIron, 32)
                    .inputItems(GTItems.EMITTER_LV, 8)
                    .inputItems(CustomTags.LV_CIRCUITS, 8)
                    .outputItems(VividMachines.getSteamModule(type, 1).asStack())
                    .EUt(30).duration(20 * 60)
                    .save(provider);

            GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder("steam_module_" + type + "_mk2")
                    .inputItems(VividMachines.getSteamModule(type, 1), 1)
                    .inputItems(TagPrefix.frameGt, GTMaterials.Bronze, 4)
                    .inputItems(TagPrefix.screw, GTMaterials.Aluminium, 8)
                    .inputItems(TagPrefix.plateDouble, GTMaterials.Aluminium, 16)
                    .inputItems(GTItems.EMITTER_MV, 8)
                    .inputItems(CustomTags.MV_CIRCUITS, 8)
                    .outputItems(VividMachines.getSteamModule(type, 2).asStack())
                    .EUt(120).duration(20 * 60)
                    .save(provider);

            GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder("steam_module_" + type + "_mk3")
                    .inputItems(VividMachines.getSteamModule(type, 2), 1)
                    .inputItems(TagPrefix.frameGt, GTMaterials.Aluminium, 4)
                    .inputItems(TagPrefix.screw, GTMaterials.StainlessSteel, 8)
                    .inputItems(TagPrefix.plateDouble, GTMaterials.StainlessSteel, 16)
                    .inputItems(GTItems.EMITTER_HV, 8)
                    .inputItems(CustomTags.HV_CIRCUITS, 8)
                    .outputItems(VividMachines.getSteamModule(type, 3).asStack())
                    .EUt(480).duration(20 * 60)
                    .save(provider);
        }

        addResourceModuleRecipes(provider);
    }

    private static void addResourceModuleRecipes(Consumer<FinishedRecipe> provider) {
        GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder("steam_module_stratum_mining_mk1")
                .inputItems(GTMachines.STEAM_MINER, 4)
                .inputItems(TagPrefix.frameGt, GTMaterials.TreatedWood, 4)
                .inputItems(TagPrefix.screw, GTMaterials.WroughtIron, 8)
                .inputItems(TagPrefix.plate, GTMaterials.WroughtIron, 32)
                .inputItems(GTItems.EMITTER_LV, 8)
                .inputItems(GTItems.ELECTRIC_MOTOR_LV, 8)
                .inputItems(CustomTags.LV_CIRCUITS, 8)
                .outputItems(VividMachines.getSteamModule("stratum_mining", 1).asStack())
                .EUt(30).duration(20 * 60)
                .save(provider);

        GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder("steam_module_stratum_drilling_mk1")
                .inputItems(GTMachines.STEAM_MINER, 4)
                .inputItems(TagPrefix.frameGt, GTMaterials.TreatedWood, 4)
                .inputItems(TagPrefix.screw, GTMaterials.WroughtIron, 8)
                .inputItems(TagPrefix.plate, GTMaterials.WroughtIron, 32)
                .inputItems(GTItems.EMITTER_LV, 8)
                .inputItems(TagPrefix.toolHeadDrill, GTMaterials.Iron, 8)
                .inputItems(CustomTags.LV_CIRCUITS, 8)
                .outputItems(VividMachines.getSteamModule("stratum_drilling", 1).asStack())
                .EUt(30).duration(20 * 60)
                .save(provider);

        addResourceModuleUpgradeRecipes(provider, "stratum_mining");
        addResourceModuleUpgradeRecipes(provider, "stratum_drilling");
    }

    private static void addResourceModuleUpgradeRecipes(Consumer<FinishedRecipe> provider, String type) {
        GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder("steam_module_" + type + "_mk2")
                .inputItems(VividMachines.getSteamModule(type, 1), 1)
                .inputItems(TagPrefix.frameGt, GTMaterials.Bronze, 4)
                .inputItems(TagPrefix.screw, GTMaterials.Aluminium, 8)
                .inputItems(TagPrefix.plateDouble, GTMaterials.Aluminium, 16)
                .inputItems(GTItems.EMITTER_MV, 8)
                .inputItems(CustomTags.MV_CIRCUITS, 8)
                .outputItems(VividMachines.getSteamModule(type, 2).asStack())
                .EUt(120).duration(20 * 60)
                .save(provider);

        GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder("steam_module_" + type + "_mk3")
                .inputItems(VividMachines.getSteamModule(type, 2), 1)
                .inputItems(TagPrefix.frameGt, GTMaterials.Aluminium, 4)
                .inputItems(TagPrefix.screw, GTMaterials.StainlessSteel, 8)
                .inputItems(TagPrefix.plateDouble, GTMaterials.StainlessSteel, 16)
                .inputItems(GTItems.EMITTER_HV, 8)
                .inputItems(CustomTags.HV_CIRCUITS, 8)
                .outputItems(VividMachines.getSteamModule(type, 3).asStack())
                .EUt(480).duration(20 * 60)
                .save(provider);
    }

    /**
     * 为所有还没有高压釜结晶配方的宝石粉补充水/蒸馏水结晶配方。
     * 水：70% 概率产出，60 秒；蒸馏水：必定产出，30 秒。
     */
    private static void addAutoclaveRecipes(Consumer<FinishedRecipe> provider) {
        for (Material material : GTCEuAPI.materialManager.getRegisteredMaterials()) {
            if (!material.hasProperty(PropertyKey.GEM) || !material.hasProperty(PropertyKey.DUST)) {
                continue;
            }
            // GT/GTLCore 会为 CRYSTALLIZABLE 材料生成同款高压釜配方。
            if (material.hasFlag(MaterialFlags.CRYSTALLIZABLE)) {
                continue;
            }
            // 兰波顿有独立的 15 粉 + 1000 mB 结晶配方，避免加入更廉价的替代路线。
            if (material == GTMaterials.Lapotron) {
                continue;
            }
            addAutoclaveRecipe(provider, material);
        }
    }

    private static void addAutoclaveRecipe(Consumer<FinishedRecipe> provider, Material material) {
        ItemStack dust = ChemicalHelper.get(TagPrefix.dust, material);
        ItemStack product = ChemicalHelper.get(TagPrefix.gem, material);
        if (dust.isEmpty() || product.isEmpty()) {
            return;
        }
        String id = material.getResourceLocation().toString().replace(':', '_');

        GTRecipeTypes.AUTOCLAVE_RECIPES.recipeBuilder("vividtech_autoclave_" + id + "_water")
                .inputItems(dust.copy())
                .inputFluids(GTMaterials.Water.getFluid(250))
                .chancedOutput(product.copy(), 7000, 0)
                .EUt(24)
                .duration(20 * 60)
                .save(provider);

        GTRecipeTypes.AUTOCLAVE_RECIPES.recipeBuilder("vividtech_autoclave_" + id + "_distilled_water")
                .inputItems(dust.copy())
                .inputFluids(GTMaterials.DistilledWater.getFluid(50))
                .outputItems(product.copy())
                .EUt(24)
                .duration(20 * 30)
                .save(provider);
    }

    private static void addForgingInputBusRecipes(Consumer<FinishedRecipe> provider) {
        addVirtualShapePartRecipes(provider, "forging_input_bus",
                GTMachines.ITEM_IMPORT_BUS, VividMachines.FORGING_INPUT_BUS);
    }

    private static void addCastingInputHatchRecipes(Consumer<FinishedRecipe> provider) {
        addVirtualShapePartRecipes(provider, "casting_input_hatch",
                GTMachines.FLUID_IMPORT_HATCH, VividMachines.CASTING_INPUT_HATCH);
    }

    private static void addVirtualShapePartRecipes(Consumer<FinishedRecipe> provider, String recipeId,
                                                   MachineDefinition[] baseParts,
                                                   MachineDefinition[] outputs) {
        for (int tier : VividMachines.MOLD_INPUT_PART_TIERS) {
            ItemEntry<Item> piston = electricPistonForTier(tier);
            if (piston == null || baseParts[tier] == null || outputs[tier] == null) {
                continue;
            }
            String tierName = GTValues.VN[tier].toLowerCase(Locale.ROOT);
            addVirtualShapePartRecipe(provider, recipeId, tier, tierName, piston, baseParts[tier], outputs[tier],
                    true);
            addVirtualShapePartRecipe(provider, recipeId, tier, tierName, piston, baseParts[tier], outputs[tier],
                    false);
        }
    }

    private static void addVirtualShapePartRecipe(Consumer<FinishedRecipe> provider, String recipeId, int tier,
                                                  String tierName, ItemEntry<Item> piston,
                                                  MachineDefinition basePart, MachineDefinition output,
                                                  boolean solder) {
        GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder(
                        recipeId + "_" + tierName + (solder ? "_solder" : "_tin"))
                .inputItems(basePart.asStack())
                .inputItems(CustomTags.CIRCUITS_ARRAY[tier], 1)
                .inputItems(piston, 1)
                .inputFluids(solder ? GTMaterials.SolderingAlloy.getFluid(144) : GTMaterials.Tin.getFluid(288))
                .outputItems(output.asStack())
                .EUt(GTValues.VA[tier]).duration(20 * 10)
                .save(provider);
    }

    private static ItemEntry<Item> electricPistonForTier(int tier) {
        return switch (tier) {
            case GTValues.LV -> GTItems.ELECTRIC_PISTON_LV;
            case GTValues.MV -> GTItems.ELECTRIC_PISTON_MV;
            case GTValues.HV -> GTItems.ELECTRIC_PISTON_HV;
            case GTValues.EV -> GTItems.ELECTRIC_PISTON_EV;
            case GTValues.IV -> GTItems.ELECTRIC_PISTON_IV;
            case GTValues.LuV -> GTItems.ELECTRIC_PISTON_LUV;
            case GTValues.ZPM -> GTItems.ELECTRIC_PISTON_ZPM;
            case GTValues.UV -> GTItems.ELECTRIC_PISTON_UV;
            case GTValues.UHV -> GTItems.ELECTRIC_PISTON_UHV;
            case GTValues.UEV -> GTItems.ELECTRIC_PISTON_UEV;
            case GTValues.UIV -> GTItems.ELECTRIC_PISTON_UIV;
            case GTValues.UXV -> GTItems.ELECTRIC_PISTON_UXV;
            case GTValues.OpV -> GTItems.ELECTRIC_PISTON_OpV;
            default -> null;
        };
    }

}
