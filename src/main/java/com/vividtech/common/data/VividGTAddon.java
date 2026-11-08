package com.vividtech.common.data;

import com.vividtech.VividTech;

import org.gtlcore.gtlcore.common.data.GTLMachines;
import org.gtlcore.gtlcore.common.data.machines.MultiBlockMachineA;

import com.gregtechceu.gtceu.api.addon.GTAddon;
import com.gregtechceu.gtceu.api.addon.IGTAddon;
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

import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.LinkedHashMap;
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
}
