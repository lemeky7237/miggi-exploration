package com.miggi.exploration.mobs;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Four-legged charger. Rams like a hoglin: head dip, then tusks toss the target up and away. */
public class WildBoarEntity extends FantasyMonster {
    public WildBoarEntity(EntityType<? extends WildBoarEntity> type, Level level) {
        super(type, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 30.0)
            .add(Attributes.MOVEMENT_SPEED, 0.30)
            .add(Attributes.ATTACK_DAMAGE, 6.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
            .add(Attributes.ATTACK_KNOCKBACK, 1.0)
            .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override public int rpgXp() { return 20; }
    @Override protected int chooseAttackAnim() { return BOAR_RAM; }
    @Override protected int windupTicks(int anim) { return 5; }
    @Override protected double chaseSpeed() { return 1.3; }

    @Override protected void onHit(ServerLevel level, LivingEntity target, int anim) {
        Vec3 away = target.position().subtract(position()).multiply(1, 0, 1).normalize();
        target.push(away.x * 0.5, 0.45, away.z * 0.5);
        target.hurtMarked = true;
    }
}
