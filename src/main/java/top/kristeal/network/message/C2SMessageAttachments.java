package top.kristeal.network.message;

import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import top.kristeal.client.screen.AttachmentContainer;
import top.kristeal.common.GunStackData;
import top.kristeal.item.GunItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Reference;
import top.kristeal.item.animated.ExoSuitItem;

/**
 * Author: MrCrayfish
 */
public class C2SMessageAttachments implements CustomPacketPayload {
    public static final Type<C2SMessageAttachments> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "c2s_attachments"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMessageAttachments> STREAM_CODEC = StreamCodec.ofMember(C2SMessageAttachments::encode, C2SMessageAttachments::decode);

    public C2SMessageAttachments() {}

    public void encode(RegistryFriendlyByteBuf buffer) {}

    public static C2SMessageAttachments decode(RegistryFriendlyByteBuf buffer) {
        return new C2SMessageAttachments();
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SMessageAttachments message, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ItemStack heldItem = player.getMainHandItem();
                // The gun branch of the reference's ServerPlayHandler.handleAttachments
                if (heldItem.getItem() instanceof GunItem && GunStackData.isPrototype(heldItem)) {
                    // Research progression: prototypes take no attachments; the key does nothing (the tooltip says so)
                } else if (heldItem.getItem() instanceof GunItem) {
                    player.openMenu(new SimpleMenuProvider((windowId, playerInventory, player1) -> new AttachmentContainer(windowId, playerInventory, heldItem),
                            Component.translatable("container.scguns.attachments")));
                } else if (heldItem.getItem() instanceof ExoSuitItem) {
                    player.openMenu(new ExoSuitItem.ExoSuitMenuProvider(InteractionHand.MAIN_HAND), buf -> {
                        buf.writeEnum(InteractionHand.MAIN_HAND);
                    });
                }
            }
        });
    }
}
