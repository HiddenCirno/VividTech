package com.vividtech.common.machine.multiblock.electric;

import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper;
import org.gtlcore.gtlcore.common.data.GTLMaterials;
import org.gtlcore.gtlcore.config.ConfigHolder;
import org.gtlcore.gtlcore.utils.MachineIO;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeHandler;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import com.vividtech.VividTech;
import com.vividtech.common.machine.multiblock.electric.AdvancedSlaughterhouseExpectationCache.BaseExpectation;
import com.vividtech.common.machine.multiblock.electric.AdvancedSlaughterhouseExpectationCache.ExpectedItem;
import com.vividtech.common.machine.multiblock.electric.AdvancedSlaughterhouseExpectationCache.ItemKey;
import com.vividtech.common.machine.multiblock.electric.AdvancedSlaughterhouseExpectationCache.MobTable;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 进阶工业屠宰场。
 *
 * <p>兼容范围刻意收窄：只处理原版生物，只读取武器上的附魔，不读取名称、耐久、词缀或其他 NBT；
 * 战利品只走原版战利品表，不触发 {@code LivingDropsEvent}，也不经过 Forge 全局战利品修改器。</p>
 *
 * <p>服务端启动时由 {@link AdvancedSlaughterhouseExpectationCache} 预计算全部原版怪物的
 * Looting × Fire 基础表；机器运行时只做 O(1) 查表，再叠加捕捉、清道夫、知识的精确公式。
 * 并行只乘期望值，不按并行数重复模拟。</p>
 */
public class AdvancedSlaughterhouseMachine extends WorkableElectricMultiblockMachine {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            AdvancedSlaughterhouseMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    private static final int MOBS_PER_RUN = 8;
    private static final ResourceLocation OUTPUT_RECIPE_ID = VividTech.id("advanced_slaughterhouse_output");

    /** 当前配方锁定的武器；在 beforeWorking 时读取，afterWorking 时使用。 */
    private ItemStack activeWeapon = ItemStack.EMPTY;

    /** 本周期实际并行数（由 PARALLEL_HATCH 写入 recipe.parallels）。 */
    private int activeParallel = 1;

    /** 期望值取整后剩余的小数/未输出部分，跨周期累计。 */
    private final Map<ItemKey, Double> itemRemainders = new HashMap<>();
    private double xpRemainder;


    public AdvancedSlaughterhouseMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public @NotNull ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public boolean beforeWorking(@NotNull GTRecipe recipe) {
        ItemStack weapon = findWeapon();
        if (weapon.isEmpty()) {
            activeWeapon = ItemStack.EMPTY;
            getRecipeLogic().setWaiting(
                    Component.translatable("vividtech.machine.advanced_slaughterhouse.waiting_weapon"));
            return false;
        }
        activeWeapon = weapon;
        activeParallel = Math.max(1, recipe.parallels);
        return super.beforeWorking(recipe);
    }

    @Override
    public void afterWorking() {
        ItemStack weapon = activeWeapon;
        activeWeapon = ItemStack.EMPTY;
        int parallel = Math.max(1, activeParallel);
        activeParallel = 1;
        if (weapon.isEmpty() || !(getLevel() instanceof ServerLevel level) || !isFormed()) {
            return;
        }
        String[] mobList = selectMobList();
        if (mobList.length == 0) {
            return;
        }
        outputExpectedResults(level, DropEnchantments.of(weapon), mobList, parallel);
    }

    /**
     * 8 只随机怪物的基础期望查表，叠加神化公式后乘以并行数。
     */
    private void outputExpectedResults(ServerLevel level, DropEnchantments enchantments, String[] mobList, int parallel) {
        Map<ItemKey, Double> totals = new LinkedHashMap<>();
        Map<ItemKey, ItemStack> templates = new HashMap<>();
        double xpTotal = 0.0D;
        double scavengerChance = scavengerChance(enchantments.scavenger());
        double capturingChance = capturingChance(enchantments.capturing());
        int knowledge = Math.max(0, enchantments.knowledge());
        int looting = Math.max(0, enchantments.looting());
        boolean fire = enchantments.fireAspect() > 0;

        for (int i = 0; i < MOBS_PER_RUN; i++) {
            String mobName = mobList[level.random.nextInt(mobList.length)];
            MobTable table = AdvancedSlaughterhouseExpectationCache.get(level, mobName);
            if (table == null) {
                continue;
            }
            BaseExpectation base = table.get(looting, fire);
            double baseXp = table.baseXp();
            double baseCount = base.totalCount();
            double extraCount = baseCount * scavengerChance;
            Item eggItem = table.eggItem();
            double eggCount = eggItem == null ? 0.0D : capturingChance;
            double totalCount = baseCount + extraCount + eggCount;

            if (knowledge > 0) {
                xpTotal += baseXp + totalCount * knowledge * 25.0D;
                continue;
            }

            double multiplier = 1.0D + scavengerChance;
            for (Map.Entry<ItemKey, ExpectedItem> entry : base.items().entrySet()) {
                totals.merge(entry.getKey(), entry.getValue().amount * multiplier, Double::sum);
                templates.putIfAbsent(entry.getKey(), entry.getValue().template);
            }
            if (eggCount > 0.0D) {
                ItemStack egg = new ItemStack(eggItem);
                ItemKey key = ItemKey.of(egg);
                totals.merge(key, eggCount, Double::sum);
                templates.putIfAbsent(key, withCount(egg, 1));
            }
            xpTotal += baseXp;
        }

        outputExpectedItems(templates, totals, parallel);

        double pendingXp = xpTotal * parallel + xpRemainder;
        long xp = (long) Math.floor(pendingXp);
        xpRemainder = pendingXp - xp;
        if (xp > 0 && !MachineIO.outputFluid(this, GTLMaterials.XpJuice.getFluid(xp))) {
            xpRemainder += xp;
        }
    }

