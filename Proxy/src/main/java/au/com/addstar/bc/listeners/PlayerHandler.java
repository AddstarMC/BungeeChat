package au.com.addstar.bc.listeners;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import au.com.addstar.bc.*;
import au.com.addstar.bc.event.BCPlayerJoinEvent;
import au.com.addstar.bc.event.BCPlayerQuitEvent;
import au.com.addstar.bc.sync.PacketManager;
import au.com.addstar.bc.sync.packet.FireEventPacket;
import au.com.addstar.bc.sync.packet.PlayerJoinPacket;
import au.com.addstar.bc.sync.packet.PlayerLeavePacket;
import au.com.addstar.bc.sync.packet.PlayerRefreshPacket;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class PlayerHandler implements Listener
{
	private PlayerSettingsManager mSettings;
	private PacketManager mPackets;
	private ProxyServer mProxy;
	
	public PlayerHandler()
	{
		mSettings = BungeeChat.instance.getManager();
		mPackets = BungeeChat.instance.getPacketManager();
		mProxy = ProxyServer.getInstance();
	}
	
	private void loadSettingsAsync(final ProxiedPlayer player)
	{
		mProxy.getScheduler().runAsync(BungeeChat.instance, new Runnable()
		{
			@Override
			public void run()
			{
				// Load this players settings
				PlayerSettings settings = mSettings.getSettings(player);
				
				if(settings.nickname.isEmpty())
					player.setDisplayName(player.getName());
				else
					player.setDisplayName(settings.nickname);
				
				Debugger.log("Applying nickname to PP %s: '%s'", player.getName(), settings.nickname);
				
				if (settings.skin != null && !settings.skin.isEmpty())
				{
					ColourTabList tablist = ((ColourTabList)player.getTabListHandler());
					tablist.setOverrideSkin(BungeeChat.instance.getSkinLibrary().getSkinWithLookupSync(UUID.fromString(settings.skin)));
					
					if (tablist.hasInited())
						BungeeChat.instance.getPacketManager().send(new PlayerRefreshPacket(player.getUniqueId()), player.getServer().getInfo());
				}
			}
		});
	}
	
	@EventHandler
	public void onFinishLogin(final PostLoginEvent event)
	{
		Debugger.log("PP join %s", event.getPlayer().getName());
		
		event.getPlayer().setTabListHandler(new ColourTabList());
		loadSettingsAsync(event.getPlayer());
	}
	
	@EventHandler
	public void onDisconnect(PlayerDisconnectEvent event)
	{
		ProxiedPlayer player = event.getPlayer();
		UUID id = player.getUniqueId();
		
		Debugger.log("PP disconnect %s", player.getName());
		
		if (player.getServer() != null)
		{
			boolean showQuitMessage = BungeeChat.instance.getSyncManager().getPropertyBoolean(player, "hasQuitMessage", true); 
			String quitMessage = ChatColor.YELLOW + ChatColor.stripColor(player.getDisplayName()) + " left the game.";
			
			if(!showQuitMessage)
				quitMessage = null;
			
			BCPlayerQuitEvent qevent = new BCPlayerQuitEvent(player, quitMessage);
			mProxy.getPluginManager().callEvent(qevent);
			quitMessage = qevent.getQuitMessage();
			
			if (quitMessage == null)
				quitMessage = "";
			
			mPackets.send(new FireEventPacket(FireEventPacket.EVENT_QUIT, id, quitMessage), player.getServer().getInfo());
		}
		else
			Debugger.log("Player %s never joined a server", player.getName());
		BungeeChat.instance.getSubHandler().unSubscribe(id);
		mPackets.broadcast(new PlayerLeavePacket(id));
		mSettings.unloadPlayer(id);
	}
	
	// Post connection established, pre login to server
	@EventHandler
	public void onFirstConnected(ServerConnectedEvent event)
	{
		if (!isOnline(event.getPlayer()))
		{
			Debugger.log("ServerConnected player not online %s", event.getPlayer().getName());
			return;
		}
		
		final ProxiedPlayer player = event.getPlayer();
		
		// First server join only
		if(player.getServer() != null)
			return;
		
		mPackets.broadcast(new PlayerJoinPacket(player.getUniqueId(), player.getName(), mSettings.getSettings(player).nickname, mSettings.getSettings(player).defaultChannel)
		);
		
		BCPlayerJoinEvent jevent = new BCPlayerJoinEvent(player, ChatColor.YELLOW + ChatColor.stripColor(event.getPlayer().getDisplayName()) + " joined the game.");
		mProxy.getPluginManager().callEvent(jevent);
		
		String message = jevent.getJoinMessage();
		if (message == null)
			message = "";
		mPackets.send(new FireEventPacket(FireEventPacket.EVENT_JOIN, player.getUniqueId(), message), event.getServer().getInfo());
		
		// Give 1 second for plugins on the server to apply tab groups to this player
		mProxy.getScheduler().schedule(BungeeChat.instance, () -> {
            if (!isOnline(player))
            {
                Debugger.log("ServerConnected-task player not online %s", player.getName());
                return;
            }
            
            if(player.getTabListHandler() instanceof ColourTabList)
            {
                ((ColourTabList)player.getTabListHandler()).onJoinPeriodComplete();
                BungeeChat.instance.getPacketManager().send(new PlayerRefreshPacket(player.getUniqueId()), player.getServer().getInfo());
            }
        }, 1, TimeUnit.SECONDS);
	}
	
	// Post login request, still before actual server login
	@EventHandler
	public void onServerSwitch(final ServerSwitchEvent event)
	{
		if (!isOnline(event.getPlayer()))
		{
			Debugger.log("ServerSwitch player not online %s", event.getPlayer().getName());
			return;
		}
		
		mProxy.getScheduler().schedule(BungeeChat.instance, () -> {
            if (!isOnline(event.getPlayer()))
                return;
            
            mSettings.updateSettings(event.getPlayer());
        }, 10, TimeUnit.MILLISECONDS);
		
		ColourTabList.updateAll();
	}
	
	private boolean isOnline(ProxiedPlayer player)
	{
		return mProxy.getPlayer(player.getUniqueId()) != null;
	}
}
