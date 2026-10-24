package com.miggi.exploration;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Dedicated worldgen blocks and collectible item for the Arcane Forest. */
public final class ArcaneContent {
    private ArcaneContent() {}
    private static Block block(String name, BlockBehaviour.Properties settings) {
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, MiggiExploration.id(name));
        Block result = Registry.register(BuiltInRegistries.BLOCK, blockKey, new Block(settings.setId(blockKey)));
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, MiggiExploration.id(name));
        Registry.register(BuiltInRegistries.ITEM, itemKey,
            new BlockItem(result, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
        return result;
    }
    private static Block leaves(String name) {
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, MiggiExploration.id(name));
        Block result = Registry.register(BuiltInRegistries.BLOCK, blockKey,
            new LeavesBlock(AmbientLeavesBlockSoundPlayer.noAmbientSound(),
                BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES).setId(blockKey)));
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, MiggiExploration.id(name));
        Registry.register(BuiltInRegistries.ITEM, itemKey,
            new BlockItem(result, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
        return result;
    }
    private static Item item(String name) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, MiggiExploration.id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key)));
    }
    public static final Block ARCANE_LOG = block("arcane_log", BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS));
    public static final Block ARCANE_LEAVES = leaves("arcane_leaves");
    public static final Block RUNE_STONE = block("rune_stone", BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS));
    public static final Block CRACKED_RUNE_STONE = block("cracked_rune_stone", BlockBehaviour.Properties.ofFullCopy(Blocks.CRACKED_STONE_BRICKS));
    public static final Block GLOWING_RUNE_STONE = block("glowing_rune_stone", BlockBehaviour.Properties.ofFullCopy(Blocks.SEA_LANTERN));
    public static final Item ARCANE_SHARD = item("arcane_shard");
    public static final Block ARCANE_PLANKS = block("arcane_planks", BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS));
    public static final Block ANCIENT_BARK = block("ancient_bark", BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS));
    public static final Block MOSSY_RUNE_STONE = block("mossy_rune_stone", BlockBehaviour.Properties.ofFullCopy(Blocks.MOSSY_STONE_BRICKS));
    public static final Block RUNE_PILLAR = block("rune_pillar", BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS));
    public static final Block RUNE_ALTAR = block("rune_altar", BlockBehaviour.Properties.ofFullCopy(Blocks.CHISELED_STONE_BRICKS));
    public static final Item GOBLIN_TALISMAN = item("goblin_talisman");
    public static final Item ORC_FANG = item("orc_fang");
    public static final Item MINOTAUR_HORN = item("minotaur_horn");
    public static final Item ANCIENT_TABLET = item("ancient_tablet");
    public static void initialize() { /* force static registrations during mod init */ }
}
