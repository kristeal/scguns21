package top.kristeal.common;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.locale.Language;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import top.kristeal.Reference;
import top.kristeal.config.MobGuideConfig;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Ported to NeoForge 1.21.1: written books store their content in DataComponents.WRITTEN_BOOK_CONTENT instead of the
 * title/author/generation/pages/resolved NBT keys. The values are the same: the title and author are plain strings
 * (resolved with getString() as in the reference), pages are the guide's translatable components, resolved = false.
 */
public class MobGuideHelper {

    @Nullable
    public static ItemStack createGuideBook(EntityType<?> entityType) {
        MobGuideConfig.MobGuide guide = MobGuideConfig.getGuide(entityType);
        if (guide == null) {
            return null;
        }

        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);

        String title = resolve(guide.titleKey());
        String author = resolve(guide.titleKey().replace(".title", ".author"));

        List<Filterable<Component>> pages = new ArrayList<>();

        for (MobGuideConfig.GuidePage page : guide.getPages()) {
            Component textComponent = page.getTextComponent();
            pages.add(Filterable.passThrough(textComponent));
        }

        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough(title), author, 0, pages, false));

        return book;
    }

    /** English text bundled with the mod, for dedicated servers, which have no mod translations loaded. */
    @Nullable
    private static Map<String, String> bundledEnglish;

    /**
     * Fix for a reference bug: written books store their title and author as plain strings, so they are resolved here. A
     * dedicated server resolves mod keys to the key itself; then the mod's own en_us.json is used instead.
     */
    private static String resolve(String key) {
        Language language = Language.getInstance();
        if (language.has(key)) {
            return language.getOrDefault(key);
        }
        if (bundledEnglish == null) {
            bundledEnglish = new HashMap<>();
            try (InputStream in = MobGuideHelper.class.getResourceAsStream("/assets/" + Reference.MOD_ID + "/lang/en_us.json")) {
                if (in != null) {
                    Language.loadFromJson(in, bundledEnglish::put);
                }
            } catch (IOException ignored) {
                // falls back to the key
            }
        }
        return bundledEnglish.getOrDefault(key, key);
    }
}
