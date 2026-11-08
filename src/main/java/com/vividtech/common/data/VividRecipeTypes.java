package com.vividtech.common.data;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.data.GTSoundEntries;

import static com.lowdragmc.lowdraglib.gui.texture.ProgressTexture.FillDirection.LEFT_TO_RIGHT;

public class VividRecipeTypes {

    public static void init() {}

    public static final GTRecipeType EYE_OF_WOOD_RECIPES = GTRecipeTypes.register("eye_of_wood",
            GTRecipeTypes.MULTIBLOCK)
            .setMaxIOSize(0, 0, 2, 1)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT)
            .setSound(GTSoundEntries.MINER);

    public static final GTRecipeType DREAM_ELEVATOR_RECIPES = GTRecipeTypes.register("dream_elevator",
            GTRecipeTypes.MULTIBLOCK)
            .setMaxIOSize(0, 0, 0, 0)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT)
            .setSound(GTSoundEntries.ASSEMBLER);

    public static final GTRecipeType STEAM_STRATUM_MINING_RECIPES = GTRecipeTypes.register("steam_stratum_mining",
            GTRecipeTypes.MULTIBLOCK)
            .setEUIO(IO.IN)
            .setMaxIOSize(0, 4, 0, 1)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT)
            .setSound(GTSoundEntries.MINER);

    public static final GTRecipeType STEAM_STRATUM_DRILLING_RECIPES = GTRecipeTypes.register("steam_stratum_drilling",
            GTRecipeTypes.MULTIBLOCK)
            .setEUIO(IO.IN)
            .setMaxIOSize(0, 0, 1, 1)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT)
            .setSound(GTSoundEntries.CHEMICAL);
}
