package au.com.addstar.bc;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;

import au.com.addstar.bc.sync.IMethodCallback;
import au.com.addstar.bc.utils.Utilities;

public class MuteHandler implements CommandExecutor, TabCompleter, Listener
{
	public MuteHandler(Plugin plugin)
	{
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	@Override
	public List<String> onTabComplete( CommandSender sender, Command command, String label, String[] args )
	{
		if(args.length == 1)
			return BungeeChat.getPlayerManager().matchNames(args[0]);
		
		return null;
	}

	@Override
	public boolean onCommand( final CommandSender sender, Command command, String label, String[] args )
	{
		if(command.getName().equals("mutelist"))
		{
			if(args.length != 0)
				return false;
			
			BungeeChat.getSyncManager().callSyncMethod("bchat:getMuteList", new IMethodCallback<List<String>>()
			{
				@Override
				public void onFinished( List<String> data )
				{
					if(data.isEmpty())
					{
						sender.sendMessage(ChatColor.GOLD + "There are no muted players.");
					}
					else
					{
						sender.sendMessage(ChatColor.GOLD + "Muted players:");
						StringBuilder builder = new StringBuilder();
						for(String entry : data)
						{
							if(builder.length() > 0)
								builder.append(", ");
							
							String[] parts = entry.split(":");
							long time = Long.valueOf(parts[1]);
							time = time - System.currentTimeMillis();
							
							builder.append(parts[0]);
							builder.append('(');
							builder.append(Utilities.timeDiffToStringShort(time));
							builder.append(')');
						}
						
						sender.sendMessage(ChatColor.GRAY + builder.toString());
					}
				}

				@Override
				public void onError( String type, String message )
				{
					throw new RuntimeException(type + ":" + message);
				}
			});
			
			return true;
		}
		else
		{
			if(args.length < 1)
				return false;
			
			CommandSender player = BungeeChat.getPlayerManager().getPlayer(args[0]);
			
			if(!(player instanceof Player) && !(player instanceof RemotePlayer))
			{
				sender.sendMessage(ChatColor.RED + "Unknown player");
				return true;
			}
			
			String name = player.getName();
			if(player instanceof Player)
				name = ((Player)player).getDisplayName();
			
			if(command.getName().equals("mute"))
			{
				if(args.length != 2)
					return false;
				
				long time = Utilities.parseDateDiff(args[1]);
				if(time <= 0)
				{
					sender.sendMessage(ChatColor.RED + "Bad time format. Expected 5m, 2h or 30m2h");
					return true;
				}
				
				String timeString = Utilities.timeDiffToString(time);
				
				time = System.currentTimeMillis() + time;
				BungeeChat.getPlayerManager().setPlayerMuteTime(player, time);
				sender.sendMessage(ChatColor.AQUA + name + " has been muted for " + timeString);
				
				player.sendMessage(ChatColor.AQUA + "You have been muted for " + timeString);
				
				return true;
			}
			else if(command.getName().equals("unmute"))
			{
				if(args.length != 1)
					return false;
				
				BungeeChat.getPlayerManager().setPlayerMuteTime(player, 0);
				sender.sendMessage(ChatColor.AQUA + name + " has been unmuted");
				player.sendMessage(ChatColor.AQUA + "You can talk again.");
				return true;
			}
		}
		
		return false;
	}

	@EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
	private void onPlayerChat(AsyncPlayerChatEvent event)
	{
		PlayerSettings settings = BungeeChat.getPlayerManager().getPlayerSettings(event.getPlayer());
		
		if(settings.muteTime == -1 || System.currentTimeMillis() < settings.muteTime)
		{
			event.getPlayer().sendMessage(ChatColor.AQUA + "You are muted. You may not talk.");
			event.setCancelled(true);
		}
	}
}
