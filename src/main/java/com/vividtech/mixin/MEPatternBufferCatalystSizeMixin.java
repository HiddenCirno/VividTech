package com.vividtech.mixin;

import com.vividtech.common.machine.multiblock.part.MEUltimateExtendedPatternBufferPartMachine;

import org.gtlcore.gtlcore.common.machine.multiblock.part.ae.MEPatternBufferPartMachineBase;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Gives the VividTech ultimate extended buffer 6x6 shared catalyst storage without changing GTLCore.
 */
@Mixin(MEPatternBufferPartMachineBase.class)
public abstract class MEPatternBufferCatalystSizeMixin {

    @ModifyArg(
               method = "<init>(Lcom/gregtechceu/gtceu/api/machine/IMachineBlockEntity;Lcom/gregtechceu/gtceu/api/capability/recipe/IO;)V",
               at = @At(
                        value = "INVOKE",
                        target = "Lcom/hepdd/gtmthings/common/block/machine/trait/CatalystItemStackHandler;<init>(Lcom/gregtechceu/gtceu/api/machine/MetaMachine;ILcom/gregtechceu/gtceu/api/capability/recipe/IO;Lcom/gregtechceu/gtceu/api/capability/recipe/IO;)V"),
               index = 1,
               remap = false,
               require = 1)
    private int vividtech$sharedItemSlots(int slots) {
        return ((Object) this).getClass() == MEUltimateExtendedPatternBufferPartMachine.class ?
                MEUltimateExtendedPatternBufferPartMachine.SHARED_CATALYST_SLOTS : slots;
    }

    @ModifyArg(
               method = "<init>(Lcom/gregtechceu/gtceu/api/machine/IMachineBlockEntity;Lcom/gregtechceu/gtceu/api/capability/recipe/IO;)V",
               at = @At(
                        value = "INVOKE",
                        target = "Lcom/hepdd/gtmthings/common/block/machine/trait/CatalystFluidStackHandler;<init>(Lcom/gregtechceu/gtceu/api/machine/MetaMachine;IJLcom/gregtechceu/gtceu/api/capability/recipe/IO;Lcom/gregtechceu/gtceu/api/capability/recipe/IO;)V"),
               index = 1,
               remap = false,
               require = 1)
    private int vividtech$sharedFluidSlots(int slots) {
        return ((Object) this).getClass() == MEUltimateExtendedPatternBufferPartMachine.class ?
                MEUltimateExtendedPatternBufferPartMachine.SHARED_CATALYST_SLOTS : slots;
    }
}
