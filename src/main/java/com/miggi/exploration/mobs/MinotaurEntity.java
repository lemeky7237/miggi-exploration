package com.miggi.exploration.mobs;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.level.Level;

/** Ravager-based boss combat prototype: roar, charge and melee.
 * Maze-only spawning must be handled by a future maze structure/spawner.
 */
public class MinotaurEntity extends Ravager {
    public MinotaurEntity(EntityType<? extends MinotaurEntity> type, Level level) { super(type, level); }
}
