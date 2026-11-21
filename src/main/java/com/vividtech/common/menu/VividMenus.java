package com.vividtech.common.menu;

import com.vividtech.VividTech;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class VividMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, VividTech.MOD_ID);

    public static final RegistryObject<MenuType<PatternReplacerMenu>> PATTERN_REPLACER =
            MENUS.register("pattern_ingredient_replacer",
                    () -> IForgeMenuType.create(PatternReplacerMenu::new));

    private VividMenus() {}
}
