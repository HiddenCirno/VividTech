package com.vividtech.common.data;

import com.vividtech.common.machine.multiblock.electric.AdvancedSlaughterhouseMachine;

import com.gregtechceu.gtceu.api.GTValues;
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

    /** 物质重塑：原子重构器的配方类型。 */
    public static final GTRecipeType MATTER_RESHAPING_RECIPES = GTRecipeTypes.register("matter_reshaping",
            GTRecipeTypes.MULTIBLOCK)
            .setEUIO(IO.IN)
            .setMaxIOSize(9, 9, 4, 2)
            .setIconSupplier(VividRecipeTypes::matterReshapingIcon)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT)
            .setSound(GTSoundEntries.CHEMICAL);

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

    /**
     * 进阶工业屠宰场：配方只用于计时/耗电，产物由机器模拟击杀生成。
     * 电路 1 使用被动生物表，电路 2 使用敌对生物表；武器不进入配方，避免被消耗。
     */
    public static final GTRecipeType ADVANCED_SLAUGHTERHOUSE_RECIPES = GTRecipeTypes.register("advanced_slaughterhouse",
            GTRecipeTypes.MULTIBLOCK)
            .setEUIO(IO.IN)
            .setMaxIOSize(0, 0, 0, 0)
            .setIconSupplier(VividRecipeTypes::advancedSlaughterhouseIcon)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT)
            .setSound(GTSoundEntries.MACERATOR)
            // 无材料配方的工具化：GT 的空配方不会进入普通查找表，这里给机器提供一个
            // 每次搜索都能拿到的原始配方，实际产出/耗电由机器逻辑处理。
            .addCustomRecipeLogic(holder -> {
                if (!(holder instanceof AdvancedSlaughterhouseMachine)) {
                    return null;
                }
                return createAdvancedSlaughterhouseRecipe();
            });

    private static com.gregtechceu.gtceu.api.recipe.GTRecipe createAdvancedSlaughterhouseRecipe() {
        return ADVANCED_SLAUGHTERHOUSE_RECIPES.recipeBuilder("processing")
                .EUt(GTValues.VA[GTValues.HV])
                .duration(20)
                .buildRawRecipe();
    }

    private static ItemStack advancedSlaughterhouseIcon() {
        Item item = BuiltInRegistries.ITEM.get(new ResourceLocation("gtceu", "advanced_slaughterhouse"));
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static ItemStack atmosphericCollectionIcon() {
        Item item = BuiltInRegistries.ITEM.get(new ResourceLocation("gtceu", "atmospheric_collection_unit"));
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static ItemStack matterReshapingIcon() {
        Item item = BuiltInRegistries.ITEM.get(new ResourceLocation("gtceu", "atomic_reconstructor"));
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }
}
