package com.miggi.exploration.client.mobs;

import com.miggi.exploration.MiggiExploration;
import com.miggi.exploration.mobs.FantasyMonster;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public final class FantasyMobRenderer<T extends FantasyMonster> extends MobRenderer<T,FantasyRenderState,FantasyModel> {
    private final Identifier texture;
    private final int kind;
    public FantasyMobRenderer(EntityRendererProvider.Context context,ModelLayerLocation layer,String name,int kind,float shadow) {
        super(context,new FantasyModel(context.bakeLayer(layer),kind),shadow);
        this.texture=MiggiExploration.id("textures/entity/"+name+".png");
        this.kind=kind;
    }
    @Override public FantasyRenderState createRenderState(){ return new FantasyRenderState(); }
    @Override public void extractRenderState(T entity,FantasyRenderState state,float partialTick) {
        super.extractRenderState(entity,state,partialTick);
        state.animation=entity.currentAnim(partialTick);
        state.animationTicks=entity.animTicks(partialTick);
        state.aggressive=entity.isAggressive();
    }
    @Override public Identifier getTextureLocation(FantasyRenderState state){ return texture; }
}
