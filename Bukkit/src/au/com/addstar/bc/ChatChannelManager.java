package au.com.addstar.bc;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.Plugin;

import au.com.addstar.bc.config.ChatChannelConfig;
import au.com.addstar.bc.event.ChatChannelEvent;
import au.com.addstar.bc.sync.SyncConfig;


public class ChatChannelManager implements Listener, CommandExecutor
{
	private HashMap<String, ChatChannel> mChannels = new HashMap<String, ChatChannel>();
	
	public ChatChannelManager(Plugin plugin)
	{
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	private void onChatChannel(ChatChannelEvent event)
	{
		if(event.getChannelType() != ChannelType.Custom)
			return;
		
		ChatChannel channelObj = mChannels.get(event.getChannelName());
		if(channelObj != null)
		{
			if(channelObj.listenPermission != null)
				Bukkit.broadcast(event.getMessage(), channelObj.listenPermission);
			else
				Bukkit.broadcastMessage(event.getMessage());
		}
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
			if(!channel.command.isEmpty() && channel.command.equals(command))
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
	
	@Override
	public boolean onCommand( CommandSender sender, Command command, String label, String[] args )
	{
		if(args.length < 2)
			return false;
		
		String channelCmd = args[0];
		
		String message = "";
		for(int i = 1; i < args.length; ++i)
		{
			if(!message.isEmpty())
				message += " ";
			
			message += args[i];
		}
		
		for(ChatChannel channel : mChannels.values())
		{
			if(!channel.command.isEmpty() && channel.command.equalsIgnoreCase(channelCmd))
			{
				if(channel.permission != null && !sender.hasPermission(channel.permission))
					break;
				
				if(message != null)
					channel.say(sender, message);
				
				return true;
			}
		}
		
		return true;
	}
	
	public void unregisterAll()
	{
		for(ChatChannel channel : mChannels.values())
			channel.unregisterChannel();
		
		mChannels.clear();
	}
	
	public void register(String name, String command, String format, String permission, String listenPerm)
	{
		ChatChannel channel = new ChatChannel(name, command, format, permission, listenPerm);
		channel.registerChannel();
		mChannels.put(channel.name, channel);
	}
	
	public void unregister(String name)
	{
		ChatChannel channel = mChannels.remove(name);
		if(channel != null)
			channel.unregisterChannel();
	}
	
	public void load(SyncConfig config)
	{
		unregisterAll();
		
		SyncConfig channels = config.getSection("channels");
		
		for(String key : channels.getKeys())
		{
			ChatChannelConfig setting = (ChatChannelConfig) channels.get(key, null);
			register(key, setting.command, setting.format, setting.permission, setting.listenPermission);
		}
	}
	
}
