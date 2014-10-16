package au.com.addstar.bc;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;

public class RealnameCommand implements CommandExecutor, TabCompleter
{
	@Override
	public List<String> onTabComplete( CommandSender sender, Command command, String label, String[] args )
	{
		if(args.length == 1)
			return BungeeChat.getPlayerManager().matchNames(args[0]);
		return null;
	}

	@Override
	public boolean onCommand( CommandSender sender, Command command, String label, String[] args )
	{
		if(args.length != 1)
			return false;
		
		CommandSender player = BungeeChat.getPlayerManager().getPlayer(args[0]);
		if(player == null || player instanceof ConsoleCommandSender)
		{
			sender.sendMessage(ChatColor.RED + "Unknown player " + args[0]);
			return true;
		}
		
		String nick = BungeeChat.getPlayerManager().getPlayerNickname(player);
		if (nick == null)
			nick = player.getName();
		sender.sendMessage(ChatColor.GOLD + nick + ChatColor.GRAY + " is actually " + ChatColor.GOLD + player.getName());
		
		return true;
	}

}
