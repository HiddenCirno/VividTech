package com.vividtech.common.data;

import com.vividtech.VividTech;

import com.gregtechceu.gtceu.api.addon.GTAddon;
import com.gregtechceu.gtceu.api.addon.IGTAddon;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

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
                .inputItems(GTItems.MICROPROCESSOR_LV, 64)
                .inputItems(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE))
                .circuitMeta(17)
                .outputItems(VividMachines.EYE_OF_WOOD.asStack())
                .EUt(VA[LV]).duration(20 * 114)
                .save(provider);
    }
}
