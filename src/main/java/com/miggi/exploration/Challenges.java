package com.miggi.exploration;

import java.util.List;
import java.util.Set;
import java.util.function.ToIntFunction;

/** Exploration challenges (도전과제). Shown in the journal; each one grants a title (칭호). */
public final class Challenges {
    private Challenges() {}

    /** @param progress how far the player is; done when progress >= goal */
    public record Challenge(String id, int goal, ToIntFunction<ExploreData> progress) {
        public String nameKey() { return "challenge.miggi_exploration." + id; }
        public String descKey() { return "challenge.miggi_exploration." + id + ".desc"; }
        public String titleKey() { return "challenge.miggi_exploration." + id + ".title"; }
        public int clampedProgress(ExploreData data) { return Math.min(goal, progress.applyAsInt(data)); }
        public boolean done(ExploreData data) { return progress.applyAsInt(data) >= goal; }
    }

    private static final Set<String> NETHER = Set.of("minecraft:nether_wastes", "minecraft:crimson_forest",
        "minecraft:warped_forest", "minecraft:soul_sand_valley", "minecraft:basalt_deltas");
    private static final Set<String> END = Set.of("minecraft:the_end", "minecraft:end_highlands",
        "minecraft:end_midlands", "minecraft:small_end_islands", "minecraft:end_barrens");

    private static int places(ExploreData d) { return d.places().size(); }
    private static int has(ExploreData d, String id) { return d.places().contains(id) ? 1 : 0; }
    private static int count(ExploreData d, String part) { return (int) d.placesMatching(part); }
    private static int countIn(ExploreData d, Set<String> set) { return (int) d.places().stream().filter(set::contains).count(); }

    public static final List<Challenge> ALL = List.of(
        new Challenge("first_step", 1, Challenges::places),
        new Challenge("river", 1, d -> has(d, "minecraft:river") + has(d, "minecraft:frozen_river")),
        new Challenge("wanderer", 10, Challenges::places),
        new Challenge("cherry", 1, d -> has(d, "minecraft:cherry_grove")),
        new Challenge("ocean", 3, d -> count(d, "ocean")),
        new Challenge("peaks", 1, d -> count(d, "peaks")),
        new Challenge("forest", 5, d -> count(d, "forest") + count(d, "taiga") + count(d, "jungle")),
        new Challenge("desert", 2, d -> has(d, "minecraft:desert") + count(d, "badlands")),
        new Challenge("deep", 1, d -> has(d, "minecraft:deep_dark") + has(d, "minecraft:lush_caves") + has(d, "minecraft:dripstone_caves")),
        new Challenge("pathfinder", 25, Challenges::places),
        new Challenge("nether", 3, d -> countIn(d, NETHER)),
        new Challenge("end", 1, d -> countIn(d, END)),
        new Challenge("world_walker", 50, Challenges::places)
    );

    public static Challenge byId(String id) {
        for (Challenge c : ALL) if (c.id().equals(id)) return c;
        return null;
    }
}
