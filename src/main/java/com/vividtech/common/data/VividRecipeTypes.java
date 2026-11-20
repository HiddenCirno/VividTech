package com.vividtech.common.data;

import com.gregtechceu.gtceu.api.block.ICoilType;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.data.GTSoundEntries;

import com.lowdragmc.lowdraglib.utils.LocalizationUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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

    public static final GTRecipeType CRYSTAL_AGGREGATION_RECIPES = GTRecipeTypes.register("crystal_aggregation",
            GTRecipeTypes.MULTIBLOCK)
            .setEUIO(IO.IN)
            .setMaxIOSize(4, 1, 3, 0)
            .addDataInfo(data -> {
                int temperature = data.getInt("ebf_temp");
                return temperature > 0 ? LocalizationUtils.format("gtceu.recipe.temperature", temperature) : "";
            })
            .addDataInfo(data -> {
                int temperature = data.getInt("ebf_temp");
                if (temperature <= 0) {
                    return "";
                }
                ICoilType coil = ICoilType.getMinRequiredType(temperature);
                if (coil == null || coil.getMaterial() == null) {
                    return "";
                }
                return LocalizationUtils.format("gtceu.recipe.coil.tier",
                        coil.getMaterial().getLocalizedName().getString());
            })
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT)
            .setSound(GTSoundEntries.COOLING);

    /**
     * 大气捕集：无物品/流体输入，只按当前维度输出对应的空气。
     * 图标使用大气捕集单元主机方块；维度由配方条件在 EMI 中显示。
     */
    public static final GTRecipeType ATMOSPHERIC_COLLECTION_RECIPES = GTRecipeTypes.register("atmospheric_collection",
            GTRecipeTypes.MULTIBLOCK)
            .setEUIO(IO.IN)
            .setMaxIOSize(0, 0, 0, 1)
            .setIconSupplier(VividRecipeTypes::atmosphericCollectionIcon)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT)
            .setSound(GTSoundEntries.COOLING);

    private static ItemStack atmosphericCollectionIcon() {
        Item item = BuiltInRegistries.ITEM.get(new ResourceLocation("gtceu", "atmospheric_collection_unit"));
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }
}
