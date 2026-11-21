package com.vividtech.client;

import com.vividtech.VividTech;
import com.vividtech.client.gui.PatternReplacerScreen;
import com.vividtech.common.menu.VividMenus;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = VividTech.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class VividClientSetup {

    private VividClientSetup() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() ->
                MenuScreens.register(VividMenus.PATTERN_REPLACER.get(), PatternReplacerScreen::new));
    }
}
