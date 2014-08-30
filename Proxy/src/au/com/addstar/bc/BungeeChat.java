package au.com.addstar.bc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import au.com.addstar.bc.config.ChatChannel;
import au.com.addstar.bc.config.Config;
import au.com.addstar.bc.config.KeywordHighlighterSettings;
import au.com.addstar.bc.config.PermissionSetting;
import au.com.addstar.bc.sync.Packet;
import au.com.addstar.bc.sync.PacketManager;
import au.com.addstar.bc.sync.ProxyComLink;
import au.com.addstar.bc.sync.SyncConfig;
import au.com.addstar.bc.sync.SyncManager;
import au.com.addstar.bc.sync.SyncUtil;
import au.com.addstar.bc.sync.packet.FireEventPacket;
import au.com.addstar.bc.sync.packet.GlobalMutePacket;
import au.com.addstar.bc.sync.packet.MirrorPacket;
import au.com.addstar.bc.sync.packet.PlayerJoinPacket;
import au.com.addstar.bc.sync.packet.PlayerLeavePacket;
import au.com.addstar.bc.sync.packet.PlayerListPacket;
import au.com.addstar.bc.sync.packet.QuitMessagePacket;
import net.cubespace.Yamler.Config.InvalidConfigurationException;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.event.EventHandler;

public class BungeeChat extends Plugin implements Listener
{
	private Config mConfig;
	private SyncConfig mConfigSync;
	
	private HashMap<String, String> mKeywordSettings = new HashMap<String, String>();
	private PlayerSettingsManager mSettings;
	private HashMap<UUID, ScheduledTask> mWaitingQuitMessages = new HashMap<UUID, ScheduledTask>();
	
	public static BungeeChat instance;
	
	private SyncManager mSyncManager;
	private PacketManager mPacketManager;
	private long mGMuteTime;
	
	private ProxyComLink mComLink;
	
