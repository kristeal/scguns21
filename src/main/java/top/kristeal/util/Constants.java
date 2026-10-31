package top.kristeal.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Rarity;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;

import java.util.function.UnaryOperator;

// Port note: 1.20.1 Rarity.create(...) is gone; 1.21 Rarity is an extensible enum. Each constant is an EnumProxy referenced
// from META-INF/enumextensions.json (id -1 is replaced by the loader, names must carry the mod id). Use getValue().
public class Constants {

    public static final EnumProxy<Rarity> OCEANIC = rarity("scguns:oceanic", style -> style.withColor(ChatFormatting.BLUE));
    public static final EnumProxy<Rarity> UNIQUE = rarity("scguns:unique", style -> style.withColor(ChatFormatting.GREEN));
    public static final EnumProxy<Rarity> PIGLISH = rarity("scguns:piglish", style -> style.withColor(ChatFormatting.GOLD));
    public static final EnumProxy<Rarity> SCORCHED = rarity("scguns:scorched", style -> style.withColor(ChatFormatting.RED));
    public static final EnumProxy<Rarity> DEEP_DARK = rarity("scguns:deep_dark", style -> style.withColor(ChatFormatting.DARK_AQUA));
    public static final EnumProxy<Rarity> ENDISH = rarity("scguns:endish", style -> style.withColor(ChatFormatting.DARK_PURPLE));
    public static final EnumProxy<Rarity> BIZARRE = rarity("scguns:bizarre", style -> style.withColor(ChatFormatting.GRAY));
    public static final EnumProxy<Rarity> TREATED_BRASS = rarity("scguns:treated_brass", style -> style.withColor(ChatFormatting.YELLOW));
    public static final EnumProxy<Rarity> DIAMOND_STEEL = rarity("scguns:diamond_steel", style -> style.withColor(ChatFormatting.AQUA));
    public static final EnumProxy<Rarity> WRECKER = rarity("scguns:wrecker", style -> style.withColor(ChatFormatting.DARK_GRAY));
    public static final EnumProxy<Rarity> RUSTY = rarity("scguns:rusty", style -> style.withColor(0xFF8C00));

    private static EnumProxy<Rarity> rarity(String name, UnaryOperator<Style> styleModifier) {
        return new EnumProxy<>(Rarity.class, -1, name, styleModifier);
    }
}
