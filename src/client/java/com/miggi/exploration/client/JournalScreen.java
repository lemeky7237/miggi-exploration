package com.miggi.exploration.client;

import com.miggi.exploration.Challenges;
import com.miggi.exploration.ExploreData;
import com.miggi.exploration.MiggiExploration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

import java.util.List;

/** 탐험 일지: left page = discovered places, right page = challenges and current title. */
public class JournalScreen extends Screen {
    private static final Identifier BOOK = MiggiExploration.id("textures/gui/journal.png");
    private static final Identifier ARROWS = MiggiExploration.id("textures/gui/arrows.png");
    private static final int BOOK_W = 384, BOOK_H = 240, TEX_W = 512, TEX_H = 256;

    private static final int INK = 0xFF5C3A24, FAINT = 0xFF9C8064, GREEN = 0xFF34703A, GOLD = 0xFFB07A2E;
    private static final int LEFT_CX = 107, RIGHT_CX = 277;
    private static final int PLACES_PER_PAGE = 6, CHALLENGES_PER_PAGE = 5;

    private int left, top;
    private int placePage, challengePage, placePages = 1, chPages = 1;

    public JournalScreen() { super(Component.translatable("gui.miggi_exploration.journal")); }

    @Override protected void init() {
        left = (width - BOOK_W) / 2;
        top = (height - BOOK_H) / 2;
    }

    @Override public boolean isPauseScreen() { return false; }

    private ExploreData data() {
        var player = Minecraft.getInstance().player;
        return player == null ? ExploreData.EMPTY : player.getAttachedOrElse(MiggiExploration.DATA, ExploreData.EMPTY);
    }

    private int totalPlaces(ExploreData data) {
        var level = Minecraft.getInstance().level;
        long n = level == null ? 0 : level.registryAccess().lookupOrThrow(Registries.BIOME).listElements().count();
        return (int) Math.max(n, data.places().size());
    }

    static void centered(GuiGraphicsExtractor g, Font font, Component text, int cx, int y, int color) {
        g.text(font, text, cx - font.width(text) / 2, y, color, false);
    }

    private static void scaled(GuiGraphicsExtractor g, Font font, Component text, int cx, int y, float s, int color) {
        g.pose().pushMatrix();
        g.pose().translate(cx, y);
        g.pose().scale(s);
        centered(g, font, text, 0, 0, color);
        g.pose().popMatrix();
    }

    private static int pages(int count, int per) { return Math.max(1, (count + per - 1) / per); }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        Font font = this.font;
        ExploreData data = data();
        int total = totalPlaces(data);

        g.blit(RenderPipelines.GUI_TEXTURED, BOOK, left, top, 0f, 0f, BOOK_W, BOOK_H, TEX_W, TEX_H);

        // ---- left page: places ----
        int lx = left + LEFT_CX;
        scaled(g, font, Component.translatable("gui.miggi_exploration.journal"), lx, top + 32, 1.5f, INK);
        centered(g, font, Component.translatable("gui.miggi_exploration.found", data.places().size(), total), lx, top + 50, FAINT);

        placePages = pages(total, PLACES_PER_PAGE);
        placePage = Math.min(placePage, placePages - 1);
        for (int i = 0; i < PLACES_PER_PAGE; i++) {
            int idx = placePage * PLACES_PER_PAGE + i;
            if (idx >= total) break;
            Component line;
            int color;
            if (idx < data.places().size()) {
                line = Component.literal((idx + 1) + ". ").append(Component.translatable(MiggiClient.biomeKey(data.places().get(idx))));
                color = INK;
            } else {
                line = Component.literal((idx + 1) + ". ???");
                color = FAINT;
            }
            centered(g, font, line, lx, top + 78 + i * 19, color);
        }
        pager(g, font, lx, placePage, placePages, mouseX, mouseY);

