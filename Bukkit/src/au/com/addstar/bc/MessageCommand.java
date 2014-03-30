package au.com.addstar.bc;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class MessageCommand implements CommandExecutor, TabCompleter
{
	@Override
	public List<String> onTabComplete( CommandSender sender, Command command, String label, String[] args )
	{
		if(command.getName().equals("msg"))
		{
			if(args.length == 1)
				return BungeeChat.matchPlayers(args[0]);
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
	
	@Override
	public boolean onCommand( CommandSender sender, Command command, String label, String[] args )
	{
		if(command.getName().equals("msg"))
		{
			if(args.length < 2)
				return false;
			
			CommandSender player = BungeeChat.getPlayer(args[0]);
			if(player == null)
			{
				sender.sendMessage(ChatColor.RED + "Cannot find player " + args[0]);
				return true;
			}
			
			String message = concat(args, 1);
			message = BungeeChat.colorize(message, sender);
			
			String displayName = player.getName();
			if(player instanceof Player)
				displayName = ((Player)player).getDisplayName();
			
			String displayNameIn = player.getName();
			if(sender instanceof Player)
				displayNameIn = ((Player)sender).getDisplayName();
			
			String fullMessageOut = String.format(Formatter.getPMFormat(player, false), displayName, message);
			String fullMessageIn = String.format(Formatter.getPMFormat(sender, true), displayNameIn, message);
			
			if(player instanceof RemotePlayer)
			{
				// Remote
				sender.sendMessage(fullMessageOut);
				BungeeChat.sendMessage((RemotePlayer)player, fullMessageIn);
			}
			else
			{
				// Local
				player.sendMessage(fullMessageIn);
				sender.sendMessage(fullMessageOut);
			}
			
			BungeeChat.setLastMsgTarget(sender, player);
			BungeeChat.setLastMsgTarget(player, sender);
			
			return true;
		}
		else if(command.getName().equals("reply"))
		{
			if(args.length == 0)
				return false;
			
			CommandSender player = BungeeChat.getLastMsgTarget(sender);
			if(player == null)
			{
				sender.sendMessage(ChatColor.RED + "You have nobody to reply to");
				return true;
			}
			
			String message = concat(args, 0);
			message = BungeeChat.colorize(message, sender);
			
			String displayName = player.getName();
			if(player instanceof Player)
				displayName = ((Player)player).getDisplayName();
			
			String displayNameIn = player.getName();
			if(sender instanceof Player)
				displayNameIn = ((Player)sender).getDisplayName();
			
			String fullMessageOut = String.format(Formatter.getPMFormat(player, false), displayName, message);
			String fullMessageIn = String.format(Formatter.getPMFormat(sender, true), displayNameIn, message);
			
			if(player instanceof RemotePlayer)
			{
				// Remote
				sender.sendMessage(fullMessageOut);
				BungeeChat.sendMessage((RemotePlayer)player, fullMessageIn);
			}
			else
			{
				// Local
				player.sendMessage(fullMessageIn);
				sender.sendMessage(fullMessageOut);
			}
			
			BungeeChat.setLastMsgTarget(sender, player);
			BungeeChat.setLastMsgTarget(player, sender);
			
			return true;
		}
		return false;
	}

}
