package com.vividtech.common.item;

import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour;

import org.gtlcore.gtlcore.integration.ae2.pattern.PatternEncoderMetadata;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.AEProcessingPattern;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Arrays;
import java.util.Objects;

/**
 * Writes or removes the programming circuit of an encoded AE processing pattern.
 *
 * <p>Kept separate from the pattern box UI so the operation can also be tested and reused without a
 * machine-side circuit inventory.</p>
 */
public final class PatternCircuitHelper {

    private PatternCircuitHelper() {}

    public static ItemStack applyCircuit(ItemStack originalPatternStack, int circuitConfig, boolean replaceExisting,
                                         Level level) {
        if (circuitConfig < 0 || circuitConfig > IntCircuitBehaviour.CIRCUIT_MAX) {
            return originalPatternStack;
        }
        return rebuildPatternCircuit(originalPatternStack, circuitConfig, replaceExisting, level);
    }

    public static ItemStack clearCircuit(ItemStack originalPatternStack, Level level) {
        return rebuildPatternCircuit(originalPatternStack, -1, true, level);
    }

    private static ItemStack rebuildPatternCircuit(ItemStack originalPatternStack, int circuitConfig,
                                                   boolean replaceExisting, Level level) {
        if (!(PatternDetailsHelper.decodePattern(originalPatternStack, level) instanceof AEProcessingPattern processingPattern)) {
            return ItemStack.EMPTY;
        }

        var filteredInputs = new ObjectArrayList<GenericStack>();
        boolean hasCircuit = false;

        for (var input : Arrays.stream(processingPattern.getSparseInputs()).filter(Objects::nonNull).toList()) {
            boolean isCircuit = input.what() instanceof AEItemKey itemKey &&
                    itemKey.getItem() == GTItems.INTEGRATED_CIRCUIT.asItem();

            if (isCircuit) {
                hasCircuit = true;
            } else {
                filteredInputs.add(input);
            }
        }

        if (circuitConfig < 0 && !hasCircuit) {
            return originalPatternStack;
        }
        if (circuitConfig >= 0 && hasCircuit && !replaceExisting) {
            return originalPatternStack;
        }
        if (circuitConfig >= 0) {
            filteredInputs.add(0, GenericStack.fromItemStack(IntCircuitBehaviour.stack(circuitConfig)));
        }

        ItemStack rebuilt = PatternDetailsHelper.encodeProcessingPattern(
                filteredInputs.toArray(new GenericStack[0]), processingPattern.getSparseOutputs());
        PatternEncoderMetadata.copyMetadata(originalPatternStack, rebuilt);
        return rebuilt;
    }
}
