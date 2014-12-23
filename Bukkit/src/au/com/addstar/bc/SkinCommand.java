package au.com.addstar.bc;

import java.util.List;

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
			BungeeChat.getSyncManager().callSyncMethod("bchat:setSkin", null, PlayerManager.getUniqueId(player), null);
			
			sender.sendMessage(ChatColor.GREEN + "Restored " + player.getName() + "'s skin");
		}
		else
		{
			final String playerName = player.getName();
			@SuppressWarnings( "deprecation" )
			final OfflinePlayer oplayer = Bukkit.getOfflinePlayer(name);
			BungeeChat.getSyncManager().callSyncMethod("bchat:setSkin", new IMethodCallback<Void>()
			{
				@Override
				public void onFinished( Void data )
				{
					sender.sendMessage(ChatColor.GREEN + "Setting " + playerName + "'s skin to be " + oplayer.getName() + "'s skin.");
				}
				
				@Override
				public void onError( String type, String message )
				{
					sender.sendMessage(ChatColor.RED + message);
				}
			}, PlayerManager.getUniqueId(player), oplayer.getUniqueId());
		}
		
		return true;
	}

}
