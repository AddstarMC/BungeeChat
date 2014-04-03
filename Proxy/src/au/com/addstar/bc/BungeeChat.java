package au.com.addstar.bc;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.cubespace.Yamler.Config.InvalidConfigurationException;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

public class BungeeChat extends Plugin implements Listener
{
	private Config mConfig;
	
	private HashMap<String, String> mKeywordSettings = new HashMap<String, String>();
	private PlayerSettingsManager mSettings;
	
	@Override
	public void onEnable()
	{
		File configFile = new File(getDataFolder(), "config.yml");
		if(!getDataFolder().exists())
			getDataFolder().mkdirs();
		
		mSettings = new PlayerSettingsManager(new File(getDataFolder(), "players"));
		
		saveResource("/keywords.txt", false);
		
		mConfig = new Config(configFile);
		
		loadConfig();
		
		getProxy().registerChannel("BungeeChat");
		getProxy().getPluginManager().registerListener(this, this);
		getProxy().getPluginManager().registerCommand(this, new ManagementCommand(this));
		
		resync();
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
	
	public void resync()
	{
		for(ServerInfo server : getProxy().getServers().values())
			sendInfo(server);
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
	
	private void sendInfo(ServerInfo server)
	{
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		DataOutputStream output = new DataOutputStream(stream);
		
		try
		{
			output.writeUTF("Update");
			output.writeUTF(server.getName());
			output.writeUTF(mConfig.consoleName);
			output.writeUTF(mConfig.pmFormatIn);
			output.writeUTF(mConfig.pmFormatOut);
			
			Map<String, PermissionSetting> permissions = mConfig.permSettings;
			output.writeShort(permissions.size());
			for(Entry<String, PermissionSetting> entry : permissions.entrySet())
			{
				PermissionSetting setting = entry.getValue();

				output.writeUTF((setting.permission == null ? "" : setting.permission));
				output.writeShort(setting.priority);
				output.writeUTF(setting.format);
				output.writeUTF(setting.color);
			}
			
			Map<String, ChatChannel> channels = mConfig.channels;
			output.writeShort(channels.size());
			for(Entry<String, ChatChannel> entry : channels.entrySet())
			{
				ChatChannel channel = entry.getValue();
				
				output.writeUTF(entry.getKey());
				
				output.writeUTF(channel.command);
				output.writeUTF(channel.format);
				output.writeUTF(channel.permission);
				output.writeUTF(channel.listenPermission);
			}
			
			output.writeBoolean(mConfig.keywordHighlighter.enabled);
			if(mConfig.keywordHighlighter.enabled)
			{
				KeywordHighlighterSettings settings = mConfig.keywordHighlighter;
				output.writeUTF(settings.permission);
				output.writeShort(settings.allowInChannels.size());
				for(String channel : settings.allowInChannels)
					output.writeUTF(channel);
				
				output.writeShort(mKeywordSettings.size());
				for(Entry<String, String> entry : mKeywordSettings.entrySet())
				{
					output.writeUTF(entry.getKey());
					output.writeUTF(entry.getValue());
				}
			}
			
			output.writeShort(mConfig.socialSpyKeywords.size());
			for(String keyword : mConfig.socialSpyKeywords)
				output.writeUTF(keyword);
			
			server.sendData("BungeeChat", stream.toByteArray());
		}
		catch(IOException e)
		{
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
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		DataOutputStream output = new DataOutputStream(stream);
		
		try
		{
			output.writeUTF("Message");
			output.writeUTF(player.getName());
			output.writeUTF(message);
		}
		catch(IOException e)
		{
		}
		
		player.getServer().sendData("BungeeChat", stream.toByteArray());
	}
	
	private void setLastMsgTarget(String player, String target, ServerInfo from)
	{
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		DataOutputStream output = new DataOutputStream(stream);
		
		ProxiedPlayer pplayer = getProxy().getPlayer(player);
		if(pplayer.getServer().getInfo() == from)
			return;
		
		try
		{
			output.writeUTF("MsgTarget");
			output.writeUTF(player);
			output.writeUTF(target);
		}
		catch(IOException e)
		{
		}
		
		pplayer.getServer().sendData("BungeeChat", stream.toByteArray());
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
					sendInfo(((Server)event.getSender()).getInfo());
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
				else if(subChannel.equals("MsgTarget"))
				{
					String player = input.readUTF();
					String target = input.readUTF();
					
					mSettings.getSettings(player).lastMsgTarget = target;
					setLastMsgTarget(player, target, ((Server)event.getSender()).getInfo());
				}
				else if(subChannel.equals("SocialSpy"))
				{
					String player = input.readUTF();
					boolean on = input.readBoolean();
					
					mSettings.getSettings(player).socialSpyState = (on ? 1 : 0);
					mSettings.savePlayer(player);
				}
			}
			catch(IOException e)
			{
			}
		}
	}
	
	private void sendPlayerUpdates(ServerInfo server)
	{
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		DataOutputStream output = new DataOutputStream(stream);
		
		try
		{
			output.writeUTF("UpdatePlayers");
			
			Collection<ProxiedPlayer> players = getProxy().getPlayers();
			output.writeShort(players.size());
			
			for(ProxiedPlayer player : players)
				output.writeUTF(player.getName());
			
			byte[] data = stream.toByteArray();
			
			if(server == null)
			{
				for(ServerInfo s : getProxy().getServers().values())
					s.sendData("BungeeChat", data);
			}
			else
				server.sendData("BungeeChat", data);
				
		}
		catch(IOException e)
		{
		}
	}
	
	@EventHandler
	public void onPlayerJoin(final PostLoginEvent event)
	{
		BungeeCord.getInstance().getScheduler().schedule(this, new Runnable()
		{
			@Override
			public void run()
			{
				// Load this players settings
				mSettings.getSettings(event.getPlayer());
				sendPlayerUpdates(null);
			}
			
		}, 10, TimeUnit.MILLISECONDS);
	}
	
	@EventHandler
	public void onPlayerDC(PlayerDisconnectEvent event)
	{
		BungeeCord.getInstance().getScheduler().schedule(this, new Runnable()
		{
			@Override
			public void run()
			{
				sendPlayerUpdates(null);
			}
			
		}, 10, TimeUnit.MILLISECONDS);
	}
	
	@EventHandler
	public void onServerSwitch(final ServerSwitchEvent event)
	{
		final PlayerSettings settings = mSettings.getSettings(event.getPlayer());
		
		getProxy().getScheduler().schedule(this, new Runnable()
		{
			@Override
			public void run()
			{
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				DataOutputStream output = new DataOutputStream(stream);
				
				try
				{
					output.writeUTF("MsgTarget");
					output.writeUTF(event.getPlayer().getName());
					output.writeUTF(settings.lastMsgTarget == null ? "" : settings.lastMsgTarget);
				}
				catch(IOException e)
				{
				}
				
				event.getPlayer().getServer().sendData("BungeeChat", stream.toByteArray());
				
				stream = new ByteArrayOutputStream();
				output = new DataOutputStream(stream);
				
				try
				{
					output.writeUTF("SocialSpy");
					output.writeUTF(event.getPlayer().getName());
					output.writeByte(settings.socialSpyState);
				}
				catch(IOException e)
				{
				}
				
				event.getPlayer().getServer().sendData("BungeeChat", stream.toByteArray());
			}
		}, 10, TimeUnit.MILLISECONDS);
	}
			
}
