package local.muyu;

import java.math.BigInteger;
import java.util.List;
import net.minecraft.util.RandomSource;

/** Percentages are expressed in units of one hit per ten thousand. */
public final class Balance {
    public static final int ROLL_SIZE = 10_000;
    public static final int HIT_COOLDOWN = 6;
    public static final int EVENT_COOLDOWN = 120;
    public static final int EVENT_CHANCE = 90;
    public static final int BONUS_MERIT = 10;
    public static final int AUSPICIOUS_XP = 4_000;
    public static final int BELL_COOLDOWN = 1_200;
    public static final int ECHO_COUNT = 4;
    public static final int ECHO_INTERVAL = 7;
    public static final int STRIKE_TICKS = 3;

    public static final int TEMPLE = 80, SPEAK = 110, BLESS = 172, POINT = 228, FINALE = 268;
    // The first seven IDs are also stored in unfinished ceremonies from 1.0.0.
    public static final List<String> DIALOGUE = List.of(
            "arrival", "seen", "kind", "complete", "blessing", "amitabha", "receipt", "lotus_pool",
            "radiance", "recollection", "lotus_birth", "mind_eye", "brow", "faith", "bliss");
    public static final List<String> VERSES = List.of(
            "diamond", "heart", "dhamma", "appearances", "dream", "unborn", "nirvana", "few_wishes",
            "enough", "no_contest", "bare_earth", "water", "honesty", "pure_land", "kindness");

    public static String randomVerse(RandomSource random) {
        return VERSES.get(random.nextInt(VERSES.size()));
    }

    public enum Tier {
        MORTAL(0, 0, 3), NOVICE(100, 1, 5), FULFILLED(500, 3, 8),
        AWAKENED(2000, 8, 10), BUDDHA(5000, 15, 12), BOUNDLESS(10000, 30, 16);

        public final BigInteger minimum;
        public final int chance, particles;

        Tier(int minimum, int chance, int particles) {
            this.minimum = BigInteger.valueOf(minimum);
            this.chance = chance;
            this.particles = particles;
        }

        public String key() { return "muyu.tier." + name().toLowerCase(java.util.Locale.ROOT); }
        public boolean ascends(int roll) { return roll >= 0 && roll < chance; }
        public Tier next() { return ordinal() + 1 < values().length ? values()[ordinal() + 1] : null; }
        public static Tier at(BigInteger merit) {
            Tier result = MORTAL;
            for (Tier tier : values()) {
                if (merit.compareTo(tier.minimum) < 0) break;
                result = tier;
            }
            return result;
        }
    }

    public interface Weighted { int weight(); }

    public enum Gift implements Weighted {
        APPLE("buddhas_apple", 47), SHARD("divine_shard", 30),
        BELL("destiny_bell", 18), SWORD("godslayer", 5);
        public final String id;
        private final int weight;
        Gift(String id, int weight) { this.id = id; this.weight = weight; }
        public int weight() { return weight; }
    }

    public enum Omen implements Weighted {
        BONUS(16), HALO(30), EXPERIENCE(3), VERSE(32), LIGHTNING(2), ECHO(17);
        private final int weight;
        Omen(int weight) { this.weight = weight; }
        public int weight() { return weight; }
    }

    public static <T extends Weighted> T draw(T[] pool, RandomSource random) {
        int total = 0;
        for (T entry : pool) total += entry.weight();
        return pick(pool, random.nextInt(total));
    }

    static <T extends Weighted> T pick(T[] pool, int roll) {
        if (roll < 0) throw new IllegalArgumentException("Negative roll");
        for (T entry : pool) {
            if (roll < entry.weight()) return entry;
            roll -= entry.weight();
        }
        throw new IllegalArgumentException("Roll outside the pool");
    }

    private Balance() {}
}
