package top.kristeal.world;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import top.kristeal.Reference;

import java.util.ArrayList;
import java.util.List;

//Villager homes
public class VillageStructures {
    public static void addNewVillageBuilding(final ServerAboutToStartEvent event) {
        Registry<StructureTemplatePool> templatePools = event.getServer().registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
        Registry<StructureProcessorList> processorLists = event.getServer().registryAccess().registryOrThrow(Registries.PROCESSOR_LIST);
        VillageStructures.addBuildingToPool(templatePools, processorLists, ResourceLocation.withDefaultNamespace("village/plains/houses"), Reference.MOD_ID + ":village/houses/plains_gunsmith_house", 8);
        VillageStructures.addBuildingToPool(templatePools, processorLists, ResourceLocation.withDefaultNamespace("village/snowy/houses"), Reference.MOD_ID + ":village/houses/snowy_gunsmith_house", 8);
        VillageStructures.addBuildingToPool(templatePools, processorLists, ResourceLocation.withDefaultNamespace("village/savanna/houses"), Reference.MOD_ID + ":village/houses/savanna_gunsmith_house", 8);
        VillageStructures.addBuildingToPool(templatePools, processorLists, ResourceLocation.withDefaultNamespace("village/desert/houses"), Reference.MOD_ID + ":village/houses/desert_gunsmith_house", 8);
        VillageStructures.addBuildingToPool(templatePools, processorLists, ResourceLocation.withDefaultNamespace("village/taiga/houses"), Reference.MOD_ID + ":village/houses/taiga_gunsmith_house", 8);
    }

    public static void addBuildingToPool(Registry<StructureTemplatePool> templatePoolRegistry, Registry<StructureProcessorList> processorListRegistry, ResourceLocation poolRL, String nbtPieceRL, int weight) {
        StructureTemplatePool pool = templatePoolRegistry.get(poolRL);
        if (pool == null) return; // Port note: the reference asserted; a missing pool (e.g. a datapack removing it) is skipped
        ResourceLocation emptyProcessor = ResourceLocation.withDefaultNamespace("empty");
        Holder<StructureProcessorList> processorHolder = processorListRegistry.getHolderOrThrow(ResourceKey.create(Registries.PROCESSOR_LIST, emptyProcessor));

        SinglePoolElement piece = SinglePoolElement.single(nbtPieceRL, processorHolder).apply(StructureTemplatePool.Projection.RIGID);

        for (int i = 0; i < weight; i++) {
            pool.templates.add(piece);
        }

        List<Pair<StructurePoolElement, Integer>> listOfPieceEntries = new ArrayList<>(pool.rawTemplates);
        listOfPieceEntries.add(new Pair<>(piece, weight));
        pool.rawTemplates = listOfPieceEntries;
    }
}
