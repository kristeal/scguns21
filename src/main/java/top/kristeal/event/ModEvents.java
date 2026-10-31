package top.kristeal.event;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.event.village.WandererTradesEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import top.kristeal.Reference;
import top.kristeal.init.ModItems;
import top.kristeal.init.ModVillagers;

import java.util.List;

@EventBusSubscriber(modid = Reference.MOD_ID)
public class ModEvents {

    @SubscribeEvent
    public static void addCustomTrades(VillagerTradesEvent event) {

        if(event.getType() == ModVillagers.GUNSMITH.get()) {
            Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();

            // Level 1 Trades
            trades.get(1).add((pTrader, pRandom) -> new MerchantOffer(
                    new ItemCost(Items.EMERALD, 8),
                    new ItemStack(ModItems.GUN_GRIP.get(), 1),
                    10, 2, 0.02f));
            trades.get(1).add((pTrader, pRandom) -> new MerchantOffer(
                    new ItemCost(Items.EMERALD, 9),
                    new ItemStack(ModItems.GUN_BARREL.get(), 1),
                    10, 2, 0.02f));
            trades.get(1).add((pTrader, pRandom) -> new MerchantOffer(
                    new ItemCost(ModItems.SMALL_COPPER_CASING.get(), 6),
                    new ItemStack(Items.EMERALD, 1),
                    10, 2, 0.02f));
            trades.get(1).add((pTrader, pRandom) -> new MerchantOffer(
                    new ItemCost(Items.IRON_INGOT, 4),
                    new ItemStack(Items.EMERALD, 1),
                    12, 5, 0.02f));
            trades.get(1).add((pTrader, pRandom) -> new MerchantOffer(
                    new ItemCost(ModItems.BUCKSHOT.get(), 4),
                    new ItemStack(Items.EMERALD, 1),
                    16, 5, 0.02f));
            trades.get(1).add((pTrader, pRandom) -> new MerchantOffer(
                    new ItemCost(Items.EMERALD, 15),
                    new ItemStack(ModItems.LONGARM.get(), 1),
                    3, 10, 0.05f));
            trades.get(1).add((pTrader, pRandom) -> new MerchantOffer(
                    new ItemCost(Items.EMERALD, 2),
                    new ItemStack(ModItems.SMALL_IRON_CASING.get(), 8),
                    10, 10, 0.05f));
            trades.get(1).add((pTrader, pRandom) -> new MerchantOffer(
                    new ItemCost(ModItems.ANTHRALITE_INGOT.get(), 3),
                    new ItemStack(Items.EMERALD, 1),
                    12, 10, 0.05f));

            // Level 2 Trades
            trades.get(2).add((pTrader, pRandom) -> new MerchantOffer(
                    new ItemCost(Items.EMERALD, 25),
                    new ItemStack(ModItems.COPPER_FLARE.get(), 1),
                    2, 12, 0.05f));

            // Level 3 Trades
            trades.get(3).add((pTrader, pRandom) -> new MerchantOffer(
                    new ItemCost(Items.EMERALD, 9),
                    new ItemStack(ModItems.LASER_SIGHT.get(), 1),
                    6, 15, 0.05f));
            trades.get(3).add((pTrader, pRandom) -> new MerchantOffer(
                    new ItemCost(Items.EMERALD, 8),
                    new ItemStack(ModItems.RIFLE_AMMO_BOX.get(), 1),
                    6, 15, 0.05f));
            trades.get(3).add((pTrader, pRandom) -> new MerchantOffer(
                    new ItemCost(ModItems.GUN_PARTS.get(), 1),
                    new ItemStack(Items.EMERALD, 4),
                    10, 15, 0.05f));
            trades.get(3).add((pTrader, pRandom) -> new MerchantOffer(
                    new ItemCost(Items.GUNPOWDER, 8),
                    new ItemStack(Items.EMERALD, 1),
                    16, 15, 0.05f));
            trades.get(3).add((pTrader, pRandom) -> new MerchantOffer(
                    new ItemCost(Items.EMERALD, 8),
                    new ItemStack(ModItems.EXTENDED_MAG.get(), 1),
                    10, 10, 0.05f));
            trades.get(3).add((pTrader, pRandom) -> new MerchantOffer(
                    new ItemCost(Items.EMERALD, 15),
                    new ItemStack(ModItems.MUSKET.get(), 1),
                    2, 12, 0.05f));

            // Level 4 Trades
            trades.get(4).add((pTrader, pRandom) -> new MerchantOffer(
                    new ItemCost(Items.EMERALD, 25),
                    new ItemStack(ModItems.FENCER_CARABINE.get(), 1),
                    4, 20, 0.05f));
            trades.get(4).add((pTrader, pRandom) -> new MerchantOffer(
                    new ItemCost(Items.EMERALD, 16),
                    new ItemStack(ModItems.LONG_SCOPE.get(), 1),
                    4, 20, 0.05f));
            trades.get(4).add((pTrader, pRandom) -> new MerchantOffer(
                    new ItemCost(ModItems.MEDIUM_BRASS_CASING.get(), 6),
                    new ItemStack(Items.EMERALD, 1),
                    10, 20, 0.05f));
            trades.get(4).add((pTrader, pRandom) -> new MerchantOffer(
                    new ItemCost(Items.BLAZE_POWDER, 4),
                    new ItemStack(Items.EMERALD, 1),
                    12, 20, 0.05f));
            trades.get(4).add((pTrader, pRandom) -> new MerchantOffer(
                    new ItemCost(Items.EMERALD, 1),
                    new ItemStack(ModItems.BLANK_MOLD.get(), 1),
                    5, 20, 0.05f));

            // Level 5 Trades (Fix: the reference added these two to level 4)
            trades.get(5).add((pTrader, pRandom) -> new MerchantOffer(
                    new ItemCost(Items.EMERALD, 32),
                    new ItemStack(ModItems.SAKETINI.get(), 1),
                    1, 25, 0.05f));
            trades.get(5).add((pTrader, pRandom) -> new MerchantOffer(
                    new ItemCost(Items.EMERALD, 35),
                    new ItemStack(ModItems.WINNIE_MILLEND.get(), 1),
                    1, 25, 0.05f));
            trades.get(5).add((pTrader, pRandom) -> new MerchantOffer(
                    new ItemCost(Items.EMERALD, 25),
                    new ItemStack(ModItems.IRON_FLARE.get(), 1),
                    1, 30, 0.05f));
            trades.get(5).add((pTrader, pRandom) -> new MerchantOffer(
                    new ItemCost(ModItems.NITRO_POWDER.get(), 1),
                    new ItemStack(Items.EMERALD, 28),
                    8, 25, 0.05f));
            trades.get(5).add((pTrader, pRandom) -> new MerchantOffer(
                    new ItemCost(ModItems.ANCIENT_BRASS.get(), 32),
                    new ItemStack(ModItems.WRECKER_FLARE.get(), 1),
                    8, 25, 0.05f));
        }
    }

    @SubscribeEvent
    public static void addCustomWanderingTrades(WandererTradesEvent event) {
        List<VillagerTrades.ItemListing> rareTrades = event.getRareTrades();
        List<VillagerTrades.ItemListing> trades = event.getGenericTrades();

        trades.add((pTrader, pRandom) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 24),
                new ItemStack(ModItems.SCAMP_PACKAGE.get(), 1),
                1, 12, 0.15f));
        trades.add((pTrader, pRandom) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 24),
                new ItemStack(ModItems.WRECKER_FLARE.get(), 1),
                1, 12, 0.15f));
        trades.add((pTrader, pRandom) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 29),
                new ItemStack(ModItems.WRECKER_FLARE.get(), 1),
                1, 12, 0.15f));
        trades.add((pTrader, pRandom) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 29),
                new ItemStack(ModItems.OCEAN_FLARE.get(), 1),
                1, 12, 0.15f));
        rareTrades.add((pTrader, pRandom) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 23),
                new ItemStack(ModItems.VEHEMENT_COAL.get(), 1),
                1, 12, 0.15f));
    }
}
