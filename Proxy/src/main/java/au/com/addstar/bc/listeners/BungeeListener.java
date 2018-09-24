package au.com.addstar.bc.listeners;

import au.com.addstar.bc.BungeeChat;
import au.com.addstar.bc.sync.packet.MirrorPacket;
import au.com.addstar.bc.sync.packet.SendPacket;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.UUID;

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
            ByteArrayInputStream stream = new ByteArrayInputStream(event.getData());
            DataInput input = new DataInputStream(stream);
            
            try
            {
                String subChannel = input.readUTF();
                if(subChannel.equals("Mirror"))
                {
                    String chatChannel = input.readUTF();
                    String message = input.readUTF();
                    
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
    
                    plugin.getPacketManager().broadcast(new SendPacket(id, input.readUTF()));
                }
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}
