package com.miggi.exploration.client.mobs;

import com.miggi.exploration.mobs.FantasyMonster;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.util.Mth;

/**
 * Animated model for the four monsters. Geometry comes from {@link MobLayers} (generated from
 * tools/models/specs.py); this class only poses the parts.
 *
 * <p>Every attack animation is a list of keyframes in ticks. The blow lands on the tick given by the
 * entity's wind-up (see FantasyMonster#windupTicks), so the swing visibly connects at the moment of damage.
 * The first and last keyframe use the part's current pose (walk cycle / rest pose) so attacks blend in and out.
 */
public final class FantasyModel extends EntityModel<FantasyRenderState> {
    public static final int GOBLIN = 0, ORC = 1, BOAR = 2, MINOTAUR = 3;

    private final int kind;
    private final ModelPart head, body;
    // bipeds
    private ModelPart rightArm, leftArm, rightLeg, leftLeg, weapon, jaw, rightEar, leftEar;
    // boar
    private ModelPart frontRight, frontLeft, backRight, backLeft, tail, snout;

    private final PartPose bodyRest;

    public FantasyModel(ModelPart root, int kind) {
        super(root);
        this.kind = kind;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.bodyRest = body.getInitialPose();
        if (kind == BOAR) {
            frontRight = root.getChild("front_right_leg");
            frontLeft = root.getChild("front_left_leg");
            backRight = root.getChild("back_right_leg");
            backLeft = root.getChild("back_left_leg");
            tail = body.getChild("tail");
            snout = head.getChild("snout");
            rightEar = head.getChild("right_ear");
            leftEar = head.getChild("left_ear");
        } else {
            rightArm = root.getChild("right_arm");
            leftArm = root.getChild("left_arm");
            rightLeg = root.getChild("right_leg");
            leftLeg = root.getChild("left_leg");
            weapon = kind == GOBLIN ? rightArm.getChild("club") : rightArm.getChild("axe");
            if (kind == ORC) jaw = head.getChild("jaw");
            if (kind == MINOTAUR) tail = body.getChild("tail");
            if (kind != MINOTAUR) {
                rightEar = head.getChild("right_ear");
                leftEar = head.getChild("left_ear");
            }
        }
    }

    // ------------------------------------------------------------------ keyframes

    /** Smoothly interpolates keyframes {t0, v0, t1, v1, ...} at time t. */
    private static float key(float t, float... kv) {
        if (t <= kv[0]) return kv[1];
        for (int i = 2; i < kv.length; i += 2) {
            if (t <= kv[i]) {
                float a = kv[i - 2], b = kv[i];
                float f = (t - a) / Math.max(1e-4f, b - a);
                f = f * f * (3 - 2 * f);
                return Mth.lerp(f, kv[i - 1], kv[i + 1]);
            }
        }
        return kv[kv.length - 1];
    }

    @Override public void setupAnim(FantasyRenderState s) {
        super.setupAnim(s);                       // resets every part to its rest pose
        float age = s.ageInTicks;
        float pos = s.walkAnimationPos;
        float amt = Mth.clamp(s.walkAnimationSpeed, 0f, 1f);
        head.yRot += s.yRot * Mth.DEG_TO_RAD;
        head.xRot += s.xRot * Mth.DEG_TO_RAD;
        if (kind == BOAR) boar(s, age, pos, amt);
        else biped(s, age, pos, amt);
    }

    // ------------------------------------------------------------------ body-relative parts

    /**
     * Head and arms are separate root parts (so they can swing freely). When the body leans, twists,
     * bobs or lunges, carry them along: rotate their rest offset from the waist by the body's change in
     * pitch and its yaw, then add the same rotation to the part itself.
     */
    private void follow(ModelPart part, float yawShare) {
        PartPose rest = part.getInitialPose();
        float rx = rest.x() - bodyRest.x(), ry = rest.y() - bodyRest.y(), rz = rest.z() - bodyRest.z();
        float dx = body.xRot - bodyRest.xRot(), yr = body.yRot;
        float cx = Mth.cos(dx), sx = Mth.sin(dx), cy = Mth.cos(yr), sy = Mth.sin(yr);
        float y1 = ry * cx - rz * sx, z1 = ry * sx + rz * cx;
        float x2 = rx * cy + z1 * sy, z2 = -rx * sy + z1 * cy;
        part.x = body.x + x2;
        part.y = body.y + y1;
        part.z = body.z + z2;
        part.xRot += dx;
        part.yRot += yr * yawShare;
    }