	@Override
	public void onEnable()
	{
		instance = this;
		
		File configFile = new File(getDataFolder(), "config.yml");
		if(!getDataFolder().exists())
			getDataFolder().mkdirs();
		
		mConfig = new Config(configFile);
		loadConfig();
		
		mSettings = new PlayerSettingsManager(new File(getDataFolder(), "players"));
		
		mComLink = new ProxyComLink();
		// This setup is needed as the redis connection cannot be established on the main thread, but we need it to be established before continuing
		final CountDownLatch setupWait = new CountDownLatch(1);
		getProxy().getScheduler().runAsync(this, new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					System.out.println("Setup");
					mComLink.init(mConfig.redis.host, mConfig.redis.port, mConfig.redis.password);
				}
				finally
				{
					System.out.println("Done");
					setupWait.countDown();
				}
			}
		});
		
		try
		{
			System.out.println("Wait");
			setupWait.await();
			System.out.println("Go");
		}
		catch(InterruptedException e)
		{
		}
		
		mPacketManager = new PacketManager(this);
		mPacketManager.initialize();
		mPacketManager.addHandler(new PacketHandler(), (Class<? extends Packet>[])null);
		
		mSyncManager = new SyncManager(this);
		applySyncConfig();
		
		StandardServMethods methods = new StandardServMethods();
		mSyncManager.addMethod("bungee:getServerName", methods);
		mSyncManager.addMethod("bchat:isAFK", methods);
		mSyncManager.addMethod("bchat:canMsg", methods);
		mSyncManager.addMethod("bchat:setAFK", methods);
		mSyncManager.addMethod("bchat:toggleAFK", methods);
		mSyncManager.addMethod("bchat:setTabColor", methods);
		mSyncManager.addMethod("bchat:setMute", methods);
		mSyncManager.addMethod("bchat:setMsgTarget", methods);
		mSyncManager.addMethod("bchat:getMuteList", methods);
		
		SyncUtil.addSerializer(ChatChannel.class, "ChatChannel");
		SyncUtil.addSerializer(KeywordHighlighterSettings.class, "KHSettings");
		SyncUtil.addSerializer(PermissionSetting.class, "PermSetting");
		
		saveResource("/keywords.txt", false);
		
		getProxy().getPluginManager().registerListener(this, this);
		getProxy().getPluginManager().registerCommand(this, new ManagementCommand(this));

		getProxy().getScheduler().schedule(this, new UnmuteTimer(), 5, 5, TimeUnit.SECONDS);
		
		ColourTabList.initialize(this);
		
		mSyncManager.sendConfig("bungeechat");
		System.out.println("Send schemas");
		mPacketManager.sendSchemas();
		System.out.println("All Done");
	}
	
	public boolean loadConfig()
	{
		try
		{
			mConfig.init();
			
			for(ChatChannel channel : mConfig.channels.values())
			{
				if(channel.listenPermission == null)
					channel.listenPermission = channel.permission;
				else if(channel.listenPermission.equals("*"))
					channel.listenPermission = "";
			}
			
			if(mConfig.keywordHighlighter.enabled)
			{
				loadKeywordFile(mConfig.keywordHighlighter.keywordFile);
			}
			return true;
		}
		catch ( InvalidConfigurationException e )
		{
			getLogger().severe("Could not load config");
			e.printStackTrace();
			return false;
		}
		catch (IOException e)
		{
			getLogger().severe("Could not load " + mConfig.keywordHighlighter.keywordFile);
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean applySyncConfig()
	{
		try
		{
			SyncConfig syncConfig = mConfig.toSyncConfig();
			if(mConfig.keywordHighlighter.enabled)
			{
				loadKeywordFile(mConfig.keywordHighlighter.keywordFile);
				SyncConfig keywords = syncConfig.createSection("keywords");
				for(Entry<String, String> entry : mKeywordSettings.entrySet())
					keywords.set(entry.getKey(), entry.getValue());
			}
			
			mConfigSync = syncConfig;
			mSyncManager.setConfig("bungeechat", mConfigSync);
			return true;
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return false;
		}
	}
	private void loadKeywordFile(String file) throws IOException
	{
		mKeywordSettings.clear();
		File onDisk = new File(getDataFolder(), file);
			
		InputStream input = new FileInputStream(onDisk);
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		
		try
		{
			int lineNo = 0;
			while(reader.ready())
			{
				++lineNo;
				String line = reader.readLine();
				if(line.startsWith("#") || line.trim().isEmpty())
					continue;
				
				String regex, colourString;
			
				if(line.contains(">"))
				{
					int pos = line.lastIndexOf('>');
					regex = line.substring(0, pos).trim();
					colourString = line.substring(pos + 1).trim();
				}
				else
				{
					regex = line.trim();
					colourString = ChatColor.GOLD.toString();
				}

				try
				{
					Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
				}
				catch(PatternSyntaxException e)
				{
					getLogger().warning("[" + file + "] Invalid regex: \"" + regex + "\" at line " + lineNo);
					continue;
				}
				
				StringBuilder colour = new StringBuilder(); 
				for(int i = 0; i < colourString.length(); ++i)
				{
					char c = colourString.charAt(i);
					ChatColor col = ChatColor.getByChar(c);
					
					if(col == null)
					{
						getLogger().warning("[" + file + "] Invalid colour code: \'" + col + "\' at line " + lineNo);
						continue;
					}
					
					colour.append(col.toString());
				}
				
				mKeywordSettings.put(regex, colour.toString());
			}
		}
		finally
		{
			input.close();
		}
	}
	
	private void saveResource(String resource, boolean overwrite)
	{
		File destination = new File(getDataFolder(), resource);
		
		if(destination.exists() && !overwrite)
			return;
		
		destination.getParentFile().mkdirs();
		
		InputStream input = getClass().getResourceAsStream(resource);
		if(input == null)
		{
			getLogger().severe("Could not save resource " + resource + ". It does not exist in the jar.");
			return;
		}
		
		try
		{
			FileOutputStream output = new FileOutputStream(destination);
			byte[] buffer = new byte[1024];
			int read = 0;
			
			while((read = input.read(buffer)) != -1)
			{
				output.write(buffer, 0, read);
			}
			
			output.close();
		}
		catch(IOException e)
		{
			getLogger().severe("Could not save resource " + resource + ". An IOException occured:");
			e.printStackTrace();
		}
	}
	
	public void sendPlayerUpdates(ServerInfo server)
	{
		Collection<ProxiedPlayer> players = getProxy().getPlayers();
		ArrayList<UUID> ids = new ArrayList<UUID>(players.size());
		ArrayList<String> names = new ArrayList<String>(players.size());
		ArrayList<String> nicknames = new ArrayList<String>(players.size());
		
		for(ProxiedPlayer player : players)
		{
			ids.add(player.getUniqueId());
			names.add(player.getName());
			PlayerSettings settings = mSettings.getSettings(player);
			nicknames.add(settings.nickname);
		}
		
		PlayerListPacket packet = new PlayerListPacket(ids, names, nicknames);
		if(server != null)
			mPacketManager.send(packet, server);
		else
			mPacketManager.broadcastNoQueue(packet);
	}
	
	@EventHandler
	public void onPlayerJoin(final PostLoginEvent event)
	{
		event.getPlayer().setTabList(new ColourTabList());
		getProxy().getScheduler().schedule(this, new Runnable()
		{
			@Override
			public void run()
			{
				// Load this players settings
				PlayerSettings settings = mSettings.getSettings(event.getPlayer());
				
				if(settings.nickname.isEmpty())
					event.getPlayer().setDisplayName(event.getPlayer().getName());
				else
					event.getPlayer().setDisplayName(settings.nickname);
				
				mPacketManager.broadcastNoQueue(new PlayerJoinPacket(event.getPlayer().getUniqueId(), event.getPlayer().getName(), settings.nickname));
			}
			
		}, 50, TimeUnit.MILLISECONDS);
	}
	
	@EventHandler
	public void onPlayerDC(final PlayerDisconnectEvent event)
	{
		if(event.getPlayer().getServer() == null)
			return;
		
		Byte showQuitMessage = (Byte)mSyncManager.getProperty(event.getPlayer(), "hasQuitMessage"); 
		String quitMessage = ChatColor.YELLOW + ChatColor.stripColor(event.getPlayer().getDisplayName()) + " left the game."; 
		if(showQuitMessage != null && showQuitMessage == 0)
			quitMessage = "";
		
		mPacketManager.send(new FireEventPacket(FireEventPacket.EVENT_QUIT, event.getPlayer().getUniqueId(), quitMessage), event.getPlayer().getServer().getInfo());
		
		final String fQuitMessage = quitMessage;
		mWaitingQuitMessages.put(event.getPlayer().getUniqueId(), ProxyServer.getInstance().getScheduler().schedule(this, new Runnable()
		{
			@Override
			public void run()
			{
				if(!fQuitMessage.isEmpty())
					mPacketManager.broadcastNoQueue(new MirrorPacket("~BC", fQuitMessage));
			}
		}, 100, TimeUnit.MILLISECONDS));
		
		getProxy().getScheduler().schedule(this, new Runnable()
		{
			@Override
			public void run()
			{
				mPacketManager.broadcastNoQueue(new PlayerLeavePacket(event.getPlayer().getUniqueId()));
			}
			
		}, 10, TimeUnit.MILLISECONDS);
	}
	
	@EventHandler
	public void onServerSwitch(final ServerSwitchEvent event)
	{
		getProxy().getScheduler().schedule(this, new Runnable()
		{
			@Override
			public void run()
			{
				if(event.getPlayer().getServer().getInfo().getPlayers().size() <= 1)
					sendPlayerUpdates(event.getPlayer().getServer().getInfo());
				mSettings.updateSettings(event.getPlayer());
			}
		}, 10, TimeUnit.MILLISECONDS);
	}
	
	@EventHandler
	public void onServerFirstJoin(ServerConnectedEvent event)
	{
		final ProxiedPlayer player = event.getPlayer();
		if(player.getServer() == null)
		{
			PlayerSettings settings = mSettings.getSettings(player);
			
			String message;
			if(settings.nickname.isEmpty())
				message = ChatColor.YELLOW + event.getPlayer().getName() + " joined the game.";
			else
				message = ChatColor.YELLOW + settings.nickname + " joined the game.";
			
			mPacketManager.send(new FireEventPacket(FireEventPacket.EVENT_JOIN, player.getUniqueId(), message), event.getServer().getInfo());
			
			getProxy().getScheduler().schedule(this, new Runnable()
			{
				@Override
				public void run()
				{
					if(player.getTabList() instanceof ColourTabList)
						((ColourTabList)player.getTabList()).updateList();
				}
			}, 1, TimeUnit.SECONDS);
		}
	}
	
	@EventHandler
	public void onPlayerChat(ChatEvent event)
	{
		if(event.getSender() instanceof ProxiedPlayer && !event.isCommand())
		{
			PlayerSettings settings = mSettings.getSettings((ProxiedPlayer)event.getSender());
			if(settings.muteTime > System.currentTimeMillis())
			{
				event.setCancelled(true);
				((ProxiedPlayer)event.getSender()).sendMessage(TextComponent.fromLegacyText(ChatColor.AQUA + "You are muted. You may not talk."));
			}
			else if(settings.muteTime != 0)
			{
				settings.muteTime = 0;
				mSettings.savePlayer((ProxiedPlayer)event.getSender());
				mSettings.updateSettings((ProxiedPlayer)event.getSender());
			}
		}
	}
	
	public PacketManager getPacketManager()
	{
		return mPacketManager;
	}
	
	public PlayerSettingsManager getManager()
	{
		return mSettings;
	}
	
	public SyncManager getSyncManager()
	{
		return mSyncManager;
	}
	
	public ProxyComLink getComLink()
	{
		return mComLink;
	}
	
	public void updateTabLists(String oldColor, ProxiedPlayer player)
	{
		if(player == null)
			return;
		
		String oldName = oldColor + player.getDisplayName();
		if(oldName.length() > 16)
			oldName = oldName.substring(0, 16);
		
		ColourTabList.nameChange(player, oldName);
	}
	
	private class UnmuteTimer implements Runnable
	{
		@Override
		public void run()
		{
			if(mGMuteTime > 0 && System.currentTimeMillis() >= mGMuteTime)
			{
				getProxy().broadcast(TextComponent.fromLegacyText(ChatColor.AQUA + "The global mute has ended"));
				mGMuteTime = 0;
				mPacketManager.broadcast(new GlobalMutePacket(0));
			}
			
			for(ProxiedPlayer player : getProxy().getPlayers())
			{
				PlayerSettings settings = mSettings.getSettings(player);
				if(settings.muteTime > 0 && System.currentTimeMillis() >= settings.muteTime)
				{
					settings.muteTime = 0;
					mSettings.updateSettings(player);
					mSettings.savePlayer(player);
					
					player.sendMessage(TextComponent.fromLegacyText(ChatColor.AQUA + "You are no longer muted. You may talk again."));
				}
			}
		}
	}

	public void setGlobalMute( long time )
	{
		mGMuteTime = time;
	}

	public void handleQuitMessage( QuitMessagePacket packet )
	{
		ScheduledTask task = mWaitingQuitMessages.remove(packet.getID());
		if(task != null)
		{
			task.cancel();
			String message = packet.getMessage();
			
			if(!message.isEmpty())
				mPacketManager.broadcastNoQueue(new MirrorPacket("~BC", message));
		}
	}
}
