package com.miggi.exploration.client;

import com.miggi.exploration.MiggiExploration;
import com.miggi.exploration.mobs.ModMobs;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;

/** Dedicated voxel renderers. Register the layers BEFORE the renderers. */
public final class MonsterRenderers {
    private MonsterRenderers() {}
    public static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(MiggiExploration.id(name),"main");
    }
    public static void initialize() {
        ModelLayerRegistry.registerModelLayer(layer("goblin"), FantasyMobModel::goblinLayer);
        ModelLayerRegistry.registerModelLayer(layer("orc"), FantasyMobModel::orcLayer);
        ModelLayerRegistry.registerModelLayer(layer("wild_boar"), FantasyMobModel::wild_boarLayer);
        ModelLayerRegistry.registerModelLayer(layer("minotaur"), FantasyMobModel::minotaurLayer);
        EntityRendererRegistry.register(ModMobs.GOBLIN, c -> new FantasyMobRenderer<>(c,"goblin",false,.3f));
        EntityRendererRegistry.register(ModMobs.ORC, c -> new FantasyMobRenderer<>(c,"orc",false,.6f));
        EntityRendererRegistry.register(ModMobs.WILD_BOAR, c -> new FantasyMobRenderer<>(c,"wild_boar",true,.7f));
        EntityRendererRegistry.register(ModMobs.MINOTAUR, c -> new FantasyMobRenderer<>(c,"minotaur",false,1.0f));
    }
}
