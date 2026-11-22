package com.vividtech.common.machine.multiblock.electric;

import org.gtlcore.gtlcore.config.ConfigHolder;

import com.gregtechceu.gtceu.GTCEu;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.ModList;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 服务端全局的进阶屠宰场基础期望表。
 *
 * <p>每只原版怪物按 Looting 0..ApotheosisMax × Fire on/off 预计算基础战利品表，
 * 不包含 Capturing / Scavenger / Knowledge；这三者由机器在运行时按精确公式叠加。
 * 表只依赖服务端的原版战利品表，因此所有机器共享。</p>
 */
public final class AdvancedSlaughterhouseExpectationCache {

    public static final int SAMPLES = 1024;

    public static final String[] FALLBACK_PASSIVE = {
            "chicken", "rabbit", "sheep", "cow", "horse", "pig", "donkey", "skeleton_horse",
            "iron_golem", "wolf", "goat", "parrot", "camel", "cat", "fox", "llama", "panda", "polar_bear"
    };
    public static final String[] FALLBACK_HOSTILE = {
            "ghast", "zombie", "pillager", "zombie_villager", "skeleton", "drowned", "witch", "spider",
            "creeper", "husk", "wither_skeleton", "blaze", "zombified_piglin", "slime", "vindicator", "enderman"
    };

    private static volatile GlobalTables current;

    private AdvancedSlaughterhouseExpectationCache() {}

    public static void onServerStarted(ServerStartedEvent event) {
        try {
            build(event.getServer());
        } catch (Exception | LinkageError e) {
            GTCEu.LOGGER.error("[VividTech] Failed to build global slaughterhouse expectation cache", e);
        }
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        current = null;
    }