    /**
     * 优先用 GT 原生输出路径一次性输出整批内容；失败时按物品类型回退，最后才回退到逐堆输出。
     */
    private void outputExpectedItems(Map<ItemKey, ItemStack> templates, Map<ItemKey, Double> totals, int parallel) {
        if (totals.isEmpty()) {
            return;
        }

        Map<ItemKey, Long> wholeAmounts = new LinkedHashMap<>();
        Map<ItemKey, Double> fractions = new HashMap<>();
        Map<ItemKey, Long> overflow = new HashMap<>();
        List<ItemStack> combined = new ArrayList<>();

        for (Map.Entry<ItemKey, Double> entry : totals.entrySet()) {
            ItemKey key = entry.getKey();
            ItemStack template = templates.get(key);
            if (template == null) {
                continue;
            }
            double pending = entry.getValue() * parallel + itemRemainders.getOrDefault(key, 0.0D);
            long whole = (long) Math.floor(pending);
            double fraction = pending - whole;
            wholeAmounts.put(key, whole);
            fractions.put(key, fraction);
            if (whole > 0) {
                int count = (int) Math.min(whole, Integer.MAX_VALUE);
                overflow.put(key, whole - count);
                combined.add(withCount(template, count));
            }
        }

        if (!combined.isEmpty() && tryNativeOutput(combined)) {
            for (ItemKey key : wholeAmounts.keySet()) {
                itemRemainders.put(key, fractions.getOrDefault(key, 0.0D) + overflow.getOrDefault(key, 0L));
            }
            return;
        }

        for (Map.Entry<ItemKey, Long> entry : wholeAmounts.entrySet()) {
            ItemKey key = entry.getKey();
            long whole = entry.getValue();
            double fraction = fractions.getOrDefault(key, 0.0D);
            ItemStack template = templates.get(key);
            if (whole <= 0 || template == null) {
                itemRemainders.put(key, fraction);
                continue;
            }
            int count = (int) Math.min(whole, Integer.MAX_VALUE);
            long over = whole - count;
            if (tryNativeOutput(List.of(withCount(template, count)))) {
                itemRemainders.put(key, fraction + over);
            } else {
                itemRemainders.put(key, fraction + outputItemCount(template, whole));
            }
        }
    }

    private boolean tryNativeOutput(List<ItemStack> stacks) {
        if (stacks.isEmpty()) {
            return true;
        }
        try {
            GTRecipeBuilder builder = new GTRecipeBuilder(OUTPUT_RECIPE_ID, GTRecipeTypes.DUMMY_RECIPES);
            for (ItemStack stack : stacks) {
                builder.outputItems(stack);
            }
            GTRecipe outputRecipe = builder.buildRawRecipe();
            if (!RecipeRunnerHelper.matchRecipeOutput(this, outputRecipe)) {
                return false;
            }
            if (!RecipeRunnerHelper.handleRecipeOutput(this, outputRecipe)) {
                GTCEu.LOGGER.warn("[VividTech] Advanced slaughterhouse output matched but failed to insert");
                return false;
            }
            return true;
        } catch (Exception e) {
            GTCEu.LOGGER.warn("[VividTech] Native advanced slaughterhouse output failed", e);
            return false;
        }
    }

    /** 输出总线容量不足时的逐堆回退。 */
    private long outputItemCount(ItemStack template, long amount) {
        int maxStackSize = Math.max(1, template.getMaxStackSize());
        long remaining = amount;
        while (remaining > 0) {
            int chunk = (int) Math.min(remaining, maxStackSize);
            try {
                if (!MachineIO.outputItem(this, withCount(template, chunk))) {
                    return remaining;
                }
            } catch (Exception e) {
                GTCEu.LOGGER.warn("[VividTech] Fallback advanced slaughterhouse output failed", e);
                return remaining;
            }
            remaining -= chunk;
        }
        return 0L;
    }

    private static ItemStack withCount(ItemStack template, int count) {
        ItemStack stack = template.copy();
        stack.setCount(count);
        return stack;
    }

    private static double scavengerChance(int level) {
        return level <= 0 ? 0.0D : Math.min(1.0D, Math.ceil(level * 2.5D) / 100.0D);
    }

