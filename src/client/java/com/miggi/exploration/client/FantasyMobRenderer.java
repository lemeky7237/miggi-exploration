package com.miggi.exploration.client;
import com.miggi.exploration.MiggiExploration;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

public class FantasyMobRenderer<T extends Mob> extends MobRenderer<T, FantasyRenderState, FantasyMobModel> {
    private final Identifier texture;
    public FantasyMobRenderer(EntityRendererProvider.Context context, String name, boolean quad, float shadow) {
        super(context, new FantasyMobModel(context.bakeLayer(MonsterRenderers.layer(name)),quad),shadow);
        texture = MiggiExploration.id("textures/entity/" + name + ".png");
    }
    @Override public FantasyRenderState createRenderState() { return new FantasyRenderState(); }
    @Override public Identifier getTextureLocation(FantasyRenderState state) { return texture; }
}
