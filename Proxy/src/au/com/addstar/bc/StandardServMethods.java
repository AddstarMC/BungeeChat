package au.com.addstar.bc;

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
}
