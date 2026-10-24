package com.miggi.exploration.client.test;

import com.miggi.exploration.MiggiExploration;
import com.miggi.exploration.RpgData;
import com.miggi.exploration.RpgSystem;
import com.miggi.exploration.client.JournalScreen;
import com.miggi.exploration.mobs.FantasyMonster;
import com.miggi.exploration.mobs.ModMobs;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Automated in-game check, run on GitHub Actions with a real (software-rendered) client:
 * screenshots of every monster and attack animation, every structure placed with /place,
 * plus small combat and RPG-XP checks. Results go to miggi-test-report.txt and screenshots/.
 * Only runs when the game is started with -Dfabric.client.gametest.
 */
public class MiggiClientGameTest implements FabricClientGameTest {
    private final List<String> report = new ArrayList<>();

    private void log(String line) {
        System.out.println("[MIGGI-TEST] " + line);
        report.add(line);
    }

    @Override public void runTest(ClientGameTestContext ctx) {
        try (TestSingleplayerContext sp = ctx.worldBuilder()
                .adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
                .create()) {
            TestServerContext server = sp.getServer();
            ctx.getInput().resizeWindow(1280, 720);
            sp.getConnection().waitForChunksRender();
            for (String rule : new String[]{"send_command_feedback", "sendCommandFeedback", "log_admin_commands", "logAdminCommands"}) {
                cmd(server, "gamerule " + rule + " false");
            }
            cmd(server, "time set noon");
            cmd(server, "weather clear");
            cmd(server, "difficulty normal");

            section("exploration ui", () -> {
                ctx.waitTicks(25);
                ctx.takeScreenshot("00_join_hud_banner");
                ctx.setScreen(JournalScreen::new);
                ctx.waitTicks(5);
                ctx.takeScreenshot("01_journal");
                ctx.setScreen(() -> null);
                ctx.waitTicks(2);
            });

            mobShots(ctx, sp, server, "goblin", ModMobs.GOBLIN, 3.2, 1.0, 16,
                new int[]{FantasyMonster.GOBLIN_SWING}, new int[][]{{2, 5}});
            mobShots(ctx, sp, server, "orc", ModMobs.ORC, 4.6, 1.6, 6,
                new int[]{FantasyMonster.ORC_CHOP, FantasyMonster.ORC_SLASH}, new int[][]{{5, 11}, {4, 8}});
            mobShots(ctx, sp, server, "wild_boar", ModMobs.WILD_BOAR, 4.6, 1.8, 16,
                new int[]{FantasyMonster.BOAR_RAM}, new int[][]{{3, 6}});
            mobShots(ctx, sp, server, "minotaur", ModMobs.MINOTAUR, 7.5, 2.6, -8,
                new int[]{FantasyMonster.MINO_SWIPE, FantasyMonster.MINO_CHARGE_WINDUP, FantasyMonster.MINO_CHARGE,
                          FantasyMonster.MINO_SMASH_WINDUP, FantasyMonster.MINO_SMASH, FantasyMonster.MINO_STUN},
                new int[][]{{4, 9}, {10}, {8}, {14}, {3}, {12}});

            combat(ctx, sp, server);
            structures(ctx, sp, server);
            showcase(ctx, sp, server);
        } catch (Throwable t) {
            log("FAIL flat world: " + t);
        }
        section("natural world: arcane forest + structures", () -> naturalWorld(ctx));
        try {
            log("done");
        } catch (Throwable t) {
            log("FATAL " + t);
            throw t;
        } finally {
            try {
                Files.write(FabricLoader.getInstance().getGameDir().resolve("miggi-test-report.txt"), report);
            } catch (IOException e) {
                System.out.println("[MIGGI-TEST] could not write report: " + e);
            }
        }
    }

    private interface Body { void run() throws Exception; }

    private void section(String name, Body body) {
        try {
            body.run();
            log("OK   " + name);
        } catch (Throwable t) {
            log("FAIL " + name + " : " + t);
        }
    }

    /** Runs a command through the dispatcher so failures are reported instead of only logged. */
    private int cmd(TestServerContext server, String command) {
        return server.computeOnServer(s -> {
            try {
                int r = s.getCommands().getDispatcher().execute(command, s.createCommandSourceStack());
                log("cmd ok  (" + r + ") /" + command);
                return r;
            } catch (Exception e) {
                log("cmd ERR /" + command + " -> " + e.getMessage());
                return -1;
            }
        });
    }

