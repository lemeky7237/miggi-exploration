package com.miggi.exploration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * One player's exploration progress. Lists keep discovery order (newest last).
 *
 * @param places     biome ids discovered, e.g. "minecraft:river"
 * @param challenges challenge ids completed
 */
public record ExploreData(List<String> places, List<String> challenges) {
    public static final ExploreData EMPTY = new ExploreData(List.of(), List.of());

    public static final Codec<ExploreData> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.listOf().fieldOf("places").forGetter(ExploreData::places),
        Codec.STRING.listOf().fieldOf("challenges").forGetter(ExploreData::challenges)
    ).apply(i, ExploreData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ExploreData> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), ExploreData::places,
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), ExploreData::challenges,
        ExploreData::new);

    private static List<String> plus(List<String> list, String value) {
        List<String> copy = new ArrayList<>(list);
        copy.add(value);
        return List.copyOf(copy);
    }

    public ExploreData withPlace(String id) { return new ExploreData(plus(places, id), challenges); }
    public ExploreData withChallenge(String id) { return new ExploreData(places, plus(challenges, id)); }

    public long placesMatching(String part) {
        return places.stream().filter(p -> p.contains(part)).count();
    }
}
