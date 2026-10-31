package top.kristeal.network.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Reference;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.kristeal.cache.HotBarrelCache;

public class S2CMessageHotBarrelSync implements CustomPacketPayload
{
    public static final Type<S2CMessageHotBarrelSync> TYPE = new Type<>(Reference.id("s2c_hot_barrel_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMessageHotBarrelSync> STREAM_CODEC = StreamCodec.ofMember(S2CMessageHotBarrelSync::encode, S2CMessageHotBarrelSync::decode);

    private int hotBarrelLevel;
    private String weaponDescriptionId;

    public S2CMessageHotBarrelSync() {}

    public S2CMessageHotBarrelSync(int hotBarrelLevel, String weaponDescriptionId) {
        this.hotBarrelLevel = hotBarrelLevel;
        this.weaponDescriptionId = weaponDescriptionId;
    }

    public void encode(RegistryFriendlyByteBuf buffer)
    {
        buffer.writeInt(this.hotBarrelLevel);
        buffer.writeUtf(this.weaponDescriptionId);
    }
    public static S2CMessageHotBarrelSync decode(RegistryFriendlyByteBuf buffer) {
        return new S2CMessageHotBarrelSync(buffer.readInt(), buffer.readUtf());
    }
    public static void handle(S2CMessageHotBarrelSync message, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player(); // S2C: the client player
            if (player != null) {
                ItemStack heldItem = player.getMainHandItem();
                if (heldItem.getItem().getDescriptionId().equals(message.weaponDescriptionId)) {
                    HotBarrelCache.setHotBarrelLevel(player, heldItem, message.hotBarrelLevel);
                }
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
