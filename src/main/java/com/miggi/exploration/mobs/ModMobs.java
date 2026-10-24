package com.miggi.exploration.mobs;

import com.miggi.exploration.MiggiExploration;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

import java.util.function.Supplier;

/**
 * Monster registration. Hitbox sizes are width x height in blocks.
 * No natural spawning and no spawn eggs yet: use /summon.
 */
public final class ModMobs {
    private ModMobs() {}

    private static <T extends FantasyMonster> EntityType<T> register(String name, EntityType.EntityFactory<T> factory,
            Supplier<AttributeSupplier.Builder> attributes, float width, float height) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, MiggiExploration.id(name));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key,
            FabricEntityType.Builder.createMob(factory, MobCategory.MONSTER, mob -> mob.defaultAttributes(attributes))
                .sized(width, height)
                .build(key));
    }

    public static final EntityType<GoblinEntity> GOBLIN =
        register("goblin", GoblinEntity::new, GoblinEntity::attributes, 0.6f, 1.35f);
    public static final EntityType<OrcEntity> ORC =
        register("orc", OrcEntity::new, OrcEntity::attributes, 1.0f, 2.35f);
    public static final EntityType<WildBoarEntity> WILD_BOAR =
        register("wild_boar", WildBoarEntity::new, WildBoarEntity::attributes, 1.2f, 1.5f);
    public static final EntityType<MinotaurEntity> MINOTAUR =
        register("minotaur", MinotaurEntity::new, MinotaurEntity::attributes, 1.8f, 3.2f);

    /** Touch the class so the static registrations run during mod init. */
    public static void initialize() {}
}
