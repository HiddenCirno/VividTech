package com.vividtech.common.data;

import com.vividtech.VividTech;
import com.vividtech.common.cover.WirelessGridTransmitCover;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.item.ComponentItem;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.client.renderer.cover.SimpleCoverRenderer;
import com.gregtechceu.gtceu.common.data.GTCreativeModeTabs;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.item.CoverPlaceBehavior;
import com.gregtechceu.gtceu.common.item.TooltipBehavior;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import com.tterrag.registrate.util.entry.ItemEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.gregtechceu.gtceu.common.registry.GTRegistration.REGISTRATE;

/**
 * 无线电网发射器覆盖板。
 *
 * <p>1A/4A 全等级；16A 从 EV 起；64A 从 LuV 起；256A 从 UV 起。</p>
 */
public final class VividCovers {

    private static final int[] TIERS_ALL = {
            GTValues.LV, GTValues.MV, GTValues.HV, GTValues.EV, GTValues.IV, GTValues.LuV,
            GTValues.ZPM, GTValues.UV, GTValues.UHV, GTValues.UEV, GTValues.UIV
    };
    private static final int[] TIERS_16A = {
            GTValues.EV, GTValues.IV, GTValues.LuV, GTValues.ZPM, GTValues.UV,
            GTValues.UHV, GTValues.UEV, GTValues.UIV
    };
    private static final int[] TIERS_64A = {
            GTValues.LuV, GTValues.ZPM, GTValues.UV, GTValues.UHV, GTValues.UEV, GTValues.UIV
    };
    private static final int[] TIERS_256A = {
            GTValues.UV, GTValues.UHV, GTValues.UEV, GTValues.UIV
    };

    public record Transmitter(int tier, int amperage, CoverDefinition definition,
                              ItemEntry<ComponentItem> item) {}

    public static final List<Transmitter> TRANSMITTERS = new ArrayList<>();

    private static boolean initialized;

    private VividCovers() {}

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        REGISTRATE.creativeModeTab(() -> VividMachines.POTENTIAL_TAB);
        try {
            registerTiered(TIERS_ALL, 1);
            registerTiered(TIERS_ALL, 4);
            registerTiered(TIERS_16A, 16);
            registerTiered(TIERS_64A, 64);
            registerTiered(TIERS_256A, 256);
        } finally {
            REGISTRATE.creativeModeTab(() -> GTCreativeModeTabs.MACHINE);
        }
    }

    private static void registerTiered(int[] tiers, int amperage) {
        for (int tier : tiers) {
            String tierName = GTValues.VN[tier].toLowerCase(Locale.ROOT);
            String name = tierName + (amperage == 1 ? "" : "_" + amperage + "a")
                    + "_wireless_grid_transmit_cover";
            String overlay = "block/cover/overlay_wireless_grid_transmit"
                    + (amperage == 1 ? "" : "_" + amperage + "a");

            CoverDefinition definition = new CoverDefinition(
                    GTCEu.id(name),
                    (coverDefinition, coverHolder, side) -> new WirelessGridTransmitCover(
                            coverDefinition, coverHolder, side, tier, amperage),
                    new SimpleCoverRenderer(VividTech.id(overlay)));
            GTRegistries.COVERS.register(GTCEu.id(name), definition);

            ItemEntry<ComponentItem> item = REGISTRATE.item(name, ComponentItem::create)
                    .lang(displayName(tier, amperage))
                    .onRegister(GTItems.attach(
                            new TooltipBehavior(lines -> addTooltips(lines, tier, amperage)),
                            new CoverPlaceBehavior(definition)))
                    .register();

            TRANSMITTERS.add(new Transmitter(tier, amperage, definition, item));
        }
    }

    private static String displayName(int tier, int amperage) {
        String prefix = amperage == 1 ? "" : amperage + "A ";
        return prefix + GTValues.VNF[tier] + " Wireless Grid Transmitter";
    }

    private static void addTooltips(List<Component> lines, int tier, int amperage) {
        lines.add(Component.translatable("vividtech.wireless_grid_transmit_cover.tooltip.0"));
        lines.add(Component.translatable("vividtech.wireless_grid_transmit_cover.tooltip.1"));
        lines.add(Component.translatable("vividtech.wireless_grid_transmit_cover.tooltip.2",
                FormattingUtil.formatNumbers(GTValues.V[tier] * (long) amperage)));
        lines.add(Component.translatable("vividtech.wireless_grid_transmit_cover.tooltip.3"));
        VividMachines.VIVID_ADD.accept(ItemStack.EMPTY, lines);
    }
}
