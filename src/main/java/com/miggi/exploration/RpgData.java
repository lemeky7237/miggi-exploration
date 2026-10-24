package com.miggi.exploration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Independent RPG save data; does not alter exploration journals or vanilla XP. */
public record RpgData(int level, int xp, int mana, int bonusMana) {
    public static final RpgData INITIAL = new RpgData(1, 0, 100, 0);
    public static final Codec<RpgData> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.INT.fieldOf("level").forGetter(RpgData::level),
        Codec.INT.fieldOf("xp").forGetter(RpgData::xp),
        Codec.INT.fieldOf("mana").forGetter(RpgData::mana),
        Codec.INT.fieldOf("bonus_mana").forGetter(RpgData::bonusMana)
    ).apply(i, RpgData::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, RpgData> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT, RpgData::level,
        ByteBufCodecs.INT, RpgData::xp,
        ByteBufCodecs.INT, RpgData::mana,
        ByteBufCodecs.INT, RpgData::bonusMana,
        RpgData::new
    );

    public int maxMana() { return RpgSystem.maxMana(level, bonusMana); }
    public RpgData withMana(int value) { return new RpgData(level, xp, Math.clamp(value, 0, maxMana()), bonusMana); }
    public RpgData withBonusMana(int value) {
        int bonus = Math.max(0, value);
        return new RpgData(level, xp, Math.min(mana, RpgSystem.maxMana(level, bonus)), bonus);
    }
}