    private static <T extends FantasyMonster> List<T> find(MinecraftServer s, EntityType<T> type) {
        ServerLevel level = s.overworld();
        return new ArrayList<>(level.getEntities(type, e -> e.isAlive()));
    }

    /** Removes every non-player entity without death animations, particles or drops. */
    private void clear(TestServerContext server) {
        server.runOnServer(s -> {
            List<net.minecraft.world.entity.Entity> all = new ArrayList<>();
            s.overworld().getAllEntities().forEach(all::add);
            for (var e : all) if (!(e instanceof ServerPlayer)) e.discard();
        });
    }

    // ------------------------------------------------------------------ monsters

    private void mobShots(ClientGameTestContext ctx, TestSingleplayerContext sp, TestServerContext server, String name,
                          EntityType<? extends FantasyMonster> type, double dist, double gap, int pitch,
                          int[] anims, int[][] frames) {
        section("mob " + name, () -> {
            clear(server);
            cmd(server, "tp @p 0.5 -60 0.5 0 " + pitch);
            String id = "miggi_exploration:" + name;
            double z = 0.5 + dist;
            cmd(server, String.format("summon %s %.2f -60 %.2f {NoAI:1b,PersistenceRequired:1b,Rotation:[180f,0f]}", id, 0.5 - gap, z));
            cmd(server, String.format("summon %s %.2f -60 %.2f {NoAI:1b,PersistenceRequired:1b,Rotation:[90f,0f]}", id, 0.5 + gap, z));
            sp.getConnection().waitForClientboundEntityUpdates(type);
            ctx.waitTicks(15);
            ctx.takeScreenshot("mob_" + name + "_idle");

            double[] box = server.computeOnServer(s -> {
                var list = find(s, type);
                return list.isEmpty() ? new double[]{-1, -1} : new double[]{list.get(0).getBbWidth(), list.get(0).getBbHeight()};
            });
            log(name + " hitbox width=" + box[0] + " height=" + box[1]);

            for (int i = 0; i < anims.length; i++) {
                int anim = anims[i];
                int waited = 0;
                server.runOnServer(s -> {
                    for (FantasyMonster m : find(s, type)) if (Math.abs(m.getYRot() - 180f) < 1f || Math.abs(m.getYRot() + 180f) < 1f) m.playAnim(anim);
                });
                for (int f : frames[i]) {
                    ctx.waitTicks(Math.max(1, f - waited));
                    waited = f;
                    ctx.takeScreenshot("mob_" + name + "_anim" + anim + "_t" + f);
                }
                ctx.waitTicks(FantasyMonster.animDuration(anim) - waited + 2);
            }

            // walking: one AI-enabled copy walks across the view
            clear(server);
            cmd(server, String.format("summon %s %.2f -60 %.2f {PersistenceRequired:1b,Rotation:[90f,0f]}", id, 0.5 + 4, z));
            sp.getConnection().waitForClientboundEntityUpdates(type);
            ctx.waitTicks(10);   // let it land before asking for a path
            double startX = server.computeOnServer(s -> {
                var list = find(s, type);
                if (list.isEmpty()) return Double.NaN;
                FantasyMonster m = list.get(0);
                boolean ok = m.getNavigation().moveTo(m.getX() - 10, m.getY(), m.getZ(), 1.0);
                log(name + " path found=" + ok + " onGround=" + m.onGround() + " y=" + m.getY());
                return m.getX();
            });
            ctx.waitTicks(14);
            ctx.takeScreenshot("mob_" + name + "_walk");
            double endX = server.computeOnServer(s -> {
                var list = find(s, type);
                return list.isEmpty() ? Double.NaN : list.get(0).getX();
            });
            log(name + " walk moved " + String.format("%.2f", startX - endX) + " blocks in 14 ticks");
            clear(server);
        });
    }

    // ------------------------------------------------------------------ combat + xp

