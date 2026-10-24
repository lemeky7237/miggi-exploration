package com.miggi.exploration.client;

import com.miggi.exploration.Challenges;
import com.miggi.exploration.ExploreData;
import com.miggi.exploration.MiggiExploration;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.lwjgl.sdl.SDLScancode;

public class MiggiClient implements ClientModInitializer {
    public static KeyMapping journalKey;

    private LocalPlayer lastPlayer;
    private ExploreData last;
    private int settleTicks;

    @Override public void onInitializeClient() {
        MonsterRenderers.initialize();
        KeyMapping.Category category = KeyMapping.Category.register(MiggiExploration.id("main"));
        journalKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.miggi_exploration.journal",
            InputConstants.Type.KEYBOARD, SDLScancode.SDL_SCANCODE_J, category));

        HudElementRegistry.addLast(MiggiExploration.id("discovery_banner"), DiscoveryBanner::extract);
        HudElementRegistry.addLast(MiggiExploration.id("rpg_hud"), RpgHud::extract);

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    public static String biomeKey(String id) { return "biome." + id.replace(':', '.'); }

    private void tick(Minecraft client) {
        while (journalKey.consumeClick()) {
            if (client.player != null && client.gui.screen() == null) client.gui.setScreen(new JournalScreen());
        }

        LocalPlayer player = client.player;
        if (player == null) { lastPlayer = null; last = null; DiscoveryBanner.clear(); return; }
        ExploreData data = player.getAttachedOrElse(MiggiExploration.DATA, ExploreData.EMPTY);

        // After joining, the server syncs the whole record at once: don't celebrate old discoveries.
        if (player != lastPlayer) { lastPlayer = player; last = data; settleTicks = 60; return; }
        if (settleTicks > 0) { settleTicks--; last = data; return; }
        if (data.equals(last)) return;

        int newPlaces = 0;
        for (String p : data.places()) if (!last.places().contains(p)) newPlaces++;
        if (newPlaces <= 3) {
            for (String p : data.places()) {
                if (!last.places().contains(p)) DiscoveryBanner.push(
                    Component.translatable("gui.miggi_exploration.new_place"),
                    Component.translatable(biomeKey(p)), false);
            }
        }
        for (String c : data.challenges()) {
            Challenges.Challenge ch = Challenges.byId(c);
            if (ch != null && !last.challenges().contains(c)) DiscoveryBanner.push(
                Component.translatable("gui.miggi_exploration.challenge_done"),
                Component.translatable(ch.nameKey()), true);
        }
        last = data;
    }
}
