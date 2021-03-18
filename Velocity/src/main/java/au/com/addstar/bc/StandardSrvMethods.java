package au.com.addstar.bc;

import au.com.addstar.bc.sync.SyncMethod;
import au.com.addstar.bc.sync.packet.MirrorPacket;
import au.com.addstar.bc.sync.packet.PlayerRefreshPacket;

import au.com.addstar.bc.util.Utilities;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Created for the AddstarMC Project.
 * Created by Narimm on 6/06/2019.
 */
public class StandardSrvMethods implements SyncMethod
{
    private final ProxyServer server;

    public StandardSrvMethods(ProxyServer server) {
        this.server = server;
    }

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
        HashMap<String, String> result = new HashMap<>(ProxyChat.instance.getSubHandler().getAllSubscriptions());
        return result;
    }

    public String getServer(RegisteredServer server)
    {
        return server.getServerInfo().getName();
    }

    public boolean isAFK(UUID player) throws IllegalArgumentException
    {
        Optional<Player> pplayer = server.getPlayer(player);
        if(!pplayer.isPresent())
            throw new IllegalArgumentException("That player is not online" + this.getClass().getCanonicalName() + ":isAFK()."+player);

        PlayerSettings settings = ProxyChat.instance.getManager().getSettings(pplayer.get());
        return settings.isAFK;
    }

    public boolean canMsg(UUID player) throws IllegalArgumentException
    {
        Optional<Player> pplayer = server.getPlayer(player);
        if(!pplayer.isPresent())
            throw new IllegalArgumentException("That player is not online" + this.getClass().getCanonicalName() + ":canMsg()."+player);

        return ProxyChat.instance.getManager().getSettings(pplayer.get()).msgEnabled;
    }

    public Void setAFK(UUID player, boolean afk) throws IllegalArgumentException
    {
        Optional<Player> pplayer = server.getPlayer(player);
        if(!pplayer.isPresent())
            throw new IllegalArgumentException("That player is not online" + this.getClass().getCanonicalName() + ":setAFK()."+player);

        ProxyChat.instance.getManager().getSettings(pplayer.get()).isAFK = afk;

        return null;
    }

    public Void toggleAFK(UUID player) throws IllegalArgumentException
    {
        Optional<Player> pplayer = server.getPlayer(player);
        if(!pplayer.isPresent())
            throw new IllegalArgumentException("That player is not online" + this.getClass().getCanonicalName() + ":toggleAFK()."+player);

        PlayerSettings settings = ProxyChat.instance.getManager().getSettings(pplayer.get());
        settings.isAFK = !settings.isAFK;

        return null;
    }

    public Void setTabColor(UUID player, String color) throws IllegalArgumentException
    {
        Player pplayer = server.getPlayer(player).orElse(null);
        if(pplayer == null)
            throw new IllegalArgumentException("That player is not online" + this.getClass().getCanonicalName() + ":setTabColor()."+player);

        PlayerSettings settings = ProxyChat.instance.getManager().getSettings(pplayer);

        settings.tabColor = ProxyChat.instance.fromCharArray(color.toCharArray());
        //Call tablist update here.

        return null;
    }

    public Void setMute(UUID player, long muteEnd) throws IllegalArgumentException
    {
        Optional<Player> pplayer = server.getPlayer(player);
        if(!pplayer.isPresent())
            throw new IllegalArgumentException("That player is not online" + this.getClass().getCanonicalName() + ":setMute()."+player);

        PlayerSettings settings = ProxyChat.instance.getManager().getSettings(pplayer.get());
        settings.muteTime = muteEnd;

        ProxyChat.instance.getManager().savePlayer(player);
        ProxyChat.instance.getManager().updateSettings(player);

        return null;
    }

    public boolean setMuteIP(Object who, long muteLength) throws IllegalArgumentException
    {
        InetAddress address;
        Player player = null;
        if (who instanceof UUID)
        {
            player = server.getPlayer((UUID)who).orElse(null);
            if(player == null)
                throw new IllegalArgumentException("That player is not online" + this.getClass().getCanonicalName() + ":setMuteIP()."+who);

            address = player.getRemoteAddress().getAddress();
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

            Component message;
            if (player == null)
            {
                for (Player p :server.getAllPlayers())
                {
                    if (p.getRemoteAddress().getAddress().equals(address))
                    {
                        player = p;
                        break;
                    }
                }
                if (player == null)
                    return false;
            }

            message = Component.text(player.getUsername() + " and alternate accounts have been muted for " + timeString)
                    .color(NamedTextColor.AQUA);
            ProxyChat.instance.getPacketManager().broadcast(new MirrorPacket("~BC", message));
            ProxyChat.instance.getMuteHandler().setIPMute(address, System.currentTimeMillis() + muteLength);
        }
        else
            ProxyChat.instance.getMuteHandler().setIPMute(address, 0);

        return true;
    }

    public Void setGMute(long time)
    {
        ProxyChat.instance.getMuteHandler().setGMute(server,time);
        return null;
    }

    public Void toggleGMute()
    {
        ProxyChat.instance.getMuteHandler().toggleGMute(server);
        return null;
    }

    public Void setMsgTarget(UUID player, UUID target) throws IllegalArgumentException
    {
        Player pplayer = server.getPlayer(player).orElse(null);
        if(pplayer == null)
            throw new IllegalArgumentException("That player is not online" + this.getClass().getCanonicalName() + ":setMsgTarget()."+player);

        PlayerSettings settings = ProxyChat.instance.getManager().getSettings(pplayer);
        settings.lastMsgTarget = target;

        ProxyChat.instance.getManager().updateSettings(pplayer);

        return null;
    }

    public List<String> getMuteList()
    {
        ArrayList<String> muted = new ArrayList<>();
        for(ProxiedPlayer player : ProxyServer.getInstance().getPlayers())
        {
            PlayerSettings settings = ProxyChat.instance.getManager().getSettings(player);
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
            ProxyChat.instance.getPacketManager().broadcast(new PlayerRefreshPacket(pplayer.getUniqueId()));
            ProxyChat.instance.getManager().getSettings(pplayer).skin = null;
            ProxyChat.instance.getManager().savePlayer(pplayer);
            return null;
        }

        SkinData data;
        if (skin instanceof UUID)
            data = ProxyChat.instance.getSkinLibrary().getSkinWithLookupSync((UUID)skin);
        else
            data = ProxyChat.instance.getSkinLibrary().getSkinWithLookupSync((String)skin);

        if (data != null)
        {
            ((ColourTabList)pplayer.getTabListHandler()).setOverrideSkin(data);
            ProxyChat.instance.getPacketManager().broadcast(new PlayerRefreshPacket(pplayer.getUniqueId()));
            ProxyChat.instance.getManager().getSettings(pplayer).skin = data.id.toString();
            ProxyChat.instance.getManager().savePlayer(pplayer);
        }
        else
            throw new IllegalStateException("Unable to find skin");

        return null;
    }
}
