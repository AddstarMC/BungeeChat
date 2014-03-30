package au.com.addstar.bc.utils;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permissible;

public class Utilities
{
	public static final NoConsoleChecker NO_CONSOLE = new NoConsoleChecker();
	public static final SocialSpyChecker SOCIAL_SPY_ENABLED = new SocialSpyChecker();
	
	public static void broadcast(String message, String permission, ValidChecker<CommandSender> checker)
	{
		for(Permissible perm : Bukkit.getPluginManager().getPermissionSubscriptions(permission))
		{
			if(perm instanceof CommandSender && perm.hasPermission(permission) && checker.isValid((CommandSender)perm))
				((CommandSender)perm).sendMessage(message);
		}
	}
	
	public static void broadcast(String message, String permission, CommandSender except, ValidChecker<CommandSender> checker)
	{
		for(Permissible perm : Bukkit.getPluginManager().getPermissionSubscriptions(permission))
		{
			if(perm == except)
				continue;
			
			if(perm instanceof CommandSender && perm.hasPermission(permission) && checker.isValid((CommandSender)perm))
				((CommandSender)perm).sendMessage(message);
		}
	}
}