        // ---- right page: challenges ----
        int rx = left + RIGHT_CX;
        scaled(g, font, Component.translatable("gui.miggi_exploration.challenges"), rx, top + 32, 1.5f, INK);
        centered(g, font, Component.translatable("gui.miggi_exploration.current_title"), rx, top + 50, FAINT);
        List<String> done = data.challenges();
        Component title = done.isEmpty()
            ? Component.translatable("gui.miggi_exploration.no_title")
            : Component.translatable(Challenges.byId(done.getLast()) != null ? Challenges.byId(done.getLast()).titleKey() : "gui.miggi_exploration.no_title");
        centered(g, font, title, rx, top + 71, done.isEmpty() ? FAINT : GREEN);
        centered(g, font, Component.translatable("gui.miggi_exploration.challenge_count", done.size(), Challenges.ALL.size()), rx, top + 84, GOLD);

        chPages = pages(Challenges.ALL.size(), CHALLENGES_PER_PAGE);
        challengePage = Math.min(challengePage, chPages - 1);
        for (int i = 0; i < CHALLENGES_PER_PAGE; i++) {
            int idx = challengePage * CHALLENGES_PER_PAGE + i;
            if (idx >= Challenges.ALL.size()) break;
            Challenges.Challenge c = Challenges.ALL.get(idx);
            boolean ok = done.contains(c.id());
            Component line = ok
                ? Component.literal("✔ ").append(Component.translatable(c.nameKey()))
                : Component.translatable(c.nameKey()).append(Component.literal(" (" + c.clampedProgress(data) + "/" + c.goal() + ")"));
            int y = top + 100 + i * 19;
            centered(g, font, line, rx, y, ok ? GREEN : FAINT);
            int w = font.width(line);
            if (mouseX >= rx - w / 2 - 2 && mouseX <= rx + w / 2 + 2 && mouseY >= y - 2 && mouseY <= y + 10) {
                g.setTooltipForNextFrame(font, Component.translatable(c.descKey())
                    .append(Component.literal("\n"))
                    .append(Component.translatable("gui.miggi_exploration.reward", Component.translatable(c.titleKey()))), mouseX, mouseY);
            }
        }
        pager(g, font, rx, challengePage, chPages, mouseX, mouseY);
    }

    // page arrows sit at y = top + 196, 34px either side of the page centre
    private static final int PAGER_Y = 196, ARROW_DX = 34;

    private void pager(GuiGraphicsExtractor g, Font font, int cx, int page, int pages, int mx, int my) {
        int y = top + PAGER_Y;
        centered(g, font, Component.literal((page + 1) + " / " + pages), cx, y + 4, INK);
        if (page > 0) arrow(g, cx - ARROW_DX - 16, y, true, hit(mx, my, cx - ARROW_DX - 16, y));
        if (page < pages - 1) arrow(g, cx + ARROW_DX, y, false, hit(mx, my, cx + ARROW_DX, y));
    }

    private void arrow(GuiGraphicsExtractor g, int x, int y, boolean back, boolean hover) {
        float u = hover ? 16f : 0f;
        if (back) {
            g.pose().pushMatrix();
            g.pose().translate(x + 16, y);
            g.pose().scale(-1f, 1f);
            g.blit(RenderPipelines.GUI_TEXTURED, ARROWS, 0, 0, u, 0f, 16, 16, 32, 16);
            g.pose().popMatrix();
        } else {
            g.blit(RenderPipelines.GUI_TEXTURED, ARROWS, x, y, u, 0f, 16, 16, 32, 16);
        }
    }

    private static boolean hit(double mx, double my, int x, int y) {
        return mx >= x && mx < x + 16 && my >= y && my < y + 16;
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int y = top + PAGER_Y;
        int lx = left + LEFT_CX, rx = left + RIGHT_CX;
        int before = placePage * 100 + challengePage;
        if (hit(mx, my, lx - ARROW_DX - 16, y) && placePage > 0) placePage--;
        else if (hit(mx, my, lx + ARROW_DX, y) && placePage < placePages - 1) placePage++;
        else if (hit(mx, my, rx - ARROW_DX - 16, y) && challengePage > 0) challengePage--;
        else if (hit(mx, my, rx + ARROW_DX, y) && challengePage < chPages - 1) challengePage++;
        if (before != placePage * 100 + challengePage) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
