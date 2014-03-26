package au.com.addstar.bc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;

public class Formatter
{
	static ArrayList<PermissionSetting> permissionLevels = new ArrayList<PermissionSetting>();
	static String consoleOverride = null;
	
	private static String mDefaultFormat = "<{DISPLAYNAME}> {MESSAGE}";
	
	static boolean keywordsEnabled;
	static ArrayList<String> keywordEnabledChannels = new ArrayList<String>();
	static String keywordPerm;
	static HashMap<Pattern, String> keywordPatterns = new HashMap<Pattern, String>();
	
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
		String displayName = "%1$s"; 
		
		if(consoleOverride != null && sender == Bukkit.getConsoleSender())
			displayName = consoleOverride;
		
		if(level == null)
			return displayName;
		else
			return level.color + displayName;
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
	
	public static String highlightKeywords(String message, String defaultColour)
	{
		if(defaultColour.isEmpty())
			defaultColour = ChatColor.RESET.toString();
		
		boolean matched = false;
		for(Entry<Pattern, String> entry : keywordPatterns.entrySet())
		{
			Matcher m = entry.getKey().matcher(message);
			String modified = message;
			
			int offset = 0;
			
			while(m.find())
			{
				String currentColour = ChatColor.getLastColors(message.substring(0, m.end()));
				if(currentColour.isEmpty())
					currentColour = defaultColour;
				
				modified = modified.substring(0,m.start() + offset) + entry.getValue() + m.group(0) + currentColour + modified.substring(m.end() + offset);
				offset += entry.getValue().length() + currentColour.length();
				matched = true;
			}
			
			message = modified;
		}
		
		if(matched)
			return message;
		
		return null;
	}
	
	public static void broadcastChat(String message)
	{
		if(!keywordsEnabled)
			Bukkit.broadcastMessage(message);
		else
		{
			// Broadcast to everyone that has the normal perm, but not the highlight perm
			for(Permissible permissible : Bukkit.getPluginManager().getPermissionSubscriptions(Server.BROADCAST_CHANNEL_USERS))
			{
				if(permissible instanceof CommandSender && permissible.hasPermission(Server.BROADCAST_CHANNEL_USERS) && !permissible.hasPermission(keywordPerm))
					((CommandSender)permissible).sendMessage(message);
			}
		}
	}
	
	public static void broadcastNoConsole(String message, String perm)
	{
		for(Permissible permissible : Bukkit.getPluginManager().getPermissionSubscriptions(perm))
		{
			if(permissible instanceof CommandSender && permissible.hasPermission(perm) && !(permissible instanceof ConsoleCommandSender))
				((CommandSender)permissible).sendMessage(message);
		}
	}
}
