package com.vividtech.common.machine.multiblock.steam;

import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.data.worldgen.GTOreDefinition;
import com.gregtechceu.gtceu.api.data.worldgen.IWorldGenLayer;
import com.gregtechceu.gtceu.api.data.worldgen.WorldGenLayers;
import com.gregtechceu.gtceu.api.data.worldgen.generator.VeinGenerator;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 按当前维度构建矿脉权重表，供岩层开采模块抽取原矿。
 */
public final class ResourceOreTable {

    private static final Map<ResourceKey<Level>, List<WeightedOre>> CACHE = new HashMap<>();
    private static final Map<ResourceKey<Level>, Double> TOTAL = new HashMap<>();

    private ResourceOreTable() {}

    public static ItemStack roll(Level level, int amount, RandomSource random) {
        ResourceKey<Level> dimension = level.dimension();
        List<WeightedOre> table = CACHE.get(dimension);
        if (table == null) {
            build(level);
            table = CACHE.getOrDefault(dimension, List.of());
        }
        double total = TOTAL.getOrDefault(dimension, 0.0D);
        if (table.isEmpty() || total <= 0.0D) {
            return ItemStack.EMPTY;
        }

        double value = random.nextDouble() * total;
        Material material = table.get(table.size() - 1).material();
        for (WeightedOre ore : table) {
            value -= ore.weight();
            if (value <= 0.0D) {
                material = ore.material();
                break;
            }
        }

        ItemStack raw = ChemicalHelper.get(TagPrefix.rawOre, material, amount);
        if (!raw.isEmpty()) {
            return raw;
        }
        ItemStack gem = ChemicalHelper.get(TagPrefix.gem, material, amount);
        if (!gem.isEmpty()) {
            return gem;
        }
        return ChemicalHelper.get(TagPrefix.dust, material, amount);
    }

    private static synchronized void build(Level level) {
        ResourceKey<Level> dimension = level.dimension();
        if (CACHE.containsKey(dimension)) {
            return;
        }

        List<WeightedOre> table = new ArrayList<>();
        double total = 0.0D;
        for (GTOreDefinition definition : GTRegistries.ORE_VEINS.values()) {
            if (definition == null) {
                continue;
            }

            var dimensionFilter = definition.dimensionFilter();
            if (dimensionFilter != null && !dimensionFilter.isEmpty() && !dimensionFilter.contains(dimension)) {
                continue;
            }

            IWorldGenLayer layer = definition.layer();
            if (layer != null) {
                var levels = layer.getLevels();
                boolean overworldStoneLayer = dimension.equals(Level.OVERWORLD) &&
                        (layer == WorldGenLayers.STONE || layer == WorldGenLayers.DEEPSLATE);
                boolean levelMatches = levels == null || levels.isEmpty() ||
                        levels.contains(dimension.location());
                if (!overworldStoneLayer && !levelMatches) {
                    continue;
                }
            }

            VeinGenerator generator = definition.veinGenerator();
            if (generator == null) {
                continue;
            }

            double veinWeight = Math.max(1, definition.weight());
            for (Map.Entry<Integer, Material> entry : generator.getValidMaterialsChances()) {
                Material material = entry.getValue();
                int chance = entry.getKey();
                if (material == null || chance <= 0) {
                    continue;
                }
                if (ChemicalHelper.get(TagPrefix.rawOre, material, 1).isEmpty() &&
                        ChemicalHelper.get(TagPrefix.gem, material, 1).isEmpty() &&
                        ChemicalHelper.get(TagPrefix.dust, material, 1).isEmpty()) {
                    continue;
                }
                double weight = veinWeight * chance;
                table.add(new WeightedOre(material, weight));
                total += weight;
            }
        }

        CACHE.put(dimension, List.copyOf(table));
        TOTAL.put(dimension, total);
    }

    private record WeightedOre(Material material, double weight) {}
}
