package au.com.addstar.bc;

import au.com.addstar.bc.sync.SyncMethod;
import au.com.addstar.bc.sync.packet.MirrorPacket;
import au.com.addstar.bc.sync.packet.PlayerRefreshPacket;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Created for the AddstarMC Project.
 * Created by Narimm on 6/06/2019.
 */
public class StandardSrvMethods implements SyncMethod
{
    @Override
    public Object run(String name, RegisteredServer server, Object... arguments ) throws IllegalArgumentException
    {
        switch (name) {
            case "bungee:getServerName":
                return getServer(server);
            case "bchat:isAFK":
                return isAFK((UUID) arguments[0]);
            case "bchat:canMsg":
                return canMsg((UUID) arguments[0]);
            case "bchat:setAFK":
                return setAFK((UUID) arguments[0], (Boolean) arguments[1]);
            case "bchat:toggleAFK":
                return toggleAFK((UUID) arguments[0]);
            case "bchat:setTabColor":
                return setTabColor((UUID) arguments[0], (String) arguments[1]);
            case "bchat:setMute":
                return setMute((UUID) arguments[0], (Long) arguments[1]);
            case "bchat:setMuteIP":
                return setMuteIP(arguments[0], (Long) arguments[1]);
            case "bchat:setGMute":
                return setGMute((Long) arguments[0]);
            case "bchat:toggleGMute":
                return toggleGMute();
            case "bchat:setMsgTarget":
                return setMsgTarget((UUID) arguments[0], (UUID) arguments[1]);
            case "bchat:getMuteList":
                return getMuteList();
            case "bchat:kick":
                return kickPlayer((UUID) arguments[0], (String) arguments[1]);
            case "bchat:setSkin":
                return setSkin((UUID) arguments[0], arguments[1]);
            case "bchat:getSubscribed":
                return getSubscribed();
            default:
                return null;
        }
    }

    private HashMap<String, String> getSubscribed() {
        HashMap<String, String> result = new HashMap<>(BungeeChat.instance.getSubHandler().getAllSubscriptions());
        return result;
    }

    public String getServer(RegisteredServer server)
    {
        return server.getServerInfo().getName();
    }

    public boolean isAFK(UUID player) throws IllegalArgumentException
    {
        ProxiedPlayer pplayer = ProxyServer.getInstance().getPlayer(player);
        if(pplayer == null)
            throw new IllegalArgumentException("That player is not online" + this.getClass().getCanonicalName() + ":isAFK()."+player);

        PlayerSettings settings = BungeeChat.instance.getManager().getSettings(pplayer);
        return settings.isAFK;
    }

    public boolean canMsg(UUID player) throws IllegalArgumentException
    {
        ProxiedPlayer pplayer = ProxyServer.getInstance().getPlayer(player);
        if(pplayer == null)
            throw new IllegalArgumentException("That player is not online" + this.getClass().getCanonicalName() + ":canMsg()."+player);

        return BungeeChat.instance.getManager().getSettings(pplayer).msgEnabled;
    }

    public Void setAFK(UUID player, boolean afk) throws IllegalArgumentException
    {
        ProxiedPlayer pplayer = ProxyServer.getInstance().getPlayer(player);
        if(pplayer == null)
            throw new IllegalArgumentException("That player is not online" + this.getClass().getCanonicalName() + ":setAFK()."+player);

        BungeeChat.instance.getManager().getSettings(pplayer).isAFK = afk;

        return null;
    }

    public Void toggleAFK(UUID player) throws IllegalArgumentException
    {
        ProxiedPlayer pplayer = ProxyServer.getInstance().getPlayer(player);
        if(pplayer == null)
            throw new IllegalArgumentException("That player is not online" + this.getClass().getCanonicalName() + ":toggleAFK()."+player);

        PlayerSettings settings = BungeeChat.instance.getManager().getSettings(pplayer);
        settings.isAFK = !settings.isAFK;

        return null;
    }

    public Void setTabColor(UUID player, String color) throws IllegalArgumentException
    {
        Player pplayer = BungeeChat.instance.getServer().getPlayer(player).orElse(null);
        if(pplayer == null)
            throw new IllegalArgumentException("That player is not online" + this.getClass().getCanonicalName() + ":setTabColor()."+player);

        PlayerSettings settings = BungeeChat.instance.getManager().getSettings(pplayer);

        settings.tabColor = BungeeChat.instance.fromCharArray(color.toCharArray());
        //Call tablist update here.

        return null;
    }

    public Void setMute(UUID player, long muteEnd) throws IllegalArgumentException
    {
        ProxiedPlayer pplayer = ProxyServer.getInstance().getPlayer(player);
        if(pplayer == null)
            throw new IllegalArgumentException("That player is not online" + this.getClass().getCanonicalName() + ":setMute()."+player);

        PlayerSettings settings = BungeeChat.instance.getManager().getSettings(pplayer);
        settings.muteTime = muteEnd;

        BungeeChat.instance.getManager().savePlayer(player);
        BungeeChat.instance.getManager().updateSettings(player);

        return null;
    }

