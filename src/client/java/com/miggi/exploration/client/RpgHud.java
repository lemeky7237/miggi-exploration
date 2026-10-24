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
        // Top-left panel (bottom-left is where chat lines appear).
        int x = 6, y = 6, w = 104;
        graphics.fill(x - 3, y - 3, x + w + 3, y + 33, 0x90000000);
        graphics.text(font, Component.literal("Lv." + data.level()), x, y, 0xFFFFD080, true);
        Component xpText = Component.literal("XP " + xp);
        graphics.text(font, xpText, x + w - font.width(xpText), y, 0xFFE8D8B0, true);
        float xpFrac = data.level() >= RpgSystem.LEVEL_CAP ? 1f : data.xp() / (float) Math.max(1, RpgSystem.xpToNext(data.level()));
        bar(graphics, x, y + 10, w, xpFrac, 0xFFE0B040);
        Component mp = Component.literal("MP " + data.mana() + "/" + data.maxMana());
        graphics.text(font, mp, x, y + 16, 0xFF80C8FF, true);
        bar(graphics, x, y + 26, w, data.mana() / (float) Math.max(1, data.maxMana()), 0xFF4A90E0);
    }

    private static void bar(GuiGraphicsExtractor g, int x, int y, int w, float frac, int color) {
        g.fill(x, y, x + w, y + 3, 0xFF202020);
        g.fill(x, y, x + Math.round(w * Math.max(0f, Math.min(1f, frac))), y + 3, color);
    }
}
