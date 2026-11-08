package com.vividtech.common.machine.multiblock.noenergy;

import org.gtlcore.gtlcore.api.machine.multiblock.NoEnergyMultiblockMachine;
import com.vividtech.common.machine.trait.EyeOfWoodRecipeLogic;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;

import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * 武德之眼（Eye of Wood）。
 *
 * <p>
 * 移植自 Twist Space Technology 的 {@code TST_EyeOfWood}，保留其核心逻辑：
 * 只在主世界工作；从输入仓积累水与岩浆，每个加工周期按储量计算成功率；
 * 成功时从当前维度的矿脉数据中做 8 次加权抽取并产出粉/宝石，失败时产出蒸汽。
 * </p>
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EyeOfWoodMachine extends NoEnergyMultiblockMachine {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            EyeOfWoodMachine.class, NoEnergyMultiblockMachine.MANAGED_FIELD_HOLDER);

    @Persisted
    private int storedWater;
    @Persisted
    private int storedLava;

    private boolean explosionScheduled;

    public EyeOfWoodMachine(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
    }

    @Override
    protected RecipeLogic createRecipeLogic(Object... args) {
        return new EyeOfWoodRecipeLogic(this);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public boolean keepSubscribing() {
        // 与资源模块相同：WorkableMultiblockMachine 默认 false，空闲时会被取消订阅。
        return true;
    }

    public int getStoredWater() {
        return storedWater;
    }

    public int getStoredLava() {
        return storedLava;
    }

    public void addStoredWater(long amount) {
        storedWater = (int) Math.min(Integer.MAX_VALUE, (long) storedWater + amount);
    }

    public void addStoredLava(long amount) {
        storedLava = (int) Math.min(Integer.MAX_VALUE, (long) storedLava + amount);
    }

    public void resetStored() {
        storedWater = 0;
        storedLava = 0;
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        // TST_EyeOfWood 只在主世界工作，其他维度直接炸掉多方块。
        if (getLevel() instanceof ServerLevel serverLevel && !serverLevel.dimension().equals(Level.OVERWORLD) && !explosionScheduled) {
            explosionScheduled = true;
            // 延后一个 tick，避免在多方块成型检查中直接引爆炸药包。
            serverLevel.getServer().execute(() -> {
                if (isFormed()) {
                    serverLevel.explode(null,
                            getPos().getX() + 0.5, getPos().getY() + 0.5, getPos().getZ() + 0.5,
                            4.0F, Level.ExplosionInteraction.BLOCK);
                }
            });
        }
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        super.addDisplayText(textList);
        if (isFormed()) {
            textList.add(Component.translatable("vividtech.machine.eye_of_wood.stored_water",
                    storedWater, EyeOfWoodRecipeLogic.STANDARD_WATER));
            textList.add(Component.translatable("vividtech.machine.eye_of_wood.stored_lava",
                    storedLava, EyeOfWoodRecipeLogic.STANDARD_LAVA));
            textList.add(Component.translatable("vividtech.machine.eye_of_wood.success_chance",
                    EyeOfWoodRecipeLogic.getSuccessChance(storedWater, storedLava)));
        }
    }
}
