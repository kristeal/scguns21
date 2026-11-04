package top.kristeal.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.kristeal.init.ModBlockEntities;

import java.util.ArrayList;
import java.util.List;

public class MobTrapBlockEntity extends BlockEntity {
    public static final int MAX_MOBS = 20;
    private static final int RADIUS = 10;
    private static final int HEIGHT_RADIUS = 3;
    private static final int CHECK_INTERVAL = 20;

    private final List<EntityType<?>> storedMobs = new ArrayList<>();
    private int tickCounter;

    public MobTrapBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MOB_TRAP.get(), pos, state);
    }

    public boolean addMob(EntityType<?> mobType) {
        if (storedMobs.size() < MAX_MOBS) {
            storedMobs.add(mobType);
            setChanged();
            return true;
        }
        return false;
    }

    public void releaseMobs(ServerLevel level, BlockPos pos) {
        if (storedMobs.isEmpty()) {
            return;
        }
        for (EntityType<?> mob : storedMobs) {
            Entity entity = mob.create(level);
            if (entity != null) {
                entity.moveTo(pos.getX() + level.random.nextDouble(), pos.getY(), pos.getZ() + level.random.nextDouble(), level.random.nextFloat() * 360.0F, 0.0F);
                level.addFreshEntity(entity);
                level.sendParticles(ParticleTypes.SMALL_FLAME, entity.getX(), entity.getY(), entity.getZ(), 20, 0.1, 0.1, 0.1, 0.02);
            }
        }
        storedMobs.clear();
        setChanged();
        level.destroyBlock(pos, true);
    }

    public List<EntityType<?>> getStoredMobs() {
        return this.storedMobs;
    }

    public void serverTick(Level level, BlockPos pos) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (++tickCounter < CHECK_INTERVAL) {
            return;
        }
        tickCounter = 0;
        if (storedMobs.isEmpty()) {
            return;
        }
        AABB detectionBox = new AABB(
                pos.getX() - RADIUS, pos.getY() - HEIGHT_RADIUS, pos.getZ() - RADIUS,
                pos.getX() + RADIUS, pos.getY() + HEIGHT_RADIUS, pos.getZ() + RADIUS);
        List<Player> players = serverLevel.getEntitiesOfClass(Player.class, detectionBox, player -> !player.isCreative() && !player.isSpectator());
        if (!players.isEmpty()) {
            releaseMobs(serverLevel, pos);
        }
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        storedMobs.clear();
        ListTag mobList = tag.getList("StoredMobs", Tag.TAG_STRING);
        for (int i = 0; i < mobList.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(mobList.getString(i));
            if (id != null) {
                // Unknown ids (a removed mod, a typo in a structure file) are skipped instead of becoming pigs
                BuiltInRegistries.ENTITY_TYPE.getOptional(id).ifPresent(storedMobs::add);
            }
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag mobList = new ListTag();
        for (EntityType<?> mob : storedMobs) {
            mobList.add(StringTag.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(mob).toString()));
        }
        tag.put("StoredMobs", mobList);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(@NotNull HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
