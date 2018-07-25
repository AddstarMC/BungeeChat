package au.com.addstar.bc.commands;

import java.util.List;

import au.com.addstar.bc.BungeeChat;
import au.com.addstar.bc.PlayerManager;
import au.com.addstar.bc.objects.RemotePlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import au.com.addstar.bc.sync.IMethodCallback;

public class SkinCommand implements CommandExecutor, TabCompleter
{
	@Override
	public List<String> onTabComplete( CommandSender sender, Command cmd, String label, String[] args )
	{
		if(args.length == 1)
			return BungeeChat.getPlayerManager().matchNames(args[0]);
		return null;
	}

	@Override
	public boolean onCommand( final CommandSender sender, Command cmd, String label, String[] args )
	{
		if (args.length != 1 && args.length != 2)
			return false;
		
		PlayerManager manager = BungeeChat.getPlayerManager();
		
		CommandSender player = sender;
		
		if(args.length == 2)
		{
			player = manager.getPlayer(args[0]);
			if(!(player instanceof Player || player instanceof RemotePlayer))
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

		final String name = args[args.length-1];
		if(name.equalsIgnoreCase("off"))
		{
			BungeeChat.getSyncManager().callSyncMethod("bchat:setSkin", null, PlayerManager.getUniqueId(player), null);
			
			sender.sendMessage(ChatColor.GREEN + "Restored " + player.getName() + "'s skin");
		}
		else
		{
			sender.sendMessage(ChatColor.GREEN + "Looking up skin for " + name);
			final String playerName = player.getName();
			@SuppressWarnings( "deprecation" )
			OfflinePlayer oplayer = Bukkit.getOfflinePlayer(name);
			Object target;
			// Online mode UUIDs are version 4 and so represent actual players
			if (oplayer.getUniqueId().version() == 4)
				target = oplayer.getUniqueId();
			else
				target = name;
			
			BungeeChat.getSyncManager().callSyncMethod("bchat:setSkin", new IMethodCallback<Void>()
			{
				@Override
				public void onFinished( Void data )
				{
					sender.sendMessage(ChatColor.GREEN + "Setting " + playerName + "'s skin to be " + name + "'s skin.");
				}
				
				@Override
				public void onError( String type, String message )
				{
					sender.sendMessage(ChatColor.RED + message);
				}
			}, PlayerManager.getUniqueId(player), target);
		}
		
		return true;
	}

}
