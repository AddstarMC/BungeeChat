package au.com.addstar.bc;

import java.util.List;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class NicknameCommand implements CommandExecutor, TabCompleter
{
	private Pattern mAllowed = Pattern.compile("^[a-zA-Z0-9_]+$");
	
	@Override
	public List<String> onTabComplete( CommandSender sender, Command cmd, String label, String[] args )
	{
		if(args.length == 1)
			return BungeeChat.getPlayerManager().matchNames(args[0]);
		return null;
	}

	@Override
	public boolean onCommand( CommandSender sender, Command cmd, String label, String[] args )
	{
		if(args.length != 1 && args.length != 2)
			return false;
		
		PlayerManager manager = BungeeChat.getPlayerManager();
		
		CommandSender player = sender;
		
		if(args.length == 2)
		{
			player = manager.getPlayer(args[0]);
			if(player == null || !(player instanceof Player || player instanceof RemotePlayer))
			{
				sender.sendMessage(ChatColor.RED + "Unknown player " + args[0]);
				return true;
			}
		}
		else if(!(sender instanceof Player))
		{
			sender.sendMessage(ChatColor.RED + "A player name must be specified if not called by a player.");
			return true;
		}

		String name = args[args.length-1];
		if(name.equalsIgnoreCase("off"))
		{
			manager.setPlayerNickname(player, "");
			sender.sendMessage(ChatColor.GREEN + "Removed " + player.getName() + "'s nickname");
		}
		else
		{
			if(!mAllowed.matcher(name).matches())
			{
				sender.sendMessage(ChatColor.RED + "Invalid characters in nickname. Can only use a-z 0-9 and _");
				return true;
			}
			
			if (name.length() > 16)
			{
				sender.sendMessage(ChatColor.RED + "Nickname cannot be longer than 16 characters");
				return true;
			}
			
			CommandSender other = manager.getPlayerExact(name);
			// Allow them to change the case of their name, but not to any other existing name
			if(other != null && other != player)
			{
				sender.sendMessage(ChatColor.RED + "You cannot nickname someone to an existing name");
				return true;
			}
			
			manager.setPlayerNickname(player, name);
			sender.sendMessage(ChatColor.GREEN + "Nickname changed");
		}
		
		return true;
	}

}
