package top.kristeal.init;

import top.kristeal.item.EnergyGunItem;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import top.kristeal.Reference;
import top.kristeal.blockentity.MaceratorBlockEntity;
import top.kristeal.blockentity.MechanicalPressBlockEntity;
import top.kristeal.blockentity.PoweredMaceratorBlockEntity;
import top.kristeal.blockentity.PoweredMechanicalPressBlockEntity;
import top.kristeal.item.AirCanisterItem;
import top.kristeal.item.exosuit.ExoSuitCoreItem;

/**
 * Central registry for NeoForge 1.21.1 capabilities.
 * Replaces Forge 1.20.1 Item#initCapabilities and BlockEntity#getCapability overrides across the mod.
 */
@EventBusSubscriber(modid = Reference.MOD_ID)
public class ModCapabilities {

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // ExoSuit Core energy storage capabilities (accessed both server-side for power drain and client-side for HUD)
        event.registerItem(
                Capabilities.EnergyStorage.ITEM,
                (stack, context) -> {
                    if (stack.getItem() instanceof ExoSuitCoreItem coreItem) {
                        return ExoSuitCoreItem.createEnergyStorage(stack, coreItem.getTier());
                    }
                    return null;
                },
                ModItems.EXO_SUIT_CORE.get(),
                ModItems.ADVANCED_EXO_SUIT_CORE.get()
        );

        // Air canisters store air as energy (read by air guns and the Lightning Battery once ported)
        event.registerItem(
                Capabilities.EnergyStorage.ITEM,
                (stack, context) -> stack.getItem() instanceof AirCanisterItem canister ? canister.createEnergyStorage(stack) : null,
                ModItems.AIR_CANISTER.get(),
                ModItems.REINFORCED_AIR_CANISTER.get(),
                ModItems.CREATIVE_AIR_CANISTER.get()
        );

        // Energy guns. No reference gun is an EnergyGunItem, so this registers nothing until one exists
        // (registerItem rejects an empty item list).
        net.minecraft.world.item.Item[] energyGuns = net.minecraft.core.registries.BuiltInRegistries.ITEM.stream()
                .filter(item -> item instanceof EnergyGunItem).toArray(net.minecraft.world.item.Item[]::new);
        if (energyGuns.length > 0) {
            event.registerItem(Capabilities.EnergyStorage.ITEM,
                    (stack, context) -> stack.getItem() instanceof EnergyGunItem gun ? gun.createEnergyStorage(stack) : null,
                    energyGuns);
        }

        // Processing machines (replacing 1.20.1 BlockEntity#getCapability; side mapping lives in each block entity)
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.MACERATOR.get(), MaceratorBlockEntity::getItemHandler);
        // Research progression: insert-only material buffer
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.RESEARCH_STATION.get(), top.kristeal.blockentity.ResearchStationBlockEntity::getItemHandler);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.RESEARCH_STATION.get(), top.kristeal.blockentity.ResearchStationBlockEntity::getEnergyStorage);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.POWERED_MACERATOR.get(), PoweredMaceratorBlockEntity::getItemHandler);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.MECHANICAL_PRESS.get(), MechanicalPressBlockEntity::getItemHandler);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.POWERED_MECHANICAL_PRESS.get(), PoweredMechanicalPressBlockEntity::getItemHandler);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.POWERED_MACERATOR.get(), PoweredMaceratorBlockEntity::getEnergyStorage);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.POWERED_MECHANICAL_PRESS.get(), PoweredMechanicalPressBlockEntity::getEnergyStorage);

        // Power blocks and the vent collector (lambdas because Thermolith/Cryoniter/VentCollector also keep the
        // reference's no-argument getItemHandler()). The Advanced Composter is a WorldlyContainer and has no capability, as in the reference.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.POLAR_GENERATOR.get(), (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.POLAR_GENERATOR.get(), (be, side) -> be.getEnergyStorage(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.LIGHTNING_BATTERY.get(), (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.LIGHTNING_BATTERY.get(), (be, side) -> be.getEnergyStorage(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.THERMOLITH.get(), (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.CRYONITER.get(), (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.VENT_COLLECTOR.get(), (be, side) -> be.getItemHandler(side));

        // Turrets (TurretBlockEntity#getItemHandler keeps the reference's "every side but UP" rule). The Ammo and
        // Shell Catcher modules are BaseContainerBlockEntitys, which Forge 1.20.1 exposed automatically as an unsided
        // InvWrapper; NeoForge 1.21 only does that for vanilla types, so the same wrapper is registered here.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.BASIC_TURRET.get(), (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.SHOTGUN_TURRET.get(), (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.AUTO_TURRET.get(), (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.SNIPER_TURRET.get(), (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.AMMO_MODULE.get(), (be, side) -> new InvWrapper(be));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.SHELL_CATCHER_MODULE.get(), (be, side) -> new InvWrapper(be));
        // The ammo box block (same automatic Forge 1.20.1 BaseContainerBlockEntity wrapper as the modules above)
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.AMMO_BOX.get(), (be, side) -> new InvWrapper(be));

    }
}
