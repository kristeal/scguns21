package top.kristeal.client.network;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.ParticleType;
import top.kristeal.network.message.S2CMessageEntityCasingEject;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import top.kristeal.common.Gun;
import top.kristeal.network.message.S2CMessageBeamPenetration;
import top.kristeal.network.message.S2CMessageBeamUpdate;
import top.kristeal.network.message.S2CMessageStopBeam;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeManager;
import top.kristeal.item.BlueprintItem;
import top.kristeal.research.ResearchHelper;
import top.kristeal.research.ResearchRecipe;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationController;
import top.kristeal.client.BulletTrail;
import top.kristeal.client.audio.GunShotSound;
import top.kristeal.client.handler.BulletTrailRenderingHandler;
import top.kristeal.client.handler.GunRenderingHandler;
import top.kristeal.client.handler.HUDRenderHandler;
import top.kristeal.common.GunStackData;
import top.kristeal.init.ModSyncedDataKeys;
import top.kristeal.item.GunItem;
import top.kristeal.item.animated.AnimatedGunItem;
import top.kristeal.network.message.S2CMessageBulletTrail;
import top.kristeal.network.message.S2CMessageGunSound;
import top.kristeal.network.message.S2CMessageProjectileHitBlock;
import top.kristeal.network.message.S2CMessageProjectileHitEntity;
import top.kristeal.network.message.S2CMessageRemoveProjectile;
import top.kristeal.network.message.S2CMessageStopReload;
import top.kristeal.particles.BulletHoleData;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import top.kristeal.Config;
import top.kristeal.progression.ProgressionMode;
import net.neoforged.fml.ModList;
import top.kristeal.compat.SableCompat;
import top.kristeal.client.particle.BloodParticle;
import top.kristeal.client.screen.BlueprintScreen;
import top.kristeal.common.exosuit.ExoSuitData;
import top.kristeal.client.TurretBulletTrail;
import top.kristeal.client.handler.TurretBulletTrailRenderingHandler;
import top.kristeal.init.ModParticleTypes;
import top.kristeal.item.animated.ExoSuitItem;
import top.kristeal.network.PacketHandler;
import top.kristeal.network.message.C2SMessageClearBlueprintRecipe;
import top.kristeal.network.message.S2CMessageBlood;
import top.kristeal.network.message.S2CMessageMuzzleFlash;
import top.kristeal.network.message.S2CMessageTurretBulletTrail;
import top.kristeal.network.message.S2CMessageStunGrenade;
import top.kristeal.network.message.S2CMessageSyncExoSuitUpgrades;
import top.kristeal.network.message.S2CShowTotemAnimationMessage;
import top.kristeal.network.message.S2CMessageUpdateGuns;
import top.kristeal.common.NetworkGunManager;
import top.kristeal.client.CustomGunManager;

import java.util.Objects;
import javax.annotation.Nullable;

import net.minecraft.world.entity.player.Player;
import top.kristeal.client.handler.BeamHandler;

/**
 * Author: MrCrayfish
 *
 * Client-only handlers for S2C messages. Message classes must delegate here instead of touching
 * client classes themselves, otherwise dedicated servers crash while registering payloads.
 */
public class ClientPlayHandler {

    public static void handleSyncExoSuitUpgrades(S2CMessageSyncExoSuitUpgrades message) {
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        Level level = Minecraft.getInstance().level;
        if (localPlayer == null || level == null) return;

        Player targetPlayer = null;
        for (Player player : level.players()) {
            if (player.getUUID().equals(message.getPlayerId())) {
                targetPlayer = player;
                break;
            }
        }

        if (targetPlayer != null) {
            ItemStack armorPiece = targetPlayer.getItemBySlot(message.getArmorSlot());

            if (armorPiece.isEmpty() && !message.getUpgradeData().isEmpty()) {
                return;
            }

            if (armorPiece.getItem() instanceof ExoSuitItem) {
                ExoSuitData.setUpgradeData(armorPiece, message.getUpgradeData());

                targetPlayer.setItemSlot(message.getArmorSlot(), armorPiece.copy());

                if (targetPlayer == localPlayer) {
                    localPlayer.inventoryMenu.broadcastChanges();
                }
            }
        }
    }

