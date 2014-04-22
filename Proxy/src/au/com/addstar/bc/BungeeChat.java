package au.com.addstar.bc;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import au.com.addstar.bc.config.ChatChannel;
import au.com.addstar.bc.config.Config;
import au.com.addstar.bc.config.KeywordHighlighterSettings;
import au.com.addstar.bc.config.PermissionSetting;
import au.com.addstar.bc.sync.SyncConfig;
import au.com.addstar.bc.sync.SyncManager;
import au.com.addstar.bc.sync.SyncUtil;

import net.cubespace.Yamler.Config.InvalidConfigurationException;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.protocol.packet.PlayerListItem;

public class BungeeChat extends Plugin implements Listener
{
	private Config mConfig;
	private SyncConfig mConfigSync;
	
	private HashMap<String, String> mKeywordSettings = new HashMap<String, String>();
	private PlayerSettingsManager mSettings;
	
	public static BungeeChat instance;
	
	private SyncManager mSyncManager;
	private long mGMuteTime;
	
	@Override
	public void onEnable()
	{
		instance = this;
		
		File configFile = new File(getDataFolder(), "config.yml");
		if(!getDataFolder().exists())
			getDataFolder().mkdirs();
		
		mSettings = new PlayerSettingsManager(new File(getDataFolder(), "players"));
		mSyncManager = new SyncManager(this);
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
		
		mConfig = new Config(configFile);
		
		getProxy().registerChannel("BungeeChat");
		getProxy().getPluginManager().registerListener(this, this);
		getProxy().getPluginManager().registerCommand(this, new ManagementCommand(this));

		getProxy().getScheduler().schedule(this, new UnmuteTimer(), 5, 5, TimeUnit.SECONDS);
		
		loadConfig();
		mSyncManager.sendConfig("bungeechat");
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
	
	private void mirrorChat(byte[] data, Server except)
	{
		ServerInfo exceptInfo = except.getInfo();
		for(ServerInfo server : getProxy().getServers().values())
		{
			if(!server.equals(exceptInfo) && !server.getPlayers().isEmpty())
				server.sendData("BungeeChat", data);
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
	
	private void sendMessage(ProxiedPlayer player, String message)
	{
		new MessageOutput("BungeeChat", "Message")
			.writeUTF(player.getName())
			.writeUTF(message)
			.send(player.getServer().getInfo());
	}
	
	@EventHandler
	public void onMessage(PluginMessageEvent event)
	{
		if(event.getTag().equals("BungeeChat") && event.getSender() instanceof Server)
		{
			ByteArrayInputStream stream = new ByteArrayInputStream(event.getData());
			DataInputStream input = new DataInputStream(stream);
			
			try
			{
				String subChannel = input.readUTF();
				
				if(subChannel.equals("Mirror"))
				{
					mirrorChat(event.getData().clone(), (Server)event.getSender());
					
					// Mirror chat to proxy console
					String chatChannel = input.readUTF();
					if(!chatChannel.startsWith("~")) // Ignore special channels
					{
						String message = input.readUTF();
						getProxy().getConsole().sendMessage(new TextComponent(message));
					}
				}
				else if(subChannel.equals("Update"))
				{
					sendPlayerUpdates(((Server)event.getSender()).getInfo());
				}
				else if(subChannel.equals("Send"))
				{
					String player = input.readUTF();
					String message = input.readUTF();
					
					ProxiedPlayer dest = getProxy().getPlayer(player);
					if(dest != null)
						sendMessage(dest, message);
				}
				else if(subChannel.equals("SyncPlayer"))
				{
					String player = input.readUTF();
					PlayerSettings settings = mSettings.getSettings(player);
					
					String oldName = settings.nickname;
					settings.read(input);
					mSettings.savePlayer(player);
					
					ProxiedPlayer p = getProxy().getPlayer(player);
					if(settings.nickname.isEmpty())
						p.setDisplayName(p.getName());
					else
						p.setDisplayName(settings.nickname);
					
					if(!oldName.equals(settings.nickname))
					{
						new MessageOutput("BungeeChat", "UpdateName")
						.writeUTF(player)
						.writeUTF(settings.nickname)
						.send();
					}
				}
				else if(subChannel.equals("UpdateName"))
				{
					String player = input.readUTF();
					String name = input.readUTF();
					PlayerSettings settings = mSettings.getSettings(player);
					settings.nickname = name;
					mSettings.savePlayer(player);
					mSettings.updateSettings(player);
					
					ProxiedPlayer p = getProxy().getPlayer(player);
					if(name.isEmpty())
						p.setDisplayName(p.getName());
					else
						p.setDisplayName(name);
					
					new MessageOutput("BungeeChat", "UpdateName")
						.writeUTF(player)
						.writeUTF(name)
						.send();
				}
				else if(subChannel.equals("GMute"))
				{
					long time = input.readLong();
					mGMuteTime = time;
					
					new MessageOutput("BungeeChat", "GMute")
						.writeLong(time)
						.send(true);
				}
			}
			catch(IOException e)
			{
			}
		}
	}
	
	private void sendPlayerUpdates(ServerInfo server)
	{
		MessageOutput output = new MessageOutput("BungeeChat", "Player*");
		
		Collection<ProxiedPlayer> players = getProxy().getPlayers();
		output.writeShort(players.size());
		
		for(ProxiedPlayer player : players)
		{
			PlayerSettings settings = mSettings.getSettings(player);
			output.writeUTF(player.getName());
			output.writeUTF(settings.nickname);
		}
		
		if(server == null)
			output.send();
		else
			output.send(server);
	}
	
	@EventHandler
	public void onPlayerJoin(final PostLoginEvent event)
	{
		event.getPlayer().setTabList(new ColourTabList());
		BungeeCord.getInstance().getScheduler().schedule(this, new Runnable()
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
				
				new MessageOutput("BungeeChat", "Player+")
					.writeUTF(event.getPlayer().getName())
					.writeUTF(settings.nickname)
					.send(false);
			}
			
		}, 50, TimeUnit.MILLISECONDS);
	}
	
	@EventHandler
	public void onPlayerDC(final PlayerDisconnectEvent event)
	{
		String message = null;
		Byte showQuitMessage = (Byte)mSyncManager.getProperty(event.getPlayer(), "hasQuitMessage"); 
		
		if(showQuitMessage == null || showQuitMessage != 0)
		{
			System.out.println(showQuitMessage);
			message = ChatColor.YELLOW + ChatColor.stripColor(event.getPlayer().getDisplayName()) + " left the game.";
		
			new MessageOutput("BungeeChat", "Mirror")
				.writeUTF("~BC")
				.writeUTF(message)
				.send(false);
		}
		
		BungeeCord.getInstance().getScheduler().schedule(this, new Runnable()
		{
			@Override
			public void run()
			{
				new MessageOutput("BungeeChat", "Player-")
				.writeUTF(event.getPlayer().getName())
				.send(false);
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
		if(event.getPlayer().getServer() == null)
		{
			PlayerSettings settings = mSettings.getSettings(event.getPlayer());
			
			new MessageOutput("BungeeChat", "ProxyJoin")
			.writeUTF(event.getPlayer().getName())
			.writeUTF(settings.nickname)
			.send(event.getServer().getInfo());
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
	
	public PlayerSettingsManager getManager()
	{
		return mSettings;
	}
	
	public SyncManager getSyncManager()
	{
		return mSyncManager;
	}
	
	public void updateTabLists(String newColor, ProxiedPlayer player)
	{
		if(player == null)
			return;
		
		PlayerSettings settings = mSettings.getSettings(player);
		
		String oldName = settings.tabColor + player.getDisplayName();
		if(oldName.length() > 16)
			oldName = oldName.substring(0, 16);
		String newName = newColor + player.getDisplayName();
		if(newName.length() > 16)
			newName = newName.substring(0,16);
		
		BungeeCord.getInstance().broadcast(new PlayerListItem(oldName, false, (short)9999));
		BungeeCord.getInstance().broadcast(new PlayerListItem(newName, true, (short)9999));
	}
	
	private class UnmuteTimer implements Runnable
	{
		@Override
		public void run()
		{
			if(mGMuteTime > 0 && System.currentTimeMillis() >= mGMuteTime)
			{
				BungeeCord.getInstance().broadcast(TextComponent.fromLegacyText(ChatColor.AQUA + "The global mute has ended"));
				mGMuteTime = 0;
				new MessageOutput("BungeeChat", "GMute")
					.writeLong(mGMuteTime)
					.send(true);
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
}
