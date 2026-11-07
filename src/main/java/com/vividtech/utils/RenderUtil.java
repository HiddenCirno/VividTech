package com.vividtech.utils;

import com.gregtechceu.gtceu.api.machine.MetaMachine;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RenderUtil {

    public static float getSmoothTick(MetaMachine machine, float partialTick) {
        return machine.getOffsetTimer() + partialTick;
    }
}
