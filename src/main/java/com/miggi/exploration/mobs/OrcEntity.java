package com.miggi.exploration.mobs;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Big axe brute. Alternates between an overhead chop (long wind-up, hits harder through knock-up)
 * and a horizontal slash (shorter, shoves the target sideways).
 */
public class OrcEntity extends FantasyMonster {
    public OrcEntity(EntityType<? extends OrcEntity> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 40.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.ATTACK_DAMAGE, 8.0)
            .add(Attributes.ARMOR, 4.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
            .add(Attributes.FOLLOW_RANGE, 28.0);
    }

    @Override protected double attackReach() { return 1.7; }
    @Override public int rpgXp() { return 40; }
    @Override protected int chooseAttackAnim() { return getRandom().nextBoolean() ? ORC_CHOP : ORC_SLASH; }
    @Override protected int windupTicks(int anim) { return anim == ORC_CHOP ? 10 : 7; }

    @Override protected void onHit(ServerLevel level, LivingEntity target, int anim) {
        Vec3 away = target.position().subtract(position()).multiply(1, 0, 1).normalize();
        if (anim == ORC_CHOP) {
            target.push(away.x * 0.3, 0.35, away.z * 0.3);
        } else {
            // slash sweeps from the orc's right to left: shove the target sideways
            Vec3 side = new Vec3(-away.z, 0, away.x);
            target.push(side.x * 0.7 + away.x * 0.3, 0.15, side.z * 0.7 + away.z * 0.3);
        }
    }
}
