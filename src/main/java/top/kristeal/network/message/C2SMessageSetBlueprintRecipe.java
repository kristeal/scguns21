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

public class C2SMessageSetBlueprintRecipe implements CustomPacketPayload {
    public static final Type<C2SMessageSetBlueprintRecipe> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "c2s_set_blueprint_recipe"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMessageSetBlueprintRecipe> STREAM_CODEC = StreamCodec.ofMember(C2SMessageSetBlueprintRecipe::encode, C2SMessageSetBlueprintRecipe::decode);

    private final InteractionHand hand;
    private final String recipeId;

    public C2SMessageSetBlueprintRecipe(InteractionHand hand, String recipeId) {
        this.hand = hand;
        this.recipeId = recipeId;
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(this.hand);
        buffer.writeUtf(this.recipeId);
    }

    public static C2SMessageSetBlueprintRecipe decode(RegistryFriendlyByteBuf buffer) {
        return new C2SMessageSetBlueprintRecipe(buffer.readEnum(InteractionHand.class), buffer.readUtf());
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SMessageSetBlueprintRecipe message, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ItemStack blueprint = player.getItemInHand(message.hand);

                if (blueprint.getItem() instanceof BlueprintItem) {
                    BlueprintItem.setActiveRecipe(blueprint, message.recipeId);
                }
            }
        });
    }
}