    private void combat(ClientGameTestContext ctx, TestSingleplayerContext sp, TestServerContext server) {
        section("combat + rpg xp", () -> {
            clear(server);
            cmd(server, "tp @p 0.5 -60 0.5 0 10");
            cmd(server, "gamemode survival @p");
            cmd(server, "effect give @p minecraft:resistance 600 3 true");
            cmd(server, "effect give @p minecraft:regeneration 600 2 true");
            String[] names = {"goblin", "orc", "wild_boar", "minotaur"};
            List<EntityType<? extends FantasyMonster>> types = List.of(ModMobs.GOBLIN, ModMobs.ORC, ModMobs.WILD_BOAR, ModMobs.MINOTAUR);
            for (int i = 0; i < names.length; i++) {
                String n = names[i];
                EntityType<? extends FantasyMonster> type = types.get(i);
                double d = n.equals("minotaur") ? 11 : 3.5;
                cmd(server, "effect give @p minecraft:instant_health 1 5 true");
                cmd(server, String.format("summon miggi_exploration:%s 0.5 -60 %.1f {PersistenceRequired:1b}", n, 0.5 + d));
                int ticks = n.equals("minotaur") ? 260 : 160;
                for (int k = 0; k < ticks; k += 40) {
                    ctx.waitTicks(40);
                    String probe = server.computeOnServer(s -> {
                        var list = find(s, type);
                        ServerPlayer p = s.getPlayerList().getPlayers().get(0);
                        if (list.isEmpty()) return "missing";
                        FantasyMonster m = list.get(0);
                        return String.format("dist=%.2f hp(player)=%.1f navDone=%s target=%s aggressive=%s",
                            m.distanceTo(p), p.getHealth(), m.getNavigation().isDone(), m.getTarget() != null, m.isAggressive());
                    });
                    log("  " + n + " t+" + (k + 40) + ": " + probe);
                }
                if (n.equals("minotaur")) ctx.takeScreenshot("combat_minotaur");
                String stats = server.computeOnServer(s -> {
                    var list = find(s, type);
                    if (list.isEmpty()) return "monster missing";
                    FantasyMonster m = list.get(0);
                    StringBuilder sb = new StringBuilder("hitsLanded=" + m.hitsLanded + " anims=");
                    for (int a = 1; a < FantasyMonster.ANIM_COUNT; a++) if (m.animsStarted[a] > 0) sb.append(a).append('x').append(m.animsStarted[a]).append(' ');
                    sb.append("target=").append(m.getTarget() == null ? "none" : m.getTarget().getName().getString());
                    return sb.toString();
                });
                log("combat " + n + ": " + stats);

                // RPG XP for a player kill
                String xp = server.computeOnServer(s -> {
                    ServerPlayer p = s.getPlayerList().getPlayers().get(0);
                    RpgData before = p.getAttachedOrElse(MiggiExploration.RPG_DATA, RpgData.INITIAL);
                    var list = find(s, type);
                    if (list.isEmpty()) return "monster missing";
                    FantasyMonster m = list.get(0);
                    m.hurtServer(s.overworld(), s.overworld().damageSources().playerAttack(p), 100000f);
                    RpgData after = p.getAttachedOrElse(MiggiExploration.RPG_DATA, RpgData.INITIAL);
                    return "dead=" + !m.isAlive() + " rpg before L" + before.level() + "/" + before.xp()
                        + " after L" + after.level() + "/" + after.xp() + " expected +" + m.rpgXp()
                        + " gained=" + (total(after) - total(before));
                });
                log("xp " + n + ": " + xp);
                clear(server);
            }
            cmd(server, "gamemode creative @p");
        });
    }

    private static long total(RpgData d) {
        long t = d.xp();
        for (int l = 1; l < d.level(); l++) t += RpgSystem.xpToNext(l);
        return t;
    }

    // ------------------------------------------------------------------ natural world

