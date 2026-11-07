package com.vividtech.common.machine.trait;

import com.vividtech.common.machine.multiblock.noenergy.EyeOfWoodMachine;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.data.worldgen.GTOreDefinition;
import com.gregtechceu.gtceu.api.data.worldgen.IWorldGenLayer;
import com.gregtechceu.gtceu.api.data.worldgen.WorldGenLayers;
import com.gregtechceu.gtceu.api.data.worldgen.generator.VeinGenerator;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.lowdragmc.lowdraglib.side.fluid.IFluidTransfer;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 武德之眼的处理逻辑，对应 TST 的 {@code TST_EyeOfWood#checkProcessing}。
 *
 * <p>
 * 每个周期（60 秒）：
 * <ol>
 * <li>读取内部水/岩浆储量，按与 256,000 mB 的差值计算成功率（满储量 75%）；</li>
 * <li>清空储量；</li>
 * <li>成功时进行 8 次矿脉加权抽取，把抽到的材料转成粉/宝石；失败时产出蒸汽。</li>
 * </ol>
 * 水/岩浆由输入仓持续补充，逻辑每秒把输入仓里的液体全部抽入内部储量。
 * </p>
 */
public class EyeOfWoodRecipeLogic extends RecipeLogic {

    public static final int DURATION = 20 * 60;
    public static final int STANDARD_WATER = 256_000;
    public static final int STANDARD_LAVA = 256_000;
    private static final int STANDARD_BUCKET = 256;
    private static final double STANDARD_SUBSTRATE = Math.pow(2_000_000_000d, 1d / STANDARD_BUCKET);
    private static final int ROLLS = 8;

    private static final Random RANDOM = new Random();

    private static List<WeightedOre> dropTable;
    private static double totalWeight;

    public EyeOfWoodRecipeLogic(IRecipeLogicMachine machine) {
        super(machine);
    }

    @Override
    public EyeOfWoodMachine getMachine() {
        return (EyeOfWoodMachine) super.getMachine();
    }

    @Override
    public void findAndHandleRecipe() {
        EyeOfWoodMachine machine = getMachine();
        if (!(machine.getLevel() instanceof ServerLevel level)) return;
        if (!level.dimension().equals(Level.OVERWORLD)) return;
        if (!machine.isFormed()) return;
        if (machine.getOffsetTimer() % 20 != 0) return;

        drainFluids(machine);

        GTRecipe recipe = createRecipe(machine);
        if (recipe != null) {
            setupRecipe(recipe);
        }
    }

    private GTRecipe createRecipe(EyeOfWoodMachine machine) {
        ensureDropTable();
        if (totalWeight <= 0) {
            setWaiting(Component.translatable("vividtech.machine.eye_of_wood.no_veins"));
            return null;
        }

        int chance = getSuccessChance(machine.getStoredWater(), machine.getStoredLava());
        machine.resetStored();

        GTRecipeBuilder builder = GTRecipeBuilder.ofRaw()
                .duration(DURATION)
                .EUt(0);

        if (RANDOM.nextInt(10000) < chance) {
            List<ItemStack> outputs = new ArrayList<>();
            for (int i = 0; i < ROLLS; i++) {
                addRoll(outputs);
            }
            if (!outputs.isEmpty()) {
                builder.outputItems(outputs.toArray(ItemStack[]::new));
            }
        } else {
            // TST 的失败产物：36,000 L * 成功率 的蒸汽。
            builder.outputFluids(GTMaterials.Steam.getFluid(36_000L * chance));
        }

        return builder.buildRawRecipe();
    }

    /**
     * 成功率 = {7500 - 7499 * [1 - 1/(S^dW * S^dL)]} / 10000
     * <p>
     * S ≈ 1.087，dW/dL 为储量与标准量差值向下取整的桶数。
     * </p>
     */
    public static int getSuccessChance(int water, int lava) {
        if (water == STANDARD_WATER && lava == STANDARD_LAVA) {
            return 7500;
        }
        int waterBucketDifference = Math.abs(water - STANDARD_WATER) / 1000;
        int lavaBucketDifference = Math.abs(lava - STANDARD_LAVA) / 1000;
        if (waterBucketDifference >= STANDARD_BUCKET || lavaBucketDifference >= STANDARD_BUCKET) {
            return 1;
        }
        double waterMultiplier = 1d / Math.pow(STANDARD_SUBSTRATE, waterBucketDifference);
        double lavaMultiplier = 1d / Math.pow(STANDARD_SUBSTRATE, lavaBucketDifference);
        return (int) (7500 - 7499 * (1d - waterMultiplier * lavaMultiplier));
    }

    private void drainFluids(EyeOfWoodMachine machine) {
        var handlers = machine.getCapabilitiesProxy().get(IO.IN, FluidRecipeCapability.CAP);
        if (handlers == null) return;

        Fluid water = GTMaterials.Water.getFluid();
        Fluid lava = GTMaterials.Lava.getFluid();

        for (var handler : handlers) {
            if (!(handler instanceof IFluidTransfer transfer)) continue;
            for (int i = 0; i < transfer.getTanks(); i++) {
                if (!transfer.supportsDrain(i)) continue;
                FluidStack stack = transfer.getFluidInTank(i);
                if (stack.isEmpty()) continue;

                long amount = stack.getAmount();
                if (stack.getFluid() == water) {
                    machine.addStoredWater(amount);
                } else if (stack.getFluid() == lava) {
                    machine.addStoredLava(amount);
                } else {
                    continue;
                }
                transfer.drain(i, FluidStack.create(stack.getFluid(), amount), false, false);
            }
        }
    }

    private static void addRoll(List<ItemStack> outputs) {
        Material material = rollMaterial();
        if (material == null) return;

        List<ItemStack> roll = new ArrayList<>(4);
        boolean hasGem = !ChemicalHelper.get(TagPrefix.gem, material, 1).isEmpty();

        // TST：有副产物时 64 粉，无副产物时 128 粉。GTCEu 没有材料级副产物，
        // 这里用“是否有宝石形态”作为近似分界。
        addIfPresent(roll, TagPrefix.dust, material, hasGem ? 64 : 128);

        if (hasGem) {
            addIfPresent(roll, TagPrefix.gemExquisite, material, 16);
            addIfPresent(roll, TagPrefix.gemFlawless, material, 32);
            addIfPresent(roll, TagPrefix.gem, material, 32);
        }

        if (roll.isEmpty()) {
            addIfPresent(roll, TagPrefix.rawOre, material, 64);
        }
        outputs.addAll(roll);
    }

    private static void addIfPresent(List<ItemStack> outputs, TagPrefix prefix, Material material, int amount) {
        ItemStack stack = ChemicalHelper.get(prefix, material, amount);
        if (!stack.isEmpty()) {
            outputs.add(stack);
        }
    }

    private static Material rollMaterial() {
        double value = RANDOM.nextDouble() * totalWeight;
        for (WeightedOre ore : dropTable) {
            value -= ore.weight();
            if (value <= 0) return ore.material();
        }
        return dropTable.get(dropTable.size() - 1).material();
    }

    private static synchronized void ensureDropTable() {
        if (dropTable != null) return;

        List<WeightedOre> table = new ArrayList<>();
        double total = 0;
        for (GTOreDefinition definition : GTRegistries.ORE_VEINS.values()) {
            if (definition == null) continue;

            var dimensionFilter = definition.dimensionFilter();
            if (dimensionFilter != null && !dimensionFilter.isEmpty() && !dimensionFilter.contains(Level.OVERWORLD)) {
                continue;
            }

            IWorldGenLayer layer = definition.layer();
            boolean overworldLayer = layer == null || layer == WorldGenLayers.STONE || layer == WorldGenLayers.DEEPSLATE || (layer.getLevels() != null && layer.getLevels().contains(Level.OVERWORLD.location()));
            if (!overworldLayer) continue;

            VeinGenerator generator = definition.veinGenerator();
            if (generator == null) continue;

            double veinWeight = Math.max(1, definition.weight());
            for (Map.Entry<Integer, Material> entry : generator.getValidMaterialsChances()) {
                Material material = entry.getValue();
                int chance = entry.getKey();
                if (material == null || chance <= 0) continue;
                if (ChemicalHelper.get(TagPrefix.dust, material, 1).isEmpty() && ChemicalHelper.get(TagPrefix.gem, material, 1).isEmpty()) {
                    continue;
                }
                double weight = veinWeight * chance;
                table.add(new WeightedOre(material, weight));
                total += weight;
            }
        }

        if (total > 0) {
            dropTable = List.copyOf(table);
            totalWeight = total;
        }
    }

    @Override
    public void onRecipeFinish() {
        getMachine().afterWorking();
        if (lastRecipe != null) {
            lastRecipe.postWorking(getMachine());
            handleRecipeIO(lastRecipe, IO.OUT);
        }
        setStatus(Status.IDLE);
        progress = 0;
        duration = 0;
    }

    private record WeightedOre(Material material, double weight) {}
}
