package au.com.addstar.bc.objects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import au.com.addstar.bc.BungeeChat;
import au.com.addstar.bc.PermissionSetting;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;

import au.com.addstar.bc.config.KeywordHighlighterConfig;
import au.com.addstar.bc.config.PermissionSettingConfig;
import au.com.addstar.bc.sync.SyncConfig;
import au.com.addstar.bc.utils.NoPermissionChecker;
import au.com.addstar.bc.utils.Utilities;

public class Formatter
{
	public static ArrayList<PermissionSetting> permissionLevels = new ArrayList<>();
	public static String consoleOverride = null;
	
	private static String mDefaultFormat = "<{DISPLAYNAME}> {MESSAGE}";
	private static String mRpDefaultFormat = "<{DISPLAYNAME}>({CHATNAME}) {MESSAGE}";
	public static boolean keywordsEnabled;
	public static ArrayList<String> keywordEnabledChannels = new ArrayList<>();
	public static String keywordPerm;
	public static HashMap<Pattern, String> keywordPatterns = new HashMap<>();
	
	public static String mPMFormatInbound = "[{DISPLAYNAME} -> Me]: {MESSAGE}";
	public static String mPMFormatOutbound = "[Me -> {DISPLAYNAME}]: {MESSAGE}";
	
	public static void load(SyncConfig config)
	{
		if(!config.getString("consolename", "").isEmpty())
			consoleOverride = ChatColor.translateAlternateColorCodes('&', config.getString("consolename", ""));
		else
			consoleOverride = null;
		
		mPMFormatInbound = ChatColor.translateAlternateColorCodes('&', config.getString("pm-format-in", "[{DISPLAYNAME} -> Me]: {MESSAGE}"));
		mPMFormatOutbound = ChatColor.translateAlternateColorCodes('&', config.getString("pm-format-out", "[Me -> {DISPLAYNAME}]: {MESSAGE}"));
		
		permissionLevels.clear();
		
		SyncConfig permLevels = config.getSection("perms");
		for(String key : permLevels.getKeys())
		{
			PermissionSettingConfig setting = (PermissionSettingConfig) permLevels.get(key, null);
			permissionLevels.add(setting.convert());
		}
		
		Collections.sort(Formatter.permissionLevels);
		
		KeywordHighlighterConfig kh = (KeywordHighlighterConfig)config.get("highlight", null);
		
		keywordsEnabled = kh.enabled;
		if(kh.enabled)
		{
			keywordPerm = kh.permission;
			keywordEnabledChannels.clear();
			keywordPatterns.clear();
			
			SyncConfig keywords = config.getSection("keywords");
			for(String key : keywords.getKeys())
			{
				try
				{
					Pattern pattern = Pattern.compile(key, Pattern.CASE_INSENSITIVE);
					Formatter.keywordPatterns.put(pattern, keywords.getString(key, null));
				}
				catch (PatternSyntaxException e)
				{
					// Cant happen
				}
			}
			
			try
			{
				Bukkit.getPluginManager().addPermission(new org.bukkit.permissions.Permission(keywordPerm, PermissionDefault.OP));
			}
			catch(IllegalArgumentException e)
			{
			}
		}
	}
	
	public static PermissionSetting getPermissionLevel(CommandSender sender)
	{
		if((sender instanceof ConsoleCommandSender) && !permissionLevels.isEmpty())
			return permissionLevels.get(permissionLevels.size()-1);
		
		if (!(sender instanceof Player))
			return null;
		
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
	
	private static String getFmtDisplayName(CommandSender sender, PermissionSetting level)
	{
		String displayName = "%1$s"; 
		
		if(consoleOverride != null && sender == Bukkit.getConsoleSender())
			displayName = consoleOverride;
		
		if(level == null)
			return displayName;
		else
			return level.color + displayName;
	}
	private static String getChatName(CommandSender sender){
		String chatName;
		if(sender instanceof Player) {
			chatName = BungeeChat.getPlayerManager().getPlayerChatName(sender);
		}else if (sender instanceof RemotePlayer){
			chatName = BungeeChat.getPlayerManager().getPlayerChatName(sender);
		}else{
			return null;
		}
		return chatName;
	}

	
	public static String getDisplayName(CommandSender sender, PermissionSetting level)
	{
		String displayName = sender.getName();
		
		if(sender instanceof Player)
			displayName = ((Player)sender).getDisplayName();
		else if(sender instanceof RemotePlayer)
			displayName = ((RemotePlayer)sender).getDisplayName();
		
		if(consoleOverride != null && sender == Bukkit.getConsoleSender())
			displayName = consoleOverride;
		
		if(level == null)
			return displayName;
		else
			return level.color + displayName;
	}
	
	public static String replaceKeywords(String string, CommandSender sender, PermissionSetting level)
	{
		string = string.replace("{DISPLAYNAME}", getFmtDisplayName(sender, level));
		string = string.replace("{RAWDISPLAYNAME}", ChatColor.stripColor(getDisplayName(sender, level)));
		string = string.replace("{NAME}", sender.getName());
		string = string.replace("{MESSAGE}", "%2$s");
		string = string.replace("{SERVER}", BungeeChat.serverName);
		
		if(sender instanceof Player)
		{
			Player player = (Player)sender;
			String prefix = getChatName(sender);
			if(prefix == null){
				prefix = "";
			}
			string = string.replace("{CHATNAME}", prefix );
			String group = BungeeChat.getPrimaryGroup(player);
			string = string.replace("{GROUP}", (group != null ? group : "Default"));
			string = string.replace("{WORLD}", player.getWorld().getName());
		}
		else
		{
			string = string.replace("{CHATNAME}", "" );
			string = string.replace("{GROUP}", "Server");
			string = string.replace("{WORLD}", "");
		}
		
		return string;
	}
	
	public static String replaceKeywordsPartial(String string, CommandSender sender, PermissionSetting level)
	{
		string = string.replace("{DISPLAYNAME}", getDisplayName(sender, level));
		string = string.replace("{RAWDISPLAYNAME}", ChatColor.stripColor(getDisplayName(sender, level)));
		string = string.replace("{NAME}", sender.getName());
		string = string.replace("{MESSAGE}", "%1$s");
		string = string.replace("{SERVER}", BungeeChat.serverName);
		
		if(sender instanceof Player)
		{
			Player player = (Player)sender;
			String prefix = getChatName(player);
			if(prefix == null){
				prefix = "";
			}
			string = string.replace("{CHATNAME}", prefix );
			String group = BungeeChat.getPrimaryGroup(player);
			string = string.replace("{GROUP}", (group != null ? group : "Default"));
			string = string.replace("{WORLD}", player.getWorld().getName());
		}
		else
		{
			string = string.replace("{CHATNAME}", "" );
			string = string.replace("{GROUP}", "Server");
			string = string.replace("{WORLD}", "");
		}
		
		return string;
	}
	
	public static String getPMFormat(CommandSender to, boolean inbound)
	{
		PermissionSetting level = getPermissionLevel(to);
		
		if(inbound)
			return replaceKeywordsPartial(mPMFormatInbound, to, level); 
		else
			return replaceKeywordsPartial(mPMFormatOutbound, to, level);
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
			Utilities.broadcast(message, null, null);
		else
			Utilities.broadcast(message, null, new NoPermissionChecker(keywordPerm));
	}
}