    private static double capturingChance(int level) {
        return level <= 0 ? 0.0D : Math.min(1.0D, level / 250.0D);
    }

    /** 只保留 minecraft 命名空间的生物；配置里即使写了模组生物也会被丢弃。 */
    private String[] selectMobList() {
        boolean passive = MachineIO.notConsumableCircuit(this, 1);
        String[] configured = passive ? configuredList(true) : configuredList(false);
        String[] filtered = filterVanillaMobs(configured);
        if (filtered.length > 0) {
            return filtered;
        }
        return passive ? AdvancedSlaughterhouseExpectationCache.FALLBACK_PASSIVE
                : AdvancedSlaughterhouseExpectationCache.FALLBACK_HOSTILE;
    }

    private static String[] filterVanillaMobs(@Nullable String[] source) {
        if (source == null || source.length == 0) {
            return new String[0];
        }
        List<String> result = new ArrayList<>(source.length);
        for (String name : source) {
            if (name == null || name.isBlank()) {
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(name.contains(":") ? name : "minecraft:" + name);
            if (id == null || !"minecraft".equals(id.getNamespace())) {
                continue;
            }
            if (EntityType.byString(id.toString()).isEmpty()) {
                continue;
            }
            result.add(id.toString());
        }
        return result.toArray(String[]::new);
    }

    @Nullable
    private static String[] configuredList(boolean passive) {
        if (ConfigHolder.INSTANCE == null || ConfigHolder.INSTANCE.machines == null) {
            return null;
        }
        return passive ? ConfigHolder.INSTANCE.machines.mobList1 : ConfigHolder.INSTANCE.machines.mobList2;
    }

    /**
     * 从输入总线中找一把武器。不会提取、不会伤害耐久。
     * 多把武器时优先 Looting，其次 Fire Aspect，最后基础攻击力。
     */
    private ItemStack findWeapon() {
        ItemStack best = ItemStack.EMPTY;
        int bestScore = Integer.MIN_VALUE;
        for (IO io : List.of(IO.IN, IO.BOTH)) {
            List<IRecipeHandler<?>> handlers = getCapabilitiesProxy().get(io, ItemRecipeCapability.CAP);
            if (handlers == null) continue;
            for (IRecipeHandler<?> handler : handlers) {
                for (Object content : handler.getContents()) {
                    if (!(content instanceof ItemStack stack) || stack.isEmpty() || !isWeapon(stack)) {
                        continue;
                    }
                    int score = weaponScore(stack);
                    if (score > bestScore) {
                        bestScore = score;
                        best = stack;
                    }
                }
            }
        }
        return best.isEmpty() ? ItemStack.EMPTY : best.copy();
    }

    private static boolean isWeapon(ItemStack stack) {
        if (stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES)) {
            return true;
        }
        return !stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE).isEmpty();
    }

    private static int weaponScore(ItemStack stack) {
        int looting = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MOB_LOOTING, stack);
        int fireAspect = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, stack);
        double attack = stack.getAttributeModifiers(EquipmentSlot.MAINHAND)
                .get(Attributes.ATTACK_DAMAGE).stream()
                .mapToDouble(modifier -> modifier.getAmount())
                .sum();
        return looting * 1000 + fireAspect * 100 + (int) Math.round(attack * 10.0D);
    }

    @Override
    public void addDisplayText(@NotNull List<Component> textList) {
        super.addDisplayText(textList);
        if (!isFormed()) {
            return;
        }
        ItemStack weapon = findWeapon();
        if (weapon.isEmpty()) {
            textList.add(Component.translatable("vividtech.machine.advanced_slaughterhouse.status.no_weapon"));
            return;
        }
        int looting = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MOB_LOOTING, weapon);
        int fireAspect = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, weapon);
        textList.add(Component.translatable("vividtech.machine.advanced_slaughterhouse.status.weapon",
                weapon.getHoverName()));
        textList.add(Component.translatable("vividtech.machine.advanced_slaughterhouse.status.enchant",
                looting, fireAspect));
    }

    /** 只包含会影响产出的附魔等级；没有其它 NBT。 */
    private record DropEnchantments(int looting, int fireAspect, int capturing, int scavenger, int knowledge) {

        private static DropEnchantments of(ItemStack weapon) {
            return new DropEnchantments(
                    EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MOB_LOOTING, weapon),
                    EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, weapon),
                    apotheosisLevel(weapon, "capturing"),
                    apotheosisLevel(weapon, "scavenger"),
                    apotheosisLevel(weapon, "knowledge"));
        }

        private static int apotheosisLevel(ItemStack weapon, String path) {
            Enchantment enchantment = apotheosisEnchantment(path);
            return enchantment == null ? 0 : EnchantmentHelper.getItemEnchantmentLevel(enchantment, weapon);
        }
    }

    @Nullable
    private static Enchantment apotheosisEnchantment(String path) {
        if (!ModList.get().isLoaded("apotheosis")) {
            return null;
        }
        return ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation("apotheosis", path));
    }
}
