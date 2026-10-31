package top.kristeal.network.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Reference;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import top.kristeal.item.GunItem;
import top.kristeal.network.ClientMessageHandler;
import top.kristeal.util.GunModifierHelper;

public class S2CMessageUpdateAmmo implements CustomPacketPayload
{
    public static final Type<S2CMessageUpdateAmmo> TYPE = new Type<>(Reference.id("s2c_update_ammo"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMessageUpdateAmmo> STREAM_CODEC = StreamCodec.ofMember(S2CMessageUpdateAmmo::encode, S2CMessageUpdateAmmo::decode);

    private int ammoCount;

    public S2CMessageUpdateAmmo() {}

    public S2CMessageUpdateAmmo(int ammoCount) {
        this.ammoCount = ammoCount;
    }

    public void encode(RegistryFriendlyByteBuf buffer)
    {
        buffer.writeInt(this.ammoCount);
    }
    public static S2CMessageUpdateAmmo decode(RegistryFriendlyByteBuf buffer) {
        S2CMessageUpdateAmmo message = new S2CMessageUpdateAmmo();
        message.ammoCount = buffer.readInt();
        return message;
    }
    public static void handle(S2CMessageUpdateAmmo message, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Port note: DistExecutor is gone; enqueueWork for an S2C payload already runs on the client
            ClientMessageHandler.handleUpdateAmmo(message.ammoCount);
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
