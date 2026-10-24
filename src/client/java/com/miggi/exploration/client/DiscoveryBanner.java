package com.miggi.exploration.client;

import com.miggi.exploration.MiggiExploration;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayDeque;

/** The parchment scroll that unrolls at the top of the screen: "새로운 장소 발견 / 강". */
public final class DiscoveryBanner {
    private DiscoveryBanner() {}

    private static final Identifier TEXTURE = MiggiExploration.id("textures/gui/discovery_scroll.png");
    private static final int TEX_W = 512, TEX_H = 128, SRC_W = 320, SRC_H = 80;
    private static final int W = 240, H = 60;
    private static final long SHOW_MS = 3800, FADE_IN = 350, FADE_OUT = 600;

    private record Entry(Component subtitle, Component title, boolean challenge) {}
    private static final ArrayDeque<Entry> QUEUE = new ArrayDeque<>();
    private static Entry current;
    private static long start;

    public static void push(Component subtitle, Component title, boolean challenge) {
        QUEUE.add(new Entry(subtitle, title, challenge));
    }
    public static void clear() { QUEUE.clear(); current = null; }

    public static void extract(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        long now = System.currentTimeMillis();
        if (current != null && now - start > SHOW_MS) current = null;
        if (current == null) {
            current = QUEUE.poll();
            if (current == null) return;
            start = now;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(
                current.challenge() ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F));
        }
        long t = now - start;
        float alpha = 1f;
        if (t < FADE_IN) alpha = t / (float) FADE_IN;
        else if (t > SHOW_MS - FADE_OUT) alpha = (SHOW_MS - t) / (float) FADE_OUT;
        alpha = Math.max(0f, Math.min(1f, alpha));
        if (alpha < 0.03f) return;

        int a = (int) (alpha * 255) << 24;
        int x = (graphics.guiWidth() - W) / 2;
        int y = 8 + (int) ((1f - Math.min(1f, t / (float) FADE_IN)) * -10);

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0f, 0f, W, H, SRC_W, SRC_H, TEX_W, TEX_H, a | 0xFFFFFF);

        Font font = Minecraft.getInstance().font;
        int cx = x + W / 2;
        int sub = current.challenge() ? 0x2E6B30 : 0x6E5440;
        JournalScreen.centered(graphics, font, current.subtitle(), cx, y + 15, a | sub);

        graphics.pose().pushMatrix();
        graphics.pose().translate(cx, y + 28);
        graphics.pose().scale(2f);
        JournalScreen.centered(graphics, font, current.title(), 0, 0, a | 0x46301E);
        graphics.pose().popMatrix();
    }
}
