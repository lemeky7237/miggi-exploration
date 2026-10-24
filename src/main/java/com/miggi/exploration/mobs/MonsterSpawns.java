package com.miggi.exploration.mobs;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biomes;

/**
 * Conservative Overworld spawning for ordinary monsters only.
 * Biome modifications add weighted entries; the Minotaur is reserved for a future labyrinth.
 * Spawn weights are relative to other hostile mobs, NOT percentages.
 */
public final class MonsterSpawns {
    private MonsterSpawns() {}

    public static void register() {
        // Goblins: temperate forests and dark woods, pairs and small groups.
        BiomeModifications.addSpawn(
            BiomeSelectors.includeByKey(Biomes.FOREST, Biomes.BIRCH_FOREST, Biomes.DARK_FOREST),
            MobCategory.MONSTER, ModMobs.GOBLIN, 28, 1, 3);

        // Orcs: hostile forest patrols; deliberately less common than goblins.
        BiomeModifications.addSpawn(
            BiomeSelectors.includeByKey(Biomes.DARK_FOREST, Biomes.TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA),
            MobCategory.MONSTER, ModMobs.ORC, 11, 1, 2);

        // Wild boars: forests and taiga, using the existing hostile mob AI for now.
        BiomeModifications.addSpawn(
            BiomeSelectors.includeByKey(Biomes.FOREST, Biomes.BIRCH_FOREST, Biomes.TAIGA),
            MobCategory.MONSTER, ModMobs.WILD_BOAR, 22, 1, 3);

        // Do not add Minotaur to the biome spawn lists. It is a labyrinth boss.
    }
}
