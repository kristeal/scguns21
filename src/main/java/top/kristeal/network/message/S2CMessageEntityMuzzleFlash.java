package top.kristeal.network.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Reference;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import top.kristeal.client.handler.GunRenderingHandler;

public class S2CMessageEntityMuzzleFlash implements CustomPacketPayload
{
    public static final Type<S2CMessageEntityMuzzleFlash> TYPE = new Type<>(Reference.id("s2c_entity_muzzle_flash"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMessageEntityMuzzleFlash> STREAM_CODEC = StreamCodec.ofMember(S2CMessageEntityMuzzleFlash::encode, S2CMessageEntityMuzzleFlash::decode);

    private int entityId;
    private float randomValue;
    private Vec3 flashPosition;
    private boolean useEnchantedTexture;

    public S2CMessageEntityMuzzleFlash() {
        this.flashPosition = Vec3.ZERO;
        this.useEnchantedTexture = true;
    }

    // Updated constructor
    public S2CMessageEntityMuzzleFlash(int entityId, float randomValue, Vec3 flashPosition, boolean useEnchantedTexture) {
        this.entityId = entityId;
        this.randomValue = randomValue;
        this.flashPosition = flashPosition;
        this.useEnchantedTexture = useEnchantedTexture;
    }

    public void encode(RegistryFriendlyByteBuf buffer)
    {
        buffer.writeInt(this.entityId);
        buffer.writeFloat(this.randomValue);
        buffer.writeDouble(this.flashPosition.x);
        buffer.writeDouble(this.flashPosition.y);
        buffer.writeDouble(this.flashPosition.z);
        buffer.writeBoolean(this.useEnchantedTexture);
    }
    public static S2CMessageEntityMuzzleFlash decode(RegistryFriendlyByteBuf buffer) {
        int entityId = buffer.readInt();
        float randomValue = buffer.readFloat();
        Vec3 flashPosition = new Vec3(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble()
        );
        boolean useEnchantedTexture = buffer.readBoolean();
        return new S2CMessageEntityMuzzleFlash(entityId, randomValue, flashPosition, useEnchantedTexture);
    }
    public static void handle(S2CMessageEntityMuzzleFlash message, IPayloadContext context) {
        context.enqueueWork(() -> {
            GunRenderingHandler.get().showMuzzleFlashForPlayer(message.entityId);
            GunRenderingHandler.entityIdToRandomValue.put(message.entityId, message.randomValue);
            GunRenderingHandler.entityIdToFlashPosition.put(message.entityId, message.flashPosition);
            GunRenderingHandler.entityIdToUseEnchantedTexture.put(message.entityId, message.useEnchantedTexture);
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
