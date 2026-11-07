package com.vividtech.common.data.machines.structure;

import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;

import static com.gregtechceu.gtceu.api.pattern.util.RelativeDirection.*;

/**
 * 蒸汽加工模块结构：1 宽 × 5 高 × 2 深。
 *
 * <p>
 * 控制器位于外侧切片的第二层，与 GTNH 太空电梯模块的控制器位置一致；
 * 模块嵌在梦想电梯的模块槽位中，外壳位置同时被主机结构认可。
 * </p>
 */
@SuppressWarnings("unused")
public class SteamProcessingModuleStructure {

    public static final FactoryBlockPattern MODULE = FactoryBlockPattern.start(RIGHT, DOWN, FRONT)
            .aisle("H", "H", "H", "H", "H")
            .aisle("H", "~", "H", "H", "H");
}
