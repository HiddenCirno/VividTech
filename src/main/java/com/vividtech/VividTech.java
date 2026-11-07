package com.vividtech;

import com.vividtech.common.data.VividMachines;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(VividTech.MOD_ID)
public class VividTech {

    public static final String MOD_ID = "vividtech";

    public VividTech() {
        FMLJavaModLoadingContext.get().getModEventBus();
        VividMachines.init();
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
