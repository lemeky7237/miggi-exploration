package com.miggi.exploration;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Function;

/**
 * 미끼탐험모드 (Miggi Exploration).
 * - Every biome you walk into is recorded in your exploration journal; new ones pop a scroll banner.
 * - Challenges (도전과제) unlock titles (칭호).
 * - Seven treasures hide in structure chests.
 */
public class MiggiExploration implements ModInitializer {
    public static final String ID = "miggi_exploration";
    public static Identifier id(String name) { return Identifier.fromNamespaceAndPath(ID, name); }

    /** Set by the client entrypoint; opens the journal screen. No-op on a dedicated server. */
    public static Runnable openJournal = () -> {};

    public static final AttachmentType<ExploreData> DATA = AttachmentRegistry.create(id("exploration"), b -> b
        .persistent(ExploreData.CODEC)
        .copyOnDeath()
        .initializer(() -> ExploreData.EMPTY)
        .syncWith(ExploreData.STREAM_CODEC, AttachmentSyncPredicate.targetOnly()));

    private static Item item(String name, Function<Item.Properties, Item> factory, Item.Properties props) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(props.setId(key)));
    }
    private static Item treasure(String name, Rarity rarity) {
        return item(name, Item::new, new Item.Properties().stacksTo(64).rarity(rarity));
    }

    // ---- Treasures ----
    public static final Item ANCIENT_COIN = treasure("ancient_coin", Rarity.UNCOMMON);
    public static final Item PEARL_SHELL = treasure("pearl_shell", Rarity.UNCOMMON);
    public static final Item SKY_CRYSTAL = treasure("sky_crystal", Rarity.RARE);
    public static final Item ABYSS_CRYSTAL = treasure("abyss_crystal", Rarity.EPIC);
    public static final Item BLOSSOM_CHARM = treasure("blossom_charm", Rarity.UNCOMMON);
    public static final Item SPIRIT_LEAF = treasure("spirit_leaf", Rarity.RARE);
    public static final Item GLOW_MUSHROOM = treasure("glow_mushroom", Rarity.RARE);
    public static final List<Item> TREASURES = List.of(ANCIENT_COIN, PEARL_SHELL, SKY_CRYSTAL, ABYSS_CRYSTAL,
        BLOSSOM_CHARM, SPIRIT_LEAF, GLOW_MUSHROOM);

    public static final Item JOURNAL = item("exploration_journal", JournalItem::new, new Item.Properties().stacksTo(1));

    public static class JournalItem extends Item {
        public JournalItem(Properties props) { super(props); }
        @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (level.isClientSide()) openJournal.run();
            return InteractionResult.SUCCESS;
        }
    }

    @Override public void onInitialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> {
            for (Item t : TREASURES) output.accept(t);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> output.accept(JOURNAL));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 10 != 0) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!player.isSpectator()) update(player);
            }
        });

        LootTableEvents.MODIFY_DROPS.register((holder, context, drops) -> TreasureLoot.addTreasures(holder.getRegisteredName(), context.getRandom(), drops));
    }

    private static void update(ServerPlayer player) {
        ExploreData before = player.getAttachedOrElse(DATA, ExploreData.EMPTY);
        ExploreData data = before;

        String biome = player.level().getBiome(player.blockPosition()).getRegisteredName();
        if (biome.contains(":") && !data.places().contains(biome)) data = data.withPlace(biome);

        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty() || !TREASURES.contains(stack.getItem())) continue;
            String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if (!data.treasures().contains(itemId)) data = data.withTreasure(itemId);
        }

        for (Challenges.Challenge c : Challenges.ALL) {
            if (!data.challenges().contains(c.id()) && c.done(data)) data = data.withChallenge(c.id());
        }
        if (data != before) player.setAttached(DATA, data);
    }
}