    // ------------------------------------------------------------------ bipeds

    private void biped(FantasyRenderState s, float age, float pos, float amt) {
        float heavy = kind == GOBLIN ? 1f : kind == ORC ? 0.8f : 0.65f;   // big mobs take longer strides
        float cycle = pos * 0.6662f * heavy;
        float legSwing = Mth.cos(cycle) * 1.1f * amt;
        rightLeg.xRot = legSwing;
        leftLeg.xRot = -legSwing;
        rightArm.xRot += -legSwing * 0.7f;
        leftArm.xRot += legSwing * 0.7f;
        // breathing and arm sway while idle
        float breathe = Mth.sin(age * 0.08f);
        body.xRot += breathe * 0.025f;
        head.xRot += breathe * 0.03f;
        rightArm.zRot += 0.04f + Mth.cos(age * 0.09f) * 0.04f;
        leftArm.zRot -= 0.04f + Mth.cos(age * 0.09f) * 0.04f;
        // heavy body bob while walking
        body.y += Math.abs(Mth.cos(cycle)) * amt * (kind == GOBLIN ? 0.6f : 1.2f);
        if (rightEar != null) {
            rightEar.zRot += Mth.sin(age * 0.13f) * 0.06f;
            leftEar.zRot -= Mth.sin(age * 0.13f + 1f) * 0.06f;
        }
        if (tail != null) {
            tail.zRot += Mth.sin(age * 0.1f) * 0.18f;
            tail.xRot += Mth.sin(age * 0.07f) * 0.08f;
        }

        if (s.aggressive && s.animation == FantasyMonster.ANIM_NONE) {
            if (kind == GOBLIN) {                 // zombie-style reach with both arms
                rightArm.xRot = -1.35f + Mth.sin(age * 0.2f) * 0.08f + legSwing * 0.2f;
                leftArm.xRot = -1.25f + Mth.cos(age * 0.2f) * 0.08f - legSwing * 0.2f;
                weapon.xRot -= 0.5f;
            } else {                              // weapon held ready across the body
                rightArm.xRot = -0.55f + legSwing * 0.2f;
                rightArm.yRot = 0.25f;
                weapon.xRot -= 0.35f;
            }
        }

        float t = s.animationTicks;
        switch (s.animation) {
            case FantasyMonster.GOBLIN_SWING -> goblinSwing(t);
            case FantasyMonster.ORC_CHOP -> orcChop(t);
            case FantasyMonster.ORC_SLASH -> slash(t, 5, 7, 9, 16, 1.2f, 0.55f);
            case FantasyMonster.MINO_SWIPE -> slash(t, 6, 8, 10, 16, 1.35f, 0.65f);
            case FantasyMonster.MINO_CHARGE_WINDUP -> chargeWindup(t, age);
            case FantasyMonster.MINO_CHARGE -> charge(age);
            case FantasyMonster.MINO_SMASH_WINDUP -> smashWindup(t);
            case FantasyMonster.MINO_SMASH -> smash(t);
            case FantasyMonster.MINO_STUN -> stun(t);
            default -> {}
        }
        follow(head, 0.6f);
        follow(rightArm, 1f);
        follow(leftArm, 1f);
    }

    private void goblinSwing(float t) {          // 10 ticks, hit on tick 4
        float a = rightArm.xRot, w = weapon.xRot;
        rightArm.xRot = key(t, 0, a, 3, -2.7f, 4.5f, -0.15f, 7, -0.5f, 10, a);
        rightArm.zRot += key(t, 0, 0, 3, 0.35f, 4.5f, -0.1f, 10, 0);
        weapon.xRot = key(t, 0, w, 3, 0.1f, 4.5f, 0.5f, 10, w);
        body.yRot = key(t, 0, 0, 3, 0.3f, 4.5f, -0.3f, 10, 0);
        head.xRot += key(t, 0, 0, 4, 0.2f, 10, 0);
    }

