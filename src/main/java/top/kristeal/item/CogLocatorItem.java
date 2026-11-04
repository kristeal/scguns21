package top.kristeal.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Reference;
import top.kristeal.init.ModParticleTypes;

public class CogLocatorItem extends Item {
    private static final ResourceKey<Structure> COG_CHAMBERS =
            ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "cog_chambers"));

    public CogLocatorItem(Properties properties) {
        super(properties.durability(8));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            BlockPos structurePos = findNearestCogChambers(serverLevel, player.blockPosition());

            if (structurePos != null) {
                Vec3 playerVec = player.position();
                Vec3 direction = Vec3.atCenterOf(structurePos).subtract(playerVec).normalize();
                createPlasmaRingLine(serverLevel, playerVec, direction);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6F, 1.2F);
                itemStack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                return InteractionResultHolder.success(itemStack);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.5F, 0.8F);
            return InteractionResultHolder.fail(itemStack);
        }

        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }

    private BlockPos findNearestCogChambers(ServerLevel level, BlockPos playerPos) {
        return level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(COG_CHAMBERS)
                .map(holder -> level.getChunkSource().getGenerator()
                        .findNearestMapStructure(level, HolderSet.direct(holder), playerPos, 100, false))
                .map(found -> found.getFirst())
                .orElse(null);
    }

    private void createPlasmaRingLine(ServerLevel level, Vec3 start, Vec3 direction) {
        double lineLength = 25.0;
        int ringCount = 8;

        for (int i = 0; i < ringCount; i++) {
            double progress = (double) i / (double) (ringCount - 1);
            Vec3 ringPos = start.add(
                    direction.x * lineLength * progress,
                    direction.y * lineLength * progress + 1.5,
                    direction.z * lineLength * progress
            );
            double offsetX = (level.random.nextDouble() - 0.5) * 0.2;
            double offsetY = (level.random.nextDouble() - 0.5) * 0.2;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.2;

            level.sendParticles(ModParticleTypes.PLASMA_RING.get(),
                    ringPos.x + offsetX, ringPos.y + offsetY, ringPos.z + offsetZ,
                    1, 0.0, 0.0, 0.0, 0.0);
        }
        for (int i = 0; i < 12; i++) {
            double angle = (i / 12.0) * 2 * Math.PI;
            double radius = 1.0 + (level.random.nextDouble() * 0.5);
            double offsetY = (level.random.nextDouble() - 0.5) * 0.5;

            level.sendParticles(ModParticleTypes.GREEN_FLAME.get(),
                    start.x + Math.cos(angle) * radius,
                    start.y + 1.0 + offsetY,
                    start.z + Math.sin(angle) * radius,
                    1, 0.0, 0.1, 0.0, 0.05);
        }
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return stack.isDamaged();
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        return 0x00FF88;
    }
}
