package com.miggi.exploration.client.mobs;

import com.miggi.exploration.MiggiExploration;
import com.miggi.exploration.mobs.ModMobs;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRenderers;

public final class FantasyRenderers {
    private FantasyRenderers() {}
    public static final ModelLayerLocation GOBLIN = new ModelLayerLocation(MiggiExploration.id("goblin"),"main");
    public static final ModelLayerLocation ORC = new ModelLayerLocation(MiggiExploration.id("orc"),"main");
    public static final ModelLayerLocation BOAR = new ModelLayerLocation(MiggiExploration.id("wild_boar"),"main");
    public static final ModelLayerLocation MINOTAUR = new ModelLayerLocation(MiggiExploration.id("minotaur"),"main");
    public static void register() {
        ModelLayerRegistry.registerModelLayer(GOBLIN,MobLayers::goblin);
        ModelLayerRegistry.registerModelLayer(ORC,MobLayers::orc);
        ModelLayerRegistry.registerModelLayer(BOAR,MobLayers::wildBoar);
        ModelLayerRegistry.registerModelLayer(MINOTAUR,MobLayers::minotaur);
        EntityRenderers.register(ModMobs.GOBLIN,c->new FantasyMobRenderer<>(c,GOBLIN,"goblin",0,0.35f));
        EntityRenderers.register(ModMobs.ORC,c->new FantasyMobRenderer<>(c,ORC,"orc",1,0.65f));
        EntityRenderers.register(ModMobs.WILD_BOAR,c->new FantasyMobRenderer<>(c,BOAR,"wild_boar",2,0.65f));
        EntityRenderers.register(ModMobs.MINOTAUR,c->new FantasyMobRenderer<>(c,MINOTAUR,"minotaur",3,1.1f));
    }
}
