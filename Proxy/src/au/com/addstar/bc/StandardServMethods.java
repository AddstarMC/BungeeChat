package au.com.addstar.bc;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import au.com.addstar.bc.sync.SyncMethod;
import au.com.addstar.bc.sync.packet.MirrorPacket;
import au.com.addstar.bc.sync.packet.PlayerRefreshPacket;
import au.com.addstar.bc.util.Utilities;

public class StandardServMethods implements SyncMethod
{
	@Override
	public Object run( String name, ServerInfo server, Object... arguments )
	{
		if(name.equals("bungee:getServerName"))
			return getServer(server);
		else if(name.equals("bchat:isAFK"))
			return isAFK((UUID)arguments[0]);
		else if(name.equals("bchat:canMsg"))
			return canMsg((UUID)arguments[0]);
		else if(name.equals("bchat:setAFK"))
			return setAFK((UUID)arguments[0], (Boolean)arguments[1]);
		else if(name.equals("bchat:toggleAFK"))
			return toggleAFK((UUID)arguments[0]);
		else if(name.equals("bchat:setTabColor"))
			return setTabColor((UUID)arguments[0], (String)arguments[1]);
		else if(name.equals("bchat:setMute"))
			return setMute((UUID)arguments[0], (Long)arguments[1]);
		else if(name.equals("bchat:setMuteIP"))
			return setMuteIP((Object)arguments[0], (Long)arguments[1]);
		else if(name.equals("bchat:setGMute"))
			return setGMute((Long)arguments[0]);
		else if(name.equals("bchat:toggleGMute"))
			return toggleGMute();
		else if(name.equals("bchat:setMsgTarget"))
			return setMsgTarget((UUID)arguments[0], (UUID)arguments[1]);
		else if(name.equals("bchat:getMuteList"))
			return getMuteList();
		else if(name.equals("bchat:kick"))
			return kickPlayer((UUID)arguments[0], (String)arguments[1]);
		else if(name.equals("bchat:setSkin"))
			return setSkin((UUID)arguments[0], (UUID)arguments[1]);
		return null;
	}
	
	public String getServer(ServerInfo server)
	{
		return server.getName();
	}
	
	public boolean isAFK(UUID player)
	{
		ProxiedPlayer pplayer = ProxyServer.getInstance().getPlayer(player);
		if(pplayer == null)
			throw new IllegalArgumentException("That player is not online");
		
		PlayerSettings settings = BungeeChat.instance.getManager().getSettings(pplayer);
		return settings.isAFK;
	}
	
	public boolean canMsg(UUID player)
	{
		ProxiedPlayer pplayer = ProxyServer.getInstance().getPlayer(player);
		if(pplayer == null)
			throw new IllegalArgumentException("That player is not online");
		
		return BungeeChat.instance.getManager().getSettings(pplayer).msgEnabled;
	}
	
	public Void setAFK(UUID player, boolean afk)
	{
		ProxiedPlayer pplayer = ProxyServer.getInstance().getPlayer(player);
		if(pplayer == null)
			throw new IllegalArgumentException("That player is not online");
		
		BungeeChat.instance.getManager().getSettings(pplayer).isAFK = afk;
		
		return null;
	}
	
	public Void toggleAFK(UUID player)
	{
		ProxiedPlayer pplayer = ProxyServer.getInstance().getPlayer(player);
		if(pplayer == null)
			throw new IllegalArgumentException("That player is not online");
		
		PlayerSettings settings = BungeeChat.instance.getManager().getSettings(pplayer); 
		settings.isAFK = !settings.isAFK;
		
		return null;
	}
	
	public Void setTabColor(UUID player, String color)
	{
		ProxiedPlayer pplayer = ProxyServer.getInstance().getPlayer(player);
		if(pplayer == null)
			throw new IllegalArgumentException("That player is not online");
		
		PlayerSettings settings = BungeeChat.instance.getManager().getSettings(pplayer);
		settings.tabColor = color;
		pplayer.getTabListHandler().onUpdateName();
		
		return null;
	}
	
	public Void setMute(UUID player, long muteEnd)
	{
		ProxiedPlayer pplayer = ProxyServer.getInstance().getPlayer(player);
		if(pplayer == null)
			throw new IllegalArgumentException("That player is not online");
		
		PlayerSettings settings = BungeeChat.instance.getManager().getSettings(pplayer);
		settings.muteTime = muteEnd;
		
		BungeeChat.instance.getManager().savePlayer(player);
		BungeeChat.instance.getManager().updateSettings(player);
		
		return null;
	}
	
	public boolean setMuteIP(Object who, long muteLength)
	{
		InetAddress address;
		ProxiedPlayer player = null;
		if (who instanceof UUID)
		{
			player = ProxyServer.getInstance().getPlayer((UUID)who);
			if(player == null)
				throw new IllegalArgumentException("That player is not online");
			
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
	
	public Void setMsgTarget(UUID player, UUID target)
	{
		ProxiedPlayer pplayer = ProxyServer.getInstance().getPlayer(player);
		if(pplayer == null)
			throw new IllegalArgumentException("That player is not online");
		
		PlayerSettings settings = BungeeChat.instance.getManager().getSettings(pplayer); 
		settings.lastMsgTarget = target;
		
		BungeeChat.instance.getManager().updateSettings(pplayer);
		
		return null;
	}
	
	public List<String> getMuteList()
	{
		ArrayList<String> muted = new ArrayList<String>();
		for(ProxiedPlayer player : ProxyServer.getInstance().getPlayers())
		{
			PlayerSettings settings = BungeeChat.instance.getManager().getSettings(player);
			if(System.currentTimeMillis() < settings.muteTime)
				muted.add(player.getDisplayName() + ":" + settings.muteTime);
		}
		
		return muted;
	}
	
	public Void kickPlayer(UUID player, String reason)
	{
		ProxiedPlayer pplayer = ProxyServer.getInstance().getPlayer(player);
		if(pplayer == null)
			throw new IllegalArgumentException("That player is not online");
		pplayer.disconnect(TextComponent.fromLegacyText(reason));
		
		return null;
	}
	
	public Void setSkin(UUID player, UUID skin)
	{
		final ProxiedPlayer pplayer = ProxyServer.getInstance().getPlayer(player);
		if(pplayer == null)
			throw new IllegalArgumentException("That player is not online");
		
		if (skin == null)
		{
			((ColourTabList)pplayer.getTabListHandler()).setOverrideSkin(null);
			BungeeChat.instance.getPacketManager().broadcast(new PlayerRefreshPacket(pplayer.getUniqueId()));
			BungeeChat.instance.getManager().getSettings(pplayer).skin = null;
			BungeeChat.instance.getManager().savePlayer(pplayer);
			return null;
		}
		
		SkinData data = BungeeChat.instance.getSkinLibrary().getSkinWithLookupSync(skin);
		if (data != null)
		{
			((ColourTabList)pplayer.getTabListHandler()).setOverrideSkin(data);
			BungeeChat.instance.getPacketManager().broadcast(new PlayerRefreshPacket(pplayer.getUniqueId()));
			BungeeChat.instance.getManager().getSettings(pplayer).skin = skin.toString();
			BungeeChat.instance.getManager().savePlayer(pplayer);
		}
		else
			throw new IllegalStateException("Unable to find skin");
		
		return null;
	}
}
