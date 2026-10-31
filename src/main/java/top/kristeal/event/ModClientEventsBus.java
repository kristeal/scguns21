package top.kristeal.event;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import top.kristeal.Reference;
import top.kristeal.entity.client.*;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class ModClientEventsBus {
    @SubscribeEvent
    public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayers.COG_MINION_LAYER, CogMinionModel::createBodyLayer);
        event.registerLayerDefinition(ModModelLayers.COG_KNIGHT_LAYER, CogKnightModel::createBodyLayer);
        event.registerLayerDefinition(ModModelLayers.TRAUMA_UNIT_LAYER, TraumaUnitModel::createBodyLayer);
        event.registerLayerDefinition(ModModelLayers.SKY_CARRIER_LAYER, SkyCarrierModel::createBodyLayer);
        event.registerLayerDefinition(ModModelLayers.SUPPLY_SCAMP_LAYER, SupplyScampModel::createBodyLayer);
        event.registerLayerDefinition(ModModelLayers.BLUNDERER_LAYER, BlundererModel::createBodyLayer);
        event.registerLayerDefinition(ModModelLayers.DISSIDENT_LAYER, DissidentModel::createBodyLayer);
        event.registerLayerDefinition(ModModelLayers.PRAETOR_LAYER, PraetorModel::createBodyLayer);
        event.registerLayerDefinition(ModModelLayers.VIVENTRUM_LAYER, ViventrumModel::createBodyLayer);
        event.registerLayerDefinition(ModModelLayers.HIVE_LAYER, HiveModel::createBodyLayer);
        event.registerLayerDefinition(ModModelLayers.SWARM_LAYER, SwarmModel::createBodyLayer);
        event.registerLayerDefinition(ModModelLayers.HORNLIN_LAYER, HornlinModel::createBodyLayer);
        event.registerLayerDefinition(ModModelLayers.ZOMBIFIED_HORNLIN_LAYER, ZombifiedHornlinModel::createBodyLayer);
        event.registerLayerDefinition(ModModelLayers.THE_MERCHANT_LAYER, TheMerchantModel::createBodyLayer);
        event.registerLayerDefinition(ModModelLayers.SIGNAL_BEACON_LAYER, SignalBeaconModel::createBodyLayer);
        event.registerLayerDefinition(ModModelLayers.SCAMP_TANK_LAYER, ScampTankModel::createBodyLayer);
        event.registerLayerDefinition(ModModelLayers.SCAMP_ROCKET_LAYER, ScampRocketModel::createBodyLayer);
        event.registerLayerDefinition(ModModelLayers.SCAMPLER_LAYER, ScamplerModel::createBodyLayer);
        event.registerLayerDefinition(ModModelLayers.ADJUDICATOR_LAYER, AdjudicatorModel::createBodyLayer);
        event.registerLayerDefinition(ModModelLayers.SUBJUGATOR_LAYER, SubjugatorModel::createBodyLayer);
        event.registerLayerDefinition(ModModelLayers.MOTHER_GHAST_LAYER, MotherGhastModel::createBodyLayer);
        event.registerLayerDefinition(ModModelLayers.FINFORCER_LAYER, FinforcerModel::createBodyLayer);
        event.registerLayerDefinition(ModModelLayers.SULFURHEAD_LAYER, SulfurheadModel::createBodyLayer);
    }
}

