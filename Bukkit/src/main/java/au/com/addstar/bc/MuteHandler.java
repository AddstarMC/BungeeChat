package au.com.addstar.bc;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import au.com.addstar.bc.sync.IMethodCallback;
import au.com.addstar.bc.utils.Utilities;

public class MuteHandler implements CommandExecutor, TabCompleter
{
	public MuteHandler(Plugin plugin)
	{
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
							long time = Long.parseLong(parts[1]);
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
		else if(command.getName().equals("globalmute"))
		{
			if(args.length != 0 && args.length != 1)
				return false;
			
			long time = -1;

			if(args.length == 1)
			{
				time = Utilities.parseDateDiff(args[0]);
				if(time <= 0)
				{
					sender.sendMessage(ChatColor.RED + "Bad time format. Expected 5m, 2h or 30m2h");
					return true;
				}
				
				time = System.currentTimeMillis() + time;
				
				BungeeChat.getSyncManager().callSyncMethod("bchat:setGMute", null, time);
			}
			else
				BungeeChat.getSyncManager().callSyncMethod("bchat:toggleGMute", null);

			return true;
		}
		else
		{
			if(args.length < 1)
				return false;
			
			CommandSender player = BungeeChat.getPlayerManager().getPlayer(args[0]);
			InetAddress address = null;
			
			if(!(player instanceof Player) && !(player instanceof RemotePlayer))
			{
				if (command.getName().equals("ipmute") || command.getName().equals("ipunmute"))
				{
					try
					{
						address = InetAddress.getByName(args[0]);
					}
					catch ( UnknownHostException e )
					{
						sender.sendMessage(ChatColor.RED + "Unknown player or ip address");
						return true;
					}
				}
				else
				{
					sender.sendMessage(ChatColor.RED + "Unknown player");
					return true;
				}
			}
			
			String name = null;
			if (player != null)
			{
				if(player instanceof Player)
					name = ((Player)player).getDisplayName();
				else
					name = player.getName();
			}
			else
				name = address.getHostAddress();
			
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
				String message = ChatColor.AQUA + name + " has been muted for " + timeString;
				BungeeChat.mirrorChat(message, ChannelType.Broadcast.getName());
				Bukkit.broadcastMessage(message);
				
				player.sendMessage(ChatColor.AQUA + "You have been muted for " + timeString);
				
				return true;
			}
			else if(command.getName().equals("unmute"))
			{
				if(args.length != 1)
					return false;
				
				BungeeChat.getPlayerManager().setPlayerMuteTime(player, 0);
				sender.sendMessage(ChatColor.AQUA + name + " has been unmuted");
				player.sendMessage(ChatColor.AQUA + "You are no longer muted. You may talk again.");
				return true;
			}
			else if(command.getName().equals("ipmute"))
			{
				if(args.length != 2)
					return false;
				
				long time = Utilities.parseDateDiff(args[1]);
				if(time <= 0)
				{
					sender.sendMessage(ChatColor.RED + "Bad time format. Expected 5m, 2h or 30m2h");
					return true;
				}
				
				if (player != null)
					BungeeChat.getSyncManager().callSyncMethod("bchat:setMuteIP", null, PlayerManager.getUniqueId(player), time);
				else
					BungeeChat.getSyncManager().callSyncMethod("bchat:setMuteIP", null, address.getHostAddress(), time);
				return true;
			}
			else if(command.getName().equals("ipunmute"))
			{
				if(args.length != 1)
					return false;
				
				if (player != null)
					BungeeChat.getSyncManager().callSyncMethod("bchat:setMuteIP", null, PlayerManager.getUniqueId(player), 0L);
				else
					BungeeChat.getSyncManager().callSyncMethod("bchat:setMuteIP", null, address.getHostAddress(), 0L);
				sender.sendMessage(ChatColor.AQUA + name + " has been unmuted");
				return true;
			}
		}
		
		return false;
	}
}
