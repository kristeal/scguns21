package top.kristeal;

import net.minecraft.resources.ResourceLocation;

public class Reference
{
    public static final String MOD_ID = "scguns";

    public static ResourceLocation id(String string) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, string);
    }
}