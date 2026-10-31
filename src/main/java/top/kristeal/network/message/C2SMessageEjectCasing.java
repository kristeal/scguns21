package top.kristeal.network.message;

import top.kristeal.common.GunStackData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Reference;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import top.kristeal.common.Gun;
import top.kristeal.event.GunEventBus;
import top.kristeal.init.ModEnchantments;
import top.kristeal.item.GunItem;

import java.util.Objects;

public class C2SMessageEjectCasing implements CustomPacketPayload
{
    public static final Type<C2SMessageEjectCasing> TYPE = new Type<>(Reference.id("c2s_eject_casing"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMessageEjectCasing> STREAM_CODEC = StreamCodec.ofMember(C2SMessageEjectCasing::encode, C2SMessageEjectCasing::decode);

    public C2SMessageEjectCasing() {}

    public void encode(RegistryFriendlyByteBuf buffer)
    {}

    public static C2SMessageEjectCasing decode(RegistryFriendlyByteBuf buffer) {
        return new C2SMessageEjectCasing();
    }

    public static void handle(C2SMessageEjectCasing message, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player != null && !player.isSpectator()) {
                ItemStack heldItem = player.getMainHandItem();
                if (heldItem.getItem() instanceof GunItem) {
                    if (!heldItem.getItem().getClass().getPackageName().startsWith("top.kristeal")) {
                        return;
                    }

                    Gun gun = ((GunItem)heldItem.getItem()).getModifiedGun(heldItem);
                    if (gun.getProjectile().casingType != null && !player.getAbilities().instabuild) {
                        ItemStack casingStack = new ItemStack(Objects.requireNonNull(BuiltInRegistries.ITEM.get(gun.getProjectile().casingType)));

                        double baseChance = 0.4;
                        int enchantmentLevel = ModEnchantments.getLevel(heldItem, ModEnchantments.SHELL_CATCHER);
                        double finalChance = baseChance + (enchantmentLevel * 0.15);

                        double roll = Math.random();

                        if (roll < finalChance) {
                            if (enchantmentLevel > 0) {
                                boolean addedDirectly = GunEventBus.addCasingDirectly(player, casingStack);

                                if (!addedDirectly) {
                                    GunEventBus.spawnCasingInWorld(player.level(), player, casingStack);
                                }
                            } else {
                                boolean addedToPouch = GunEventBus.addCasingToPouch(player, casingStack);

                                if (!addedToPouch) {
                                    GunEventBus.spawnCasingInWorld(player.level(), player, casingStack);
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
