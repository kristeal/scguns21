package top.kristeal.network.message;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Reference;
import top.kristeal.client.network.ClientPlayHandler;

public class S2CMessageBlood implements CustomPacketPayload
{
    public static final Type<S2CMessageBlood> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "s2c_blood"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMessageBlood> STREAM_CODEC = StreamCodec.ofMember(S2CMessageBlood::encode, S2CMessageBlood::decode);

    private double x;
    private double y;
    private double z;
    private EntityType<?> entityType;

    public S2CMessageBlood() {}

    public S2CMessageBlood(double x, double y, double z, EntityType<?> entityType)
    {
        this.x = x;
        this.y = y;
        this.z = z;
        this.entityType = entityType;
    }

    public void encode(RegistryFriendlyByteBuf buffer)
    {
        buffer.writeDouble(this.x);
        buffer.writeDouble(this.y);
        buffer.writeDouble(this.z);
        buffer.writeResourceLocation(BuiltInRegistries.ENTITY_TYPE.getKey(this.entityType));
    }

    public static S2CMessageBlood decode(RegistryFriendlyByteBuf buffer)
    {
        double x = buffer.readDouble();
        double y = buffer.readDouble();
        double z = buffer.readDouble();
        ResourceLocation entityTypeLocation = buffer.readResourceLocation();
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(entityTypeLocation);
        return new S2CMessageBlood(x, y, z, entityType);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    public static void handle(S2CMessageBlood message, IPayloadContext context)
    {
        context.enqueueWork(() -> ClientPlayHandler.handleMessageBlood(message));
    }

    public double getX() { return this.x; }
    public double getY() { return this.y; }
    public double getZ() { return this.z; }
    public EntityType<?> getEntityType() { return this.entityType; }
}
