package au.com.addstar.bc.objects;

import au.com.addstar.bc.BungeeChat;
import au.com.addstar.bc.PermissionSetting;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

public class ChatChannel
{
	public String name;
	public String format;
	public String command;
	public String permission;
	public String listenPermission;
	public boolean subscribe;
	public boolean isRP;


	public ChatChannel(String name, String command, String format, String permission, String listenPerm)
	{
		this(name, command,format,permission,listenPerm,false,false);
	}
	public ChatChannel(String name, String command, String format, String permission, String listenPerm, Boolean subscribe, Boolean rp)
	{
		this.name = name;
		this.command = command;
		this.format = ChatColor.translateAlternateColorCodes('&', format);
		if(permission != null && !permission.isEmpty())
			this.permission = permission;
		if(permission != null && !listenPerm.isEmpty())
			this.listenPermission = listenPerm;
		this.subscribe = subscribe != null && subscribe;
        isRP = rp != null && rp;

	}
	
	public void registerChannel()
	{
		try
		{
			if(listenPermission != null)
				Bukkit.getPluginManager().addPermission(new Permission(listenPermission, PermissionDefault.OP));
		}
		catch(IllegalArgumentException ignored) {}
		
		try
		{
			if(permission != null)
				Bukkit.getPluginManager().addPermission(new Permission(permission, PermissionDefault.OP));
		}
		catch(IllegalArgumentException ignored) {}
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
		PermissionSetting level = Formatter.getPermissionLevel(sender);
		
		message = BungeeChat.colorize(message, sender);
		if (ChatColor.stripColor(message).trim().isEmpty())
			return;
		
		String newFormat = Formatter.replaceKeywordsPartial(format, sender, level);
		String finalMessage = String.format(newFormat, message);
		
		if(listenPermission != null)
			Bukkit.broadcast(finalMessage, listenPermission);
		else
			Bukkit.broadcastMessage(finalMessage);
		BungeeChat.mirrorChat(finalMessage, name);
	}
}
