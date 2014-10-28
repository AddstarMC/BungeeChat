package au.com.addstar.bc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;
import java.util.WeakHashMap;

import au.com.addstar.bc.sync.PropertyChangeEvent;
import au.com.addstar.bc.sync.SyncManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.GameProfile;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.GameProfile.Property;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.tab.TabListAdapter;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.protocol.ProtocolConstants;
import net.md_5.bungee.protocol.packet.PlayerListItem;
import net.md_5.bungee.protocol.packet.PlayerListItem.Action;
import net.md_5.bungee.protocol.packet.PlayerListItem.Item;

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
	private String mHeaderContents;
	private String mFooterContents;
	private boolean mHasInited;
	private HashSet<UUID> mFakePlayers = new HashSet<UUID>();
	// ==== 1.7 compat ====
	private String mLastName;

	public ColourTabList()
	{
		synchronized(mTabLists)
		{
			mTabLists.put(this, null);
		}
	}
	
	private static String getName(ProxiedPlayer player)
	{
		PlayerSettings settings = BungeeChat.instance.getManager().getSettings(player);
		return settings.tabColor + ChatColor.stripColor(player.getDisplayName());
	}
	
	private static BaseComponent[] getDispName(ProxiedPlayer player)
	{
		return TextComponent.fromLegacyText(getName(player));
	}
	
	public static boolean isNewTab(ProxiedPlayer player)
	{
		return player.getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_SNAPSHOT;
	}
	
	@Override
	public synchronized void onConnect()
	{
		mLastName = getName(getPlayer());
		Debugger.logt("Connect %s with %s", getPlayer().getName(), mLastName);
		updateAll();
	}
	
	public synchronized void onJoinPeriodComplete()
	{
		Debugger.logt("Join over %s", getPlayer().getName());
		mHasInited = true;
		updateAll();
	}
	
	@Override
	public synchronized void onPingChange( int ping )
	{
		if ( ping - PING_THRESHOLD > lastPing && ping + PING_THRESHOLD < lastPing )
		{
			lastPing = ping;
			PlayerListItem packet = createPacket(Action.UPDATE_LATENCY, createItem(getPlayer()));
            
			for(ProxiedPlayer player : ProxyServer.getInstance().getPlayers())
			{
				if(isVisible(player, getPlayer()))
					sendPacket(packet, player);
			}
		}
	}

	@Override
	public synchronized void onDisconnect()
	{
		Debugger.logt("Disconnect %s", getPlayer().getName());
		Item item = createItem(getPlayer(), mLastName);
		
		PlayerListItem packet = createPacket(Action.REMOVE_PLAYER, item);
		
		for(ProxiedPlayer player : ProxyServer.getInstance().getPlayers())
		{
			if(isVisible(player, getPlayer()))
				sendPacket(packet, player);
		}
	}

	@Override
	public synchronized void onUpdate( PlayerListItem packet )
	{
		// Fake players do not need to be handled pre 1.8
		if (!isNewTab(getPlayer()))
			return;
		
		ArrayList<Item> items = null;
		for(Item item : packet.getItems())
		{
			// Only fake players will be allowed to pass through. This should allow citizens to work
			if (ProxyServer.getInstance().getPlayer(item.getUuid()) == null)
			{
				boolean known = false;
				switch(packet.getAction())
				{
				case ADD_PLAYER:
					mFakePlayers.add(item.getUuid());
					known = true;
					break;
				case REMOVE_PLAYER:
					known = mFakePlayers.remove(item.getUuid());
					break;
				default:
					known = mFakePlayers.contains(item.getUuid());
					break;
				}
				
				if (!known)
					continue;
				
				if (items == null)
					items = new ArrayList<Item>(packet.getItems().length);
				items.add(item);
			}
		}
		
		if (items != null)
		{
			packet.setItems(items.toArray(new Item[items.size()]));
			getPlayer().unsafe().sendPacket(packet);
		}
	}
	
	public synchronized void updateTabHeaders()
	{
		if (!isNewTab(getPlayer()))
			return;
		
		String headerString = BungeeChat.instance.getTabHeaderString(getPlayer());
		String footerString = BungeeChat.instance.getTabFooterString(getPlayer());
		
		if (!headerString.equals(mHeaderContents) || !footerString.equals(mFooterContents))
		{
			getPlayer().setTabHeader(TextComponent.fromLegacyText(headerString), TextComponent.fromLegacyText(footerString));
			mHeaderContents = headerString;
			mFooterContents = footerString;
		}
	}
	
	public synchronized void updateList()
	{
		ArrayList<Item> toAdd = new ArrayList<Item>();
		ArrayList<Item> toRemove = new ArrayList<Item>();
		
		for (ProxiedPlayer p : ProxyServer.getInstance().getPlayers())
		{
			if(isVisible(getPlayer(), p))
			{
				if(!mVisiblePlayers.containsKey(p))
				{
					toAdd.add(createItem(p));
					mVisiblePlayers.put(p, null);
				}
			}
			else if(mVisiblePlayers.containsKey(p))
			{
				mVisiblePlayers.remove(p);
				toRemove.add(createItem(p));
			}
		}
		
		if (isNewTab(getPlayer()))
		{
			if (!toAdd.isEmpty())
			{
				PlayerListItem packetAdd = createPacket(Action.ADD_PLAYER, toAdd.toArray(new Item[toAdd.size()]));
				PlayerListItem packetUpdate = createPacket(Action.UPDATE_DISPLAY_NAME, toAdd.toArray(new Item[toAdd.size()]));
				
				sendPacket(packetAdd, getPlayer());
				sendPacket(packetUpdate, getPlayer());
			}
			
			if (!toRemove.isEmpty())
			{
				PlayerListItem packetRemove = createPacket(Action.REMOVE_PLAYER, toRemove.toArray(new Item[toRemove.size()]));
				
				sendPacket(packetRemove, getPlayer());
			}
		}
		else
		{
			for (Item item : toAdd)
			{
				PlayerListItem packet = createPacket(Action.ADD_PLAYER, item);
				sendPacket(packet, getPlayer());
			}
			
			for (Item item : toRemove)
			{
				PlayerListItem packet = createPacket(Action.REMOVE_PLAYER, item);
				sendPacket(packet, getPlayer());
			}
		}
		
		updateTabHeaders();
	}
	
	/**
	 * To be visible, the to player must either have TL:seeall set, or be able to see all the groups player is in
	 */
	public static boolean isVisible(ProxiedPlayer to, ProxiedPlayer player)
	{
		if(to == player)
			return true;
		
		if (player.getTabListHandler() instanceof ColourTabList)
		{
			if(!((ColourTabList)player.getTabListHandler()).mHasInited)
				return false;
		}
		
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
		synchronized(mTabLists)
		{
			for(ColourTabList list : mTabLists.keySet())
				list.updateList();
		}
	}
	
	@Override
	public synchronized void onUpdateName()
	{
		Debugger.logt("UpdateName %s from %s to %s", getPlayer().getName(), mLastName, getName(getPlayer()));
		if (mLastName == null)
		{
			Debugger.logt("Update name cancelled %s", getPlayer().getName());
			return;
		}
		
		PlayerListItem packetUpdate = createPacket(Action.UPDATE_DISPLAY_NAME, createItem(getPlayer()));
		PlayerListItem packetRemove = createPacket(Action.REMOVE_PLAYER, createItem(getPlayer(), mLastName));
		PlayerListItem packetAdd = createPacket(Action.ADD_PLAYER, createItem(getPlayer()));
		mLastName = getName(getPlayer());
	
		for(ProxiedPlayer p : ProxyServer.getInstance().getPlayers())
		{
			// Can only update for other players if the init period is over, and the other player can see me
			if((mHasInited && isVisible(p, getPlayer())) || p == getPlayer())
			{
				if (isNewTab(p))
					sendPacket(packetUpdate, p);
				else
				{
					sendPacket(packetRemove, p);
					sendPacket(packetAdd, p);
				}
			}
		}
	}
	
	private void sendPacket(PlayerListItem packet, ProxiedPlayer player)
	{
		Debugger.logTabItem(packet, player);
		player.unsafe().sendPacket(packet);
	}
	
	private static void setProfile(Item item, GameProfile profile)
	{
		item.setUsername(profile.getUsername());
		item.setUuid(profile.getUniqueId());
		
		String[][] properties = new String[profile.getProperties().length][];
		for(int i = 0; i < properties.length; ++i)
		{
			Property prop = profile.getProperties()[i];
			if (prop.getSignature() != null)
				properties[i] = new String[] { prop.getName(), prop.getValue(), prop.getSignature() };
			else
				properties[i] = new String[] { prop.getName(), prop.getValue() };
		}
		
		item.setProperties(properties);
	}
	
	private static Item createItem(ProxiedPlayer player)
	{
		Item item = new Item();
		setProfile(item, player.getProfile());
		
		item.setDisplayName(getDispName(player));
		item.setGamemode(0);
		item.setPing(player.getPing());
		
		return item;
	}
	
	private static Item createItem(ProxiedPlayer player, String name)
	{
		Item item = new Item();
		setProfile(item, player.getProfile());
		
		item.setDisplayName(TextComponent.fromLegacyText(name));
		item.setGamemode(0);
		item.setPing(player.getPing());
		
		return item;
	}
	
	private static PlayerListItem createPacket(Action action, Item... items)
	{
		PlayerListItem packet = new PlayerListItem();
		packet.setAction(action);
		packet.setItems(items);
		return packet;
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
