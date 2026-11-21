package com.vividtech.common.menu;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Comparator;
import java.util.List;

/**
 * 客户端与服务端共用的标签/物品列表计算。
 *
 * <p>两端的索引必须完全一致，因此这里只按注册名排序，绝不使用本地化显示名。
 * 客户端据此渲染页面，服务端据此把点击的索引还原成实际物品。</p>
 */
public final class PatternReplacerData {

    private PatternReplacerData() {}

    public static List<TagKey<Item>> tagsFor(ItemStack stack) {
        return BuiltInRegistries.ITEM.getTagNames()
                .filter(stack::is)
                .filter(tag -> !itemsFor(tag).isEmpty())
                .sorted(Comparator.comparing(tag -> tag.location().toString()))
                .toList();
    }

    public static List<ItemStack> itemsFor(TagKey<Item> tag) {
        return BuiltInRegistries.ITEM.stream()
                .filter(item -> item != Items.AIR)
                .map(Item::getDefaultInstance)
                .filter(stack -> stack.is(tag))
                .sorted(Comparator.comparing(stack ->
                        BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()))
                .toList();
    }
}
