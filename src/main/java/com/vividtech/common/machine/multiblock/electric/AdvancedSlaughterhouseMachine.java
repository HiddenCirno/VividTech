package com.vividtech.common.machine.multiblock.electric;

import org.gtlcore.gtlcore.config.ConfigHolder;
import org.gtlcore.gtlcore.common.data.GTLMaterials;
import org.gtlcore.gtlcore.utils.MachineIO;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeHandler;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.living.LivingDropsEvent;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 进阶工业屠宰场（基础版）。
 *
 * <p>不生成实际生物：用假实体读取战利品表，用假玩家提供 Looting/玩家击杀上下文，
 * 用假实体着火状态提供 Fire Aspect 的熟食掉落。武器只作为催化剂读取，不消耗、不掉耐久。</p>
 *
 * <p>一次配方模拟 8 只生物；整个周期只使用一个 FakePlayer，串行掷战利品表，
 * 不接并行仓。每次模拟还会触发 Forge 的 {@link LivingDropsEvent}，
 * 让神化「捕捉」等基于掉落事件的附魔/模组逻辑生效。</p>
 */
public class AdvancedSlaughterhouseMachine extends WorkableElectricMultiblockMachine {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            AdvancedSlaughterhouseMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    /** 配置不可用时的原版兜底列表；正常情况优先读取 GTL 的 mobList1/2。 */
    private static final String[] FALLBACK_PASSIVE = {
            "chicken", "rabbit", "sheep", "cow", "horse", "pig", "donkey", "wolf", "goat", "camel",
            "cat", "fox", "llama", "panda", "polar_bear"
    };
    private static final String[] FALLBACK_HOSTILE = {
            "zombie", "skeleton", "creeper", "spider", "witch", "drowned", "husk", "stray",
            "pillager", "vindicator", "blaze", "wither_skeleton", "zombified_piglin", "enderman"
    };

    private static final int MOBS_PER_RUN = 8;

    /** 当前配方锁定的武器；在 beforeWorking 时读取，afterWorking 时使用。 */
    private ItemStack activeWeapon = ItemStack.EMPTY;

    /** 本周期实际并行数（由 PARALLEL_HATCH 写入 recipe.parallels）。 */
    private int activeParallel = 1;


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

        FakePlayer fakePlayer = FakePlayerFactory.getMinecraft(level);
        ItemStack previous = fakePlayer.getMainHandItem();
        int totalXp = 0;
        try {
            // 一个周期只创建一个 FakePlayer；8 次模拟串行复用。
            fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, weapon.copy());
            int simulated = MOBS_PER_RUN * parallel;
            for (int i = 0; i < simulated; i++) {
                try {
                    totalXp += simulateOneKill(level, weapon, fakePlayer, mobList);
                } catch (Exception e) {
                    GTCEu.LOGGER.error("[VividTech] Advanced slaughterhouse simulated kill failed", e);
                }
            }
        } finally {
            fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, previous);
        }

        if (totalXp > 0) {
            // 1 XP = 1 mB 液态经验；按整个周期汇总后输出一次。
            MachineIO.outputFluid(this, GTLMaterials.XpJuice.getFluid(totalXp));
        }
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

    /**
     * 模拟杀死一只生物，返回经验点数。
     *
     * <p>战利品表本身通过假玩家/假实体上下文读取；随后再触发一次
     * {@link LivingDropsEvent}，让神化「捕捉」等监听掉落事件的逻辑能追加刷怪蛋。</p>
     */
    private int simulateOneKill(ServerLevel level, ItemStack weapon, FakePlayer fakePlayer, String[] mobList) {
        String mobName = mobList[level.random.nextInt(mobList.length)];
        ResourceLocation mobId = ResourceLocation.tryParse(mobName.contains(":") ? mobName : "minecraft:" + mobName);
        if (mobId == null) {
            return 0;
        }

        EntityType<?> type = EntityType.byString(mobId.toString()).orElse(null);
        if (type == null) {
            return 0;
        }
        Entity created = type.create(level);
        if (!(created instanceof LivingEntity mob)) {
            return 0;
        }

        mob.moveTo(getPos().getX() + 0.5D, getPos().getY() + 1.0D, getPos().getZ() + 0.5D,
                level.random.nextFloat() * 360.0F, 0.0F);

        // Fire Aspect 只负责“是否着火”；战利品表里的 furnace_smelt 条件会读取这个状态。
        if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, weapon) > 0) {
            mob.setSecondsOnFire(8);
        }

        DamageSource source = level.damageSources().playerAttack(fakePlayer);
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, mob)
                .withParameter(LootContextParams.KILLER_ENTITY, fakePlayer)
                .withParameter(LootContextParams.DIRECT_KILLER_ENTITY, fakePlayer)
                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, fakePlayer)
                .withParameter(LootContextParams.DAMAGE_SOURCE, source)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(getPos()))
                .withParameter(LootContextParams.TOOL, weapon.copy())
                .create(LootContextParamSets.ENTITY);

        List<ItemEntity> drops = new ArrayList<>();
        ResourceLocation lootTableId = mob.getLootTable();
        if (lootTableId != null) {
            LootTable lootTable = level.getServer().getLootData().getLootTable(lootTableId);
            if (lootTable != LootTable.EMPTY) {
                for (ItemStack drop : lootTable.getRandomItems(params)) {
                    if (!shouldKeepDrop(drop)) {
                        continue;
                    }
                    drops.add(new ItemEntity(level, mob.getX(), mob.getY(), mob.getZ(), drop.copy()));
                }
            }
        }

        // 战利品表没有包含的掉落事件逻辑（神化捕捉、玩家击杀限定掉落等）在这里接入。
        int lootingLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MOB_LOOTING, weapon);
        LivingDropsEvent event = new LivingDropsEvent(mob, source, drops, lootingLevel, true);
        if (!MinecraftForge.EVENT_BUS.post(event)) {
            for (ItemEntity itemEntity : event.getDrops()) {
                ItemStack drop = itemEntity.getItem();
                if (shouldKeepDrop(drop)) {
                    MachineIO.outputItem(this, drop.copy());
                }
            }
        }

        return mob.shouldDropExperience() ? Math.max(0, mob.getExperienceReward()) : 0;
    }

    /** 不模拟怪物装备；战利品表中若带损伤装备则销毁。 */
    private static boolean shouldKeepDrop(ItemStack stack) {
        return !stack.isEmpty() && !(stack.isDamageableItem() && stack.getDamageValue() > 0);
    }

    private String[] selectMobList() {
        boolean passive = MachineIO.notConsumableCircuit(this, 1);
        String[] configured = passive ? configuredList(true) : configuredList(false);
        if (configured != null && configured.length > 0) {
            return configured;
        }
        return passive ? FALLBACK_PASSIVE : FALLBACK_HOSTILE;
    }

    @Nullable
    private static String[] configuredList(boolean passive) {
        if (ConfigHolder.INSTANCE == null || ConfigHolder.INSTANCE.machines == null) {
            return null;
        }
        return passive ? ConfigHolder.INSTANCE.machines.mobList1 : ConfigHolder.INSTANCE.machines.mobList2;
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
}
