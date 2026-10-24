package com.miggi.exploration.mobs;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

/** Temporary zombie melee AI. Custom heavy axe attack animation is a later milestone. */
public class OrcEntity extends Zombie {
    public OrcEntity(EntityType<? extends OrcEntity> type, Level level) { super(type, level); }
}