    @Nullable
    public static MobTable get(ServerLevel level, String mobName) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return null;
        }
        GlobalTables tables = current;
        if (tables == null || tables.server != server) {
            try {
                tables = build(server);
            } catch (Exception | LinkageError e) {
                GTCEu.LOGGER.error("[VividTech] Failed to lazily build global slaughterhouse expectation cache", e);
                return null;
            }
        }
        if (tables.level == null) {
            return null;
        }
        ResourceLocation id = normalizeMobId(mobName);
        if (id == null) {
            return null;
        }
        MobTable table = tables.mobs.get(id.toString());
        if (table == null) {
            try {
                table = computeMobTable(tables.level, id.toString(), tables.maxLooting, tables.bannedMobs);
                if (table != null) {
                    tables.mobs.put(id.toString(), table);
                }
            } catch (Exception | LinkageError e) {
                GTCEu.LOGGER.warn("[VividTech] Failed to build slaughterhouse table for {}", id, e);
            }
        }
        return table;
    }

    private static synchronized GlobalTables build(MinecraftServer server) {
        GlobalTables existing = current;
        if (existing != null && existing.server == server) {
            return existing;
        }

        long startedAt = System.nanoTime();
        ServerLevel level = server.overworld();
        int maxLooting = resolveMaxLooting();
        Set<ResourceLocation> bannedMobs = resolveBannedMobs();
        Map<String, MobTable> mobs = new HashMap<>();

        if (level != null) {
            for (String mobName : collectMobIds()) {
                try {
                    MobTable table = computeMobTable(level, mobName, maxLooting, bannedMobs);
                    if (table != null) {
                        mobs.put(mobName, table);
                    }
                } catch (Exception | LinkageError e) {
                    GTCEu.LOGGER.warn("[VividTech] Failed to build slaughterhouse table for {}", mobName, e);
                }
            }
        }

        GlobalTables tables = new GlobalTables(server, level, maxLooting, bannedMobs, mobs);
        current = tables;
        GTCEu.LOGGER.info(
                "[VividTech] Built slaughterhouse expectation cache: {} mobs, looting 0..{}, fire 2, {} samples/table, {} ms",
                mobs.size(), maxLooting, SAMPLES, (System.nanoTime() - startedAt) / 1_000_000L);
        return tables;
    }

    private static List<String> collectMobIds() {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        if (ConfigHolder.INSTANCE != null && ConfigHolder.INSTANCE.machines != null) {
            addMobIds(ids, ConfigHolder.INSTANCE.machines.mobList1);
            addMobIds(ids, ConfigHolder.INSTANCE.machines.mobList2);
        }
        addMobIds(ids, FALLBACK_PASSIVE);
        addMobIds(ids, FALLBACK_HOSTILE);
        return new ArrayList<>(ids);
    }

    private static void addMobIds(Set<String> ids, @Nullable String[] source) {
        if (source == null) {
            return;
        }
        for (String name : source) {
            if (name == null || name.isBlank()) {
                continue;
            }
            ResourceLocation id = normalizeMobId(name);
            if (id == null || !"minecraft".equals(id.getNamespace())) {
                continue;
            }
            if (EntityType.byString(id.toString()).isEmpty()) {
                continue;
            }
            ids.add(id.toString());
        }
    }

    @Nullable
    private static MobTable computeMobTable(ServerLevel level, String mobName, int maxLooting,
                                            Set<ResourceLocation> bannedMobs) {
        ResourceLocation mobId = normalizeMobId(mobName);
        if (mobId == null || !"minecraft".equals(mobId.getNamespace())) {
            return null;
        }
        EntityType<?> type = EntityType.byString(mobId.toString()).orElse(null);
        if (type == null) {
            return null;
        }
        Entity created = type.create(level);
        if (!(created instanceof LivingEntity mob)) {
            return null;
        }

        mob.moveTo(0.5D, 64.0D, 0.5D, 0.0F, 0.0F);
        LootTable lootTable = resolveLootTable(level, mob);
        Item eggItem = bannedMobs.contains(EntityType.getKey(type)) ? null : ForgeSpawnEggItem.fromEntityType(type);
        double baseXp = mob.shouldDropExperience() ? Math.max(0, mob.getExperienceReward()) : 0;
        BaseExpectation[][] byLootingFire = new BaseExpectation[maxLooting + 1][2];

        FakePlayer fakePlayer = FakePlayerFactory.getMinecraft(level);
        ItemStack previous = fakePlayer.getMainHandItem();
        int failures = 0;
        Exception firstFailure = null;
        try {
            for (int fire = 0; fire < 2; fire++) {
                if (fire == 0) {
                    mob.clearFire();
                } else {
                    mob.setSecondsOnFire(8);
                }
                for (int looting = 0; looting <= maxLooting; looting++) {
                    ItemStack tool = buildSamplingTool(looting, fire == 1);
                    fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, tool);
                    LootParams params = buildLootParams(level, mob, fakePlayer, tool);
                    Map<ItemKey, ExpectedItem> items = new HashMap<>();
                    for (int sample = 0; sample < SAMPLES; sample++) {
                        try {
                            addRawDrops(lootTable, params, items);
                        } catch (Exception e) {
                            failures++;
                            if (firstFailure == null) {
                                firstFailure = e;
                            }
                        }
                    }
                    for (ExpectedItem item : items.values()) {
                        item.amount /= SAMPLES;
                    }
                    byLootingFire[looting][fire] = new BaseExpectation(items);
                }
            }
        } finally {
            fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, previous);
        }

        if (failures > 0) {
            GTCEu.LOGGER.error("[VividTech] Slaughterhouse cache sampling failed {} times for {}",
                    failures, mobName, firstFailure);
        }
        return new MobTable(mobName, byLootingFire, eggItem, baseXp);
    }

    private static void addRawDrops(LootTable lootTable, LootParams params, Map<ItemKey, ExpectedItem> items) {
        if (lootTable == LootTable.EMPTY) {
            return;
        }
        lootTable.getRandomItemsRaw(params, stack -> {
            if (!shouldKeepDrop(stack)) {
                return;
            }
            ItemKey key = ItemKey.of(stack);
            ExpectedItem item = items.computeIfAbsent(key, k -> new ExpectedItem(stack));
            item.amount += stack.getCount();
        });
    }

    private static ItemStack buildSamplingTool(int looting, boolean fire) {
        ItemStack tool = new ItemStack(Items.IRON_SWORD);
        Map<Enchantment, Integer> applied = new HashMap<>();
        if (looting > 0) {
            applied.put(Enchantments.MOB_LOOTING, looting);
        }
        if (fire) {
            applied.put(Enchantments.FIRE_ASPECT, 1);
        }
        EnchantmentHelper.setEnchantments(applied, tool);
        return tool;
    }

    private static LootTable resolveLootTable(ServerLevel level, LivingEntity mob) {
        ResourceLocation lootTableId = mob.getLootTable();
        if (lootTableId == null) {
            return LootTable.EMPTY;
        }
        return level.getServer().getLootData().getLootTable(lootTableId);
    }

    private static LootParams buildLootParams(ServerLevel level, LivingEntity mob, FakePlayer fakePlayer, ItemStack tool) {
        DamageSource source = level.damageSources().playerAttack(fakePlayer);
        return new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, mob)
                .withParameter(LootContextParams.KILLER_ENTITY, fakePlayer)
                .withParameter(LootContextParams.DIRECT_KILLER_ENTITY, fakePlayer)
                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, fakePlayer)
                .withParameter(LootContextParams.DAMAGE_SOURCE, source)
                .withParameter(LootContextParams.ORIGIN, new Vec3(0.5D, 64.0D, 0.5D))
                .withParameter(LootContextParams.TOOL, tool)
                .create(LootContextParamSets.ENTITY);
    }

    private static boolean shouldKeepDrop(ItemStack stack) {
        return !stack.isEmpty() && !(stack.isDamageableItem() && stack.getDamageValue() > 0);
    }

    private static int resolveMaxLooting() {
        int vanillaMax = Enchantments.MOB_LOOTING.getMaxLevel();
        if (!ModList.get().isLoaded("apotheosis")) {
            return Math.max(0, Math.min(vanillaMax, 127));
        }
        // Apotheosis 未成功读取时至少覆盖常见的高等级 Looting，避免把 8 级武器压回原版 3 级。
        int max = Math.max(vanillaMax, 16);
        try {
            Class<?> hooks = Class.forName("dev.shadowsoffire.apotheosis.ench.asm.EnchHooks");
            Method method = hooks.getMethod("getMaxLevel", Enchantment.class);
            Object value = method.invoke(null, Enchantments.MOB_LOOTING);
            if (value instanceof Integer level) {
                max = level;
            }
        } catch (Exception | LinkageError e) {
            GTCEu.LOGGER.warn("[VividTech] Failed to read Apotheosis Looting max level, using 16", e);
        }
        return Math.max(0, Math.min(max, 127));
    }

    private static Set<ResourceLocation> resolveBannedMobs() {
        if (!ModList.get().isLoaded("apotheosis")) {
            return Set.of();
        }
        try {
            Class<?> spawnerModule = Class.forName("dev.shadowsoffire.apotheosis.spawn.SpawnerModule");
            Field bannedField = spawnerModule.getDeclaredField("bannedMobs");
            bannedField.setAccessible(true);
            Object value = bannedField.get(null);
            if (value instanceof Set<?> raw) {
                Set<ResourceLocation> result = new HashSet<>();
                for (Object element : raw) {
                    if (element instanceof ResourceLocation id) {
                        result.add(id);
                    }
                }
                return result;
            }
        } catch (Exception | LinkageError e) {
            GTCEu.LOGGER.warn("[VividTech] Failed to read Apotheosis capturing ban list", e);
        }
        return Set.of();
    }

    @Nullable
    private static ResourceLocation normalizeMobId(String name) {
        return ResourceLocation.tryParse(name.contains(":") ? name : "minecraft:" + name);
    }

    /** 基础表：物品期望数量，不含 Capturing / Scavenger / Knowledge。 */
    public static final class BaseExpectation {

        private static final BaseExpectation EMPTY = new BaseExpectation(Map.of());

        private final Map<ItemKey, ExpectedItem> items;

        private BaseExpectation(Map<ItemKey, ExpectedItem> items) {
            this.items = items;
        }

        public Map<ItemKey, ExpectedItem> items() {
            return this.items;
        }

        public double totalCount() {
            double total = 0.0D;
            for (ExpectedItem item : this.items.values()) {
                total += item.amount;
            }
            return total;
        }
    }

    public static final class MobTable {

        private final String mobName;
        private final BaseExpectation[][] byLootingFire;
        private final @Nullable Item eggItem;
        private final double baseXp;

        private MobTable(String mobName, BaseExpectation[][] byLootingFire, @Nullable Item eggItem, double baseXp) {
            this.mobName = mobName;
            this.byLootingFire = byLootingFire;
            this.eggItem = eggItem;
            this.baseXp = baseXp;
        }

        public String mobName() {
            return this.mobName;
        }

        public BaseExpectation get(int looting, boolean fire) {
            int level = Math.max(0, Math.min(looting, this.byLootingFire.length - 1));
            BaseExpectation table = this.byLootingFire[level][fire ? 1 : 0];
            return table == null ? BaseExpectation.EMPTY : table;
        }

        @Nullable
        public Item eggItem() {
            return this.eggItem;
        }

        public double baseXp() {
            return this.baseXp;
        }
    }

    public record ItemKey(Item item, CompoundTag tag) {

        public ItemKey {
            tag = tag == null ? null : tag.copy();
        }

        public static ItemKey of(ItemStack stack) {
            return new ItemKey(stack.getItem(), stack.getTag());
        }
    }

    public static final class ExpectedItem {

        public final ItemStack template;
        public double amount;

        private ExpectedItem(ItemStack template) {
            this.template = template.copy();
            this.template.setCount(1);
        }
    }

    private record GlobalTables(MinecraftServer server, @Nullable ServerLevel level, int maxLooting,
                                Set<ResourceLocation> bannedMobs, Map<String, MobTable> mobs) {}
}
