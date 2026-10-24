package com.miggi.exploration.client.mobs;

import com.miggi.exploration.mobs.FantasyMonster;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** Four hand-authored cube models with shoulder/hip pivots. Units: 16px per block. */
public final class FantasyModel extends EntityModel<FantasyRenderState> {
    private final ModelPart head, body, leftArm, rightArm, leftLeg, rightLeg;
    private final int kind;
    public FantasyModel(ModelPart root, int kind) {
        super(root);
        this.kind = kind;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.leftArm = root.getChild("left_arm");
        this.rightArm = root.getChild("right_arm");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }
    private static CubeListBuilder cube(int u, int v, float x,float y,float z,float w,float h,float d) {
        return CubeListBuilder.create().texOffs(u,v).addBox(x,y,z,w,h,d);
    }
    private static PartDefinition part(PartDefinition root, String name, CubeListBuilder cubes,float x,float y,float z) {
        return root.addOrReplaceChild(name,cubes,PartPose.offset(x,y,z));
    }
    public static LayerDefinition goblin() { return create(0); }
    public static LayerDefinition orc() { return create(1); }
    public static LayerDefinition boar() { return create(2); }
    public static LayerDefinition minotaur() { return create(3); }
    private static LayerDefinition create(int kind) {
        MeshDefinition mesh = new MeshDefinition(); PartDefinition root = mesh.getRoot();
        if (kind == 2) {
            // Hoglin-style quadruped; hip joints and head hinge are separate.
            part(root,"body",cube(0,0,-7,-7,-12,14,14,24),0,13,0);
            PartDefinition head=part(root,"head",cube(0,42,-6,-7,-8,12,12,10),0,11,-12);
            head.addOrReplaceChild("snout",cube(52,42,-4,-1,-7,8,6,6),PartPose.ZERO);
            head.addOrReplaceChild("left_tusk",cube(80,42,4,-2,-7,3,7,3),PartPose.ZERO);
            head.addOrReplaceChild("right_tusk",cube(80,42,-7,-2,-7,3,7,3),PartPose.ZERO);
            part(root,"left_arm",cube(0,80,-2,0,-2,4,11,5),5,13,-8);
            part(root,"right_arm",cube(0,80,-2,0,-2,4,11,5),-5,13,-8);
            part(root,"left_leg",cube(20,80,-2,0,-2,4,11,5),5,13,8);
            part(root,"right_leg",cube(20,80,-2,0,-2,4,11,5),-5,13,8);
        } else {
            float s=kind==0?21.6f/35f:kind==1?37.6f/35f:51.2f/35f;
            float floor=24f;
            float hip=floor-11*s, shoulder=floor-23*s, neck=floor-26*s;
            part(root,"body",cube(0,0,-6*s,-11*s,-3*s,12*s,13*s,6*s),0,shoulder+11*s,0);
            PartDefinition head=part(root,"head",cube(0,55,-5*s,-9*s,-5*s,10*s,10*s,10*s),0,neck,0);
            if (kind==0) {
                head.addOrReplaceChild("left_ear",cube(48,55,5*s,-6*s,-2*s,5*s,3*s,4*s),PartPose.ZERO);
                head.addOrReplaceChild("right_ear",cube(48,55,-10*s,-6*s,-2*s,5*s,3*s,4*s),PartPose.ZERO);
                head.addOrReplaceChild("nose",cube(72,55,-2*s,-2*s,-7*s,4*s,3*s,3*s),PartPose.ZERO);
            } else if(kind==3) {
                head.addOrReplaceChild("muzzle",cube(48,55,-4*s,-1*s,-10*s,8*s,5*s,7*s),PartPose.ZERO);
                head.addOrReplaceChild("left_horn",cube(0,100,5*s,-12*s,-2*s,4*s,9*s,4*s),PartPose.ZERO);
                head.addOrReplaceChild("right_horn",cube(0,100,-9*s,-12*s,-2*s,4*s,9*s,4*s),PartPose.ZERO);
            } else {
                head.addOrReplaceChild("left_tusk",cube(0,100,4*s,-2*s,-6*s,2*s,4*s,2*s),PartPose.ZERO);
                head.addOrReplaceChild("right_tusk",cube(0,100,-6*s,-2*s,-6*s,2*s,4*s,2*s),PartPose.ZERO);
            }
            part(root,"left_leg",cube(48,0,-3*s,0,-3*s,5*s,11*s,6*s),4*s,hip,0);
            part(root,"right_leg",cube(48,0,-2*s,0,-3*s,5*s,11*s,6*s),-4*s,hip,0);
            part(root,"left_arm",cube(80,0,-2*s,0,-3*s,5*s,12*s,6*s),7*s,shoulder,0);
            PartDefinition weaponArm=part(root,"right_arm",cube(80,0,-3*s,0,-3*s,5*s,12*s,6*s),-7*s,shoulder,0);
            if(kind==1 || kind==3) {
                // Weapon is child of weapon arm: follows the shoulder rotation.
                PartDefinition grip=weaponArm.addOrReplaceChild("axe_handle",cube(68,84,-1*s,-1*s,-1*s,2*s,20*s,2*s),PartPose.offset(-1*s,10*s,-1*s));
                grip.addOrReplaceChild("axe_head",cube(85,84,-6*s,-2*s,-2*s,12*s,7*s,4*s),PartPose.offset(0,14*s,0));
            } else {
                weaponArm.addOrReplaceChild("club",cube(68,84,-2*s,0,-2*s,4*s,14*s,4*s),PartPose.offset(0,9*s,0));
            }
        }
        return LayerDefinition.create(mesh,128,128);
    }
    @Override public void setupAnim(FantasyRenderState state) {
        super.setupAnim(state); // critical: reset each model part to default pose
        float walk=state.walkAnimationPos*0.6662f;
        float amount=Mth.clamp(state.walkAnimationSpeed,0,1);
        float swing=Mth.cos(walk)*amount*0.85f;
        leftLeg.xRot=swing; rightLeg.xRot=-swing;
        leftArm.xRot=-swing*0.6f; rightArm.xRot=swing*0.6f;
        head.yRot=state.yRot*Mth.DEG_TO_RAD;
        head.xRot=state.xRot*Mth.DEG_TO_RAD;
        if(kind==2){
            leftArm.xRot=swing; rightArm.xRot=-swing;
            head.xRot+=state.aggressive?0.2f:0f;
        } else if(state.aggressive) {
            rightArm.xRot-=0.2f;
        }
        int anim=state.animation;
        float t=state.animationTicks;
        float duration=Math.max(1,FantasyMonster.animDuration(anim));
        float p=Mth.clamp(t/duration,0f,1f);
        float strike=(float)Math.sin(Math.PI*Math.min(1f,p*1.5f));
        switch(anim){
            case FantasyMonster.GOBLIN_SWING -> rightArm.xRot=-1.8f+2.3f*p;
            case FantasyMonster.ORC_CHOP -> { rightArm.xRot=-2.5f+3.4f*p; body.yRot=0.15f; }
            case FantasyMonster.ORC_SLASH -> { rightArm.yRot=-1.3f+2.6f*p; rightArm.xRot=-0.6f; body.yRot=-0.35f+0.7f*p; }
            case FantasyMonster.BOAR_RAM -> { head.xRot=-0.35f+0.95f*p; body.xRot=-0.12f*strike; }
            case FantasyMonster.MINO_SWIPE -> { rightArm.yRot=-1.5f+3f*p; rightArm.xRot=-0.7f; body.yRot=-0.5f+p; }
            case FantasyMonster.MINO_CHARGE_WINDUP -> { head.xRot=-0.55f*p; body.xRot=-0.15f*p; }
            case FantasyMonster.MINO_CHARGE -> { head.xRot=-0.55f; body.xRot=-0.12f; leftArm.xRot=-0.6f; rightArm.xRot=-0.6f; }
            case FantasyMonster.MINO_SMASH_WINDUP -> { rightArm.xRot=-2.6f*p; leftArm.xRot=-1.1f*p; }
            case FantasyMonster.MINO_SMASH -> { rightArm.xRot=-2.6f+3.6f*p; body.xRot=0.25f*strike; }
            case FantasyMonster.MINO_STUN -> { head.zRot=0.17f*Mth.sin(t*0.45f); rightArm.xRot=0.35f; leftArm.xRot=0.35f; }
            default -> {}
        }
    }
}
