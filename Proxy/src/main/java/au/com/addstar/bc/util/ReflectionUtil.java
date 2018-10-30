package au.com.addstar.bc.util;

import au.com.addstar.bc.BungeeChat;

import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.tab.TabList;

import java.lang.reflect.Field;

/**
 * Created for use for the Add5tar MC Minecraft server
 * Created by benjamincharlton on 27/10/2018.
 */
public class ReflectionUtil {

    public static void setTablistHandler(ProxiedPlayer player, TabList tablistHandler) throws NoSuchFieldException, IllegalAccessException {
        setField(UserConnection.class, player, "tabListHandler", tablistHandler, 5);
    }

    public static TabList getTabListHandler(ProxiedPlayer player) {
        TabList result;
        try{
            result = getField(UserConnection.class,player,"tabListHandler",5);
        }catch (NoSuchFieldException| IllegalAccessException e){
            BungeeChat.instance.getLogger().warning("Could Not retrieve TablistHandler");
            result = null;
        }catch (ClassCastException e){
            BungeeChat.instance.getLogger().warning("Retrieved Tablist was not an instance of Tablist");
            result = null;
        }
        return result;
    }

    public static void setField(Class<?> clazz, Object instance, String field, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field f = clazz.getDeclaredField(field);
        f.setAccessible(true);
        f.set(instance, value);
    }

    public static void setField(Class<?> clazz, Object instance, String field, Object value, int tries) throws NoSuchFieldException, IllegalAccessException {
        while (--tries > 0) {
            try {
                setField(clazz, instance, field, value);
                return;
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
            }
        }
        setField(clazz, instance, field, value);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getField(Class<?> clazz, Object instance, String field) throws NoSuchFieldException, IllegalAccessException {
        Field f = clazz.getDeclaredField(field);
        f.setAccessible(true);
        return (T) f.get(instance);
    }

    public static <T> T getField(Class<?> clazz, Object instance, String field, int tries) throws NoSuchFieldException, IllegalAccessException {
        while (--tries > 0) {
            try {
                return getField(clazz, instance, field);
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
            }
        }
        return getField(clazz, instance, field);
    }
}
