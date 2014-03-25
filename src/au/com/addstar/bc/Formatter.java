package au.com.addstar.bc;

import java.util.ArrayList;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Formatter
{
	static ArrayList<PermissionSetting> permissionLevels = new ArrayList<PermissionSetting>();
	private static String mDefaultFormat = "<{DISPLAYNAME}> {MESSAGE}"; 
	
	public static PermissionSetting getPermissionLevel(CommandSender sender)
	{
		PermissionSetting level = null;
		for(PermissionSetting setting : permissionLevels)
		{
			if(setting.permission == null || sender.hasPermission(setting.permission))
				level = setting;
		}
		
		return level;
	}
	
	public static String getChatFormat(PermissionSetting level)
	{
		if(level != null)
			return level.format;
		else
			return mDefaultFormat;
	}
	
	public static String getChatFormatForUse(Player player, PermissionSetting level)
	{
		return replaceKeywords(getChatFormat(level), player, level);
	}
	
	private static String getDisplayName(CommandSender sender, PermissionSetting level)
	{
		// TODO: Allow custom console name
		if(level == null)
			return "%1$s";
		else
			return level.color + "%1$s";
	}
	
	public static String replaceKeywords(String string, CommandSender sender, PermissionSetting level)
	{
		string = string.replace("{DISPLAYNAME}", getDisplayName(sender, level));
		string = string.replace("{MESSAGE}", "%2$s");

		string = string.replace("{SERVER}", BungeeChat.serverName);
		
		if(sender instanceof Player)
		{
			Player player = (Player)sender;
			String group = BungeeChat.getPrimaryGroup(player);
			string = string.replace("{GROUP}", (group != null ? group : "Default"));
			string = string.replace("{WORLD}", player.getWorld().getName());
		}
		else
		{
			string = string.replace("{GROUP}", "Server");
			string = string.replace("{WORLD}", "");
		}
		
		return string;
	}
}
