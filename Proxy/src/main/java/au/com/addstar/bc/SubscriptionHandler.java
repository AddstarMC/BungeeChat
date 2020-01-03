/*
 * BungeeChat
 *
 * Copyright (c) 2015 - 2020.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy   of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is *
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR  IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
 * EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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

    public void setSubscribed(UUID uuid, String channel){
        if(channel != null && !channel.isEmpty()) {
            subscriptions.put(uuid, channel);
        }else{
            subscriptions.remove(uuid);
        }
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

    Map<String, String> getAllSubscriptions() {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<UUID,String> e : subscriptions.entrySet()){
            result.put(e.getKey().toString(),e.getValue());
        }
        return result;
    }
}
