package top.kristeal.network.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Reference;
import top.kristeal.client.handler.ClientMeleeAttackHandler;

/**
 * Port note: rewritten from a Framework PlayMessage to a CustomPacketPayload. The held stack uses the
 * registry-aware ItemStack codec (1.21; the reference wrote it with buf.writeItem). An empty stack is allowed.
 */
public class S2CMessageMeleeAttack implements CustomPacketPayload
{
    public static final Type<S2CMessageMeleeAttack> TYPE = new Type<>(Reference.id("s2c_melee_attack"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMessageMeleeAttack> STREAM_CODEC = StreamCodec.ofMember(S2CMessageMeleeAttack::encode, S2CMessageMeleeAttack::decode);

    private ItemStack heldItem;

    public S2CMessageMeleeAttack() {}

    public S2CMessageMeleeAttack(ItemStack heldItem)
    {
        this.heldItem = heldItem;
    }

    public void encode(RegistryFriendlyByteBuf buffer)
    {
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, this.heldItem);
    }

    public static S2CMessageMeleeAttack decode(RegistryFriendlyByteBuf buffer)
    {
        return new S2CMessageMeleeAttack(ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
    }

    public static void handle(S2CMessageMeleeAttack message, IPayloadContext context)
    {
        // Client code lives in ClientMeleeAttackHandler so this class never refers to client-only types
        context.enqueueWork(ClientMeleeAttackHandler::onMeleeAttackMessage);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
