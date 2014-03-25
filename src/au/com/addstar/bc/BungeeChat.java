package au.com.addstar.bc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class BungeeChat extends JavaPlugin implements PluginMessageListener, Listener
{
	private static Permission permissionManager;
	
	public static String serverName = "ERROR";
	private static BungeeChat mInstance;
	
	private Formatter mFormatter;
	
	private boolean mHasRequestedUpdate = false;
	private boolean mHasUpdated = false;
	
	private HashMap<String, ChatChannel> mChannels = new HashMap<String, ChatChannel>();
	
	@Override
	public void onEnable()
	{
		mInstance = this;
		
		RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(Permission.class);
		if (permissionProvider != null)
			permissionManager = permissionProvider.getProvider();
		else
			permissionManager = null;
		
		if(isBPermsAvailable())
			BPermsCompat.initialize();
		
		mFormatter = new Formatter();
		
		Bukkit.getPluginManager().registerEvents(new ChatHandler(mFormatter), this);
		Bukkit.getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);
		Bukkit.getMessenger().registerIncomingPluginChannel(this, "BungeeChat", this);
		Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeChat");
		Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
		
		Bukkit.getPluginManager().registerEvents(this, this);
		
		requestUpdate();
	}
	
	@Override
	public void onDisable()
	{
		for(ChatChannel channel : mChannels.values())
			channel.unregisterChannel();
	}
	
	private void requestUpdate()
	{
		Player player = null;
		
		if(Bukkit.getOnlinePlayers().length > 0)
			player = Bukkit.getOnlinePlayers()[0];
		else
			return;
		
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(stream);
		
		try
		{
			out.writeUTF("Update");
		}
		catch ( IOException e ) 
		{
		}
		
		player.sendPluginMessage(mInstance, "BungeeChat", stream.toByteArray());
		
		mHasRequestedUpdate = true;
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	private void onPlayerJoin(PlayerJoinEvent event)
	{
		if(!mHasRequestedUpdate || !mHasUpdated)
			requestUpdate();
	}
	
	private boolean processCommands(CommandSender sender, String message)
	{
		String command;
		int pos = message.indexOf(' ');
		if(pos == -1)
		{
			command = message;
			message = null;
		}
		else
		{
			command = message.substring(0, pos);
			message = message.substring(pos+1);
		}
		
		for(ChatChannel channel : mChannels.values())
		{
			if(channel.command.equals(command))
			{
				if(channel.permission != null && !sender.hasPermission(channel.permission))
					break;
				
				if(message != null)
					channel.say(sender, message);
				
				return true;
			}
		}
		
		return false;
	}
	
	@EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
	private void onPlayerCommand(PlayerCommandPreprocessEvent event)
	{
		if(processCommands(event.getPlayer(), event.getMessage().substring(1)))
		{
			event.setMessage("/nullcmd");
			event.setCancelled(true);
		}
	}
	
	@EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
	private void onServerCommand(ServerCommandEvent event)
	{
		if(processCommands(event.getSender(), event.getCommand()))
		{
			event.setCommand("/nullcmd");
		}
	}
	
	public static void mirrorChat(String fullChat, String channel)
	{
		Player[] players = Bukkit.getOnlinePlayers();
		if(players.length != 0)
			mirrorChat(players[0], fullChat, channel);
		// Cant send it without any players :(
		// TODO: This can be a problem if the console uses a sub chat without any players on this specific server
	}
	
	public static void mirrorChat(Player player, String fullChat, String channel)
	{
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(stream);
		
		try
		{
			out.writeUTF("Mirror");
			out.writeUTF(channel);
			out.writeUTF(fullChat);
		}
		catch ( IOException e ) 
		{
		}
		
		player.sendPluginMessage(mInstance, "BungeeChat", stream.toByteArray());
	}
	
	public static String getPrimaryGroup(Player player)
	{
		if(permissionManager == null)
			return null;
		
		try
		{
			return permissionManager.getPrimaryGroup(player);
		}
		catch(UnsupportedOperationException e)
		{
			if(isBPermsAvailable())
				return BPermsCompat.getPrimaryGroup(player);
			return null;
		}
	}
	
	public static boolean isInGroup(Player player, String group)
	{
		if(permissionManager == null)
			return false;
		
		try
		{
			return permissionManager.playerInGroup(player, group);
		}
		catch(UnsupportedOperationException e)
		{
			if(isBPermsAvailable())
				return BPermsCompat.isInGroup(player, group);
			return false;
		}
	}
	
	public static boolean isBPermsAvailable()
	{
		return Bukkit.getPluginManager().isPluginEnabled("BungeePermsBukkit");
	}
	
	private void update(DataInputStream input) throws IOException
	{
		mHasRequestedUpdate = true;
		mHasUpdated = true;
		
		serverName = input.readUTF();
		mFormatter.mDefaultFormat = input.readUTF();
		mFormatter.mChatFormats.clear();
		int count = input.readShort();
		for(int i = 0; i < count; ++i)
			mFormatter.mChatFormats.put(input.readUTF(), input.readUTF());
		
		for(ChatChannel channel : mChannels.values())
			channel.unregisterChannel();
		
		mChannels.clear();
		
		count = input.readShort();
		for(int i = 0; i < count; ++i)
		{
			ChatChannel channel = new ChatChannel(input.readUTF(), input.readUTF(), input.readUTF(), input.readUTF(), input.readUTF());
			channel.registerChannel();
			mChannels.put(channel.name, channel);
		}
	}

	@Override
	public void onPluginMessageReceived( String channel, Player player, byte[] data )
	{
		ByteArrayInputStream stream = new ByteArrayInputStream(data);
		DataInputStream input = new DataInputStream(stream);
		try
		{
			String subChannel = input.readUTF();
			if(channel.equals("BungeeCord"))
			{
				
			}
			else if(channel.equals("BungeeChat"))
			{
				if(subChannel.equals("Mirror"))
				{
					String chatChannel = input.readUTF();
					String message = input.readUTF();
					
					if(chatChannel.isEmpty())
						Bukkit.broadcastMessage(message);
					else
					{
						ChatChannel channelObj = mChannels.get(chatChannel);
						if(channelObj != null)
						{
							if(channelObj.listenPermission != null)
								Bukkit.broadcast(message, channelObj.listenPermission);
							else
								Bukkit.broadcastMessage(message);
						}
					}
				}
				else if(subChannel.equals("Update"))
				{
					update(input);
				}
			}
		}
		catch(IOException e) 
		{
		}
	}
	
	public static String colorize(String message, CommandSender sender)
	{
		int pos = -1;
		char colorChar = '&';
		
		StringBuffer buffer = new StringBuffer(message);
		
		boolean hasColor = sender.hasPermission("bungeechat.color");
		boolean hasReset = sender.hasPermission("bungeechat.format.reset");
		boolean hasBold = sender.hasPermission("bungeechat.format.bold");
		boolean hasItalic = sender.hasPermission("bungeechat.format.italic");
		boolean hasUnderline = sender.hasPermission("bungeechat.format.underline");
		boolean hasStrikethrough = sender.hasPermission("bungeechat.format.strikethrough");
		boolean hasMagic = sender.hasPermission("bungeechat.format.magic");
		
		while((pos = message.indexOf(colorChar, pos+1)) != -1)
		{
			if(message.length() > pos + 1)
			{
				char atPos = Character.toLowerCase(message.charAt(pos+1));
				
				boolean allow = false;
				if(((atPos >= '0' && atPos <= '9') || (atPos >= 'a' && atPos <= 'f')) && hasColor)
					allow = true;
				else if(atPos == 'r' && hasReset)
					allow = true;
				else if(atPos == 'l' && hasBold)
					allow = true;
				else if(atPos == 'm' && hasStrikethrough)
					allow = true;
				else if(atPos == 'n' && hasUnderline)
					allow = true;
				else if(atPos == 'o' && hasItalic)
					allow = true;
				else if(atPos == 'k' && hasMagic)
					allow = true;
				
				if(allow)
					buffer.setCharAt(pos, ChatColor.COLOR_CHAR);
			}
		}
		
		return buffer.toString();
	}
}