    public boolean setMuteIP(Object who, long muteLength) throws IllegalArgumentException
    {
        InetAddress address;
        Player player = null;
        if (who instanceof UUID)
        {
            player = ProxyServer.getInstance().getPlayer((UUID)who);
            if(player == null)
                throw new IllegalArgumentException("That player is not online" + this.getClass().getCanonicalName() + ":setMuteIP()."+who);

            address = player.getAddress().getAddress();
        }
        else
        {
            try
            {
                address = InetAddress.getByName((String)who);
            }
            catch ( UnknownHostException e )
            {
                throw new IllegalArgumentException("Invalid ip address");
            }
        }

        if (muteLength != 0)
        {
            String timeString = Utilities.timeDiffToString(muteLength);

            String message;
            if (player == null)
            {
                for (ProxiedPlayer p : ProxyServer.getInstance().getPlayers())
                {
                    if (p.getAddress().getAddress().equals(address))
                    {
                        player = p;
                        break;
                    }
                }
                if (player == null)
                    return false;
            }

            message = ChatColor.AQUA + player.getDisplayName() + " and alternate accounts have been muted for " + timeString;
            BungeeChat.instance.getPacketManager().broadcast(new MirrorPacket("~BC", message));
            BungeeChat.instance.getMuteHandler().setIPMute(address, System.currentTimeMillis() + muteLength);
        }
        else
            BungeeChat.instance.getMuteHandler().setIPMute(address, 0);

        return true;
    }

    public Void setGMute(long time)
    {
        BungeeChat.instance.getMuteHandler().setGMute(time);
        return null;
    }

    public Void toggleGMute()
    {
        BungeeChat.instance.getMuteHandler().toggleGMute();
        return null;
    }

    public Void setMsgTarget(UUID player, UUID target) throws IllegalArgumentException
    {
        ProxiedPlayer pplayer = ProxyServer.getInstance().getPlayer(player);
        if(pplayer == null)
            throw new IllegalArgumentException("That player is not online" + this.getClass().getCanonicalName() + ":setMsgTarget()."+player);

        PlayerSettings settings = BungeeChat.instance.getManager().getSettings(pplayer);
        settings.lastMsgTarget = target;

        BungeeChat.instance.getManager().updateSettings(pplayer);

        return null;
    }

    public List<String> getMuteList()
    {
        ArrayList<String> muted = new ArrayList<>();
        for(ProxiedPlayer player : ProxyServer.getInstance().getPlayers())
        {
            PlayerSettings settings = BungeeChat.instance.getManager().getSettings(player);
            if(System.currentTimeMillis() < settings.muteTime)
                muted.add(player.getDisplayName() + ":" + settings.muteTime);
        }

        return muted;
    }

    public Void kickPlayer(UUID player, String reason) throws IllegalArgumentException
    {
        ProxiedPlayer pplayer = ProxyServer.getInstance().getPlayer(player);
        if(pplayer == null)
            throw new IllegalArgumentException("That player is not online" + this.getClass().getCanonicalName() + ":kickPlayer()."+player);
        pplayer.disconnect(TextComponent.fromLegacyText(reason));

        return null;
    }

    public Void setSkin(UUID player, Object skin) throws IllegalArgumentException
    {
        final ProxiedPlayer pplayer = ProxyServer.getInstance().getPlayer(player);
        if(pplayer == null)
            throw new IllegalArgumentException("That player is not online" + this.getClass().getCanonicalName() + ":setSkin()."+player);

        if (skin == null)
        {
            ((ColourTabList)pplayer.getTabListHandler()).setOverrideSkin(null);
            BungeeChat.instance.getPacketManager().broadcast(new PlayerRefreshPacket(pplayer.getUniqueId()));
            BungeeChat.instance.getManager().getSettings(pplayer).skin = null;
            BungeeChat.instance.getManager().savePlayer(pplayer);
            return null;
        }

        SkinData data;
        if (skin instanceof UUID)
            data = BungeeChat.instance.getSkinLibrary().getSkinWithLookupSync((UUID)skin);
        else
            data = BungeeChat.instance.getSkinLibrary().getSkinWithLookupSync((String)skin);

        if (data != null)
        {
            ((ColourTabList)pplayer.getTabListHandler()).setOverrideSkin(data);
            BungeeChat.instance.getPacketManager().broadcast(new PlayerRefreshPacket(pplayer.getUniqueId()));
            BungeeChat.instance.getManager().getSettings(pplayer).skin = data.id.toString();
            BungeeChat.instance.getManager().savePlayer(pplayer);
        }
        else
            throw new IllegalStateException("Unable to find skin");

        return null;
    }
}
