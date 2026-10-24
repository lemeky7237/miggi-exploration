package com.miggi.exploration;

import net.minecraft.server.level.ServerPlayer;

/** All RPG mutations happen on the logical server; values are synced through an attachment. */
public final class RpgSystem {
    private RpgSystem() {}
    public static final int LEVEL_CAP = 150;
    public static final int INITIAL_MANA = 100;
    public static final int MANA_PER_LEVEL = 20;
    public static final int REGEN_INTERVAL_TICKS = 4; // 1 MP per 4 ticks = 5 MP/s at 20 TPS
    public static final int CHALLENGE_REWARD_XP = 100; // provisional balance; change here

    public static int xpToNext(int level) {
        return level >= LEVEL_CAP ? 0 : 100 + 25 * (Math.max(1, level) - 1);
    }
    public static int maxMana(int level, int bonusMana) {
        return INITIAL_MANA + (Math.clamp(level, 1, LEVEL_CAP) - 1) * MANA_PER_LEVEL + Math.max(0, bonusMana);
    }
    public static RpgData grantXp(RpgData current, int reward) {
        if (reward <= 0 || current.level() >= LEVEL_CAP) return current;
        int level = current.level();
        long remaining = (long) current.xp() + reward;
        while (level < LEVEL_CAP && remaining >= xpToNext(level)) {
            remaining -= xpToNext(level);
            level++;
        }
        if (level >= LEVEL_CAP) remaining = 0;
        return new RpgData(level, (int) remaining, current.mana(), current.bonusMana());
    }
    public static void addXp(ServerPlayer player, int amount) {
        RpgData before = player.getAttachedOrElse(MiggiExploration.RPG_DATA, RpgData.INITIAL);
        RpgData after = grantXp(before, amount);
        if (!before.equals(after)) player.setAttached(MiggiExploration.RPG_DATA, after);
    }
    /** Returns false without changing data when mana is insufficient. */
    public static boolean spendMana(ServerPlayer player, int cost) {
        if (cost < 0) return false;
        RpgData before = player.getAttachedOrElse(MiggiExploration.RPG_DATA, RpgData.INITIAL);
        if (before.mana() < cost) return false;
        if (cost > 0) player.setAttached(MiggiExploration.RPG_DATA, before.withMana(before.mana() - cost));
        return true;
    }
    /** Hook for future consumables/equipment: persist bonus separately from level-based MP. */
    public static void addBonusMana(ServerPlayer player, int amount) {
        if (amount <= 0) return;
        RpgData before = player.getAttachedOrElse(MiggiExploration.RPG_DATA, RpgData.INITIAL);
        RpgData after = before.withBonusMana(before.bonusMana() + amount);
        player.setAttached(MiggiExploration.RPG_DATA, after);
    }
    public static void regenerate(ServerPlayer player) {
        RpgData before = player.getAttachedOrElse(MiggiExploration.RPG_DATA, RpgData.INITIAL);
        if (before.mana() < before.maxMana()) {
            player.setAttached(MiggiExploration.RPG_DATA, before.withMana(before.mana() + 1));
        }
    }
}
