package local.muyu.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

final class LotusParticle extends SingleQuadParticle {
    private final float phase, size;
    LotusParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite);
        xd = vx; yd = vy + 0.026; zd = vz;
        lifetime = 26 + random.nextInt(17);
        hasPhysics = false;
        size = 0.10F + random.nextFloat() * 0.08F;
        phase = random.nextFloat() * 6.28F;
    }
    @Override public void tick() {
        xo = x; yo = y; zo = z; oRoll = roll;
        if (++age >= lifetime) { remove(); return; }
        float life = (float)age / lifetime;
        roll = 0.15F * (float)Math.sin(phase + life * 4);
        quadSize = size * (0.8F + 0.4F * life);
        alpha = Math.min(1, (1 - life) * 3);
        move(xd + Math.sin(phase + life * 5) * 0.007, yd, zd);
    }
    @Override protected Layer getLayer() { return Layer.TRANSLUCENT; }
    @Override protected int getLightCoords(float partial) { return 0xf000f0; }

    record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                                  double vx, double vy, double vz, RandomSource random) {
            return new LotusParticle(level, x, y, z, vx, vy, vz, sprites.first());
        }
    }
}
