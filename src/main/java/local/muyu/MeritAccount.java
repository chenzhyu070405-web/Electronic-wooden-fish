package local.muyu;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.math.BigInteger;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import local.muyu.Balance.Gift;

/** An immutable snapshot. Inventory and attachments are written in the same player save. */
public record MeritAccount(BigInteger merit, BigInteger peak, BigInteger ascensions, long lastAscension,
                           Map<Gift, BigInteger> history, Map<Gift, BigInteger> pending, Optional<Rite> rite) {
    public static final MeritAccount EMPTY = new MeritAccount(BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO,
            0L, Map.of(), Map.of(), Optional.empty());
    static final Codec<BigInteger> COUNT = Codec.STRING.comapFlatMap(text -> {
        try {
            BigInteger value = new BigInteger(text);
            if (value.signum() >= 0) return DataResult.success(value);
        } catch (NumberFormatException ignored) {}
        return DataResult.error(() -> "Expected a nonnegative decimal integer");
    }, BigInteger::toString);
    static final Codec<Gift> GIFT = Codec.STRING.comapFlatMap(id -> {
        for (Gift gift : Gift.values()) if (gift.id.equals(id)) return DataResult.success(gift);
        return DataResult.error(() -> "Unknown blessing: " + id);
    }, gift -> gift.id);
    private static final Codec<Map<Gift, BigInteger>> GIFTS = Codec.unboundedMap(GIFT, COUNT);
    public static final Codec<MeritAccount> CODEC = RecordCodecBuilder.create(i -> i.group(
            COUNT.fieldOf("merit").forGetter(MeritAccount::merit),
            COUNT.fieldOf("peak").forGetter(MeritAccount::peak),
            COUNT.fieldOf("ascensions").forGetter(MeritAccount::ascensions),
            Codec.LONG.fieldOf("last_ascension").forGetter(MeritAccount::lastAscension),
            GIFTS.fieldOf("history").forGetter(MeritAccount::history),
            GIFTS.fieldOf("pending").forGetter(MeritAccount::pending),
            Rite.CODEC.optionalFieldOf("rite").forGetter(MeritAccount::rite)
    ).apply(i, MeritAccount::new));

    public record Rite(String id, String dimension, long origin, String line, Gift gift, boolean blessed) {
        static final Codec<Rite> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(Rite::id), Codec.STRING.fieldOf("dimension").forGetter(Rite::dimension),
                Codec.LONG.fieldOf("origin").forGetter(Rite::origin), Codec.STRING.fieldOf("line").forGetter(Rite::line),
                GIFT.fieldOf("gift").forGetter(Rite::gift), Codec.BOOL.fieldOf("blessed").forGetter(Rite::blessed)
        ).apply(i, Rite::new));
        Rite bless() { return new Rite(id, dimension, origin, line, gift, true); }
    }

    public MeritAccount {
        history = Map.copyOf(history);
        pending = Map.copyOf(pending);
    }

    public MeritAccount earn(int amount) {
        if (amount < 0) throw new IllegalArgumentException("Negative merit");
        BigInteger sum = merit.add(BigInteger.valueOf(amount));
        return new MeritAccount(sum, peak.max(sum), ascensions, lastAscension, history, pending, rite);
    }

    public MeritAccount begin(Rite started) { return rite.isPresent() ? this : withRite(started); }
    public MeritAccount bless() {
        return rite.isPresent() && !rite.get().blessed() ? withRite(rite.get().bless()) : this;
    }
    private MeritAccount withRite(Rite current) {
        return new MeritAccount(merit, peak, ascensions, lastAscension, history, pending, Optional.of(current));
    }

    public MeritAccount finish(long time) {
        if (rite.isEmpty() || !rite.get().blessed()) return this;
        Gift gift = rite.get().gift();
        return new MeritAccount(merit, peak, ascensions.add(BigInteger.ONE), time, change(history, gift, 1),
                change(pending, gift, 1), Optional.empty());
    }

    public MeritAccount delivered(Gift gift) {
        if (!pending.containsKey(gift)) return this;
        return new MeritAccount(merit, peak, ascensions, lastAscension, history, change(pending, gift, -1), rite);
    }

    private static Map<Gift, BigInteger> change(Map<Gift, BigInteger> balances, Gift gift, int amount) {
        var copy = new EnumMap<Gift, BigInteger>(Gift.class);
        copy.putAll(balances);
        var next = copy.getOrDefault(gift, BigInteger.ZERO).add(BigInteger.valueOf(amount));
        if (next.signum() == 0) copy.remove(gift);
        else copy.put(gift, next);
        return copy;
    }
}
