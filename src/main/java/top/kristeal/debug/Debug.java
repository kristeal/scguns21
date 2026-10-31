package top.kristeal.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.apache.commons.lang3.tuple.Pair;
import top.kristeal.Reference;
import top.kristeal.client.ClientHandler;
import top.kristeal.common.Gun;
import top.kristeal.debug.client.screen.widget.DebugButton;
import top.kristeal.debug.client.screen.widget.DebugToggle;
import top.kristeal.item.GunItem;
import top.kristeal.item.ScopeItem;
import top.kristeal.item.attachment.impl.Scope;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Author: MrCrayfish
 * <p>
 * Port note: {@code DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)} is an {@link FMLEnvironment#dist} check; the
 * client classes are only touched inside it.
 */
@EventBusSubscriber(modid = Reference.MOD_ID)
public class Debug
{
    private static final Map<Item, Gun> GUNS = new HashMap<>();
    private static final Map<Item, Scope> SCOPES = new HashMap<>();
    private static boolean forceAim = false;

    @SubscribeEvent
    public static void onServerStarting(ServerStartedEvent event)
    {
        // Resets the cache every time when joining a world
        event.getServer().execute(() ->
        {
            GUNS.clear();
            SCOPES.clear();
        });
    }

    public static Gun getGun(GunItem item)
    {
        return GUNS.computeIfAbsent(item, item1 -> item.getGun().copy());
    }

    public static Scope getScope(ScopeItem item)
    {
        return SCOPES.computeIfAbsent(item, item1 -> item.getProperties().copy());
    }

    public static boolean isForceAim()
    {
        return forceAim;
    }

    public static void setForceAim(boolean forceAim)
    {
        Debug.forceAim = forceAim;
    }

    public static class Menu implements IEditorMenu
    {
        @Override
        public Component getEditorLabel()
        {
            return Component.literal("Editor Menu");
        }

        @Override
        public void getEditorWidgets(List<Pair<Component, Supplier<IDebugWidget>>> widgets)
        {
            if (FMLEnvironment.dist.isClient()) {
                ItemStack heldItem = Objects.requireNonNull(Minecraft.getInstance().player).getMainHandItem();
                if(heldItem.getItem() instanceof GunItem gunItem)
                {
                    widgets.add(Pair.of(Component.translatable(gunItem.getDescriptionId()), () -> new DebugButton(Component.literal("Edit"), btn -> {
                        Minecraft.getInstance().setScreen(ClientHandler.createEditorScreen(getGun(gunItem)));
                    })));
                }
                widgets.add(Pair.of(Component.literal("Settings"), () -> new DebugButton(Component.literal(">"), btn -> {
                    Minecraft.getInstance().setScreen(ClientHandler.createEditorScreen(new Settings()));
                })));
            }
        }
    }

    public static class Settings implements IEditorMenu
    {
        @Override
        public Component getEditorLabel()
        {
            return Component.literal("Settings");
        }

        @Override
        public void getEditorWidgets(List<Pair<Component, Supplier<IDebugWidget>>> widgets)
        {
            if (FMLEnvironment.dist.isClient()) {
                widgets.add(Pair.of(Component.literal("Force Aim"), () -> new DebugToggle(Debug.forceAim, value -> Debug.forceAim = value)));
            }
        }
    }
}