    // handleSyncUpgradeRegistry is inlined in S2CMessageSyncUpgradeRegistry (no client classes).
    // Gun-holding mobs eject casings
    public static void handleEntityCasingEject(S2CMessageEntityCasingEject message) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;

        Entity entity = level.getEntity(message.getEntityId());
        if (!(entity instanceof LivingEntity livingEntity)) return;

        Vec3 lookVec = livingEntity.getLookAngle();
        Vec3 rightVec = new Vec3(-lookVec.z, 0, lookVec.x).normalize();
        Vec3 forwardVec = new Vec3(lookVec.x, 0, lookVec.z).normalize();

        double offsetX = rightVec.x * 0.5 + forwardVec.x * 0.5;
        double offsetY = livingEntity.getEyeHeight() - 0.4;
        double offsetZ = rightVec.z * 0.5 + forwardVec.z * 0.5;

        Vec3 particlePos = livingEntity.getPosition(1).add(offsetX, offsetY, offsetZ);

        ParticleType<?> particleType = BuiltInRegistries.PARTICLE_TYPE.get(message.getParticleLocation());
        if (particleType instanceof SimpleParticleType simpleParticleType) {
            level.addParticle(simpleParticleType,
                    particlePos.x, particlePos.y, particlePos.z,
                    0, 0, 0);
        }
    }
    public static void handleBeamPenetration(S2CMessageBeamPenetration message) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null) return;

        BeamHandler.BeamInfo beamInfo = BeamHandler.activeBeams.get(message.getPlayerId());
        if (beamInfo == null) return;
        beamInfo.glassPenetrationPoints = message.getPenetrations().stream()
                .map(S2CMessageBeamPenetration.GlassPenetrationData::getPosition)
                .collect(Collectors.toList());
        Player player = level.getPlayerByUUID(message.getPlayerId());
        if (player == null) return;

        ItemStack heldItem = player.getMainHandItem();
        if (!(heldItem.getItem() instanceof GunItem gunItem)) return;

        Gun modifiedGun = gunItem.getModifiedGun(heldItem);
        if (modifiedGun == null) return;

        float[] interpolatedColor = BeamHandler.getBeamColorForWeapon(heldItem, modifiedGun);
        for (S2CMessageBeamPenetration.GlassPenetrationData penetration : message.getPenetrations()) {
            Vec3 pos = penetration.getPosition();
            for (int i = 0; i < 1; i++) {
                double offsetX = (level.random.nextDouble() - 0.5) * 0.1;
                double offsetY = (level.random.nextDouble() - 0.5) * 0.1;
                double offsetZ = (level.random.nextDouble() - 0.5) * 0.1;
                level.addParticle(ModParticleTypes.BLOOD.get(), false,
                        pos.x + offsetX,
                        pos.y + offsetY,
                        pos.z + offsetZ,
                        interpolatedColor[0],
                        interpolatedColor[1],
                        interpolatedColor[2]);
            }
        }
    }
    public static void handleMessageBlood(S2CMessageBlood message)
    {
        if (!Config.CLIENT.particle.enableBlood.get())
        {
            return;
        }
        Level world = Minecraft.getInstance().level;
        if (world != null)
        {
            EntityType<?> entityType = message.getEntityType();
            for (int i = 0; i < 10; i++)
            {
                Particle particle = Minecraft.getInstance().particleEngine.createParticle(ModParticleTypes.BLOOD.get(), message.getX(), message.getY(), message.getZ(), 0.5, 0, 0.5);
                if (particle instanceof BloodParticle)
                {
                    ((BloodParticle) particle).setColorBasedOnEntity(entityType);
                }
            }
        }
    }
    // Beam weapons
    public static void handleBeamUpdate(S2CMessageBeamUpdate message) {
        UUID playerId = message.getPlayerId();
        Vec3 startPos = message.getStartPos();
        Vec3 endPos = message.getEndPos();

        BeamHandler.updateBeam(playerId, startPos, endPos);
    }
    /** Was inline in the reference S2CShowTotemAnimationMessage (DistExecutor). */
    public static void handleShowTotemAnimation(S2CShowTotemAnimationMessage message)
    {
        Minecraft.getInstance().gameRenderer.displayItemActivation(message.getItemStack());
    }

    // Moved here from S2CMessageTurretBulletTrail#handle (Framework message)
    public static void handleTurretBulletTrail(S2CMessageTurretBulletTrail message)
    {
        TurretBulletTrail trail = new TurretBulletTrail(
                message.getEntityId(),
                message.getPosition(),
                message.getMotion(),
                message.getTrailColor(),
                message.getTrailLengthMultiplier(),
                message.getMaxAge(),
                message.getTrailThickness()
        );
        TurretBulletTrailRenderingHandler.get().add(trail);
    }

    // Moved here from S2CMessageMuzzleFlash#handle (Framework message)
    public static void handleTurretMuzzleFlash(S2CMessageMuzzleFlash message)
    {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null)
        {
            level.addParticle(
                    ModParticleTypes.TURRET_MUZZLE_FLASH.get(),
                    message.getPosition().x,
                    message.getPosition().y,
                    message.getPosition().z,
                    0, 0, 0
            );
        }
    }

    public static void handleExplosionStunGrenade(S2CMessageStunGrenade message)
    {
        Minecraft mc = Minecraft.getInstance();
        ParticleEngine particleManager = mc.particleEngine;
        Level world = Objects.requireNonNull(mc.level);
        double x = message.getX();
        double y = message.getY();
        double z = message.getZ();

        /* Spawn lingering smoke particles */
        for(int i = 0; i < 30; i++)
        {
            spawnParticle(particleManager, ParticleTypes.CLOUD, x, y, z, world.random, 0.2);
        }

        /* Spawn fast moving smoke/spark particles */
        for(int i = 0; i < 30; i++)
        {
            Particle smoke = spawnParticle(particleManager, ParticleTypes.SMOKE, x, y, z, world.random, 4.0);
            smoke.setLifetime((int) ((8 / (Math.random() * 0.1 + 0.4)) * 0.5));
            spawnParticle(particleManager, ParticleTypes.CRIT, x, y, z, world.random, 4.0);
        }
    }

    private static Particle spawnParticle(ParticleEngine manager, ParticleOptions data, double x, double y, double z, RandomSource rand, double velocityMultiplier)
    {
        return manager.createParticle(data, x, y, z, (rand.nextDouble() - 0.5) * velocityMultiplier, (rand.nextDouble() - 0.5) * velocityMultiplier, (rand.nextDouble() - 0.5) * velocityMultiplier);
    }
    // Beam weapons
    public static void handleStopBeam(S2CMessageStopBeam message) {
        BeamHandler.stopBeam(message.getPlayerId());
    }

    /* Client-only blueprint helpers, called from BlueprintItem so common code never links BlueprintScreen/Minecraft */

    public static void openBlueprintScreen(ItemStack blueprintStack, Player player, InteractionHand hand) {
        Minecraft.getInstance().setScreen(new BlueprintScreen(blueprintStack, player, hand));
    }

    public static void clearBlueprintRecipe(InteractionHand hand) {
        PacketHandler.getPlayChannel().sendToServer(new C2SMessageClearBlueprintRecipe(hand));
    }

    @Nullable
    public static String getBlueprintActiveRecipeName(ItemStack blueprint) {
        return BlueprintScreen.getActiveRecipeName(blueprint);
    }

    /** Research progression: the blueprint tooltip's research summary. */
    public static void addBlueprintResearchTooltip(ItemStack blueprint, List<Component> tooltip) {
        Level level = Minecraft.getInstance().level;
        if (level == null || !ProgressionMode.isResearch()) {
            return;
        }
        RecipeManager recipes = level.getRecipeManager();
        List<ResearchRecipe> entries = ResearchHelper.getRecipesFor(recipes, blueprint.getItem());
        if (entries.isEmpty()) {
            return;
        }
        int researched = 0;
        int prototypes = 0;
        for (ResearchRecipe entry : entries) {
            switch (ResearchHelper.getState(blueprint, entry)) {
                case RESEARCHED -> researched++;
                case PROTOTYPE -> prototypes++;
                default -> {}
            }
        }
        tooltip.add(Component.translatable("item.scguns.blueprint.tooltip.researched", researched, entries.size())
                .withStyle(ChatFormatting.GOLD));
        if (prototypes > 0) {
            tooltip.add(Component.translatable("item.scguns.blueprint.tooltip.prototypes", prototypes)
                    .withStyle(ChatFormatting.GRAY));
        }

        ResourceLocation activeRecipeId = BlueprintItem.getActiveRecipe(blueprint);
        if (activeRecipeId != null) {
            recipes.byKey(activeRecipeId).ifPresent(holder -> {
                ItemStack result = holder.value().getResultItem(level.registryAccess());
                ResearchHelper.find(recipes, blueprint.getItem(), result.getItem()).ifPresent(entry ->
                        tooltip.add(Component.translatable("item.scguns.blueprint.tooltip.active_research",
                                        Math.round(ResearchHelper.getProgress(blueprint, entry) * 100))
                                .withStyle(ChatFormatting.GRAY)));
            });
        }
    }

    /** The server's progression mode: stored for a remote server, then the research UI is shown or hidden. */
    public static void handleProgressionMode(ProgressionMode mode, boolean remote) {
        if (remote) {
            ProgressionMode.setClientOverride(mode);
        }
        if (ModList.get().isLoaded("jei")) {
            top.kristeal.compat.JEIScorchedPlugin.onProgressionModeChanged();
        }
    }

    // Gun definitions and custom guns from the server, on join and /reload
    public static void handleUpdateGuns(S2CMessageUpdateGuns message)
    {
        NetworkGunManager.updateRegisteredGuns(message);
        CustomGunManager.updateCustomGuns(message);
    }

    // ---- Firing, hits, trails and reload state ----

    public static void handleMessageBulletTrail(S2CMessageBulletTrail message)
    {
        Level world = Minecraft.getInstance().level;
        if(world != null)
        {
            int[] entityIds = message.getEntityIds();
            Vec3[] positions = message.getPositions();
            Vec3[] motions = message.getMotions();
            ItemStack item = message.getItem();
            int trailColor = message.getTrailColor();
            double trailLengthMultiplier = message.getTrailLengthMultiplier();
            int life = message.getLife();
            double gravity = message.getGravity();
            int shooterId = message.getShooterId();
            boolean enchanted = message.isEnchanted();
            ParticleOptions data = message.getParticleData();
            boolean isVisible = message.isVisible();
            double trailThickness = message.getTrailThickness();

            for(int i = 0; i < message.getCount(); i++)
            {
                BulletTrailRenderingHandler.get().add(new BulletTrail(entityIds[i], positions[i], motions[i],
                        item, trailColor, trailLengthMultiplier, life, gravity, shooterId, enchanted, data,
                        isVisible, trailThickness));
            }
        }
    }

    public static void handleMessageGunSound(S2CMessageGunSound message) {
        Minecraft mc = Minecraft.getInstance();
        if(mc.player == null || mc.level == null)
            return;

        if(message.showMuzzleFlash()) {
            GunRenderingHandler.get().showMuzzleFlashForPlayer(message.getShooterId());
        }

        if(message.getShooterId() == mc.player.getId()) {
            Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(
                    message.getId(),
                    SoundSource.PLAYERS,
                    message.getVolume(),
                    message.getPitch(),
                    mc.level.getRandom(),
                    false,
                    0,
                    SoundInstance.Attenuation.NONE,
                    0, 0, 0,
                    true));
        } else {
            Minecraft.getInstance().getSoundManager().play(new GunShotSound(
                    message.getId(),
                    SoundSource.PLAYERS,
                    message.getX(),
                    message.getY(),
                    message.getZ(),
                    message.getVolume(),
                    message.getPitch(),
                    message.isReload(),
                    mc.level.getRandom()));
        }
    }

    public static void handleProjectileHitBlock(S2CMessageProjectileHitBlock message)
    {
        Minecraft mc = Minecraft.getInstance();
        Level world = mc.level;
        if(world != null)
        {
            BlockState state = world.getBlockState(message.getPos());
            double holeX = message.getX() + 0.005 * message.getFace().getStepX();
            double holeY = message.getY() + 0.005 * message.getFace().getStepY();
            double holeZ = message.getZ() + 0.005 * message.getFace().getStepZ();
            double distance = Math.sqrt(mc.player.distanceToSqr(message.getX(), message.getY(), message.getZ()));
            // A bullet hole would not follow a moving Sable sub-level, and its face is in the sub-level's frame
            if(!SableCompat.isInSubLevel(world, message.getPos()))
            {
                world.addParticle(new BulletHoleData(message.getFace(), message.getPos()), false, holeX, holeY, holeZ, 0, 0, 0);
            }
            if(distance < Config.CLIENT.particle.impactParticleDistance.get())
            {
                for(int i = 0; i < 4; i++)
                {
                    Vec3i normal = message.getFace().getNormal();
                    Vec3 motion = new Vec3(normal.getX(), normal.getY(), normal.getZ());
                    motion = motion.add(getRandomDir(world.random), getRandomDir(world.random), getRandomDir(world.random)); // Fix: the result was discarded
                    world.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state), false, message.getX(), message.getY(), message.getZ(), motion.x, motion.y, motion.z);
                }
            }
            if(distance <= Config.CLIENT.sounds.impactSoundDistance.get())
            {
                world.playLocalSound(message.getX(), message.getY(), message.getZ(), state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 2.0F, 2.0F, false);
            }
        }
    }

    private static double getRandomDir(RandomSource random)
    {
        return -0.25 + random.nextDouble() * 0.5;
    }

    public static void handleProjectileHitEntity(S2CMessageProjectileHitEntity message)
    {
        Minecraft mc = Minecraft.getInstance();
        Level world = mc.level;
        if(world == null)
            return;

        HUDRenderHandler.playHitMarker(message.isCritical() || message.isHeadshot());
        SoundEvent event = getHitSound(message.isCritical(), message.isHeadshot(), message.isPlayer());
        if(event == null)
            return;

        mc.getSoundManager().play(SimpleSoundInstance.forUI(event, 1.0F, 1.0F + world.random.nextFloat() * 0.2F));
    }

    @Nullable
    private static SoundEvent getHitSound(boolean critical, boolean headshot, boolean player)
    {
        if(critical)
        {
            if(Config.CLIENT.sounds.playSoundWhenCritical.get())
            {
                SoundEvent event = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse(Config.CLIENT.sounds.criticalSound.get()));
                return event != null ? event : SoundEvents.PLAYER_ATTACK_CRIT;
            }
        }
        else if(headshot)
        {
            if(Config.CLIENT.sounds.playSoundWhenHeadshot.get())
            {
                SoundEvent event = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse(Config.CLIENT.sounds.headshotSound.get()));
                return event != null ? event : SoundEvents.PLAYER_ATTACK_KNOCKBACK;
            }
        }
        else if(player)
        {
            return SoundEvents.PLAYER_HURT;
        }
        return null;
    }

    public static void handleRemoveProjectile(S2CMessageRemoveProjectile message)
    {
        BulletTrailRenderingHandler.get().remove(message.getEntityId());
    }

    public static void handleReloadState(boolean reloading) {
        if (Minecraft.getInstance().player != null) {
            ItemStack heldItem = Minecraft.getInstance().player.getMainHandItem();
            if (heldItem.getItem() instanceof GunItem) {
                CompoundTag tag = GunStackData.liveState(heldItem); // Port note: CUSTOM_DATA state

                if (!reloading) {
                    tag.remove("IsReloading");
                    tag.remove("scguns:IsReloading");
                    tag.putBoolean("scguns:ReloadComplete", true);
                    ModSyncedDataKeys.RELOADING.setValue(Minecraft.getInstance().player, false);
                } else {
                    ModSyncedDataKeys.RELOADING.setValue(Minecraft.getInstance().player, true);
                }
            }
        }
    }

    public static void handleStopReload(S2CMessageStopReload message) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        ItemStack heldItem = player.getMainHandItem();
        if (!(heldItem.getItem() instanceof AnimatedGunItem gunItem)) return;

        CompoundTag tag = GunStackData.liveState(heldItem); // Port note: CUSTOM_DATA state
        tag.putString("scguns:ReloadState", "STOPPING");
        tag.putBoolean("scguns:IsPlayingReloadStop", true);
        tag.remove("InReloadLoop");
        tag.remove("scguns:IsReloading");
        ModSyncedDataKeys.RELOADING.setValue(player, false);

        long id = GeoItem.getId(heldItem);
        AnimationController<GeoAnimatable> animationController = gunItem.getAnimatableInstanceCache()
                .getManagerForId(id)
                .getAnimationControllers()
                .get("controller");

        if (animationController != null) {
            animationController.stop();
            animationController.setAnimationSpeed(1.0);
            animationController.tryTriggerAnimation(
                    gunItem.isInCarbineMode(heldItem) ? "carbine_reload_stop" : "reload_stop"
            );
        }
    }
}
