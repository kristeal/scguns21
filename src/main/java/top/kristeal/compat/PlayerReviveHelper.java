package top.kristeal.compat;

import net.minecraft.world.entity.player.Player;
import top.kristeal.ScorchedGuns;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Author: MrCrayfish
 * Reached by reflection, as in the reference, so PlayerRevive stays optional.
 * Port note: PlayerRevive 2.1 (1.21.1) returns the {@code team.creative.playerrevive.api.IBleeding} interface from
 * {@code PlayerReviveServer#getBleeding}; the reference looked {@code isBleeding} up on the {@code cap.Bleeding} class.
 */
public class PlayerReviveHelper
{
    private static boolean disable = false;
    private static Method getBleeding;
    private static Method isBleeding;

    public static boolean isBleeding(Player player)
    {
        if(!ScorchedGuns.playerReviveLoaded || disable)
            return false;
        try
        {
            init();
            if(disable)
                return false;
            Object object = getBleeding.invoke(null, player);
            return object != null && (boolean) isBleeding.invoke(object);
        }
        catch(InvocationTargetException | IllegalAccessException e)
        {
            disable = true;
        }
        return false;
    }

    private static void init()
    {
        if(getBleeding == null)
        {
            try
            {
                Class<?> playerReviveServer = Class.forName("team.creative.playerrevive.server.PlayerReviveServer");
                getBleeding = playerReviveServer.getDeclaredMethod("getBleeding", Player.class);
                Class<?> bleeding = Class.forName("team.creative.playerrevive.api.IBleeding");
                isBleeding = bleeding.getDeclaredMethod("isBleeding");
            }
            catch(ClassNotFoundException | NoSuchMethodException ignored)
            {
                disable = true;
            }
        }
    }
}
