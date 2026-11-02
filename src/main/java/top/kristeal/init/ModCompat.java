package top.kristeal.init;

import net.neoforged.fml.ModList;
//Moved from main class
public class ModCompat {
    public static boolean isCreateLoaded() {
        return ModList.get().isLoaded("create");
    }
    public static boolean isFarmersDelightLoaded() {
        return ModList.get().isLoaded("farmersdelight");
    }
    public static boolean isIELoaded() {
        return ModList.get().isLoaded("immersiveengineering");
    }
    public static boolean isMekanismLoaded() {
        return ModList.get().isLoaded("mekanism");
    }
    public static boolean isCreateIronworksLoaded() {
        return ModList.get().isLoaded("create_ironworks");
    }
    public static boolean playerReviveLoaded(){ return ModList.get().isLoaded("playerrevive");}
    public static boolean soulFiredLoaded(){ return ModList.get().isLoaded("soul_fire_d");}
}