    private void naturalWorld(ClientGameTestContext ctx) {
        try (TestSingleplayerContext sp = ctx.worldBuilder()
                .setUseConsistentSettings(false)
                .adjustSettings(c -> {
                    c.setSeed("20261024");
                    c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
                })
                .create()) {
            TestServerContext server = sp.getServer();
            sp.getConnection().waitForChunksRender();
            cmd(server, "gamerule send_command_feedback false");
            cmd(server, "time set noon");
            cmd(server, "gamerule advance_time false");
            int found = cmd(server, "locate biome miggi_exploration:arcane_forest");
            log("biolith loaded=" + FabricLoader.getInstance().isModLoaded("biolith"));
            String where = server.computeOnServer(s -> {
                var level = s.overworld();
                var pos = level.findClosestBiome3d(h -> h.unwrapKey().map(k -> k.toString().contains("miggi_exploration:arcane_forest")).orElse(false),
                    s.getPlayerList().getPlayers().get(0).blockPosition(), 6400, 32, 64);
                return pos == null ? "none" : pos.getFirst().getX() + " " + pos.getFirst().getZ();
            });
            log("arcane forest at: " + where);
            if (!where.equals("none")) {
                String[] xz = where.split(" ");
                cmd(server, "gamemode spectator @p");
                int fx = Integer.parseInt(xz[0]), fz = Integer.parseInt(xz[1]);
                cmd(server, "forceload add " + (fx - 32) + " " + (fz - 32) + " " + (fx + 32) + " " + (fz + 32));
                cmd(server, "tp @p " + fx + " 130 " + (fz - 40) + " 0 35");
                sp.getConnection().waitForChunksRender();
                ctx.waitTicks(40);
                sp.getConnection().waitForChunksRender();
                ctx.takeScreenshot("world_arcane_forest_air");
                int ground = server.computeOnServer(s -> s.overworld().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, fx, fz));
                log("arcane forest ground height " + ground);
                cmd(server, "tp @p " + fx + " " + (ground + 2) + " " + fz + " 30 5");
                sp.getConnection().waitForChunksRender();
                ctx.waitTicks(30);
                ctx.takeScreenshot("world_arcane_forest_ground");
            }
            for (String st : new String[]{"forgotten_ruin", "arcane_grove", "arcane_watchtower"}) {
                cmd(server, "locate structure miggi_exploration:" + st);
            }
        }
    }

    // ------------------------------------------------------------------ structures

    private void structures(ClientGameTestContext ctx, TestSingleplayerContext sp, TestServerContext server) {
        cmd(server, "gamemode spectator @p");
        String[] templates = {"arcane_grove", "arcane_watchtower", "forgotten_ruin", "labyrinth_entrance", "labyrinth_trial", "labyrinth_vault"};
        int x = 400;
        for (String t : templates) {
            int bx = x;
            section("template " + t, () -> {
                clear(server);
                cmd(server, "tp @p " + (bx + 22) + " -30 " + (-26) + " 0 40");
                cmd(server, "forceload add " + (bx - 16) + " -16 " + (bx + 64) + " 64");
                sp.getConnection().waitForChunksRender();
                ctx.waitTicks(10);
                cmd(server, "place template miggi_exploration:" + t + " " + bx + " -60 0");
                ctx.waitTicks(20);
                sp.getConnection().waitForChunksRender();
                ctx.takeScreenshot("struct_template_" + t);
            });
            x += 200;
        }
        String[] structs = {"arcane_grove", "arcane_watchtower", "forgotten_ruin"};
        for (String t : structs) {
            int bx = x;
            section("structure " + t, () -> {
                cmd(server, "tp @p " + (bx + 22) + " -30 " + (-26) + " 0 40");
                cmd(server, "forceload add " + (bx - 64) + " -64 " + (bx + 96) + " 96");
                sp.getConnection().waitForChunksRender();
                ctx.waitTicks(10);
                cmd(server, "place structure miggi_exploration:" + t + " " + bx + " -60 0");
                ctx.waitTicks(20);
                sp.getConnection().waitForChunksRender();
                ctx.takeScreenshot("struct_structure_" + t);
            });
            x += 200;
        }
        cmd(server, "gamemode creative @p");
    }

    // ------------------------------------------------------------------ blocks and items

    private void showcase(ClientGameTestContext ctx, TestSingleplayerContext sp, TestServerContext server) {
        section("blocks + items", () -> {
            clear(server);
            cmd(server, "gamemode creative @p");
            cmd(server, "tp @p 3000.5 -60 -6.5 0 12");
            cmd(server, "forceload add 2990 -16 3020 16");
            sp.getConnection().waitForChunksRender();
            String[] blocks = {"arcane_log", "arcane_log[axis=x]", "ancient_bark", "arcane_planks", "arcane_leaves",
                "rune_stone", "cracked_rune_stone", "mossy_rune_stone", "glowing_rune_stone", "rune_pillar", "rune_altar"};
            for (int i = 0; i < blocks.length; i++) {
                int bx = 2994 + i;
                cmd(server, "setblock " + bx + " -60 0 miggi_exploration:" + blocks[i]);
                cmd(server, "setblock " + bx + " -59 0 miggi_exploration:" + blocks[i]);
            }
            cmd(server, "clear @p");
            String[] items = {"arcane_shard", "goblin_talisman", "orc_fang", "minotaur_horn", "ancient_tablet", "arcane_log", "arcane_leaves", "rune_altar", "glowing_rune_stone"};
            for (String it : items) cmd(server, "give @p miggi_exploration:" + it);
            ctx.waitTicks(20);
            sp.getConnection().waitForChunksRender();
            ctx.takeScreenshot("blocks_and_items");
        });
    }
}
