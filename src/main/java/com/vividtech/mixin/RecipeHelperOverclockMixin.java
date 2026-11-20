package com.vividtech.mixin;

import org.gtlcore.gtlcore.api.recipe.IAdvancedOCResult;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.logic.OCParams;
import com.gregtechceu.gtceu.api.recipe.logic.OCResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 统一普通超频的 1t 处理。
 *
 * <p>GTLCore 的 {@code OverclockingLogicMixin} 在普通超频的下一级耗时不足 1t 时
 * 只标记 {@code subTickOverclockAvailable} 然后直接停止。这里在它的结果上补上
 * 最后一级，并把耗时取整到 1t，随后清除标记，避免再进入 subtick/批量处理。</p>
 *
 * <p>只拦截普通超频调用路径，subtick 逻辑仍由 GTLCore 原样处理。</p>
 */
@Mixin(RecipeHelper.class)
public abstract class RecipeHelperOverclockMixin {

    @Redirect(
              method = "performOverclocking(Lcom/gregtechceu/gtceu/api/recipe/OverclockingLogic;Lcom/gregtechceu/gtceu/api/recipe/GTRecipe;JJLcom/gregtechceu/gtceu/api/recipe/logic/OCParams;Lcom/gregtechceu/gtceu/api/recipe/logic/OCResult;)V",
              at = @At(
                       value = "INVOKE",
                       target = "Lcom/gregtechceu/gtceu/api/recipe/OverclockingLogic$Logic;runOverclockingLogic(Lcom/gregtechceu/gtceu/api/recipe/logic/OCParams;Lcom/gregtechceu/gtceu/api/recipe/logic/OCResult;J)V"),
              remap = false,
              require = 1)
    private static void vividtech$applyOneTickClamp(OverclockingLogic.Logic logic, OCParams params, OCResult result,
                                                    long maxVoltage, OverclockingLogic overclocking,
                                                    GTRecipe recipe, long recipeEUt, long ignoredMaxVoltage,
                                                    OCParams ignoredParams, OCResult ignoredResult) {
        logic.runOverclockingLogic(params, result, maxVoltage);

        if (!((Object) result instanceof IAdvancedOCResult advanced) || !advanced.isSubTickOverclockAvailable()) {
            return;
        }

        // 已经处于 1t 的配方不再为了“超频”额外抬高 EU/t。
        if (result.getDuration() <= 1) {
            advanced.setSubTickOverclockAvailable(false);
            return;
        }

        double durationFactor = advanced.getDurationFactor();
        double voltageFactor = advanced.getVoltageFactor();
        long eut = result.getEut();
        if (durationFactor == 0.0D || voltageFactor == 0.0D || eut == 0L) {
            advanced.setSubTickOverclockAvailable(false);
            return;
        }

        long nextEut = Math.round(eut * voltageFactor);
        if (maxVoltage > 0L && Math.abs(nextEut) > maxVoltage) {
            advanced.setSubTickOverclockAvailable(false);
            return;
        }

        int ocLevel = result.getOcLevel() + 1;
        advanced.init(nextEut, 1, 1, nextEut, ocLevel, ocLevel, durationFactor, voltageFactor);
        advanced.setSubTickOverclockAvailable(false);
    }
}
