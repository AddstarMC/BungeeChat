package au.com.addstar.bc;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Listener;

import java.util.*;

/**
 * Created for use for the Add5tar MC Minecraft server
 * Created by benjamincharlton on 22/08/2017.
 */
public class SubscriptionHandler implements Listener {
    private HashMap<UUID, String > subscriptions;
    private PlayerSettingsManager mPlayerManager;


    public SubscriptionHandler(BungeeChat plugin) {
        this.subscriptions = new HashMap<>();
        this.mPlayerManager = plugin.getManager();
        ProxyServer.getInstance().getPluginManager().registerListener(plugin, this);

    }

    public void setSubscribed(UUID uuid, String channel){
        subscriptions.put(uuid, channel);
    }

    public void unSubscribe(UUID uuid){
        subscriptions.remove(uuid);
    }

    public List<UUID> getSubscriptions(String channel) {
        List<UUID> result = new ArrayList<>();
        for(Map.Entry<UUID, String> entry: subscriptions.entrySet()){
            if(entry.getValue().equals(channel)){
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public Map<UUID, String> getAllSubscriptions() {
        return subscriptions;
    }
}
