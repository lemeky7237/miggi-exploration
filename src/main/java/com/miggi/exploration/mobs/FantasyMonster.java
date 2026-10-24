package com.miggi.exploration.mobs;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Shared base for the four fantasy monsters.
 *
 * <p>Combat: the vanilla {@link MeleeAttackGoal} decides <i>when</i> to attack. Instead of hitting at once,
 * {@link #doHurtTarget} starts an attack animation and the blow lands {@link #windupTicks} later, if the
 * target is still in reach. That gives every attack a readable wind-up.
 *
 * <p>Animation sync: the server broadcasts an entity event ({@code ANIM_EVENT_BASE + anim}); the client
 * remembers which animation started on which tick and the renderer turns that into poses.
 */
public abstract class FantasyMonster extends Monster {
    public static final byte ANIM_EVENT_BASE = 100;

    // Animation ids (shared by server and client)
    public static final int ANIM_NONE = 0;
    public static final int GOBLIN_SWING = 1;
    public static final int ORC_CHOP = 2;
    public static final int ORC_SLASH = 3;
    public static final int BOAR_RAM = 4;
    public static final int MINO_SWIPE = 5;
    public static final int MINO_CHARGE_WINDUP = 6;
    public static final int MINO_CHARGE = 7;
    public static final int MINO_SMASH_WINDUP = 8;
    public static final int MINO_SMASH = 9;
    public static final int MINO_STUN = 10;
    public static final int ANIM_COUNT = 11;

    /** How long each animation plays, in ticks. */
    public static int animDuration(int anim) {
        return switch (anim) {
            case GOBLIN_SWING -> 10;
            case ORC_CHOP -> 18;
            case ORC_SLASH -> 16;
            case BOAR_RAM -> 12;
            case MINO_SWIPE -> 16;
            case MINO_CHARGE_WINDUP -> 20;
            case MINO_CHARGE -> 40;
            case MINO_SMASH_WINDUP -> 20;
            case MINO_SMASH -> 14;
            case MINO_STUN -> 40;
            default -> 0;
        };
    }

    // ---- client-side animation clock ----
    private int clientAnim = ANIM_NONE;
    private int clientAnimStart;

    // ---- server-side delayed hit ----
    private LivingEntity pendingTarget;
    private int pendingAnim;
    private int pendingHitTick = -1;

    /** Server-side counters, read by the automated in-game test (and handy for debugging). */
    public final int[] animsStarted = new int[ANIM_COUNT];
    public int hitsLanded;

    protected FantasyMonster(EntityType<? extends FantasyMonster> type, Level level) {
        super(type, level);
    }

    /** RPG XP given to the player who kills this monster. */
    public abstract int rpgXp();

    /** Which attack animation the next melee attack uses. */
    protected abstract int chooseAttackAnim();

    /** Ticks between the start of an attack animation and the moment the blow lands. */
    protected abstract int windupTicks(int anim);

    /** Weapon reach beyond the two bodies touching, in blocks. Big mobs with long weapons reach further. */
    protected double attackReach() { return 0.9; }

    /** Speed multiplier while chasing a target. */
    protected double chaseSpeed() { return 1.0; }

    /** Extra effect after a blow lands (knockback, toss...). */
    protected void onHit(ServerLevel level, LivingEntity target, int anim) {}

    /** Hook for subclasses that need extra goals (e.g. boss abilities). */
    protected void registerExtraGoals() {}

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, chaseSpeed(), true));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        registerExtraGoals();
    }

    // ------------------------------------------------------------------ combat

    @Override public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (pendingHitTick >= 0) return false;               // already winding up a blow
        if (!(target instanceof LivingEntity living)) return super.doHurtTarget(level, target);
        int anim = chooseAttackAnim();
        playAnim(anim);
        int windup = windupTicks(anim);
        if (windup <= 0) return landHit(level, living, anim);
        pendingTarget = living;
        pendingAnim = anim;
        pendingHitTick = tickCount + windup;
        return true;
    }

    protected boolean landHit(ServerLevel level, LivingEntity target, int anim) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit) {
            hitsLanded++;
            onHit(level, target, anim);
        }
        return hit;
    }

    @Override protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (pendingHitTick >= 0 && tickCount >= pendingHitTick) {
            LivingEntity target = pendingTarget;
            pendingHitTick = -1;
            pendingTarget = null;
            // a little extra reach so a target that stepped back during the wind-up can still be hit
            if (target != null && target.isAlive() && inReach(target, 0.6)) {
                landHit(level, target, pendingAnim);
            }
        }
    }

    /**
     * Melee range used by MeleeAttackGoal. Vanilla derives it from the hitbox only, which left the big orc
     * and minotaur standing just out of reach of the player (they reached ~1.8 blocks and stopped swinging).
     */
    @Override public boolean isWithinMeleeAttackRange(LivingEntity target) {
        return inReach(target, 0);
    }

    private boolean inReach(LivingEntity target, double slack) {
        double reach = getBbWidth() * 0.5 + target.getBbWidth() * 0.5 + attackReach() + slack;
        double dx = target.getX() - getX(), dz = target.getZ() - getZ();
        double dy = target.getY() - getY();
        return dx * dx + dz * dz <= reach * reach && dy > -target.getBbHeight() - 0.5 && dy < getBbHeight() + 0.5;
    }

    /** Starts an animation on every client tracking this entity (server side). */
    public void playAnim(int anim) {
        if (level().isClientSide()) return;
        if (anim > 0 && anim < ANIM_COUNT) animsStarted[anim]++;
        level().broadcastEntityEvent(this, (byte) (ANIM_EVENT_BASE + anim));
    }

    @Override public void handleEntityEvent(byte id) {
        if (id >= ANIM_EVENT_BASE && id < ANIM_EVENT_BASE + ANIM_COUNT) {
            clientAnim = id - ANIM_EVENT_BASE;
            clientAnimStart = tickCount;
        } else {
            super.handleEntityEvent(id);
        }
    }

    /** Client: animation currently playing, or {@link #ANIM_NONE} once it has finished. */
    public int currentAnim(float partialTick) {
        if (clientAnim == ANIM_NONE) return ANIM_NONE;
        return animTicks(partialTick) < animDuration(clientAnim) ? clientAnim : ANIM_NONE;
    }

    /** Client: ticks since the current animation started (with partial tick). */
    public float animTicks(float partialTick) {
        return tickCount - clientAnimStart + partialTick;
    }
}
