package au.com.addstar.bc;

import au.com.addstar.bc.sync.IPacketHandler;
import au.com.addstar.bc.sync.Packet;
import au.com.addstar.bc.sync.PacketManager;
import au.com.addstar.bc.sync.packet.GlobalMutePacket;
import au.com.addstar.bc.sync.packet.MirrorPacket;
import au.com.addstar.bc.sync.packet.PlayerListRequestPacket;
import au.com.addstar.bc.sync.packet.PlayerSettingsPacket;
import au.com.addstar.bc.sync.packet.UpdateNamePacket;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.player.PlayerSettings;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

/**
 * Created for the AddstarMC Project.
 * Created by Narimm on 6/06/2019.
 */
public class PacketHandler implements IPacketHandler
{
    private ProxyChat plugin;

    public PacketHandler(ProxyChat plugin) {
        this.plugin = plugin;
    }

    private PacketManager getPacketManager()
    {
        return plugin.getPacketManager();
    }

    private PlayerSettingsManager getManager()
    {
        return plugin.getManager();
    }

    @Override
    public void handle(Packet packet, RegisteredServer sender )
    {
        if(packet instanceof MirrorPacket)
            handleMirror((MirrorPacket)packet, sender.getServerInfo());
        else if(packet instanceof PlayerSettingsPacket)
            handlePlayerSettings((PlayerSettingsPacket)packet);
        else if(packet instanceof UpdateNamePacket)
            handleUpdateName((UpdateNamePacket)packet);
        else if(packet instanceof GlobalMutePacket)
            handleGMute((GlobalMutePacket)packet);
        else if(packet instanceof PlayerListRequestPacket)
            handlePlayerListRequest((PlayerListRequestPacket)packet, sender.getServerInfo());
    }

    private void handleMirror(MirrorPacket packet, ServerInfo sender)
    {
        plugin.getServer().getEventManager().fire(new BCChatEvent(packet.getChannel(), packet.getMessage()));
        if(!packet.getChannel().startsWith("~"))
            ProxyServer.getInstance().getConsole().sendMessage(TextComponent.fromLegacyText(packet.getMessage()));
    }

    private void handlePlayerSettings(PlayerSettingsPacket packet)
    {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(packet.getID());
        if (player == null)
        {
            ProxyChat.instance.getLogger().severe("!!!!! A request to update settings for offline player " + packet.getID() + " was made. This is an error !!!!");
            return;
        }

        Debugger.log("Updating player settings %s", player.getName());
        PlayerSettings settings = getManager().getSettings(player);
        settings.read(packet);
        getManager().savePlayer(player);
        String defaultChannel = settings.defaultChannel;
        if(defaultChannel.isEmpty()){
            ProxyChat.instance.getSubHandler().unSubscribe(packet.getID());
        }else{
            ProxyChat.instance.getSubHandler().setSubscribed(player.getUniqueId(),defaultChannel);
        }
        String oldName = settings.nickname;
        if(settings.nickname.isEmpty())
            player.setDisplayName(player.getName());
        else
            player.setDisplayName(settings.nickname);

        if(!oldName.equals(settings.nickname))
        {
            Debugger.log("Updating player name %s to '%s'", player.getName(), settings.nickname);
            getPacketManager().broadcast(new UpdateNamePacket(packet.getID(), settings.nickname));
        }
    }

    private void handleUpdateName(UpdateNamePacket packet)
    {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(packet.getID());
        if (player == null)
            return;

        PlayerSettings settings = getManager().getSettings(player);
        settings.nickname = packet.getName();

        getManager().savePlayer(player);

        String oldName = settings.nickname;
        if(settings.nickname.isEmpty())
            player.setDisplayName(player.getName());
        else
            player.setDisplayName(settings.nickname);

        if(!oldName.equals(settings.nickname))
        {
            Debugger.log("Updating player name %s to '%s'", player.getName(), settings.nickname);
            getPacketManager().broadcast(new UpdateNamePacket(packet.getID(), settings.nickname));
        }
    }

    private void handleGMute(GlobalMutePacket packet)
    {
        ProxyChat.instance.getMuteHandler().setGMute(packet.getTime());
    }

    private void handlePlayerListRequest(PlayerListRequestPacket packet, ServerInfo sender )
    {
        Debugger.log("Server %s requested player list", sender.getName());
        ProxyChat.instance.sendPlayerUpdates(sender);
    }
}
