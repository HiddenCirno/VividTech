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
 * 梦想电梯子系统仓室的渲染器：
 * 外壳使用与蒸汽模块一致的青铜镀层机械外壳，正面使用指定 GT 仓室的贴图。
 */
public class SubsystemHatchRenderer extends MachineRenderer {

    private static final ResourceLocation BASE_TEXTURE =
            GTCEu.id("block/casings/solid/machine_casing_bronze_plated_bricks");

    private final IModelRenderer overlayModel;

    public SubsystemHatchRenderer(ResourceLocation frontOverlay) {
        super(GTCEu.id("block/machine/hull_machine"));
        setTextureOverride(Map.of(
                "bottom", BASE_TEXTURE,
                "top", BASE_TEXTURE,
                "side", BASE_TEXTURE));
        this.overlayModel = new IModelRenderer(frontOverlay);
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
