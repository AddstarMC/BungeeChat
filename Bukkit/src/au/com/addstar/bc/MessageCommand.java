package au.com.addstar.bc;

import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import au.com.addstar.bc.sync.IMethodCallback;

public class MessageCommand implements CommandExecutor, TabCompleter
{
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

	private void doSendMessage(CommandSender to, CommandSender from, String message)
	{
		String fullMessageOut = String.format(Formatter.getPMFormat(to, false), message);
		String fullMessageIn = String.format(Formatter.getPMFormat(from, true), message);
		
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
		
		BungeeChat.getAFKHandler().checkAFK(from, to, ChatColor.GRAY + "This player is AFK. They may not see this message.");
	}
	
	@Override
	public boolean onCommand( final CommandSender sender, Command command, String label, String[] args )
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
			
			final CommandSender player = BungeeChat.getPlayerManager().getPlayer(args[0]);
			if(player == null)
			{
				sender.sendMessage(ChatColor.RED + "Cannot find player " + args[0]);
				return true;
			}
			
			final String message = BungeeChat.colorize(concat(args, 1), sender);
			
			if(ChatColor.stripColor(message).trim().isEmpty())
				return false;
			
			if((sender instanceof Player) && !sender.hasPermission("bungeechat.message.override"))
			{
				if(player instanceof RemotePlayer)
				{
					BungeeChat.getSyncManager().callSyncMethod("bchat:canMsg", new IMethodCallback<Byte>()
					{
						@Override
						public void onError( String type, String message )
						{
							throw new RuntimeException(type + ": " + message);
						}
						
						@Override
						public void onFinished( Byte data )
						{
							if(data == 0)
								sender.sendMessage(ChatColor.RED + "That player has messaging disabled.");
							else
								doSendMessage(player, sender, message);
						}
						
					}, player.getName());
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
}
