package top.kristeal.network.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Reference;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class C2SMessageChargeSync implements CustomPacketPayload
{
    public static final Type<C2SMessageChargeSync> TYPE = new Type<>(Reference.id("c2s_charge_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMessageChargeSync> STREAM_CODEC = StreamCodec.ofMember(C2SMessageChargeSync::encode, C2SMessageChargeSync::decode);

    private float chargeProgress;

    public C2SMessageChargeSync() {}

    public C2SMessageChargeSync(float chargeProgress) {
        this.chargeProgress = chargeProgress;
    }

    public void encode(RegistryFriendlyByteBuf buffer)
    {
        buffer.writeFloat(this.chargeProgress);
    }
    public static C2SMessageChargeSync decode(RegistryFriendlyByteBuf buffer) {
        return new C2SMessageChargeSync(buffer.readFloat());
    }
    public static void handle(C2SMessageChargeSync message, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if(player != null) {
                player.getPersistentData().putFloat("ChargeProgress", message.chargeProgress);
            }
        });
    }

    public float getChargeProgress() {
        return this.chargeProgress;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
