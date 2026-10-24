package com.miggi.exploration.client;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/** Original procedural cuboid meshes (256x256 texture atlases). Not borrowed from other mods. */
public class FantasyMobModel extends EntityModel<FantasyRenderState> {
  private final ModelPart head, leftArm, rightArm, leftLeg, rightLeg;
  private final ModelPart frontLeft, frontRight, backLeft, backRight;
  private final boolean quadruped;
  public FantasyMobModel(ModelPart root, boolean quadruped) {
    super(root); this.quadruped=quadruped;
    head = find(root,"head"); leftArm=find(root,"left_arm"); rightArm=find(root,"right_arm");
    leftLeg=find(root,"left_leg"); rightLeg=find(root,"right_leg");
    frontLeft=find(root,"front_left"); frontRight=find(root,"front_right");
    backLeft=find(root,"back_left"); backRight=find(root,"back_right");
  }
  private static ModelPart find(ModelPart root,String name) { return root.hasChild(name)?root.getChild(name):null; }
  private static void cube(PartDefinition root, String name, float w,float h,float d,
                           float x,float y,float z,int tile) {
    // Each tile uses one 64x64 area of the texture atlas.
    int u=(tile%4)*64, v=(tile/4)*64;
    root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u,v)
      .addBox(-w/2f,-h/2f,-d/2f,w,h,d), PartPose.offset(x,y,z));
  }
  @Override public void setupAnim(FantasyRenderState state) {
    super.setupAnim(state);
    float speed=state.walkAnimationSpeed, pos=state.walkAnimationPos;
    if(head!=null){head.xRot=state.xRot*Mth.DEG_TO_RAD; head.yRot=state.yRot*Mth.DEG_TO_RAD;}
    if(quadruped) {
      if(frontLeft!=null)frontLeft.xRot=Mth.cos(pos*.55f)*.7f*speed;
      if(frontRight!=null)frontRight.xRot=Mth.cos(pos*.55f+Mth.PI)*.7f*speed;
      if(backLeft!=null)backLeft.xRot=Mth.cos(pos*.55f+Mth.PI)*.7f*speed;
      if(backRight!=null)backRight.xRot=Mth.cos(pos*.55f)*.7f*speed;
    } else {
      if(leftLeg!=null)leftLeg.xRot=Mth.cos(pos*.55f)*.8f*speed;
      if(rightLeg!=null)rightLeg.xRot=Mth.cos(pos*.55f+Mth.PI)*.8f*speed;
      if(leftArm!=null)leftArm.xRot=Mth.cos(pos*.55f+Mth.PI)*.5f*speed;
      if(rightArm!=null)rightArm.xRot=Mth.cos(pos*.55f)*.5f*speed;
    }
  }

  public static LayerDefinition goblinLayer() {
    MeshDefinition mesh=new MeshDefinition(); PartDefinition root=mesh.getRoot();
    cube(root,"body",10f,11f,6f,0f,12f,0f,1);
    cube(root,"head",9f,8f,8f,0f,2f,0f,0);
    cube(root,"nose",3f,2f,3f,0f,4f,-5f,0);
    cube(root,"ear_l",5f,2f,2f,-7f,3f,0f,0);
    cube(root,"ear_r",5f,2f,2f,7f,3f,0f,0);
    cube(root,"eye_l",2f,2f,1f,-2f,4f,-4.7f,4);
    cube(root,"eye_r",2f,2f,1f,2f,4f,-4.7f,4);
    cube(root,"belt",11f,2f,7f,0f,17f,0f,6);
    cube(root,"left_arm",4f,11f,4f,-7f,13f,0f,0);
    cube(root,"right_arm",4f,11f,4f,7f,13f,0f,0);
    cube(root,"left_leg",4f,7f,4f,-3f,21f,0f,0);
    cube(root,"right_leg",4f,7f,4f,3f,21f,0f,0);
    cube(root,"club",3f,12f,3f,9f,18f,-3f,6);
    return LayerDefinition.create(mesh,256,256); }
  public static LayerDefinition orcLayer() {
    MeshDefinition mesh=new MeshDefinition(); PartDefinition root=mesh.getRoot();
    cube(root,"body",16f,18f,9f,0f,2f,0f,1);
    cube(root,"head",12f,11f,11f,0f,-8f,-1f,0);
    cube(root,"jaw",11f,4f,9f,0f,-3f,-4f,0);
    cube(root,"tusk_l",2f,4f,2f,-4f,-7f,-8f,5);
    cube(root,"tusk_r",2f,4f,2f,4f,-7f,-8f,5);
    cube(root,"eye_l",2f,2f,1f,-4f,-10f,-7f,4);
    cube(root,"eye_r",2f,2f,1f,4f,-10f,-7f,4);
    cube(root,"shoulder_l",7f,8f,11f,-12f,-1f,0f,2);
    cube(root,"shoulder_r",7f,8f,11f,12f,-1f,0f,2);
    cube(root,"left_arm",6f,17f,6f,-12f,6f,0f,0);
    cube(root,"right_arm",6f,17f,6f,12f,6f,0f,0);
    cube(root,"left_leg",7f,13f,7f,-5f,17f,0f,3);
    cube(root,"right_leg",7f,13f,7f,5f,17f,0f,3);
    cube(root,"axe_handle",2f,28f,2f,16f,0f,-3f,6);
    cube(root,"axe_blade",11f,12f,3f,12f,-12f,-3f,7);
    return LayerDefinition.create(mesh,256,256); }
  public static LayerDefinition wild_boarLayer() {
    MeshDefinition mesh=new MeshDefinition(); PartDefinition root=mesh.getRoot();
    cube(root,"body",19f,17f,27f,0f,9f,0f,0);
    cube(root,"mane",15f,5f,17f,0f,-1f,1f,3);
    cube(root,"head",16f,13f,14f,0f,7f,-17f,0);
    cube(root,"snout",11f,7f,8f,0f,11f,-27f,2);
    cube(root,"nose_l",2f,2f,1f,-3f,11f,-31f,3);
    cube(root,"nose_r",2f,2f,1f,3f,11f,-31f,3);
    cube(root,"eye_l",2f,2f,1f,-7f,4f,-21f,4);
    cube(root,"eye_r",2f,2f,1f,7f,4f,-21f,4);
    cube(root,"tusk_l",3f,10f,4f,-9f,9f,-26f,5);
    cube(root,"tusk_r",3f,10f,4f,9f,9f,-26f,5);
    cube(root,"ear_l",6f,5f,3f,-10f,-2f,-17f,0);
    cube(root,"ear_r",6f,5f,3f,10f,-2f,-17f,0);
    cube(root,"front_left",5f,12f,5f,-7f,19f,-9f,3);
    cube(root,"front_right",5f,12f,5f,7f,19f,-9f,3);
    cube(root,"back_left",5f,12f,5f,-7f,19f,9f,3);
    cube(root,"back_right",5f,12f,5f,7f,19f,9f,3);
    return LayerDefinition.create(mesh,256,256); }
  public static LayerDefinition minotaurLayer() {
    MeshDefinition mesh=new MeshDefinition(); PartDefinition root=mesh.getRoot();
    cube(root,"body",23f,27f,12f,0f,-2f,0f,0);
    cube(root,"chest_plate",24f,12f,14f,0f,-9f,-1f,2);
    cube(root,"head",17f,16f,17f,0f,-15f,-2f,0);
    cube(root,"muzzle",12f,9f,12f,0f,-11f,-14f,3);
    cube(root,"nose_l",2f,2f,1f,-3f,-11f,-20f,3);
    cube(root,"nose_r",2f,2f,1f,3f,-11f,-20f,3);
    cube(root,"horn_l",5f,14f,5f,-12f,-22f,-2f,5);
    cube(root,"horn_r",5f,14f,5f,12f,-22f,-2f,5);
    cube(root,"horn_tip_l",3f,6f,3f,-17f,-26f,-2f,5);
    cube(root,"horn_tip_r",3f,6f,3f,17f,-26f,-2f,5);
    cube(root,"eye_l",3f,2f,1f,-6f,-17f,-11f,4);
    cube(root,"eye_r",3f,2f,1f,6f,-17f,-11f,4);
    cube(root,"left_arm",8f,24f,9f,-17f,1f,0f,0);
    cube(root,"right_arm",8f,24f,9f,17f,1f,0f,0);
    cube(root,"left_leg",9f,20f,9f,-8f,15f,0f,3);
    cube(root,"right_leg",9f,20f,9f,8f,15f,0f,3);
    cube(root,"loincloth",14f,12f,3f,0f,8f,-6f,1);
    cube(root,"axe_handle",3f,36f,3f,20f,-3f,-3f,6);
    cube(root,"axe_blade",17f,17f,4f,14f,-16f,-3f,7);
    return LayerDefinition.create(mesh,256,256); }
}