package au.com.addstar.bc;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
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
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import au.com.addstar.bc.config.ChatChannel;
import au.com.addstar.bc.config.Config;
import au.com.addstar.bc.config.KeywordHighlighterSettings;
import au.com.addstar.bc.config.PermissionSetting;
import au.com.addstar.bc.config.ServerConfig;
import au.com.addstar.bc.sync.Packet;
import au.com.addstar.bc.sync.PacketManager;
import au.com.addstar.bc.sync.ProxyComLink;
import au.com.addstar.bc.sync.SyncConfig;
import au.com.addstar.bc.sync.SyncManager;
import au.com.addstar.bc.sync.SyncUtil;
import au.com.addstar.bc.sync.packet.MirrorPacket;
import au.com.addstar.bc.sync.packet.PlayerListPacket;
import au.com.addstar.bc.sync.packet.SendPacket;
import net.cubespace.Yamler.Config.InvalidConfigurationException;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

public class BungeeChat extends Plugin implements Listener
{
	private Config mConfig;
	private SyncConfig mConfigSync;
	
	private HashMap<String, String> mKeywordSettings = new HashMap<String, String>();
	private PlayerSettingsManager mSettings;
	
	public static BungeeChat instance;
	
	private SyncManager mSyncManager;
	private PacketManager mPacketManager;
	private MuteHandler mMuteHandler;
	private SkinLibrary mSkins;
	
	private ProxyComLink mComLink;
	
