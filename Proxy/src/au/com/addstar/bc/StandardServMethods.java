package au.com.addstar.bc;

import java.util.ArrayList;
import java.util.List;

import net.md_5.bungee.BungeeCord;
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
			return isAFK((String)arguments[0]);
		else if(name.equals("bchat:canMsg"))
			return canMsg((String)arguments[0]);
		else if(name.equals("bchat:setAFK"))
			return setAFK((String)arguments[0], (Byte)arguments[1]);
		else if(name.equals("bchat:toggleAFK"))
			return toggleAFK((String)arguments[0]);
		else if(name.equals("bchat:setTabColor"))
			return setTabColor((String)arguments[0], (String)arguments[1]);
		else if(name.equals("bchat:setMute"))
			return setMute((String)arguments[0], (Long)arguments[1]);
		else if(name.equals("bchat:setMsgTarget"))
			return setMsgTarget((String)arguments[0], (String)arguments[1]);
		else if(name.equals("bchat:getMuteList"))
			return getMuteList();
		return null;
	}
	
	public String getServer(ServerInfo server)
	{
		return server.getName();
	}
	
	public boolean isAFK(String player)
	{
		ProxiedPlayer pplayer = BungeeCord.getInstance().getPlayer(player);
		if(pplayer == null)
			throw new IllegalArgumentException("That player is not online");
		
		PlayerSettings settings = BungeeChat.instance.getManager().getSettings(pplayer);
		return settings.isAFK;
	}
	
	public boolean canMsg(String player)
	{
		ProxiedPlayer pplayer = BungeeCord.getInstance().getPlayer(player);
		if(pplayer == null)
			throw new IllegalArgumentException("That player is not online");
		
		return BungeeChat.instance.getManager().getSettings(pplayer).msgEnabled;
	}
	
	public Void setAFK(String player, byte status)
	{
		ProxiedPlayer pplayer = BungeeCord.getInstance().getPlayer(player);
		if(pplayer == null)
			throw new IllegalArgumentException("That player is not online");
		
		BungeeChat.instance.getManager().getSettings(pplayer).isAFK = status != 0;
		
		return null;
	}
	
	public Void toggleAFK(String player)
	{
		ProxiedPlayer pplayer = BungeeCord.getInstance().getPlayer(player);
		if(pplayer == null)
			throw new IllegalArgumentException("That player is not online");
		
		PlayerSettings settings = BungeeChat.instance.getManager().getSettings(pplayer); 
		settings.isAFK = !settings.isAFK;
		
		return null;
	}
	
	public Void setTabColor(String player, String color)
	{
		ProxiedPlayer pplayer = BungeeCord.getInstance().getPlayer(player);
		if(pplayer == null)
			throw new IllegalArgumentException("That player is not online");
		
		PlayerSettings settings = BungeeChat.instance.getManager().getSettings(pplayer);
		BungeeChat.instance.updateTabLists(color, pplayer);
		settings.tabColor = color;
		
		return null;
	}
	
	public Void setMute(String player, long muteEnd)
	{
		ProxiedPlayer pplayer = BungeeCord.getInstance().getPlayer(player);
		if(pplayer == null)
			throw new IllegalArgumentException("That player is not online");
		
		PlayerSettings settings = BungeeChat.instance.getManager().getSettings(pplayer);
		settings.muteTime = muteEnd;
		
		BungeeChat.instance.getManager().savePlayer(player);
		BungeeChat.instance.getManager().updateSettings(player);
		
		return null;
	}
	
	public Void setMsgTarget(String player, String target)
	{
		ProxiedPlayer pplayer = BungeeCord.getInstance().getPlayer(player);
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
		for(ProxiedPlayer player : BungeeCord.getInstance().getPlayers())
		{
			PlayerSettings settings = BungeeChat.instance.getManager().getSettings(player);
			if(System.currentTimeMillis() < settings.muteTime)
				muted.add(player.getDisplayName() + ":" + settings.muteTime);
		}
		
		return muted;
	}
}
