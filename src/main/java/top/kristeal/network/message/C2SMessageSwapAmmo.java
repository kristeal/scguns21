package top.kristeal.network.message;

import top.kristeal.common.GunStackData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.kristeal.Reference;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.kristeal.common.Gun;
import top.kristeal.common.ReloadType;
import top.kristeal.init.ModSyncedDataKeys;
import top.kristeal.item.GunItem;
import top.kristeal.network.PacketHandler;

public class C2SMessageSwapAmmo implements CustomPacketPayload
{
    public static final Type<C2SMessageSwapAmmo> TYPE = new Type<>(Reference.id("c2s_swap_ammo"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMessageSwapAmmo> STREAM_CODEC = StreamCodec.ofMember(C2SMessageSwapAmmo::encode, C2SMessageSwapAmmo::decode);


    public C2SMessageSwapAmmo() {
    }

    public void encode(RegistryFriendlyByteBuf buffer)
    {
    }
    public static C2SMessageSwapAmmo decode(RegistryFriendlyByteBuf buffer) {
        return new C2SMessageSwapAmmo();
    }
    public static void handle(C2SMessageSwapAmmo message, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null) return;

            ItemStack heldItem = player.getMainHandItem();
            if (!(heldItem.getItem() instanceof GunItem gunItem)) {
                return;
            }

            Gun modifiedGun = gunItem.getModifiedGun(heldItem);
            Gun.General general = modifiedGun.getGeneral();

            if (!general.allowsAmmoChange()) {
                player.sendSystemMessage(
                        Component.translatable("message.scguns.ammo_swap.not_supported")
                                .withStyle(ChatFormatting.RED),
                        true
                );
                return;
            }

            if (general.getAvailableAmmoTypes().isEmpty()) {
                player.sendSystemMessage(
                        Component.translatable("message.scguns.ammo_swap.no_types")
                                .withStyle(ChatFormatting.RED),
                        true
                );
                return;
            }

            // Port note: ammo count is a component; reload keys stay in the live state compound
            CompoundTag tag = GunStackData.liveState(heldItem);
            int currentAmmo = GunStackData.getAmmoCount(heldItem);

            if (currentAmmo > 0 && !player.isCreative()) {
                Item currentAmmoItem = modifiedGun.getProjectile(heldItem).getItem();
                assert currentAmmoItem != null;
                ItemStack ammoStack = new ItemStack(currentAmmoItem, currentAmmo);
                if (!player.getInventory().add(ammoStack)) {
                    player.drop(ammoStack, false);
                }
            }

            GunStackData.setAmmoCount(heldItem, 0);
            PacketHandler.getPlayChannel().sendToPlayer(() -> player, new S2CMessageUpdateAmmo(0));

            int currentIndex = general.getCurrentAmmoTypeIndex();
            int nextIndex = (currentIndex + 1) % general.getAvailableAmmoTypes().size();

            // Port note: the "Gun" compound lives in the GUN_OVERRIDE component
            GunStackData.updateGunOverride(heldItem, override -> {
                CompoundTag gunTag = override.getCompound("Gun");
                CompoundTag generalTag = gunTag.getCompound("General");
                generalTag.putInt("CurrentAmmoTypeIndex", nextIndex);
                gunTag.put("General", generalTag);
                override.put("Gun", gunTag);
            });

            modifiedGun.getGeneral().setCurrentAmmoTypeIndex(nextIndex);

            String ammoType = general.getAvailableAmmoTypes().get(nextIndex);

            String displayName = ammoType;
            if (ammoType.contains(":")) {
                displayName = ammoType.substring(ammoType.indexOf(":") + 1);
            }
            displayName = displayName.replace("_", " ");
            String[] words = displayName.split(" ");
            StringBuilder titleCase = new StringBuilder();
            for (String word : words) {
                if (!word.isEmpty()) {
                    titleCase.append(Character.toUpperCase(word.charAt(0)))
                            .append(word.substring(1).toLowerCase())
                            .append(" ");
                }
            }

            Component statusMessage = Component.translatable("message.scguns.ammo_swap.changed")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(titleCase.toString().trim()).withStyle(ChatFormatting.YELLOW));

            player.sendSystemMessage(statusMessage, true);

            var ammoContext = Gun.findAmmo(player, modifiedGun.getProjectile(heldItem).getItem());
            if (!ammoContext.stack().isEmpty() || player.isCreative()) {
                ModSyncedDataKeys.RELOADING.setValue(player, true);
                tag.putBoolean("IsReloading", true);
                tag.putBoolean("scguns:IsReloading", true);

                if (modifiedGun.getReloads().getReloadType() == ReloadType.MANUAL) {
                    tag.putBoolean("IsManualReload", true);
                    tag.putString("scguns:ReloadState", "NONE");
                } else {
                    tag.putBoolean("InCriticalReloadPhase", true);
                }

                PacketHandler.getPlayChannel().sendToPlayer(() -> player, new S2CMessageReload(true));
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
