package top.kristeal.network.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Reference;
import top.kristeal.client.network.ClientPlayHandler;

/**
 * Port note: Framework PlayMessage -> NeoForge CustomPacketPayload. The client-only
 * GameRenderer#displayItemActivation call moved to ClientPlayHandler (replaces DistExecutor).
 */
public class S2CShowTotemAnimationMessage implements CustomPacketPayload {
    public static final Type<S2CShowTotemAnimationMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "s2c_show_totem_animation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CShowTotemAnimationMessage> STREAM_CODEC = StreamCodec.ofMember(S2CShowTotemAnimationMessage::encode, S2CShowTotemAnimationMessage::decode);

    private final ItemStack itemStack;

    public S2CShowTotemAnimationMessage(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, this.itemStack);
    }

    public static S2CShowTotemAnimationMessage decode(RegistryFriendlyByteBuf buffer) {
        return new S2CShowTotemAnimationMessage(ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
    }

    public ItemStack getItemStack() {
        return this.itemStack;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CShowTotemAnimationMessage message, IPayloadContext context) {
        context.enqueueWork(() -> ClientPlayHandler.handleShowTotemAnimation(message));
    }
}
