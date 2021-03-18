package au.com.addstar.bc.sync;

import au.com.addstar.bc.ProxyChat;
import au.com.addstar.bc.sync.packet.CallFailedResponsePacket;
import au.com.addstar.bc.sync.packet.CallPacket;
import au.com.addstar.bc.sync.packet.CallSuccessResponsePacket;
import au.com.addstar.bc.sync.packet.ConfigPacket;
import au.com.addstar.bc.sync.packet.ConfigRequestPacket;

import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

import java.beans.PropertyChangeEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Created for the AddstarMC Project.
 * Created by Narimm on 6/06/2019.
 */
public class SyncManager implements EventHandler<DisconnectEvent>, IPacketHandler
{
    private HashMap<String, SyncMethod> mMethods;
    private HashMap<String, SyncConfig> mConfigs;
    private ProxyChat plugin;

    private WeakHashMap<Player, HashMap<String, Object>> mPlayerProperties;

    public SyncManager(ProxyChat plugin)
    {
        //todo do we need to register this listener??
        this.plugin = plugin;
        mMethods = new HashMap<>();
        mConfigs = new HashMap<>();
        mPlayerProperties = new WeakHashMap<>();
        plugin.addHandler(this, CallPacket.class, ConfigRequestPacket.class);

        StorageMethods storage = new StorageMethods();
        addMethod("bungee:setProperty", storage);
        addMethod("bungee:getProperty", storage);
        addMethod("bungee:getProperties", storage);
    }

    public void addMethod(String name, SyncMethod method)
    {
        mMethods.put(name, method);
    }

    public void setConfig(String name, SyncConfig config)
    {
        mConfigs.put(name, config);
        sendConfig(name);
    }

    @Override
    public void handle( Packet packet, RegisteredServer sender )
    {
        if(packet instanceof CallPacket)
            doSyncMethod((CallPacket)packet, sender);
        else if(packet instanceof ConfigRequestPacket)
            sendConfig(((ConfigRequestPacket)packet).getName(), sender.getServerInfo());
    }

    public void execute(DisconnectEvent event)
    {
        mPlayerProperties.remove(event.getPlayer());
    }

    private void doSyncMethod(CallPacket packet, RegisteredServer caller)
    {
        String name = packet.getMethod();
        int id = packet.getId();
        List<Object> args = packet.getArgs();

        SyncMethod method = mMethods.get(name);

        if(method != null)
        {
            try
            {
                Object result = method.run(name, caller, args.toArray());

                plugin.getPacketManager().send(new CallSuccessResponsePacket(id, result), caller);
            }
            catch (IllegalArgumentException e){
                plugin.getPacketManager().send(new CallFailedResponsePacket(id, e.getClass().getSimpleName(), e.getMessage() == null ? "" : e.getMessage()), caller);
                plugin.getLogger().warn(e.getMessage());
            }
            catch(Exception e)
            {
                plugin.getPacketManager().send(new CallFailedResponsePacket(id, e.getClass().getSimpleName(), e.getMessage() == null ? "" : e.getMessage()), caller);
                e.printStackTrace();
            }
        }
        else
        {
            plugin.getPacketManager().send(new CallFailedResponsePacket(id, "NoSuchMethodException", name + " cannot be found"), caller);
        }
    }

    public void  sendConfig(String name, ServerInfo server)
    {
        SyncConfig config = mConfigs.get(name);
        if(config == null)
            return;

        ConfigPacket packet = new ConfigPacket(name, config);

        if(server == null)
            plugin.getPacketManager().broadcast(packet);
        else
            plugin.getPacketManager().send(packet, server);
    }

    public void sendConfig(String name)
    {
        sendConfig(name, null);
    }

    public void setProperty(Player player, String property, Object value)
    {
        HashMap<String, Object> values = mPlayerProperties.computeIfAbsent(player, k -> new HashMap<>());

        Object oldValue;
        if(value == null)
            oldValue = values.remove(property);
        else
            oldValue = values.put(property, value);

        plugin.getPluginManager().callEvent(new PropertyChangeEvent(player, property, oldValue, value));
    }

    public Object getProperty(Player player, String property)
    {
        HashMap<String, Object> values = mPlayerProperties.get(player);
        if(values == null)
            return null;

        return values.get(property);
    }

    public Map<String, Object> getProperties(Player player){
        return mPlayerProperties.get(player);
    }

    public Map<String, Object> getProperties(String property)
    {
        HashMap<String, Object> values = new HashMap<>();
        for(Map.Entry<Player, HashMap<String, Object>> map : mPlayerProperties.entrySet())
        {
            HashMap<String, Object> properties = map.getValue();
            if(properties.containsKey(property))
                values.put(map.getKey().getUniqueId().toString(), properties.get(property));
        }

        return values;
    }

    public boolean getPropertyBoolean(Player player, String property, boolean def)
    {
        Object value = getProperty(player, property);
        if(value == null)
            return def;

        if(value instanceof Number)
            return ((Number)value).byteValue() != 0;
        if(value instanceof Boolean)
            return (Boolean)value;

        return def;
    }

    public Collection<String> getPropertyNames(Player player, String prefix)
    {
        HashMap<String, Object> values = mPlayerProperties.get(player);
        if(values == null)
            return Collections.emptySet();

        HashSet<String> properties = new HashSet<>();
        for(String key : values.keySet())
        {
            if(key.startsWith(prefix))
                properties.add(key);
        }

        return properties;
    }


    private class StorageMethods implements SyncMethod
    {
        @Override
        public Object run(String name, RegisteredServer server, Object... arguments )
        {
            switch (name) {
                case "bungee:setProperty": {
                    if (arguments.length != 3)
                        throw new IllegalArgumentException("Arguments: <player> <property> <value>");

                    Player player = plugin.getPlayer((UUID) arguments[0]);
                    if (player == null)
                        throw new IllegalArgumentException("Unknown player");

                    setProperty(player, (String) arguments[1], arguments[2]);

                    return null;
                }
                case "bungee:getProperty": {
                    if (arguments.length != 2)
                        throw new IllegalArgumentException("Arguments: <player> <property>");

                    Player player = plugin.getPlayer((UUID) arguments[0]);
                    if (player == null)
                        throw new IllegalArgumentException("Unknown player");

                    return getProperty(player, (String) arguments[1]);
                }
                case "bungee:getProperties":
                    if (arguments.length != 1)
                        throw new IllegalArgumentException("Arguments: <property>");

                    return getProperties((String) arguments[0]);
                default:
                    return null;
            }
        }
    }
}
