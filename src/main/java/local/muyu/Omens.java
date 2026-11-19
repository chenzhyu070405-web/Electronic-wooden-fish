package local.muyu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import static local.muyu.Balance.*;

final class Omens {
    static void sound(ServerLevel level, BlockPos pos, Content.Cue cue, float pitch) {
        level.playSound(null, pos, cue.sound, SoundSource.BLOCKS, cue.volume, pitch);
    }

    static void hit(ServerPlayer player, BlockPos pos, Tier tier) {
        var level = player.level();
        WoodenFishBlock.pulse(level, pos);
        sound(level, pos, Content.Cue.KNOCK, 0.94F + tier.ordinal() * 0.045F + level.getRandom().nextFloat() * 0.04F);
        level.sendParticles(tier == Tier.MORTAL ? ParticleTypes.CRIT : ParticleTypes.WAX_ON,
                pos.getX() + 0.5, pos.getY() + 0.75, pos.getZ() + 0.5, tier.particles, 0.22, 0.1, 0.22, 0.06);
        if (tier.ordinal() >= Tier.FULFILLED.ordinal()) {
            level.sendParticles(Content.LOTUS, pos.getX() + 0.5, pos.getY() + 0.82, pos.getZ() + 0.5,
                    tier.ordinal() - 1, 0.27, 0.14, 0.27, 0.025);
        }
        if (tier == Tier.BOUNDLESS) ring(player, 10, 0.85, 0.18);
    }

    static void ring(ServerPlayer p, int points, double radius, double height) {
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            p.level().sendParticles(i % 4 == 0 ? Content.LOTUS : ParticleTypes.WAX_ON,
                    p.getX() + Math.cos(angle) * radius, p.getY() + height, p.getZ() + Math.sin(angle) * radius,
                    1, 0, 0.06, 0, 0.01);
        }
    }

    static void apply(ServerPlayer p, BlockPos pos, Omen omen, Practice.Rhythm rhythm) {
        var level = p.level();
        sound(level, pos, Content.Cue.OMEN, 1.0F);
        String key = "muyu.omen." + omen.name().toLowerCase(java.util.Locale.ROOT);
        p.sendSystemMessage(switch (omen) {
            case BONUS -> Muyu.gold(key, BONUS_MERIT);
            case EXPERIENCE -> Muyu.gold(key, AUSPICIOUS_XP);
            case ECHO -> Muyu.gold(key, ECHO_COUNT);
            default -> Muyu.gold(key);
        });
        switch (omen) {
            case BONUS -> {} // The extra ten are booked with the hit before the ascension roll.
            case HALO -> LocalBloom.start(p);
            case EXPERIENCE -> p.giveExperiencePoints(AUSPICIOUS_XP);
            case VERSE -> p.sendSystemMessage(Component.translatable("muyu.verse." + randomVerse(level.getRandom())));
            case LIGHTNING -> {
                var bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
                if (bolt != null) {
                    bolt.setPos(p.position());
                    bolt.setVisualOnly(true);
                    level.addFreshEntity(bolt);
                    // One real lightning hit on the practitioner; the visual bolt cannot ignite nearby builds.
                    p.thunderHit(level, bolt);
                }
            }
            case ECHO -> {
                rhythm.echoOrigin = pos.immutable();
                rhythm.echoDimension = level.dimension();
                rhythm.echoes = ECHO_COUNT;
                rhythm.nextEcho = level.getGameTime() + ECHO_INTERVAL;
            }
        }
    }
    private Omens() {}
}
