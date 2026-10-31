package top.kristeal.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RocketExplosionParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    /** Last tick's size, so the growth is smooth between ticks. */
    private float oQuadSize;

    protected RocketExplosionParticle(ClientLevel pLevel, double pX, double pY, double pZ, double pSizeMultiplier, SpriteSet pSprites) {
        super(pLevel, pX, pY, pZ, 0.0, 0.0, 0.0);

        this.lifetime = 13 + this.random.nextInt(4);

        float baseSize = 3.0F;
        this.quadSize = baseSize * (float)Math.max(1.0, pSizeMultiplier);
        this.sprites = pSprites;

        this.pickSprite(pSprites);

        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;
        this.hasPhysics = false;

        // Full white. Vertex colours are stored as bytes, so values above 1 wrap around instead of brightening.
        this.alpha = 1.0F;
        this.oQuadSize = this.quadSize;
    }

    @Override
    public void tick() {
        this.oQuadSize = this.quadSize;
        super.tick();
        this.setSpriteFromAge(this.sprites);
        this.quadSize *= 1.08F;
    }

    @Override
    public float getQuadSize(float pPartialTick) {
        return Mth.lerp(pPartialTick, this.oQuadSize, this.quadSize);
    }

    @Override
    public int getLightColor(float pPartialTick) {
        return 15728880;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet pSprites) {
            this.sprites = pSprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
            return new RocketExplosionParticle(pLevel, pX, pY, pZ, pXSpeed, this.sprites);
        }
    }
}