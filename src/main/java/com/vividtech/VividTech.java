package com.vividtech;

import com.vividtech.common.data.VividMachines;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(VividTech.MOD_ID)
public class VividTech {

    public static final String MOD_ID = "vividtech";

    public VividTech() {
        FMLJavaModLoadingContext.get().getModEventBus();
        // Recipe types must be registered before GTCEu freezes its registries. GTCEu's
        // registry unfreeze check requires the active container to be "gtceu".
        ModLoadingContext context = ModLoadingContext.get();
        ModContainer vividContainer = context.getActiveContainer();
        ModContainer gtceuContainer = ModList.get().getModContainerById("gtceu").orElse(null);
        if (gtceuContainer != null) {
            context.setActiveContainer(gtceuContainer);
        }
        try {
            com.vividtech.common.data.VividRecipeTypes.init();
        } finally {
            context.setActiveContainer(vividContainer);
        }
        // Machine registration happens from VividGTAddon.initializeAddon(), after GTCEu content setup.
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
