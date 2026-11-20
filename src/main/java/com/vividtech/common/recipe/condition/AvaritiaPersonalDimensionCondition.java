package com.vividtech.common.recipe.condition;

import com.vividtech.common.data.VividRecipeConditions;
import com.vividtech.common.machine.multiblock.electric.AtmosphericCollectionUnitMachine;

import com.gregtechceu.gtceu.api.data.DimensionMarker;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeCondition;
import com.gregtechceu.gtceu.api.recipe.condition.RecipeConditionType;
import com.gregtechceu.gtceu.common.recipe.condition.DimensionCondition;
import com.gregtechceu.gtceu.config.ConfigHolder;

import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * 无尽贪婪私人维度条件。
 *
 * <p>私人维度 ID 带 UUID，不能用普通的 {@link DimensionCondition} 逐个枚举。
 * 该条件沿用 {@link DimensionCondition} 的维度图标 UI，但测试逻辑改为匹配
 * {@code avaritia:personal_<uuid>}。图标直接使用无尽贪婪的水晶矩阵块。</p>
 */
public class AvaritiaPersonalDimensionCondition extends DimensionCondition {

    /** 仅用于 UI 的伪维度 ID，不对应真实世界。 */
    private static final ResourceLocation DIMENSION_MARKER = new ResourceLocation("vividtech", "avaritia_personal");

    /** 穹宇洞虚之界的维度标记：物品图标 + T0 + 维度名称。 */
    private static final DimensionMarker PERSONAL_MARKER = new DimensionMarker(
            0,
            () -> {
                Item item = BuiltInRegistries.ITEM.get(new ResourceLocation("avaritia", "full_matter_cluster"));
                return item == Items.AIR ? Blocks.BARRIER : item;
            },
            "vividtech.dimension.avaritia_personal");

    public static final Codec<AvaritiaPersonalDimensionCondition> CODEC = RecordCodecBuilder.create(instance ->
            RecipeCondition.isReverse(instance).apply(instance, AvaritiaPersonalDimensionCondition::new));

    public AvaritiaPersonalDimensionCondition() {
        this(false);
    }

    public AvaritiaPersonalDimensionCondition(boolean isReverse) {
        super(isReverse, DIMENSION_MARKER);
    }

    @Override
    public RecipeConditionType<?> getType() {
        return VividRecipeConditions.AVARITIA_PERSONAL;
    }

    @Override
    public Component getTooltips() {
        return Component.translatable("recipe.condition.dimension.tooltip",
                Component.translatable("vividtech.dimension.avaritia_personal"));
    }

    @Override
    public SlotWidget setupDimensionMarkers(int x, int y) {
        ItemStackTransfer transfer = new ItemStackTransfer(1);
        transfer.setStackInSlot(0, PERSONAL_MARKER.getIcon());

        SlotWidget widget = new SlotWidget(transfer, 0, x, y, true, false);
        widget.setIngredientIO(IngredientIO.INPUT);
        if (ConfigHolder.INSTANCE.compat.showDimensionTier) {
            widget.setOverlay(new TextTexture("T" + PERSONAL_MARKER.getTier())
                    .scale(0.75F)
                    .transform(-3.0F, 5.0F));
        }
        return widget;
    }

    @Override
    public boolean test(@NotNull GTRecipe recipe, @NotNull RecipeLogic recipeLogic) {
        Level level = recipeLogic.machine.self().getLevel();
        if (level == null) {
            return false;
        }
        ResourceLocation dimension = level.dimension().location();
        return AtmosphericCollectionUnitMachine.isAvaritiaPersonalDimension(dimension);
    }

    @Override
    public RecipeCondition createTemplate() {
        return new AvaritiaPersonalDimensionCondition();
    }
}
