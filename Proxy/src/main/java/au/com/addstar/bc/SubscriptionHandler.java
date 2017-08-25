package au.com.addstar.bc;

import java.util.*;

/**
 * Created for use for the Add5tar MC Minecraft server
 * Created by benjamincharlton on 22/08/2017.
 */
public class SubscriptionHandler {
    private HashMap<UUID, String > subscriptions;


    SubscriptionHandler(BungeeChat plugin) {
        this.subscriptions = new HashMap<>();
    }

    void setSubscribed(UUID uuid, String channel){
        if(channel != null && !channel.isEmpty()) {
            subscriptions.put(uuid, channel);
        }else{
            subscriptions.remove(uuid);
        }
    }

    void unSubscribe(UUID uuid){
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

    Map<UUID, String> getAllSubscriptions() {
        return subscriptions;
    }
}
