package com.miggi.exploration;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import com.miggi.exploration.mobs.FantasyMonster;
import com.miggi.exploration.mobs.MinotaurEntity;
import com.miggi.exploration.mobs.ModMobs;
import com.miggi.exploration.mobs.MonsterSpawns;
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

    public static final AttachmentType<RpgData> RPG_DATA = AttachmentRegistry.create(id("rpg"), b -> b
        .persistent(RpgData.CODEC)
        .copyOnDeath()
        .initializer(() -> RpgData.INITIAL)
        .syncWith(RpgData.STREAM_CODEC, AttachmentSyncPredicate.targetOnly()));

    @Override public void onInitialize() {
        ModMobs.initialize();
        MonsterSpawns.register();

        // RPG XP for killing a fantasy monster (arrows etc. count: getEntity() is the shooter).
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof FantasyMonster monster && source.getEntity() instanceof ServerPlayer killer) {
                RpgSystem.addXp(killer, monster.rpgXp());
                if (monster instanceof MinotaurEntity boss) boss.onBossDefeated(killer);
            }
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!alive) {
                RpgData data = newPlayer.getAttachedOrElse(RPG_DATA, RpgData.INITIAL);
                newPlayer.setAttached(RPG_DATA, data.withMana(data.maxMana()));
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long tick = server.getTickCount();
            if (tick % 10 != 0 && tick % RpgSystem.REGEN_INTERVAL_TICKS != 0) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.isSpectator()) continue;
                if (tick % RpgSystem.REGEN_INTERVAL_TICKS == 0) RpgSystem.regenerate(player);
                if (tick % 10 == 0) update(player);
            }
        });
    }

    private static void update(ServerPlayer player) {
        ExploreData before = player.getAttachedOrElse(DATA, ExploreData.EMPTY);
        ExploreData data = before;

        String biome = player.level().getBiome(player.blockPosition()).getRegisteredName();
        if (biome.contains(":") && !data.places().contains(biome)) data = data.withPlace(biome);

        int newlyCompleted = 0;
        for (Challenges.Challenge c : Challenges.ALL) {
            if (!data.challenges().contains(c.id()) && c.done(data)) {
                data = data.withChallenge(c.id());
                newlyCompleted++;
            }
        }
        if (data != before) player.setAttached(DATA, data);
        // Award only for the transition from incomplete to complete. Saved challenge IDs
        // make this idempotent across ticks, reconnects and restarts.
        if (newlyCompleted > 0) RpgSystem.addXp(player, newlyCompleted * RpgSystem.CHALLENGE_REWARD_XP);
    }
}
