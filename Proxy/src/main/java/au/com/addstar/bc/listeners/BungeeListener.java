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

package au.com.addstar.bc.listeners;

/*-
 * #%L
 * BungeeChat-Proxy
 * %%
 * Copyright (C) 2015 - 2020 AddstarMC
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import au.com.addstar.bc.BungeeChat;
import au.com.addstar.bc.sync.packet.MirrorPacket;
import au.com.addstar.bc.sync.packet.SendPacket;
import au.com.addstar.bc.util.Utilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Created for the AddstarMC Project. Created by Narimm on 24/09/2018.
 * Moved from the main class - this listener simply takes a old style pluginMessage and
 * rebroadcasts it on the redis connection...
 */
public class BungeeListener implements Listener {
    private String channelName;
    
    private BungeeChat plugin;
    
    public BungeeListener(String channelName, BungeeChat plugin) {
        this.channelName = channelName;
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onOldMessage(PluginMessageEvent event)
    {
        if (event.getTag().equals(channelName) && event.getSender() instanceof Server)
        {
            BungeeChat.instance.getLogger().log(Level.INFO,event.getTag()+" using PluginMessageChannel");
            ByteArrayInputStream stream = new ByteArrayInputStream(event.getData());
            DataInput input = new DataInputStream(stream);
            
            try
            {
                String subChannel = input.readUTF();
                if(subChannel.equals("Mirror"))
                {
                    String chatChannel = input.readUTF();
                    Component message = Utilities.SERIALIZER.deserialize(input.readUTF());
                    
                    plugin.getPacketManager().broadcast(new MirrorPacket(chatChannel, message));
                }
                else if (subChannel.equals("Send"))
                {
                    String idString = input.readUTF();
                    UUID id;
                    try
                    {
                        id = UUID.fromString(idString);
                    }
                    catch(IllegalArgumentException e)
                    {
                        ProxiedPlayer player = plugin.getProxy().getPlayer(idString);
                        if(player == null)
                            return;
                        id = player.getUniqueId();
                    }
    
                    plugin.getPacketManager().broadcast(new SendPacket(id, Utilities.SERIALIZER.deserialize(input.readUTF())));
                }
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}
