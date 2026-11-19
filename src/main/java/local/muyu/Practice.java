package local.muyu;

import java.math.BigInteger;
import java.util.UUID;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import static local.muyu.Balance.*;

/** All inventory, ledger and simulation mutations run on the integrated game thread. */
public final class Practice {
    private static final AttachmentType<MeritAccount> ACCOUNT = AttachmentRegistry.create(Muyu.id("account"),
            b -> b.initializer(() -> MeritAccount.EMPTY).persistent(MeritAccount.CODEC).copyOnDeath());
    private static final AttachmentType<Rhythm> RHYTHM = AttachmentRegistry.createDefaulted(Muyu.id("rhythm"), Rhythm::new);
    private static final AttachmentType<Boolean> HARDCORE_RETURN = AttachmentRegistry.create(Muyu.id("hardcore_return"),
            b -> b.persistent(com.mojang.serialization.Codec.BOOL).copyOnDeath());

    static final class Rhythm {
        long nextHit, nextEvent, nextEcho;
        int echoes, sceneTick;
        BlockPos echoOrigin;
        ResourceKey<Level> echoDimension;
        boolean rehearse;
    }
    public static MeritAccount account(ServerPlayer p) { return p.getAttachedOrCreate(ACCOUNT); }
    private static void save(ServerPlayer p, MeritAccount a) { p.setAttached(ACCOUNT, a); }
    static boolean owner(ServerPlayer p) { return p.level().getServer().isSingleplayerOwner(p.nameAndId()); }

