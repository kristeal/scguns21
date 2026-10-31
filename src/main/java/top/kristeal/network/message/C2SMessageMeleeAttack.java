package top.kristeal.network.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Reference;
import top.kristeal.client.handler.GunRenderingHandler;
import top.kristeal.client.handler.MeleeAttackHandler;
import top.kristeal.init.ModSyncedDataKeys;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A melee stab with the held gun. The bayonet charge has its own payload ({@link C2SMessageBanzai}).
 */
public class C2SMessageMeleeAttack implements CustomPacketPayload
{
    public static final Type<C2SMessageMeleeAttack> TYPE = new Type<>(Reference.id("c2s_melee_attack"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMessageMeleeAttack> STREAM_CODEC = StreamCodec.ofMember(C2SMessageMeleeAttack::encode, C2SMessageMeleeAttack::decode);

	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public void encode(RegistryFriendlyByteBuf buffer)
    {
		// No data to encode
	}

	public static C2SMessageMeleeAttack decode(RegistryFriendlyByteBuf buffer) {
		return new C2SMessageMeleeAttack();
	}

	public static void handle(C2SMessageMeleeAttack message, IPayloadContext context) {
		context.enqueueWork(() -> {
			ServerPlayer player = (ServerPlayer) context.player();
			if (player == null || player.isSpectator() || MeleeAttackHandler.isBanzaiActive(player)) return;
			if (ModSyncedDataKeys.MELEE.getValue(player) ||
					player.getCooldowns().isOnCooldown(player.getMainHandItem().getItem())) {
				return;
			}

			ModSyncedDataKeys.MELEE.setValue(player, true);
			MeleeAttackHandler.performMeleeAttack(player);
			scheduler.schedule(() -> player.server.execute(() -> ModSyncedDataKeys.MELEE.setValue(player, false)), // Fix: on the server thread
					(long) GunRenderingHandler.MELEE_DURATION, TimeUnit.MILLISECONDS);
		});
	}

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
