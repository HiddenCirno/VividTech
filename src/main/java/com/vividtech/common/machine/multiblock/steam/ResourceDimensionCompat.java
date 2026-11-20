package com.vividtech.common.machine.multiblock.steam;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Re-Avaritia 的穹宇洞虚之戒会创建 {@code avaritia:personal_<uuid>} 私人维度。
 * 这些维度没有 GTCEu 的矿脉权重；资源采集模块把它们视为主世界处理。
 */
public final class ResourceDimensionCompat {

    private static final String AVARITIA_NAMESPACE = "avaritia";
    private static final String PERSONAL_PREFIX = "personal_";
    private static final ResourceLocation ANCIENT_WORLD = new ResourceLocation("kubejs", "ancient_world");

    private ResourceDimensionCompat() {}

    public static boolean isAvaritiaPersonal(ResourceKey<Level> dimension) {
        ResourceLocation id = dimension.location();
        return AVARITIA_NAMESPACE.equals(id.getNamespace()) && id.getPath().startsWith(PERSONAL_PREFIX);
    }

    /** 主世界，或应被视为拥有主世界矿脉/流体的私人维度。 */
    public static boolean isOverworldLike(ResourceKey<Level> dimension) {
        return dimension.equals(Level.OVERWORLD) || isAvaritiaPersonal(dimension);
    }

    /** KubeJS 远古世界：有自己独立的矿脉表，但资源模块允许进入。 */
    public static boolean isAncientWorld(ResourceKey<Level> dimension) {
        return dimension.location().equals(ANCIENT_WORLD);
    }

    /**
     * 钻探模块的主世界判定：私人维度与远古世界都按主世界处理，
     * 允许抽取石油/盐水。矿脉表仍由 {@link #effectiveDimension} 单独处理。
     */
    public static boolean isDrillingOverworldLike(ResourceKey<Level> dimension) {
        return isOverworldLike(dimension) || isAncientWorld(dimension);
    }

    /** 用于矿脉权重查询和产出形态选择的实际维度。 */
    public static ResourceKey<Level> effectiveDimension(ResourceKey<Level> dimension) {
        return isAvaritiaPersonal(dimension) ? Level.OVERWORLD : dimension;
    }
}