	@Override
	public void onEnable()
	{
		instance = this;
		
		File configFile = new File(getDataFolder(), "config.yml");
		if(!getDataFolder().exists())
			getDataFolder().mkdirs();
		
		saveResource("/keywords.txt", false);
		
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
					mComLink.init(mConfig.redis.host, mConfig.redis.port, mConfig.redis.password);
				}
				finally
				{
					setupWait.countDown();
				}
			}
		});
		
		try
		{
			setupWait.await();
		}
		catch(InterruptedException e)
		{
		}
		
		mPacketManager = new PacketManager(this);
		mPacketManager.initialize();
		mPacketManager.addHandler(new PacketHandler(), (Class<? extends Packet>[])null);
		
		mSyncManager = new SyncManager(this);
		SyncUtil.addSerializer(ChatChannel.class, "ChatChannel");
		SyncUtil.addSerializer(KeywordHighlighterSettings.class, "KHSettings");
		SyncUtil.addSerializer(PermissionSetting.class, "PermSetting");
		
		applySyncConfig();
		
		StandardServMethods methods = new StandardServMethods();
		mSyncManager.addMethod("bungee:getServerName", methods);
		mSyncManager.addMethod("bchat:isAFK", methods);
		mSyncManager.addMethod("bchat:canMsg", methods);
		mSyncManager.addMethod("bchat:setAFK", methods);
		mSyncManager.addMethod("bchat:toggleAFK", methods);
		mSyncManager.addMethod("bchat:setTabColor", methods);
		mSyncManager.addMethod("bchat:setMute", methods);
		mSyncManager.addMethod("bchat:setMuteIP", methods);
		mSyncManager.addMethod("bchat:setGMute", methods);
		mSyncManager.addMethod("bchat:toggleGMute", methods);
		mSyncManager.addMethod("bchat:setMsgTarget", methods);
		mSyncManager.addMethod("bchat:getMuteList", methods);
		mSyncManager.addMethod("bchat:kick", methods);
		mSyncManager.addMethod("bchat:setSkin", methods);
		
		getProxy().registerChannel("BungeeChat");
		getProxy().getPluginManager().registerListener(this, this);
		getProxy().getPluginManager().registerCommand(this, new ManagementCommand(this));
		getProxy().getPluginManager().registerCommand(this, new Debugger());
		getProxy().getPluginManager().registerListener(this, new PlayerHandler());

		mMuteHandler = new MuteHandler(this);
		mMuteHandler.updateSettings(mConfig);
		
		mSkins = new SkinLibrary();
		
		ColourTabList.initialize(this);
		
		mSyncManager.sendConfig("bungeechat");
		mPacketManager.sendSchemas();
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
			
			ColourTabList.updateAll();
			if(mMuteHandler != null)
				mMuteHandler.updateSettings(mConfig);
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
		
		try
		{
			BufferedReader reader = new BufferedReader(new InputStreamReader(input));
			
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
						getLogger().warning("[" + file + "] Invalid colour code: \'" + c + "\' at line " + lineNo);
						continue;
					}
					
					colour.append(col.toString());
				}
				
				mKeywordSettings.put(regex, colour.toString());
			}
			reader.close();
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
		
		FileOutputStream output = null;
		try
		{
			output = new FileOutputStream(destination);
			byte[] buffer = new byte[1024];
			int read = 0;
			
			while((read = input.read(buffer)) != -1)
			{
				output.write(buffer, 0, read);
			}
		}
		catch(IOException e)
		{
			getLogger().severe("Could not save resource " + resource + ". An IOException occured:");
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (output != null)
					output.close();
			}
			catch ( IOException e )
			{
			}
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
			mPacketManager.broadcast(packet);
	}

	@EventHandler
	public void onOldMessage(PluginMessageEvent event)
	{
		if (event.getTag().equals("BungeeChat") && event.getSender() instanceof Server)
		{
			ByteArrayInputStream stream = new ByteArrayInputStream(event.getData());
			DataInput input = new DataInputStream(stream);
			
			try
			{
				String subChannel = input.readUTF();
				if(subChannel.equals("Mirror"))
				{
					String chatChannel = input.readUTF();
					String message = input.readUTF();
					
					mPacketManager.broadcast(new MirrorPacket(chatChannel, message));
				}
				else if (subChannel.equals("Send"))
				{
					String idString = input.readUTF();
					UUID id;
					try
					{
						id = UUID.fromString(idString);
					}
					catch(IllegalArgumentException e)
					{
						ProxiedPlayer player = getProxy().getPlayer(idString);
						if(player == null)
							return;
						id = player.getUniqueId();
					}
					
					mPacketManager.broadcast(new SendPacket(id, input.readUTF()));
				}
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public String getTabHeaderString(ProxiedPlayer player)
	{
		String header = null;
		if (player.getServer() != null)
		{
			ServerConfig config = mConfig.servers.get(player.getServer().getInfo().getName()); 
			if (config != null)
				header = config.tabListHeader;
		}
		if (header == null)
			header = mConfig.tabListHeader;
		if (header == null)
			return "";
		
		return formatHeaderString(header, player);
	}
	
	public String getTabFooterString(ProxiedPlayer player)
	{
		String header = null;
		if (player.getServer() != null)
		{
			ServerConfig config = mConfig.servers.get(player.getServer().getInfo().getName()); 
			if (config != null)
				header = config.tabListFooter;
		}
		if (header == null)
			header = mConfig.tabListFooter;
		if (header == null)
			return "";
		
		return formatHeaderString(header, player);
	}
	
	private String formatHeaderString(String string, ProxiedPlayer player)
	{
		PlayerSettings settings = mSettings.getSettings(player);
		return ChatColor.translateAlternateColorCodes('&', string
				.replace("{PLAYER}", player.getName())
				.replace("{DISPLAYNAME}", player.getDisplayName())
				.replace("{TABNAME}", settings.tabColor + player.getDisplayName())
				.replace("{SERVER}", player.getServer() != null ? player.getServer().getInfo().getName() : "")
				.replace("{COUNT}", String.valueOf(getPlayerCount(player)))
				.replace("{MAX}", String.valueOf(player.getPendingConnection().getListener().getMaxPlayers())));
	}
	
	private int getPlayerCount(ProxiedPlayer player)
	{
		int count = 0;
		for(ProxiedPlayer other : getProxy().getPlayers())
		{
			if (ColourTabList.isVisible(player, other))
				++count;
		}
		
		return count;
	}
	
	public ChatChannel getChannel(String name)
	{
		return mConfig.channels.get(name);
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
	
	public MuteHandler getMuteHandler()
	{
		return mMuteHandler;
	}
	
	public SkinLibrary getSkinLibrary()
	{
		return mSkins;
	}
	
}