    static void initialize() {
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            if (!server.isSingleplayer()) return;
            for (var p : server.getPlayerList().getPlayers()) if (owner(p) && p.isAlive()) tick(p);
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
                !(entity instanceof ServerPlayer p) || !locked(p) || source.is(Content.WEST));
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayer p) || source.is(Content.WEST)) return true;
            if (locked(p)) return false;
            if (p.hasAttached(HARDCORE_RETURN)) p.removeAttached(HARDCORE_RETURN);
            return true;
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!alive && owner(newPlayer)) {
                deliver(newPlayer);
                if (!account(newPlayer).pending().isEmpty()) newPlayer.sendSystemMessage(Muyu.gold("muyu.claim.wait"));
            }
        });
        ServerPlayerEvents.LEAVE.register(p -> { if (owner(p)) { LocalScene.clear(); LocalBloom.clear(); } });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> LocalScene.clear());
    }
    private static boolean locked(ServerPlayer p) {
        var ledger = p.getAttached(ACCOUNT);
        return ledger != null && ledger.rite().isPresent();
    }
    public static boolean consumeHardcoreReturn(ServerPlayer p) {
        return owner(p) && Boolean.TRUE.equals(p.removeAttached(HARDCORE_RETURN));
    }

    public static void knock(ServerPlayer p, BlockPos pos) {
        if (!owner(p) || p.isSpectator() || !p.isAlive() || locked(p) || !p.level().getBlockState(pos).is(Content.FISH)) return;
        var r = p.getAttachedOrCreate(RHYTHM);
        var level = p.level();
        long time = level.getGameTime();
        if (time < r.nextHit) return;
        r.nextHit = time + HIT_COOLDOWN;

        Omen omen = null;
        if (time >= r.nextEvent && level.getRandom().nextInt(ROLL_SIZE) < EVENT_CHANCE) {
            omen = draw(Omen.values(), level.getRandom());
            r.nextEvent = time + EVENT_COOLDOWN;
        }
        var previous = account(p);
        var earned = previous.earn(1 + (omen == Omen.BONUS ? BONUS_MERIT : 0));
        Tier tier = Tier.at(earned.merit());
        boolean ascends = tier.chance > 0 && (tier.ascends(level.getRandom().nextInt(ROLL_SIZE)) || r.rehearse);
        if (ascends) {
            var rite = new MeritAccount.Rite(UUID.randomUUID().toString(), level.dimension().identifier().toString(),
                    pos.asLong(), DIALOGUE.get(level.getRandom().nextInt(DIALOGUE.size())), draw(Gift.values(), level.getRandom()), false);
            earned = earned.begin(rite);
            r.sceneTick = 0;
            r.echoes = 0;
            r.rehearse = false;
        }
        save(p, earned);
        Omens.hit(p, pos, tier);
        p.sendOverlayMessage(Muyu.gold("muyu.hit", Component.translatable(tier.key()), earned.merit().toString()));
        if (tier != Tier.at(previous.merit())) p.sendSystemMessage(Muyu.gold("muyu.promoted", Component.translatable(tier.key())));
        if (omen != null && (!ascends || omen == Omen.BONUS)) Omens.apply(p, pos, omen, r);
    }

    static void ringBell(ServerPlayer p, InteractionHand hand) {
        if (!owner(p) || !p.isAlive() || p.isSpectator() || locked(p)) return;
        var stack = p.getItemInHand(hand);
        if (p.getCooldowns().isOnCooldown(stack)) return;
        account(p);
        p.getCooldowns().addCooldown(stack, BELL_COOLDOWN);
        var omen = draw(Omen.values(), p.level().getRandom());
        if (omen == Omen.BONUS) {
            var before = account(p);
            var after = before.earn(BONUS_MERIT);
            save(p, after);
            if (Tier.at(before.merit()) != Tier.at(after.merit()))
                p.sendSystemMessage(Muyu.gold("muyu.promoted", Component.translatable(Tier.at(after.merit()).key())));
        }
        var rhythm = p.getAttachedOrCreate(RHYTHM);
        rhythm.nextEvent = p.level().getGameTime() + EVENT_COOLDOWN;
        Omens.apply(p, p.blockPosition(), omen, rhythm);
    }

    static void rehearse(ServerPlayer p) {
        if (!owner(p) || locked(p)) return;
        var a = account(p);
        if (a.merit().compareTo(BigInteger.valueOf(99)) < 0) save(p, a.earn(99 - a.merit().intValueExact()));
        p.getAttachedOrCreate(RHYTHM).rehearse = true;
        p.sendSystemMessage(Muyu.gold("muyu.rehearse"));
    }

    private static void tick(ServerPlayer p) {
        var a = p.getAttached(ACCOUNT);
        if (a == null) return;
        var rhythm = p.getAttachedOrCreate(RHYTHM);
        if (a.rite().isPresent()) {
            advance(p, a, rhythm);
            return;
        }
        // Offer outstanding gifts first, before nearby ground items take a newly opened slot.
        if (!a.pending().isEmpty()) deliver(p);
        if (rhythm.echoes > 0 && p.level().getGameTime() >= rhythm.nextEcho) {
            if (p.level().dimension().equals(rhythm.echoDimension)) {
                Omens.sound(p.level(), rhythm.echoOrigin, Content.Cue.KNOCK, 1.02F);
                if (p.level().isLoaded(rhythm.echoOrigin)) WoodenFishBlock.pulse(p.level(), rhythm.echoOrigin);
            }
            rhythm.echoes--;
            rhythm.nextEcho = p.level().getGameTime() + ECHO_INTERVAL;
        }
    }

    private static void advance(ServerPlayer p, MeritAccount a, Rhythm rhythm) {
        var rite = a.rite().orElseThrow();
        var previous = LocalScene.current();
        if (previous != null && previous.riteId().equals(rite.id()) && !LocalScene.wasPresented(previous)) return;
        int t = rhythm.sceneTick;
        if (t >= FINALE) {
            finish(p, a);
            return;
        }
        if (t == 0) {
            p.sendSystemMessage(Muyu.gold("muyu.ascent"));
            Omens.sound(p.level(), p.blockPosition(), Content.Cue.ASCENT, 1);
        }
        if (t < TEMPLE && t % 9 == 0) {
            BlockPos pos = BlockPos.of(rite.origin());
            if (!p.level().dimension().identifier().toString().equals(rite.dimension()) || !p.level().isLoaded(pos))
                pos = p.blockPosition();
            WoodenFishBlock.pulse(p.level(), pos);
            Omens.sound(p.level(), pos, Content.Cue.KNOCK, 0.85F + t * 0.006F);
            Omens.ring(p, 30, 1.7 - 0.009 * t, 0.22 + t * 0.023);
        }
        if (t == TEMPLE) Omens.sound(p.level(), p.blockPosition(), Content.Cue.AUDIENCE, 1);
        if (t == BLESS) {
            if (!rite.blessed()) {
                save(p, a.bless());
                p.sendSystemMessage(Muyu.gold("muyu.bestow", p.getName(), Content.GIFTS.get(rite.gift()).getDefaultInstance().getHoverName()));
            }
            Omens.sound(p.level(), p.blockPosition(), Content.Cue.BLESSING, 1);
        }
        if (t == POINT) Omens.sound(p.level(), p.blockPosition(), Content.Cue.PASSAGE, 1);
        LocalScene.show(new LocalScene.Frame(p.getUUID(), rite.id(), t, rite.line(), rite.gift()));
        rhythm.sceneTick++;
    }

    private static void finish(ServerPlayer p, MeritAccount a) {
        var level = p.level();
        var source = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(Content.WEST));
        p.hurtServer(level, source, Float.MAX_VALUE);
        if (!p.isDeadOrDying()) return;
        save(p, a.finish(System.currentTimeMillis()));
        if (level.getServer().isHardcore()) p.setAttached(HARDCORE_RETURN, true);
        LocalScene.clear();
    }

    /** Deliver only into a real slot, even in creative where Inventory.add may discard an overflow. */
    public static void deliver(ServerPlayer p) {
        var a = account(p);
        if (!p.isAlive() || a.rite().isPresent() || a.pending().isEmpty()) return;
        var inventory = p.getInventory();
        for (var gift : a.pending().keySet()) {
            ItemStack stack = new ItemStack(Content.GIFTS.get(gift));
            int merge = inventory.getSlotWithRemainingSpace(stack);
            if (merge >= 0) inventory.getItem(merge).grow(1);
            else {
                int empty = inventory.getFreeSlot();
                if (empty < 0) continue;
                inventory.setItem(empty, stack);
            }
            a = a.delivered(gift);
            save(p, a);
            inventory.setChanged();
            p.sendSystemMessage(Muyu.gold("muyu.delivered", stack.getHoverName()));
        }
    }
    private Practice() {}
}
