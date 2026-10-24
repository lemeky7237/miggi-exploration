package com.miggi.exploration.mobs;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.level.Level;

/** Hoglin-based collision, knockback and melee behavior. Not a Nether-only creature. */
public class WildBoarEntity extends Hoglin {
    public WildBoarEntity(EntityType<? extends WildBoarEntity> type, Level level) { super(type, level); }
}
