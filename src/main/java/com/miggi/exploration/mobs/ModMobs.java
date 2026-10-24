package com.miggi.exploration.mobs;

import com.miggi.exploration.MiggiExploration;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.Ravager;

/** Prototype registration following Fabric's modern ResourceKey-based entity API.
 * No natural spawning or egg registration yet: use /summon for test.
 */
public final class ModMobs {
    private ModMobs() {}
    private static ResourceKey<EntityType<?>> key(String id) {
        return ResourceKey.create(Registries.ENTITY_TYPE, MiggiExploration.id(id));
    }
    private static <T extends Entity> EntityType<T> register(String id, EntityType.Builder<T> builder) {
        ResourceKey<EntityType<?>> key = key(id);
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }
    private static <T extends LivingEntity> EntityType<T> register(String id, EntityType.Builder<T> builder, AttributeSupplier.Builder attributes) {
        EntityType<T> type = register(id, builder);
        FabricDefaultAttributeRegistry.register(type, attributes);
        return type;
    }
    public static final EntityType<GoblinEntity> GOBLIN = register("goblin",
        EntityType.Builder.of(GoblinEntity::new, MobCategory.MONSTER).sized(0.55f, 1.35f),
        Zombie.createAttributes());
    public static final EntityType<OrcEntity> ORC = register("orc",
        EntityType.Builder.of(OrcEntity::new, MobCategory.MONSTER).sized(0.9f, 2.35f),
        Zombie.createAttributes());
    public static final EntityType<WildBoarEntity> WILD_BOAR = register("wild_boar",
        EntityType.Builder.of(WildBoarEntity::new, MobCategory.MONSTER).sized(1.25f, 1.5f),
        Hoglin.createAttributes());
    public static final EntityType<MinotaurEntity> MINOTAUR = register("minotaur",
        EntityType.Builder.of(MinotaurEntity::new, MobCategory.MONSTER).sized(1.75f, 3.2f),
        Ravager.createAttributes());
    public static void initialize() {}
}
