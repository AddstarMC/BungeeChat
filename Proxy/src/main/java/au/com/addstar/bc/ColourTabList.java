package au.com.addstar.bc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Sets;

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
	private static Set<ColourTabList> mTabLists = Sets.newConcurrentHashSet();
	
	public static void initialize(Plugin plugin)
	{
		ProxyServer.getInstance().getPluginManager().registerListener(plugin, mUpdater);
	}
	
	private int lastPing;
	private Set<ProxiedPlayer> mVisiblePlayers = Sets.newConcurrentHashSet();
	private String mHeaderContents;
	private String mFooterContents;
	private boolean mHasInited;
	private SkinData mForcedSkinData;
	// ==== 1.7 compat ====
	private String mLastName;

	public ColourTabList()
	{
		synchronized(mTabLists)
		{
			mTabLists.add(this);
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
		return player.getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_8;
	}
	
	public void setOverrideSkin(SkinData skin)
	{
		mForcedSkinData = skin;
		PlayerListItem packet = createPacket(Action.ADD_PLAYER, createItem(getPlayer()));
		
		for (ProxiedPlayer p : ProxyServer.getInstance().getPlayers())
		{
			if(isVisible(p, getPlayer()) && isNewTab(p))
				p.unsafe().sendPacket(packet);
		}
	}
	
	@Override
	public void onConnect()
	{
		mLastName = getName(getPlayer());
		Debugger.logt("Connect %s with %s", getPlayer().getName(), mLastName);
		updateAll();
	}
	
	public void onJoinPeriodComplete()
	{
		Debugger.logt("Join over %s", getPlayer().getName());
		mHasInited = true;
		updateAll();
	}
	
	@Override
	public void onPingChange( int ping )
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
	public void onDisconnect()
	{
		Debugger.logt("Disconnect %s", getPlayer().getName());
		Item item = createItem(getPlayer(), mLastName);
		
		PlayerListItem packet = createPacket(Action.REMOVE_PLAYER, item);
		
		for(ProxiedPlayer player : ProxyServer.getInstance().getPlayers())
		{
			if(isVisible(player, getPlayer()))
				sendPacket(packet, player);
		}
		
		ProxyServer.getInstance().getScheduler().schedule(BungeeChat.instance, new Runnable()
		{
			@Override
			public void run()
			{
				updateAllHeaders();
			}
		}, 50, TimeUnit.MILLISECONDS);
		
		mVisiblePlayers.clear();
		mTabLists.remove(this);
		
		synchronized(mTabLists) {
			for (ColourTabList tablist : mTabLists) {
				tablist.mVisiblePlayers.remove(getPlayer());
			}
		}
	}

	private void onUpdateGamemode( PlayerListItem packet )
	{
		ArrayList<Item> items = null;
		for(Item item : packet.getItems())
		{
			ProxiedPlayer other = ProxyServer.getInstance().getPlayer(item.getUuid());
			if (other == null)
				continue;
			
			if (isVisible(getPlayer(), other))
			{
				if (items == null)
					items = new ArrayList<Item>(packet.getItems().length);
				items.add(item);
			}
		}
		
		if (items != null)
		{
			final Item[] array = items.toArray(new Item[items.size()]);
			packet.setItems(array);
			getPlayer().unsafe().sendPacket(packet);
		}
	}
	
	@Override
	public void onUpdate( PlayerListItem packet )
	{
		// Fake players do not need to be handled pre 1.8
		if (!isNewTab(getPlayer()))
			return;
		
		// Allow gamemode changes to be sent correctly through the proxy
		if (packet.getAction() == Action.UPDATE_GAMEMODE)
		{
			onUpdateGamemode(packet);
			return;
		}
		
		if (packet.getAction() != Action.ADD_PLAYER)
			return;
		
		ArrayList<Item> items = null;
		for(Item item : packet.getItems())
		{
			// Only fake players will be allowed to pass through. This should allow citizens to work
			if (ProxyServer.getInstance().getPlayer(item.getUuid()) == null)
			{
				if (items == null)
					items = new ArrayList<Item>(packet.getItems().length);
				items.add(item);
			}
		}
		
		if (items != null)
		{
			final Item[] array = items.toArray(new Item[items.size()]);
			packet.setItems(array);
			getPlayer().unsafe().sendPacket(packet);
			// Remove them so they dont really show in tab
			ProxyServer.getInstance().getScheduler().schedule(BungeeChat.instance, new Runnable()
			{
				@Override
				public void run()
				{
					PlayerListItem packetRemove = new PlayerListItem();
					packetRemove.setAction(Action.REMOVE_PLAYER);
					packetRemove.setItems(array);
					getPlayer().unsafe().sendPacket(packetRemove);
				}
			}, 50, TimeUnit.MILLISECONDS);
		}
	}
	
	public void updateTabHeaders()
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
	
	public void updateList()
	{
		ArrayList<Item> toAdd = new ArrayList<Item>();
		ArrayList<Item> toRemove = new ArrayList<Item>();
		
		for (ProxiedPlayer p : ProxyServer.getInstance().getPlayers())
		{
			if(isVisible(getPlayer(), p))
			{
				if(!mVisiblePlayers.contains(p))
				{
					toAdd.add(createItem(p));
					mVisiblePlayers.add(p);
				}
			}
			else if(mVisiblePlayers.contains(p))
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
			for(ColourTabList list : mTabLists)
				list.updateList();
		}
	}
	
	public static void updateAllHeaders()
	{
		synchronized(mTabLists)
		{
			for(ColourTabList list : mTabLists)
				list.updateTabHeaders();
		}
	}
	
	@Override
	public void onUpdateName()
	{
		Debugger.logt("UpdateName %s from %s to %s", getPlayer().getName(), mLastName, getName(getPlayer()));
		if (mLastName == null)
		{
			Debugger.logt("Update name cancelled %s", getPlayer().getName());
			return;
		}
		
		PlayerListItem packetRemove = createPacket(Action.REMOVE_PLAYER, createItem(getPlayer(), mLastName));
		PlayerListItem packetAdd = createPacket(Action.ADD_PLAYER, createItem(getPlayer()));
		mLastName = getName(getPlayer());
	
		for(ProxiedPlayer p : ProxyServer.getInstance().getPlayers())
		{
			// Can only update for other players if the init period is over, and the other player can see me
			if((mHasInited && isVisible(p, getPlayer())) || p == getPlayer())
			{
				if (isNewTab(p))
					sendPacket(packetAdd, p);
				else
				{
					sendPacket(packetRemove, p);
					sendPacket(packetAdd, p);
				}
			}
		}
	}
	
	public boolean hasInited()
	{
		return mHasInited;
	}
	
	private void sendPacket(PlayerListItem packet, ProxiedPlayer player)
	{
		Debugger.logTabItem(packet, player);
		player.unsafe().sendPacket(packet);
	}
	
	private static void setProfile(Item item, ProxiedPlayer player)
	{
		GameProfile profile = player.getProfile();
		item.setUsername(ChatColor.stripColor(player.getDisplayName()));
		item.setUuid(profile.getUniqueId());
		
		ColourTabList tab = (ColourTabList)player.getTabListHandler();
		
		String[][] properties = new String[profile.getProperties().length][];
		for(int i = 0; i < properties.length; ++i)
		{
			Property prop = profile.getProperties()[i];
			if (prop.getName().equals("textures"))
			{
				if (tab.mForcedSkinData != null)
					prop = new Property("textures", tab.mForcedSkinData.value, tab.mForcedSkinData.signature);
			}
			
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
		setProfile(item, player);
		
		item.setDisplayName(getDispName(player));
		item.setGamemode(0);
		item.setPing(player.getPing());
		
		return item;
	}
	
	private static Item createItem(ProxiedPlayer player, String name)
	{
		Item item = new Item();
		setProfile(item, player);
		
		if (name != null)
			item.setDisplayName(TextComponent.fromLegacyText(name));
		else
			item.setDisplayName(TextComponent.fromLegacyText(player.getName()));

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
