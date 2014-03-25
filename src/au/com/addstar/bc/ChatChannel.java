package au.com.addstar.bc;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

public class ChatChannel
{
	public String name;
	public String format;
	public String command;
	public String permission;
	public String listenPermission;
	
	public ChatChannel(String name, String command, String format, String permission, String listenPerm)
	{
		this.name = name;
		this.command = command;
		this.format = format;
		if(!permission.isEmpty())
			this.permission = permission;

		if(!listenPerm.isEmpty())
			this.listenPermission = listenPerm;
	}
	
	public void registerChannel()
	{
		try
		{
			if(listenPermission != null)
				Bukkit.getPluginManager().addPermission(new Permission(listenPermission, PermissionDefault.OP));
		}
		catch(IllegalArgumentException e) {}
		
		try
		{
			if(permission != null)
				Bukkit.getPluginManager().addPermission(new Permission(permission, PermissionDefault.OP));
		}
		catch(IllegalArgumentException e) {}
	}
	
	public void unregisterChannel()
	{
		if(listenPermission != null)
			Bukkit.getPluginManager().removePermission(listenPermission);
		
		if(permission != null)
			Bukkit.getPluginManager().removePermission(permission);
	}
	
	public void say(CommandSender sender, String message)
	{
		message = BungeeChat.colorize(message, sender);
		String newFormat = Formatter.replaceKeywords(format, sender);
		String senderName;
		
		if(sender instanceof Player)
			senderName = ((Player)sender).getDisplayName();
		else
			senderName = sender.getName();
		
		String finalMessage = String.format(newFormat, senderName, message);
		
		if(listenPermission != null)
			Bukkit.broadcast(finalMessage, listenPermission);
		else
			Bukkit.broadcastMessage(finalMessage);
		BungeeChat.mirrorChat(finalMessage, name);
	}
}
