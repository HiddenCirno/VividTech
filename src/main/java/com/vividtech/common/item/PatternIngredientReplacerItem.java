package com.vividtech.common.item;

import com.vividtech.common.command.PatternKind;
import com.vividtech.common.command.PatternReplacerService;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 样板原料替换器。
 *
 * <p>右击 ME 网络组件时扫描网络里的处理样板，然后通过可点击聊天消息完成选择与保存。
 * 不打开自定义 GUI，避免额外的菜单/同步代码。</p>
 */
public class PatternIngredientReplacerItem extends Item {

    private final PatternKind kind;

    public PatternIngredientReplacerItem(Properties properties, PatternKind kind) {
        super(properties.stacksTo(1));
        this.kind = kind;
    }

    public PatternKind getKind() {
        return kind;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            PatternReplacerService.open(serverPlayer, serverLevel,
                    context.getClickedPos(), context.getClickedFace(), kind);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String base = kind == PatternKind.CRAFTING ?
                "item.gtceu.crafting_pattern_ingredient_replacer" :
                "item.gtceu.pattern_ingredient_replacer";
        tooltip.add(Component.translatable(base + ".tooltip.0").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(base + ".tooltip.1").withStyle(ChatFormatting.GRAY));
    }
}
