package au.com.addstar.bc;

import java.util.Collection;
import java.util.WeakHashMap;

import au.com.addstar.bc.sync.PropertyChangeEvent;
import au.com.addstar.bc.sync.SyncManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.tab.TabListAdapter;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.protocol.packet.PlayerListItem;

public class ColourTabList extends TabListAdapter
{
	private static final int PING_THRESHOLD = 20;
	private static ListUpdater mUpdater = new ListUpdater();
	private static WeakHashMap<ColourTabList, Void> mTabLists = new WeakHashMap<ColourTabList, Void>();
	
	public static void initialize(Plugin plugin)
	{
		ProxyServer.getInstance().getPluginManager().registerListener(plugin, mUpdater);
	}
	
	private int lastPing;
	private WeakHashMap<ProxiedPlayer, Void> mVisiblePlayers = new WeakHashMap<ProxiedPlayer, Void>();

	public ColourTabList()
	{
		mTabLists.put(this, null);
	}
	
	private static String getName(ProxiedPlayer player)
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
	}
	
	@Override
	public void onPingChange( int ping )
	{
		if ( ping - PING_THRESHOLD > lastPing && ping + PING_THRESHOLD < lastPing )
		{
			lastPing = ping;
			PlayerListItem packet = new PlayerListItem(getName(getPlayer()), true, ping);
			
			for(ProxiedPlayer player : ProxyServer.getInstance().getPlayers())
			{
				if(isVisible(player, getPlayer()))
					player.unsafe().sendPacket(packet);
			}
		}
	}

	@Override
	public void onDisconnect()
	{
		PlayerListItem packet = new PlayerListItem(getName(getPlayer()), false, 9999);
		
		for(ProxiedPlayer player : ProxyServer.getInstance().getPlayers())
		{
			if(isVisible(player, getPlayer()))
				player.unsafe().sendPacket(packet);
		}
	}

	@Override
	public boolean onListUpdate( String name, boolean online, int ping )
	{
		return false;
	}
	
	public void updateList()
	{
		for (ProxiedPlayer p : ProxyServer.getInstance().getPlayers())
		{
			String theirName = getName(p);
			
			if(isVisible(getPlayer(), p))
			{
				if(!mVisiblePlayers.containsKey(p))
				{
					getPlayer().unsafe().sendPacket(new PlayerListItem(theirName, true, (short)p.getPing()));
					mVisiblePlayers.put(p, null);
				}
			}
			else if(mVisiblePlayers.containsKey(p))
			{
				mVisiblePlayers.remove(p);
				getPlayer().unsafe().sendPacket(new PlayerListItem(theirName, false, (short)9999));
			}
		}
	}
	
	/**
	 * To be visible, the to player must either have TL:seeall set, or be able to see all the groups player is in
	 */
	public static boolean isVisible(ProxiedPlayer to, ProxiedPlayer player)
	{
		if(to == player)
			return true;
		
		SyncManager manager = BungeeChat.instance.getSyncManager();
		
		boolean canSeeAll = manager.getPropertyBoolean(to, "TL:seeall", false);
		if(canSeeAll)
			return true;
		
		Collection<String> names = manager.getPropertyNames(player, "TL:group:");
		
		if(names.isEmpty())
			return true;
		
		for(String name : names)
		{
			String group = name.split(":")[2];
			if(manager.getPropertyBoolean(player, "TL:group:" + group, false) && !manager.getPropertyBoolean(to, "TL:see:" + group, false))
				return false;
		}
		
		return true;
	}
	
	public static void updateAll()
	{
		for(ColourTabList list : mTabLists.keySet())
			list.updateList();
	}
	
	public static void nameChange(ProxiedPlayer player, String oldName)
	{
		String name = getName(player);
		
		if(oldName.equals(name))
			return;
		
		PlayerListItem packet1 = new PlayerListItem(oldName, false, (short)9999);
		PlayerListItem packet2 = new PlayerListItem(name, true, (short)player.getPing());
		
		for(ProxiedPlayer p : ProxyServer.getInstance().getPlayers())
		{
			if(isVisible(p, player))
			{
				p.unsafe().sendPacket(packet1);
				p.unsafe().sendPacket(packet2);
			}
		}
	}
	
	public static class ListUpdater implements Listener
	{
		@EventHandler
		public void onPropertyChange(PropertyChangeEvent event)
		{
			if(!event.getProperty().startsWith("TL:"))
				return;
			
			updateAll();
		}
	}
}
