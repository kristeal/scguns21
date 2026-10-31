package top.kristeal.network.message;

import com.google.common.collect.ImmutableMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Reference;
import top.kristeal.client.network.ClientPlayHandler;
import top.kristeal.common.CustomGun;
import top.kristeal.common.CustomGunLoader;
import top.kristeal.common.Gun;
import top.kristeal.common.NetworkGunManager;

/**
 * Author: MrCrayfish
 * <p>
 * Port note: rewritten from a Framework PlayMessage to a CustomPacketPayload. The wire format
 * is unchanged. It is now also what joining players receive, replacing the Framework login data (see
 * NetworkGunManager#onDatapackSync).
 */
public class S2CMessageUpdateGuns implements CustomPacketPayload
{
    public static final Type<S2CMessageUpdateGuns> TYPE = new Type<>(Reference.id("s2c_update_guns"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMessageUpdateGuns> STREAM_CODEC = StreamCodec.ofMember(S2CMessageUpdateGuns::encode, S2CMessageUpdateGuns::decode);

    private ImmutableMap<ResourceLocation, Gun> registeredGuns;
    private ImmutableMap<ResourceLocation, CustomGun> customGuns;

    public S2CMessageUpdateGuns() {}

    public void encode(RegistryFriendlyByteBuf buffer)
    {
        Validate.notNull(NetworkGunManager.get());
        Validate.notNull(CustomGunLoader.get());
        NetworkGunManager.get().writeRegisteredGuns(buffer);
        CustomGunLoader.get().writeCustomGuns(buffer);
    }

    public static S2CMessageUpdateGuns decode(RegistryFriendlyByteBuf buffer)
    {
        S2CMessageUpdateGuns message = new S2CMessageUpdateGuns();
        message.registeredGuns = NetworkGunManager.readRegisteredGuns(buffer);
        message.customGuns = CustomGunLoader.readCustomGuns(buffer);
        return message;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    public static void handle(S2CMessageUpdateGuns message, IPayloadContext context)
    {
        context.enqueueWork(() -> ClientPlayHandler.handleUpdateGuns(message));
    }

    public ImmutableMap<ResourceLocation, Gun> getRegisteredGuns()
    {
        return this.registeredGuns;
    }

    public ImmutableMap<ResourceLocation, CustomGun> getCustomGuns()
    {
        return this.customGuns;
    }
}