    private void orcChop(float t) {              // 18 ticks, hit on tick 10: two-handed overhead chop
        float ra = rightArm.xRot, la = leftArm.xRot, w = weapon.xRot;
        rightArm.xRot = key(t, 0, ra, 8, -3.0f, 10, -0.35f, 12, 0.05f, 18, ra);
        leftArm.xRot = key(t, 0, la, 8, -2.7f, 10, -0.6f, 12, -0.2f, 18, la);
        rightArm.zRot += key(t, 0, 0, 8, -0.25f, 10, 0, 18, 0);
        leftArm.zRot += key(t, 0, 0, 8, 0.35f, 10, 0.1f, 18, 0);
        weapon.xRot = key(t, 0, w, 6, 0.15f, 10, 0.55f, 18, w);
        body.xRot += key(t, 0, 0, 8, -0.2f, 10, 0.35f, 13, 0.3f, 18, 0);
        head.xRot += key(t, 0, 0, 8, -0.25f, 10, 0.25f, 18, 0);
        if (jaw != null) jaw.xRot += key(t, 0, 0, 8, 0.1f, 10, 0.35f, 14, 0.1f, 18, 0);
    }

    /** Horizontal sweep from the right to the left. windup / hit / follow-through / end in ticks. */
    private void slash(float t, float wind, float hit, float follow, float end, float reach, float twist) {
        float ra = rightArm.xRot, w = weapon.xRot;
        rightArm.xRot = key(t, 0, ra, wind, -1.45f, follow, -1.35f, end, ra);
        rightArm.yRot = key(t, 0, 0, wind, reach, hit, -0.15f, follow, -reach * 0.85f, end, 0);
        weapon.xRot = key(t, 0, w, wind, 0.05f, follow, 0.15f, end, w);
        weapon.zRot = key(t, 0, 0, wind, -0.4f, hit, 0, follow, 0.3f, end, 0);
        body.yRot = key(t, 0, 0, wind, twist, hit, 0, follow, -twist, end, 0);
        leftArm.xRot += key(t, 0, 0, wind, -0.4f, follow, 0.3f, end, 0);
        if (jaw != null) jaw.xRot += key(t, 0, 0, hit, 0.3f, end, 0);
    }

    private void chargeWindup(float t, float age) {   // 20 ticks: lower the horns and paw the ground
        float lower = key(t, 0, 0, 6, 1, 20, 1);
        head.xRot += 0.65f * lower;
        body.xRot += 0.28f * lower;
        rightArm.xRot += 0.35f * lower;
        leftArm.xRot += 0.35f * lower;
        if (t > 5 && t < 18) rightLeg.xRot = -0.25f + Mth.sin((t - 5) * 1.1f) * 0.45f;   // scraping hoof
        head.zRot += Mth.sin(age * 0.9f) * 0.04f * lower;                              // snorting
    }

    private void charge(float age) {             // full gallop with the head down
        float c = age * 1.25f;
        head.xRot += 0.7f;
        body.xRot += 0.38f;
        rightLeg.xRot = Mth.cos(c) * 1.1f;
        leftLeg.xRot = -Mth.cos(c) * 1.1f;
        rightArm.xRot = -Mth.cos(c) * 0.9f + 0.2f;
        leftArm.xRot = Mth.cos(c) * 0.9f + 0.2f;
        weapon.xRot += 0.2f;
    }

