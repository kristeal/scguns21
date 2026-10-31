package top.kristeal.network.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Reference;
import top.kristeal.client.network.RaidFlareBurstHandler;

import java.util.ArrayList;
import java.util.List;

public class S2CMessageRaidFlareBurst implements CustomPacketPayload
{
    public static final Type<S2CMessageRaidFlareBurst> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "s2c_raid_flare_burst"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMessageRaidFlareBurst> STREAM_CODEC = StreamCodec.ofMember(S2CMessageRaidFlareBurst::encode, S2CMessageRaidFlareBurst::decode);

    private final double x, y, z;
    private final String patternType;
    private final double scale;
    private final int repetitions;
    private final List<ParticleData> particles;

    public S2CMessageRaidFlareBurst(double x, double y, double z, String patternType, double scale, int repetitions, List<ParticleData> particles)
    {
        this.x = x;
        this.y = y;
        this.z = z;
        this.patternType = patternType;
        this.scale = scale;
        this.repetitions = repetitions;
        this.particles = particles;
    }

    public void encode(RegistryFriendlyByteBuf buf)
    {
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeUtf(this.patternType);
        buf.writeDouble(this.scale);
        buf.writeInt(this.repetitions);
        buf.writeInt(this.particles.size());
        for (ParticleData particle : this.particles)
        {
            buf.writeUtf(particle.particleId);
            buf.writeInt(particle.count);
            buf.writeDouble(particle.spread);
            buf.writeDouble(particle.speed);
            buf.writeInt(particle.color);
        }
    }

    public static S2CMessageRaidFlareBurst decode(RegistryFriendlyByteBuf buf)
    {
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        String patternType = buf.readUtf();
        double scale = buf.readDouble();
        int repetitions = buf.readInt();
        int particleCount = buf.readInt();
        List<ParticleData> particles = new ArrayList<>();
        for (int i = 0; i < particleCount; i++)
        {
            particles.add(new ParticleData(buf.readUtf(), buf.readInt(), buf.readDouble(), buf.readDouble(), buf.readInt()));
        }
        return new S2CMessageRaidFlareBurst(x, y, z, patternType, scale, repetitions, particles);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    public static void handle(S2CMessageRaidFlareBurst message, IPayloadContext context)
    {
        context.enqueueWork(() -> RaidFlareBurstHandler.handle(message));
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public String getPatternType() { return patternType; }
    public double getScale() { return scale; }
    public int getRepetitions() { return repetitions; }
    public List<ParticleData> getParticles() { return particles; }

    public static class ParticleData
    {
        public final String particleId;
        public final int count;
        public final double spread;
        public final double speed;
        public final int color;

        public ParticleData(String particleId, int count, double spread, double speed, int color)
        {
            this.particleId = particleId;
            this.count = count;
            this.spread = spread;
            this.speed = speed;
            this.color = color;
        }
    }
}
