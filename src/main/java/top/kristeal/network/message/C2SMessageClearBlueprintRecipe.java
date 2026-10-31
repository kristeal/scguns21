package top.kristeal.network.message;

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
import top.kristeal.item.BlueprintItem;

public class C2SMessageClearBlueprintRecipe implements CustomPacketPayload {
    public static final Type<C2SMessageClearBlueprintRecipe> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "c2s_clear_blueprint_recipe"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMessageClearBlueprintRecipe> STREAM_CODEC = StreamCodec.ofMember(C2SMessageClearBlueprintRecipe::encode, C2SMessageClearBlueprintRecipe::decode);

    private final InteractionHand hand;

    public C2SMessageClearBlueprintRecipe(InteractionHand hand) {
        this.hand = hand;
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(this.hand);
    }

    public static C2SMessageClearBlueprintRecipe decode(RegistryFriendlyByteBuf buffer) {
        return new C2SMessageClearBlueprintRecipe(buffer.readEnum(InteractionHand.class));
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SMessageClearBlueprintRecipe message, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ItemStack blueprint = player.getItemInHand(message.hand);

                if (blueprint.getItem() instanceof BlueprintItem) {
                    BlueprintItem.clearActiveRecipe(blueprint);
                }
            }
        });
    }
}
