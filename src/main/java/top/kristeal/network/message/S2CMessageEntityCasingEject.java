package top.kristeal.network.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Reference;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.registries.BuiltInRegistries;
import top.kristeal.client.network.ClientPlayHandler;

public class S2CMessageEntityCasingEject implements CustomPacketPayload
{
    public static final Type<S2CMessageEntityCasingEject> TYPE = new Type<>(Reference.id("s2c_entity_casing_eject"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMessageEntityCasingEject> STREAM_CODEC = StreamCodec.ofMember(S2CMessageEntityCasingEject::encode, S2CMessageEntityCasingEject::decode);

    private int entityId;
    private ResourceLocation particleLocation;

    public S2CMessageEntityCasingEject() {}

    public S2CMessageEntityCasingEject(int entityId, ResourceLocation particleLocation) {
        this.entityId = entityId;
        this.particleLocation = particleLocation;
    }

    public void encode(RegistryFriendlyByteBuf buffer)
    {
        buffer.writeInt(this.entityId);
        buffer.writeResourceLocation(this.particleLocation);
    }
    public static S2CMessageEntityCasingEject decode(RegistryFriendlyByteBuf buffer) {
        return new S2CMessageEntityCasingEject(buffer.readInt(), buffer.readResourceLocation());
    }
    public static void handle(S2CMessageEntityCasingEject message, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientPlayHandler.handleEntityCasingEject(message);
        });
    }

    public int getEntityId() {
        return entityId;
    }

    public ResourceLocation getParticleLocation() {
        return particleLocation;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
