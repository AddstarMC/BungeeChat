package au.com.addstar.bc;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;
import java.util.WeakHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class MessageCommand implements CommandExecutor, TabCompleter, PluginMessageListener
{
	private WeakHashMap<CommandSender, StoredMessage> mWaitingForResponse = new WeakHashMap<CommandSender, StoredMessage>();
	
	@Override
	public List<String> onTabComplete( CommandSender sender, Command command, String label, String[] args )
	{
		if(command.getName().equals("tell"))
		{
			if(args.length == 1)
				return BungeeChat.getPlayerManager().matchNames(args[0]);
		}
		
		return null;
	}

	private String concat(String[] args, int start)
	{
		String str = "";
		for(int i = start; i < args.length; ++i)
		{
			if(!str.isEmpty())
				str += " ";
			str += args[i];
		}
		
		return str;
	}
	
	private void sendCanMessageRequest(CommandSender from, CommandSender target, String message)
	{
		new MessageOutput("BungeeChat", "MsgCheck")
			.writeUTF(target.getName())
			.writeUTF(from.getName())
			.send(BungeeChat.getInstance());
		mWaitingForResponse.put(from, new StoredMessage(message, target, from));
	}
	
	private void doSendMessage(CommandSender to, CommandSender from, String message)
	{
		String displayName = to.getName();
		if(to instanceof Player)
			displayName = ((Player)to).getDisplayName();
		else if(to instanceof RemotePlayer)
			displayName = ((RemotePlayer)to).getDisplayName();
			
		String displayNameIn = from.getName();
		if(from instanceof Player)
			displayNameIn = ((Player)from).getDisplayName();
		
		String fullMessageOut = String.format(Formatter.getPMFormat(to, false), displayName, message);
		String fullMessageIn = String.format(Formatter.getPMFormat(from, true), displayNameIn, message);
		
		if(to instanceof RemotePlayer)
		{
			// Remote
			from.sendMessage(fullMessageOut);
			BungeeChat.sendMessage((RemotePlayer)to, fullMessageIn);
		}
		else
		{
			// Local
			to.sendMessage(fullMessageIn);
			from.sendMessage(fullMessageOut);
		}
		
		BungeeChat.setLastMsgTarget(from, to);
		BungeeChat.setLastMsgTarget(to, from);
	}
	
	@Override
	public boolean onCommand( CommandSender sender, Command command, String label, String[] args )
	{
		if(command.getName().equals("tell"))
		{
			if(args.length < 2)
				return false;

			if(BungeeChat.getPlayerManager().isPlayerMuted(sender))
			{
				sender.sendMessage(ChatColor.AQUA + "You are muted. You may not talk");
				return true;
			}
			
			CommandSender player = BungeeChat.getPlayerManager().getPlayer(args[0]);
			if(player == null)
			{
				sender.sendMessage(ChatColor.RED + "Cannot find player " + args[0]);
				return true;
			}
			
			String message = concat(args, 1);
			message = BungeeChat.colorize(message, sender);
			
			if(ChatColor.stripColor(message).trim().isEmpty())
				return false;
			
			if((sender instanceof Player) && !sender.hasPermission("bungeechat.message.override"))
			{
				if(player instanceof RemotePlayer)
				{
					sendCanMessageRequest(sender, player, message);
					return true;
				}
				else if(player instanceof Player)
				{
					if(!BungeeChat.getPlayerManager().getPlayerSettings(player).msgEnabled)
					{
						sender.sendMessage(ChatColor.RED + "That player has messaging disabled.");
						return true;
					}
				}
			}
			
			doSendMessage(player, sender, message);
			
			return true;
		}
		else if(command.getName().equals("reply"))
		{
			if(args.length == 0)
				return false;
			
			if(BungeeChat.getPlayerManager().isPlayerMuted(sender))
			{
				sender.sendMessage(ChatColor.AQUA + "You are muted. You may not talk");
				return true;
			}
			
			CommandSender player = BungeeChat.getPlayerManager().getPlayerSettings(sender).getLastMsgTarget();
			if(player == null)
			{
				sender.sendMessage(ChatColor.RED + "You have nobody to reply to");
				return true;
			}
			
			String message = concat(args, 0);
			message = BungeeChat.colorize(message, sender);
			
			if(ChatColor.stripColor(message).trim().isEmpty())
				return false;
			
			doSendMessage(player, sender, message);
			
			return true;
		}
		else if(command.getName().equals("msgtoggle"))
		{
			if(args.length != 0)
				return false;
			
			if(!(sender instanceof Player))
				return false;
			
			PlayerSettings settings = BungeeChat.getPlayerManager().getPlayerSettings(sender);
			settings.msgEnabled = !settings.msgEnabled;
			
			if(settings.msgEnabled)
				sender.sendMessage(ChatColor.GREEN + "Incoming Messaging Enabled");
			else
				sender.sendMessage(ChatColor.GOLD + "Incoming Messaging Disabled");
			
			BungeeChat.getPlayerManager().updatePlayerSettings(sender);
			return true;
		}
		return false;
	}
	
	@Override
	public void onPluginMessageReceived( String channel, Player sender, byte[] data )
	{
		if(!channel.equals("BungeeChat"))
			return;
		
		ByteArrayInputStream stream = new ByteArrayInputStream(data);
		DataInputStream input = new DataInputStream(stream);
		try
		{
			String subChannel = input.readUTF();
			if(subChannel.equals("MsgCheck"))
			{
				String player = input.readUTF();
				boolean ok = input.readBoolean();
				
				Player p = Bukkit.getPlayerExact(player);
				StoredMessage stored = mWaitingForResponse.get(p);
				
				if(stored != null)
				{
					if(!ok)
						p.sendMessage(ChatColor.RED + "That player has messaging disabled.");
					else
						doSendMessage(stored.to, stored.from, stored.message);
				}
			}
		}
		catch(IOException e)
		{
		}
	}
	
	private static class StoredMessage
	{
		public String message;
		public CommandSender to;
		public CommandSender from;
		
		public StoredMessage(String message, CommandSender to, CommandSender from)
		{
			this.message = message;
			this.to = to;
			this.from = from;
		}
	}

}
