package au.com.addstar.bc.commands;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import au.com.addstar.bc.BungeeChat;
import au.com.addstar.bc.objects.ChannelType;
import au.com.addstar.bc.PlayerManager;
import au.com.addstar.bc.objects.RemotePlayer;
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
		switch (command.getName()) {
			case "mutelist":
				if (args.length != 0)
					return false;
				BungeeChat.getSyncManager().callSyncMethod("bchat:getMuteList", new IMethodCallback<List<String>>() {
					@Override
					public void onFinished(List<String> data) {
						if (data.isEmpty()) {
							sender.sendMessage(ChatColor.GOLD + "There are no muted players.");
						} else {
							sender.sendMessage(ChatColor.GOLD + "Muted players:");
							StringBuilder builder = new StringBuilder();
							for (String entry : data) {
								if (builder.length() > 0)
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
					public void onError(String type, String message) {
						throw new RuntimeException(type + ":" + message);
					}
				});

				break;
			case "globalmute":
				if (args.length != 0 && args.length != 1)
					return false;

				long time;

				if (args.length == 1) {
					time = Utilities.parseDateDiff(args[0]);
					if (time <= 0) {
						sender.sendMessage(ChatColor.RED + "Bad time format. Expected 5m, 2h or 30m2h");
						return true;
					}

					time = System.currentTimeMillis() + time;

					BungeeChat.getSyncManager().callSyncMethod("bchat:setGMute", null, time);
				} else
					BungeeChat.getSyncManager().callSyncMethod("bchat:toggleGMute", null);

				break;
			default:
				if (args.length < 1)
					return false;

				CommandSender target = BungeeChat.getPlayerManager().getPlayer(args[0]);
				InetAddress address = null;

				if (!(target instanceof Player) && !(target instanceof RemotePlayer)) {
					if (command.getName().equals("ipmute") || command.getName().equals("ipunmute")) {
						try {
							address = InetAddress.getByName(args[0]);
						} catch (UnknownHostException e) {
							sender.sendMessage(ChatColor.RED + "Unknown player or ip address");
							return true;
						}
					} else {
						sender.sendMessage(ChatColor.RED + "Unknown player");
						return true;
					}
				}

				String name;
				if (target != null) {
					name = BungeeChat.getPlayerManager().getPlayerNickname(target);
					if(name == null || name.length() < 1) {
						if (target instanceof Player ) {
							name = ((Player) target).getDisplayName();
						}else if(target instanceof RemotePlayer){
							name = ((RemotePlayer)target).getDisplayName();
						}
					}
				} else{
					name= address.getHostAddress();
				}
				switch (command.getName()){

				case "mute":
					if (args.length != 2)
						return false;

					long mutetime = Utilities.parseDateDiff(args[1]);
					if (mutetime <= 0) {
						sender.sendMessage(ChatColor.RED + "Bad time format. Expected 5m, 2h or 30m2h");
						return true;
					}

					String timeString = Utilities.timeDiffToString(mutetime);

					mutetime = System.currentTimeMillis() + mutetime;
					BungeeChat.getPlayerManager().setPlayerMuteTime(target, mutetime);
					String message = ChatColor.AQUA + name + " has been muted for " + timeString;
					BungeeChat.mirrorChat(message, ChannelType.Broadcast.getName());
					Bukkit.broadcastMessage(message);
					if(target != null) {
						target.sendMessage(ChatColor.AQUA + "You have been muted for " + timeString);
					}

					break;
				case "unmute":
					if (args.length != 1)
						return false;

					BungeeChat.getPlayerManager().setPlayerMuteTime(target, 0);
					sender.sendMessage(ChatColor.AQUA + name + " has been unmuted");
					if(target !=null) {
						target.sendMessage(ChatColor.AQUA + "You are no longer muted. You may talk again.");
					}
					break;
				case "ipmute":
					if (args.length != 2)
						return false;

					long ipmutetime = Utilities.parseDateDiff(args[1]);
					if (ipmutetime <= 0) {
						sender.sendMessage(ChatColor.RED + "Bad time format. Expected 5m, 2h or 30m2h");
						return true;
					}

					if (target != null)
						BungeeChat.getSyncManager().callSyncMethod("bchat:setMuteIP", null,
								PlayerManager.getUniqueId(target), ipmutetime);
					else
						BungeeChat.getSyncManager().callSyncMethod("bchat:setMuteIP", null,
								address.getHostAddress(), ipmutetime);
					break;
				case "ipunmute":
					if (args.length != 1)
						return false;

					if (target != null)
						BungeeChat.getSyncManager().callSyncMethod("bchat:setMuteIP", null,
								PlayerManager.getUniqueId(target), 0L);
					else
						BungeeChat.getSyncManager().callSyncMethod("bchat:setMuteIP", null,
								address.getHostAddress(), 0L);
					sender.sendMessage(ChatColor.AQUA + name + " has been unmuted");
					break;
				default:
					return false;
			}
			return true;
		}
		return true;
	}
}
