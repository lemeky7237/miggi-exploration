package com.miggi.exploration;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * 미끼탐험모드 (Miggi Exploration).
 * - Every biome you walk into is recorded in your exploration journal (J key); new ones pop a scroll banner.
 * - Challenges (도전과제) unlock titles (칭호).
 *
 * Progress lives on the server as a player attachment that is saved with the player and
 * synced to that player's client automatically; the client only reads it.
 */
public class MiggiExploration implements ModInitializer {
    public static final String ID = "miggi_exploration";
    public static Identifier id(String name) { return Identifier.fromNamespaceAndPath(ID, name); }

    public static final AttachmentType<ExploreData> DATA = AttachmentRegistry.create(id("exploration"), b -> b
        .persistent(ExploreData.CODEC)
        .copyOnDeath()
        .initializer(() -> ExploreData.EMPTY)
        .syncWith(ExploreData.STREAM_CODEC, AttachmentSyncPredicate.targetOnly()));

    @Override public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 10 != 0) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!player.isSpectator()) update(player);
            }
        });
    }

    private static void update(ServerPlayer player) {
        ExploreData before = player.getAttachedOrElse(DATA, ExploreData.EMPTY);
        ExploreData data = before;

        String biome = player.level().getBiome(player.blockPosition()).getRegisteredName();
        if (biome.contains(":") && !data.places().contains(biome)) data = data.withPlace(biome);

        for (Challenges.Challenge c : Challenges.ALL) {
            if (!data.challenges().contains(c.id()) && c.done(data)) data = data.withChallenge(c.id());
        }
        if (data != before) player.setAttached(DATA, data);
    }
}
