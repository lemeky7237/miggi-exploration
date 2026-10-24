package com.miggi.exploration.client;

import com.miggi.exploration.MiggiExploration;
import com.miggi.exploration.RpgData;
import com.miggi.exploration.RpgSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Lightweight text-only HUD, using Minecraft 26.3's GuiGraphicsExtractor API. */
public final class RpgHud {
    private RpgHud() {}
    public static void extract(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        var player = Minecraft.getInstance().player;
        if (player == null) return; // HUD elements are already skipped while F1 hides the GUI
        RpgData data = player.getAttachedOrElse(MiggiExploration.RPG_DATA, RpgData.INITIAL);
        var font = Minecraft.getInstance().font;
        String xp = data.level() >= RpgSystem.LEVEL_CAP ? "MAX" : data.xp() + "/" + RpgSystem.xpToNext(data.level());
        graphics.text(font, Component.literal("Lv." + data.level() + "  XP " + xp), 8, graphics.guiHeight() - 48, 0xFFFFD080, true);
        graphics.text(font, Component.literal("MP " + data.mana() + "/" + data.maxMana()), 8, graphics.guiHeight() - 36, 0xFF80C8FF, true);
    }
}
