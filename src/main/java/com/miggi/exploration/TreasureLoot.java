package com.miggi.exploration;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Puts treasures into vanilla structure chests (and any modded chest whose id matches). */
public final class TreasureLoot {
    private TreasureLoot() {}

    private record Rule(String match, Item item, float chance, int max) {}

    private static List<Rule> rules() {
        return List.of(
            new Rule("desert_pyramid", MiggiExploration.ANCIENT_COIN, .45f, 3),
            new Rule("jungle_temple", MiggiExploration.ANCIENT_COIN, .45f, 3),
            new Rule("buried_treasure", MiggiExploration.ANCIENT_COIN, .60f, 4),
            new Rule("ruined_portal", MiggiExploration.ANCIENT_COIN, .25f, 2),
            new Rule("village/", MiggiExploration.ANCIENT_COIN, .15f, 1),

            new Rule("shipwreck", MiggiExploration.PEARL_SHELL, .40f, 2),
            new Rule("underwater_ruin", MiggiExploration.PEARL_SHELL, .45f, 2),
            new Rule("buried_treasure", MiggiExploration.PEARL_SHELL, .50f, 2),

            new Rule("igloo_chest", MiggiExploration.SKY_CRYSTAL, .60f, 2),
            new Rule("abandoned_mineshaft", MiggiExploration.SKY_CRYSTAL, .20f, 1),
            new Rule("pillager_outpost", MiggiExploration.SKY_CRYSTAL, .30f, 1),

            new Rule("ancient_city", MiggiExploration.ABYSS_CRYSTAL, .35f, 1),
            new Rule("stronghold", MiggiExploration.ABYSS_CRYSTAL, .20f, 1),
            new Rule("end_city", MiggiExploration.ABYSS_CRYSTAL, .25f, 1),

            new Rule("village/", MiggiExploration.BLOSSOM_CHARM, .20f, 1),
            new Rule("woodland_mansion", MiggiExploration.BLOSSOM_CHARM, .35f, 2),
            new Rule("trial_chambers", MiggiExploration.BLOSSOM_CHARM, .15f, 1),

            new Rule("jungle_temple", MiggiExploration.SPIRIT_LEAF, .40f, 2),
            new Rule("woodland_mansion", MiggiExploration.SPIRIT_LEAF, .30f, 1),
            new Rule("village_taiga", MiggiExploration.SPIRIT_LEAF, .25f, 1),

            new Rule("abandoned_mineshaft", MiggiExploration.GLOW_MUSHROOM, .25f, 2),
            new Rule("simple_dungeon", MiggiExploration.GLOW_MUSHROOM, .35f, 2),
            new Rule("ancient_city", MiggiExploration.GLOW_MUSHROOM, .30f, 2),
            new Rule("bastion", MiggiExploration.GLOW_MUSHROOM, .20f, 1)
        );
    }
    private static List<Rule> RULES;

    public static void addTreasures(String tableId, RandomSource random, List<ItemStack> drops) {
        if (tableId == null || !tableId.contains("chests/")) return;
        if (RULES == null) RULES = rules();
        for (Rule r : RULES) {
            if (tableId.contains(r.match()) && random.nextFloat() < r.chance()) {
                drops.add(new ItemStack(r.item(), 1 + random.nextInt(r.max())));
            }
        }
    }
}
