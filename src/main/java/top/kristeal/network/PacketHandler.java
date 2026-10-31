package top.kristeal.network;

import top.kristeal.network.message.S2CMessageTurretConfigs;
import top.kristeal.network.message.S2CMessageProgressionMode;
import top.kristeal.network.message.S2CMessageEntityCasingEject;
import top.kristeal.network.message.S2CMessageEntityMuzzleFlash;
import top.kristeal.network.message.C2SMessageChargeSync;
import top.kristeal.network.message.C2SMessagePreFireSound;
import top.kristeal.network.message.C2SMessageStopBeam;
import top.kristeal.network.message.C2SMessageSwapAmmo;
import top.kristeal.network.message.S2CMessageBeamImpact;
import top.kristeal.network.message.S2CMessageBeamPenetration;
import top.kristeal.network.message.S2CMessageBeamUpdate;
import top.kristeal.network.message.S2CMessageStopBeam;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import top.kristeal.Reference;
import top.kristeal.network.message.*;

import java.util.function.Supplier;

public class PacketHandler {
    private static final PlayChannel PLAY_CHANNEL = new PlayChannel();

    public static PlayChannel getPlayChannel() {
        return PLAY_CHANNEL;
    }
//REPLACED FRAMEWORKS NETWORKING
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Reference.MOD_ID).versioned("1");
        registrar.playToServer(C2SMessageJetpackState.TYPE, C2SMessageJetpackState.STREAM_CODEC, C2SMessageJetpackState::handle);
        registrar.playToServer(C2SMessageJetpackThrust.TYPE, C2SMessageJetpackThrust.STREAM_CODEC, C2SMessageJetpackThrust::handle);
        registrar.playToServer(C2SMessageToggleExoSuitPower.TYPE, C2SMessageToggleExoSuitPower.STREAM_CODEC, C2SMessageToggleExoSuitPower::handle);
        registrar.playToServer(C2SMessageUtilityAction.TYPE, C2SMessageUtilityAction.STREAM_CODEC, C2SMessageUtilityAction::handle);
        registrar.playToServer(C2SMessageSaveExoSuitUpgrades.TYPE, C2SMessageSaveExoSuitUpgrades.STREAM_CODEC, C2SMessageSaveExoSuitUpgrades::handle);
        registrar.playToServer(C2SMessageAttachments.TYPE, C2SMessageAttachments.STREAM_CODEC, C2SMessageAttachments::handle);
        registrar.playToServer(C2SMessageSetBlueprintRecipe.TYPE, C2SMessageSetBlueprintRecipe.STREAM_CODEC, C2SMessageSetBlueprintRecipe::handle);
        registrar.playToServer(C2SMessageClearBlueprintRecipe.TYPE, C2SMessageClearBlueprintRecipe.STREAM_CODEC, C2SMessageClearBlueprintRecipe::handle);
        registrar.playToClient(S2CMessageSyncExoSuitUpgrades.TYPE, S2CMessageSyncExoSuitUpgrades.STREAM_CODEC, S2CMessageSyncExoSuitUpgrades::handle);
        registrar.playToClient(S2CMessageSyncUpgradeRegistry.TYPE, S2CMessageSyncUpgradeRegistry.STREAM_CODEC, S2CMessageSyncUpgradeRegistry::handle);
        registrar.playToClient(S2CMessageStunGrenade.TYPE, S2CMessageStunGrenade.STREAM_CODEC, S2CMessageStunGrenade::handle);
        registrar.playToClient(S2CMessageRaidFlareBurst.TYPE, S2CMessageRaidFlareBurst.STREAM_CODEC, S2CMessageRaidFlareBurst::handle);
        registrar.playToClient(S2CMessageBlood.TYPE, S2CMessageBlood.STREAM_CODEC, S2CMessageBlood::handle);
        registrar.playToClient(S2CMessageTurretConfigs.TYPE, S2CMessageTurretConfigs.STREAM_CODEC, S2CMessageTurretConfigs::handle); // turret tooltips on dedicated servers
        registrar.playToClient(S2CMessageProgressionMode.TYPE, S2CMessageProgressionMode.STREAM_CODEC, S2CMessageProgressionMode::handle); // Raid / Research mode
        registrar.playToClient(S2CShowTotemAnimationMessage.TYPE, S2CShowTotemAnimationMessage.STREAM_CODEC, S2CShowTotemAnimationMessage::handle);
        registrar.playToClient(S2CMessageMuzzleFlash.TYPE, S2CMessageMuzzleFlash.STREAM_CODEC, S2CMessageMuzzleFlash::handle);
        registrar.playToClient(S2CMessageTurretBulletTrail.TYPE, S2CMessageTurretBulletTrail.STREAM_CODEC, S2CMessageTurretBulletTrail::handle);
        registrar.playToClient(S2CMessageUpdateGuns.TYPE, S2CMessageUpdateGuns.STREAM_CODEC, S2CMessageUpdateGuns::handle);
        // Firing, reloading, aiming, trails, hits, casings
        registrar.playToServer(C2SMessageAim.TYPE, C2SMessageAim.STREAM_CODEC, C2SMessageAim::handle);
        registrar.playToServer(C2SMessageGunLoaded.TYPE, C2SMessageGunLoaded.STREAM_CODEC, C2SMessageGunLoaded::handle);
        registrar.playToServer(C2SMessageLeftOverAmmo.TYPE, C2SMessageLeftOverAmmo.STREAM_CODEC, C2SMessageLeftOverAmmo::handle);
        registrar.playToServer(C2SMessageReload.TYPE, C2SMessageReload.STREAM_CODEC, C2SMessageReload::handle);
        registrar.playToServer(C2SMessageShoot.TYPE, C2SMessageShoot.STREAM_CODEC, C2SMessageShoot::handle);
        registrar.playToServer(C2SMessageShooting.TYPE, C2SMessageShooting.STREAM_CODEC, C2SMessageShooting::handle);
        registrar.playToServer(C2SMessageUnload.TYPE, C2SMessageUnload.STREAM_CODEC, C2SMessageUnload::handle);
        registrar.playToServer(C2SMessageEjectCasing.TYPE, C2SMessageEjectCasing.STREAM_CODEC, C2SMessageEjectCasing::handle);
        registrar.playToClient(S2CMessageBulletTrail.TYPE, S2CMessageBulletTrail.STREAM_CODEC, S2CMessageBulletTrail::handle);
        registrar.playToClient(S2CMessageGunSound.TYPE, S2CMessageGunSound.STREAM_CODEC, S2CMessageGunSound::handle);
        registrar.playToClient(S2CMessageHotBarrelSync.TYPE, S2CMessageHotBarrelSync.STREAM_CODEC, S2CMessageHotBarrelSync::handle);
        registrar.playToClient(S2CMessageProjectileHitBlock.TYPE, S2CMessageProjectileHitBlock.STREAM_CODEC, S2CMessageProjectileHitBlock::handle);
        registrar.playToClient(S2CMessageProjectileHitEntity.TYPE, S2CMessageProjectileHitEntity.STREAM_CODEC, S2CMessageProjectileHitEntity::handle);
        registrar.playToClient(S2CMessageReload.TYPE, S2CMessageReload.STREAM_CODEC, S2CMessageReload::handle);
        registrar.playToClient(S2CMessageRemoveProjectile.TYPE, S2CMessageRemoveProjectile.STREAM_CODEC, S2CMessageRemoveProjectile::handle);
        registrar.playToClient(S2CMessageStopReload.TYPE, S2CMessageStopReload.STREAM_CODEC, S2CMessageStopReload::handle);
        registrar.playToClient(S2CMessageUpdateAmmo.TYPE, S2CMessageUpdateAmmo.STREAM_CODEC, S2CMessageUpdateAmmo::handle);
        // Charge, ammo swap and beam payloads
        registrar.playToServer(C2SMessageChargeSync.TYPE, C2SMessageChargeSync.STREAM_CODEC, C2SMessageChargeSync::handle);
        registrar.playToServer(C2SMessagePreFireSound.TYPE, C2SMessagePreFireSound.STREAM_CODEC, C2SMessagePreFireSound::handle);
        registrar.playToServer(C2SMessageStopBeam.TYPE, C2SMessageStopBeam.STREAM_CODEC, C2SMessageStopBeam::handle);
        registrar.playToServer(C2SMessageSwapAmmo.TYPE, C2SMessageSwapAmmo.STREAM_CODEC, C2SMessageSwapAmmo::handle);
        registrar.playToClient(S2CMessageBeamImpact.TYPE, S2CMessageBeamImpact.STREAM_CODEC, S2CMessageBeamImpact::handle);
        registrar.playToClient(S2CMessageBeamPenetration.TYPE, S2CMessageBeamPenetration.STREAM_CODEC, S2CMessageBeamPenetration::handle);
        registrar.playToClient(S2CMessageBeamUpdate.TYPE, S2CMessageBeamUpdate.STREAM_CODEC, S2CMessageBeamUpdate::handle);
        registrar.playToClient(S2CMessageStopBeam.TYPE, S2CMessageStopBeam.STREAM_CODEC, S2CMessageStopBeam::handle);
        // Melee payloads
        registrar.playToServer(C2SMessageMeleeAttack.TYPE, C2SMessageMeleeAttack.STREAM_CODEC, C2SMessageMeleeAttack::handle);
        registrar.playToServer(C2SMessageOffhandMelee.TYPE, C2SMessageOffhandMelee.STREAM_CODEC, C2SMessageOffhandMelee::handle);
        registrar.playToServer(C2SMessageBanzai.TYPE, C2SMessageBanzai.STREAM_CODEC, C2SMessageBanzai::handle);
        registrar.playToClient(S2CMessageMeleeImpact.TYPE, S2CMessageMeleeImpact.STREAM_CODEC, S2CMessageMeleeImpact::handle);
        registrar.playToClient(S2CMessageMeleeAttack.TYPE, S2CMessageMeleeAttack.STREAM_CODEC, S2CMessageMeleeAttack::handle);
        // Gun-holding mob effects
        registrar.playToClient(S2CMessageEntityMuzzleFlash.TYPE, S2CMessageEntityMuzzleFlash.STREAM_CODEC, S2CMessageEntityMuzzleFlash::handle);
        registrar.playToClient(S2CMessageEntityCasingEject.TYPE, S2CMessageEntityCasingEject.STREAM_CODEC, S2CMessageEntityCasingEject::handle);
    }

    public static class PlayChannel {
        public void sendToServer(CustomPacketPayload payload) {
            PacketDistributor.sendToServer(payload);
        }

        public void sendToPlayer(Supplier<ServerPlayer> playerSupplier, CustomPacketPayload payload) {
            ServerPlayer player = playerSupplier.get();
            if (player != null) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }

        // Replaces Framework's sendToNearbyPlayers(() -> LevelLocation.create(level, x, y, z, radius), message)
        public void sendToNearbyPlayers(ServerLevel level, double x, double y, double z, double radius, CustomPacketPayload payload) {
            PacketDistributor.sendToPlayersNear(level, null, x, y, z, radius, payload);
        }

        // Replaces Framework's sendToTrackingChunk(() -> level.getChunkAt(pos), message); only meaningful on the server
        public void sendToTrackingChunk(Supplier<LevelChunk> chunkSupplier, CustomPacketPayload payload) {
            LevelChunk chunk = chunkSupplier.get();
            if (chunk != null && chunk.getLevel() instanceof ServerLevel serverLevel) {
                PacketDistributor.sendToPlayersTrackingChunk(serverLevel, chunk.getPos(), payload);
            }
        }

        public void sendToTracking(Supplier<Entity> entitySupplier, CustomPacketPayload payload) {
            Entity entity = entitySupplier.get();
            if (entity != null) {
                PacketDistributor.sendToPlayersTrackingEntity(entity, payload);
            }
        }
    }
}
