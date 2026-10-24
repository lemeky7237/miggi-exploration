package com.miggi.exploration.mobs;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** Small, quick green raider. Zombie-style melee: arms raised while hunting, club swing on attack. */
public class GoblinEntity extends FantasyMonster {
    public GoblinEntity(EntityType<? extends GoblinEntity> type, Level level) {
        super(type, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 16.0)
            .add(Attributes.MOVEMENT_SPEED, 0.30)
            .add(Attributes.ATTACK_DAMAGE, 3.0)
            .add(Attributes.ARMOR, 1.0)
            .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override public int rpgXp() { return 15; }
    @Override protected int chooseAttackAnim() { return GOBLIN_SWING; }
    @Override protected int windupTicks(int anim) { return 4; }
    @Override protected double chaseSpeed() { return 1.1; }
}