    private void smashWindup(float t) {           // 20 ticks: both hands raise the axe over the head
        float ra = rightArm.xRot, la = leftArm.xRot, w = weapon.xRot;
        rightArm.xRot = key(t, 0, ra, 12, -3.05f, 20, -3.15f);
        leftArm.xRot = key(t, 0, la, 12, -2.8f, 20, -2.9f);
        rightArm.zRot += key(t, 0, 0, 12, -0.2f, 20, -0.2f);
        leftArm.zRot += key(t, 0, 0, 12, 0.3f, 20, 0.3f);
        weapon.xRot = key(t, 0, w, 10, -0.05f, 20, -0.1f);
        body.xRot += key(t, 0, 0, 12, -0.3f, 20, -0.36f);
        head.xRot += key(t, 0, 0, 12, -0.25f, 20, -0.3f);
    }

    private void smash(float t) {                 // 14 ticks: the axe slams down on tick 0..3
        float ra = rightArm.xRot, la = leftArm.xRot, w = weapon.xRot;
        rightArm.xRot = key(t, 0, -3.15f, 3, -0.1f, 7, 0.1f, 14, ra);
        leftArm.xRot = key(t, 0, -2.9f, 3, -0.35f, 7, -0.1f, 14, la);
        rightArm.zRot += key(t, 0, -0.2f, 3, 0, 14, 0);
        leftArm.zRot += key(t, 0, 0.3f, 3, 0.05f, 14, 0);
        weapon.xRot = key(t, 0, -0.1f, 3, 0.6f, 8, 0.7f, 14, w);
        body.xRot += key(t, 0, -0.36f, 3, 0.48f, 8, 0.4f, 14, 0);
        head.xRot += key(t, 0, -0.3f, 3, 0.35f, 14, 0);
        body.y += key(t, 0, 0, 3, 2.0f, 8, 1.5f, 14, 0);
    }

    private void stun(float t) {                  // dazed after hitting a wall
        float sway = Mth.sin(t * 0.45f);
        head.xRot += 0.35f;
        head.zRot += sway * 0.28f;
        head.yRot += Mth.cos(t * 0.3f) * 0.2f;
        body.xRot += 0.22f;
        body.zRot += sway * 0.06f;
        rightArm.xRot = 0.25f;
        leftArm.xRot = 0.25f;
        rightArm.zRot += 0.3f;
        leftArm.zRot -= 0.3f;
        weapon.xRot += 0.4f;
    }

    // ------------------------------------------------------------------ boar

    private void boar(FantasyRenderState s, float age, float pos, float amt) {
        float c = pos * 0.9f;
        float swing = Mth.cos(c) * 1.0f * amt;
        frontRight.xRot = swing;
        backLeft.xRot = swing;
        frontLeft.xRot = -swing;
        backRight.xRot = -swing;
        body.y += Math.abs(Mth.sin(c)) * amt * 0.8f;
        head.xRot += Mth.sin(age * 0.07f) * 0.04f + (s.aggressive ? 0.25f : 0f);
        tail.zRot += Mth.sin(age * 0.35f) * 0.3f;
        rightEar.zRot += Mth.sin(age * 0.2f) * 0.1f;
        leftEar.zRot -= Mth.sin(age * 0.2f + 0.7f) * 0.1f;
        snout.xRot += Mth.sin(age * 0.5f) * 0.02f;    // sniffing

        if (s.animation == FantasyMonster.BOAR_RAM) { // 12 ticks, hit on tick 5: dip, then toss the head up
            float t = s.animationTicks;
            float h = head.xRot;
            head.xRot = key(t, 0, h, 3, 0.8f, 5, -0.5f, 7, -0.4f, 12, h);
            body.xRot += key(t, 0, 0, 3, 0.1f, 5, -0.1f, 12, 0);
            body.z += key(t, 0, 0, 3, 1.0f, 5, -2.0f, 8, -1.0f, 12, 0);
            frontRight.xRot += key(t, 0, 0, 3, -0.45f, 5, 0.35f, 12, 0);
            frontLeft.xRot += key(t, 0, 0, 3, -0.45f, 5, 0.35f, 12, 0);
            backRight.xRot += key(t, 0, 0, 3, 0.3f, 5, -0.4f, 12, 0);
            backLeft.xRot += key(t, 0, 0, 3, 0.3f, 5, -0.4f, 12, 0);
        }
        follow(head, 0.5f);
    }
}
