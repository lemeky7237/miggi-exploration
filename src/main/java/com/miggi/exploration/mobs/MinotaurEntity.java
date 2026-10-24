package com.miggi.exploration.mobs;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Labyrinth boss. Three attacks:
 * <ul>
 *   <li>basic: one-handed axe swipe (vanilla melee timing, wind-up 8 ticks)</li>
 *   <li>charge: paws the ground, then rushes in a straight line; slams into walls and is stunned</li>
 *   <li>smash: raises the axe over its head and brings it down, hitting everything in front</li>
 * </ul>
 * The two special attacks live in {@link MinotaurAbilityGoal}. No natural spawning: it is meant to be
 * placed by a future labyrinth structure (or /summon).
 *
 * <p>Boss extension points: {@link #onBossDefeated} (rewards) and a boss health bar can be added here later.
 */
public class MinotaurEntity extends FantasyMonster {
    public int chargeCooldown = 60;
    public int smashCooldown = 40;

    public MinotaurEntity(EntityType<? extends MinotaurEntity> type, Level level) {
        super(type, level);
        this.xpReward = 50;
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 300.0)
            .add(Attributes.MOVEMENT_SPEED, 0.28)
            .add(Attributes.ATTACK_DAMAGE, 14.0)
            .add(Attributes.ARMOR, 10.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.ATTACK_KNOCKBACK, 1.5)
            .add(Attributes.FOLLOW_RANGE, 40.0)
            .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override protected double attackReach() { return 2.6; }
    @Override public int rpgXp() { return 500; }
    @Override protected int chooseAttackAnim() { return MINO_SWIPE; }
    @Override protected int windupTicks(int anim) { return 8; }

    @Override protected void registerExtraGoals() {
        goalSelector.addGoal(1, new MinotaurAbilityGoal(this));
    }

    @Override protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (chargeCooldown > 0) chargeCooldown--;
        if (smashCooldown > 0) smashCooldown--;
    }

    /** A boss never despawns. */
    @Override public boolean removeWhenFarAway(double distanceToClosestPlayer) { return false; }

    /** Called once when a player kills the minotaur. Boss rewards go here later. */
    public void onBossDefeated(ServerPlayer killer) {}
}
