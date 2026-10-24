package com.miggi.exploration.mobs;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** The minotaur's two special attacks: charge and overhead axe smash. Runs instead of plain melee. */
public class MinotaurAbilityGoal extends Goal {
    private enum Phase { NONE, CHARGE_WINDUP, CHARGING, STUNNED, SMASH_WINDUP, RECOVER }

    private static final int CHARGE_COOLDOWN = 160, SMASH_COOLDOWN = 100;
    private static final double CHARGE_SPEED = 0.62;

    private final MinotaurEntity mob;
    private Phase phase = Phase.NONE;
    private boolean nextIsSmash;
    private int timer;
    private Vec3 dir = Vec3.ZERO;

    public MinotaurAbilityGoal(MinotaurEntity mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override public boolean canUse() {
        LivingEntity t = mob.getTarget();
        if (t == null || !t.isAlive() || !mob.onGround()) return false;
        double d = mob.distanceTo(t);
        if (mob.smashCooldown <= 0 && d < 4.5) { nextIsSmash = true; return true; }
        if (mob.chargeCooldown <= 0 && d > 7 && d < 22 && mob.getSensing().hasLineOfSight(t)) { nextIsSmash = false; return true; }
        return false;
    }

    @Override public boolean canContinueToUse() { return phase != Phase.NONE; }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public boolean isInterruptable() { return false; }

    @Override public void start() {
        mob.getNavigation().stop();
        if (nextIsSmash) {
            phase = Phase.SMASH_WINDUP;
            timer = 20;
            mob.smashCooldown = SMASH_COOLDOWN;
            mob.playAnim(FantasyMonster.MINO_SMASH_WINDUP);
        } else {
            phase = Phase.CHARGE_WINDUP;
            timer = 20;
            mob.chargeCooldown = CHARGE_COOLDOWN;
            mob.playAnim(FantasyMonster.MINO_CHARGE_WINDUP);
        }
    }

    @Override public void stop() {
        phase = Phase.NONE;
        mob.setDeltaMovement(mob.getDeltaMovement().multiply(0.2, 1, 0.2));
    }

    private float damage(float factor) {
        return (float) mob.getAttributeValue(Attributes.ATTACK_DAMAGE) * factor;
    }

    private void face(Vec3 d) {
        float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90f;
        mob.setYRot(yaw);
        mob.setYBodyRot(yaw);
        mob.setYHeadRot(yaw);
    }

    @Override public void tick() {
        if (!(mob.level() instanceof ServerLevel level)) return;
        LivingEntity t = mob.getTarget();
        switch (phase) {
            case CHARGE_WINDUP -> {
                mob.getNavigation().stop();
                if (t != null) mob.getLookControl().setLookAt(t, 30f, 30f);
                if (--timer <= 0) {
                    Vec3 aim = t != null ? t.position() : mob.position().add(mob.getLookAngle());
                    dir = aim.subtract(mob.position()).multiply(1, 0, 1);
                    dir = dir.lengthSqr() < 1e-4 ? mob.getLookAngle().multiply(1, 0, 1).normalize() : dir.normalize();
                    phase = Phase.CHARGING;
                    timer = 40;
                    mob.playAnim(FantasyMonster.MINO_CHARGE);
                }
            }
            case CHARGING -> {
                face(dir);
                mob.setDeltaMovement(dir.x * CHARGE_SPEED, mob.getDeltaMovement().y, dir.z * CHARGE_SPEED);
                AABB box = mob.getBoundingBox().inflate(0.6);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != mob && e.isAlive())) {
                    if (e != t && !(e instanceof Player)) continue;
                    if (e.hurtServer(level, mob.damageSources().mobAttack(mob), damage(1.6f))) mob.hitsLanded++;
                    e.push(dir.x * 1.6, 0.6, dir.z * 1.6);
                    phase = Phase.RECOVER;
                    timer = 12;
                    return;
                }
                if (mob.horizontalCollision) {      // ran into a wall: dazed
                    level.sendParticles(ParticleTypes.CRIT, mob.getX() + dir.x * 1.5, mob.getY() + 2.5, mob.getZ() + dir.z * 1.5, 12, 0.6, 0.6, 0.6, 0.1);
                    phase = Phase.STUNNED;
                    timer = 40;
                    mob.playAnim(FantasyMonster.MINO_STUN);
                    return;
                }
                if (--timer <= 0) { phase = Phase.RECOVER; timer = 10; }
            }
            case SMASH_WINDUP -> {
                mob.getNavigation().stop();
                if (t != null) mob.getLookControl().setLookAt(t, 30f, 30f);
                if (--timer <= 0) smash(level);
            }
            case STUNNED, RECOVER -> {
                mob.getNavigation().stop();
                if (--timer <= 0) phase = Phase.NONE;
            }
            default -> phase = Phase.NONE;
        }
    }

    private void smash(ServerLevel level) {
        mob.playAnim(FantasyMonster.MINO_SMASH);
        Vec3 fwd = Vec3.directionFromRotation(0, mob.getYRot());
        Vec3 c = mob.position().add(fwd.scale(2.2));
        AABB area = new AABB(c.x - 3, c.y - 1, c.z - 3, c.x + 3, c.y + 3, c.z + 3);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != mob && e.isAlive())) {
            if (e.distanceToSqr(c) > 3.5 * 3.5) continue;
            if (e.hurtServer(level, mob.damageSources().mobAttack(mob), damage(2.0f))) mob.hitsLanded++;
            Vec3 away = e.position().subtract(mob.position()).multiply(1, 0, 1).normalize();
            e.push(away.x * 1.0, 0.7, away.z * 1.0);
        }
        level.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.2, c.z, 3, 1.0, 0.1, 1.0, 0.0);
        level.sendParticles(ParticleTypes.CLOUD, c.x, c.y + 0.2, c.z, 20, 1.5, 0.1, 1.5, 0.05);
        mob.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.0f, 0.7f);
        phase = Phase.RECOVER;
        timer = 14;
    }
}
