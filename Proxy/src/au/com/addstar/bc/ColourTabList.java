package au.com.addstar.bc;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.tab.TabListAdapter;
import net.md_5.bungee.protocol.packet.PlayerListItem;

public class ColourTabList extends TabListAdapter
{
	private static final int PING_THRESHOLD = 20;
	private int lastPing;

	private String getName(ProxiedPlayer player)
	{
		PlayerSettings settings = BungeeChat.instance.getManager().getSettings(player);
		String name = settings.tabColor + ChatColor.stripColor(player.getDisplayName());
		if(name.length() > 16)
			return name.substring(0, 16);
		return name;
	}
	
	@Override
	public void onConnect()
	{
		for (ProxiedPlayer p : ProxyServer.getInstance().getPlayers())
			getPlayer().unsafe().sendPacket(new PlayerListItem(getName(p), true, (short) p.getPing()));

		BungeeCord.getInstance().broadcast(new PlayerListItem(getName(getPlayer()), true, (short) getPlayer().getPing()));
	}

	@Override
	public void onPingChange( int ping )
	{
		if ( ping - PING_THRESHOLD > lastPing && ping + PING_THRESHOLD < lastPing )
		{
			lastPing = ping;
			BungeeCord.getInstance().broadcast(new PlayerListItem(getName(getPlayer()), true, (short) ping));
		}
	}

	@Override
	public void onDisconnect()
	{
		BungeeCord.getInstance().broadcast(new PlayerListItem(getName(getPlayer()), false, (short) 9999));
	}

	@Override
	public boolean onListUpdate( String name, boolean online, int ping )
	{
		return false;
	}
}
