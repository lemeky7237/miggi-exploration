package com.miggi.exploration.mobs;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

/** First-playable AI prototype: inherits vanilla zombie arm-swing melee and target goals. */
public class GoblinEntity extends Zombie {
    public GoblinEntity(EntityType<? extends GoblinEntity> type, Level level) { super(type, level); }
}
