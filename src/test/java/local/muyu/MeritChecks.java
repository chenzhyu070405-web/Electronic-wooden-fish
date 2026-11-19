package local.muyu;

import com.mojang.serialization.JsonOps;
import java.math.BigInteger;
import java.util.*;
import static local.muyu.Balance.*;

public final class MeritChecks {
    public static void main(String[] args) {
        check(VERSES.size() == 15 && new HashSet<>(VERSES).size() == 15, "15 distinct equiprobable verses");
        var rolls = net.minecraft.util.RandomSource.create(20261118L);
        var expected = net.minecraft.util.RandomSource.create(20261118L);
        var selected = new HashSet<String>();
        for (int i = 0; i < 1024; i++) {
            var verse = randomVerse(rolls);
            check(verse.equals(VERSES.get(expected.nextInt(15))), "one independent uniform draw per verse event");
            selected.add(verse);
        }
        check(selected.size() == 15, "every verse is reachable");
        check(DIALOGUE.size() == 15 && new HashSet<>(DIALOGUE).size() == 15, "15 distinct Buddha lines");
        check(Collections.disjoint(VERSES, DIALOGUE), "separate audiences");
        for (String locale : List.of("zh_cn", "en_us")) verifyLineKeys(locale);
        int[] limits = {0,100,500,2000,5000,10000};
        int[] probabilities = {0,1,3,8,15,30};
        for (int i = 0; i < limits.length; i++) {
            var tier = Tier.values()[i];
            check(Tier.at(BigInteger.valueOf(limits[i])) == tier, "lower threshold");
            if (i > 0) check(Tier.at(BigInteger.valueOf(limits[i] - 1)) == Tier.values()[i - 1], "upper threshold");
            int hits = 0;
            for (int roll = 0; roll < ROLL_SIZE; roll++) if (tier.ascends(roll)) hits++;
            check(hits == probabilities[i], "independent hit probability");
        }
        for (Gift g : Gift.values()) {
            int n = 0;
            for (int i=0; i<100; i++) if (pick(Gift.values(), i) == g) n++;
            check(n == g.weight(), "gift weights");
        }
        for (Omen e : Omen.values()) {
            int n = 0;
            for (int i=0; i<100; i++) if (pick(Omen.values(), i) == e) n++;
            check(n == e.weight(), "omen weights");
        }
        var huge = BigInteger.TEN.pow(90);
        var a = new MeritAccount(huge, huge, BigInteger.ZERO, 0, Map.of(), Map.of(), Optional.empty()).earn(1);
        check(a.peak().equals(huge.add(BigInteger.ONE)) && a.merit().equals(a.peak()), "unbounded merit");
        var rite = new MeritAccount.Rite("test", "minecraft:overworld", 208, "blessing", Gift.SWORD, false);
        var active = roundtrip(a.begin(rite));
        check(active.begin(rite) == active && active.finish(5) == active, "single unblessed event");
        var blessed = roundtrip(active.bless());
        check(blessed.bless().equals(blessed), "idempotent blessing");
        var done = roundtrip(blessed.finish(1234));
        check(done.rite().isEmpty() && done.lastAscension() == 1234 && done.finish(888) == done, "death committed once");
        check(done.history().equals(Map.of(Gift.SWORD, BigInteger.ONE)) && done.pending().equals(done.history()), "gift in custody");
        var twice = roundtrip(done.begin(rite).bless().finish(4321));
        check(twice.history().get(Gift.SWORD).intValueExact() == 2 && twice.ascensions().intValueExact() == 2, "repeat rewards");
        var delivered = roundtrip(twice.delivered(Gift.SWORD).delivered(Gift.SWORD));
        check(delivered.pending().isEmpty() && delivered.history().equals(twice.history()) && delivered.merit().equals(a.merit()), "inventory receipt");
        check(delivered.delivered(Gift.SWORD) == delivered && roundtrip(MeritAccount.EMPTY).equals(MeritAccount.EMPTY), "empty receipt");
        System.out.println("Verified six rank intervals, all 60,000 probability outcomes, both weighted pools, arbitrary-precision counts and persistent ceremony/escrow transitions.");
    }
    private static MeritAccount roundtrip(MeritAccount a) {
        var json = MeritAccount.CODEC.encodeStart(JsonOps.INSTANCE, a).getOrThrow();
        var restored = MeritAccount.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        check(restored.equals(a), "save round trip");
        return restored;
    }
    private static void verifyLineKeys(String locale) {
        try (var stream = MeritChecks.class.getResourceAsStream("/assets/muyu/lang/" + locale + ".json")) {
            var lang = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(
                    Objects.requireNonNull(stream), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            for (String id : VERSES) check(lang.has("muyu.verse." + id), "missing monk text " + id);
            for (String id : DIALOGUE) check(lang.has("muyu.line." + id), "missing Buddha text " + id);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }
    private static void check(boolean ok, String what) { if (!ok) throw new AssertionError(what); }
}
