package au.com.addstar.bc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import au.com.addstar.bc.sync.SyncMethod;

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
		else if(name.equals("bchat:setMsgTarget"))
			return setMsgTarget((UUID)arguments[0], (UUID)arguments[1]);
		else if(name.equals("bchat:getMuteList"))
			return getMuteList();
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
		String oldColor = settings.tabColor;
		settings.tabColor = color;
		BungeeChat.instance.updateTabLists(oldColor, pplayer);
		
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
}
