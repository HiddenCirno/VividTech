package com.vividtech.client.renderer.machine;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.client.renderer.machine.MachineRenderer;

import com.lowdragmc.lowdraglib.client.renderer.impl.IModelRenderer;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import java.util.List;
import java.util.Map;

/**
 * 创造模式蒸汽输入仓的渲染器：侧面使用鸿蒙之眼角质外壳，正面使用大型蒸汽输入仓的贴图。
 */
public class CreativeSteamInputHatchRenderer extends MachineRenderer {

    private static final ResourceLocation BASE_TEXTURE =
            new ResourceLocation("gtlcore", "block/casings/dimensionally_transcendent_casing");
    private static final ResourceLocation FRONT_OVERLAY = GTCEu.id("block/machine/part/steam_hatch");

    private final IModelRenderer overlayModel = new IModelRenderer(FRONT_OVERLAY);

    public CreativeSteamInputHatchRenderer() {
        super(GTCEu.id("block/machine/hull_machine"));
        setTextureOverride(Map.of(
                "bottom", BASE_TEXTURE,
                "top", BASE_TEXTURE,
                "side", BASE_TEXTURE));
    }

    @Override
    public void renderMachine(List<BakedQuad> list, MachineDefinition definition, MetaMachine machine,
                              Direction frontFacing, Direction side, RandomSource random, Direction facing,
                              ModelState modelState) {
        super.renderMachine(list, definition, machine, frontFacing, side, random, facing, modelState);
        list.addAll(overlayModel.getRotatedModel(frontFacing)
                .getQuads(definition.defaultBlockState(), side, random));
    }
}
