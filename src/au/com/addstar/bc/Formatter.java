package au.com.addstar.bc;

import java.util.HashMap;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Formatter
{
	HashMap<String, String> mChatFormats = new HashMap<String, String>();
	String mDefaultFormat = "[{GROUP}] <{DISPLAYNAME}> {MESSAGE}"; 
	
	public String getChatFormat(Player player)
	{
		String group = BungeeChat.getPrimaryGroup(player);
		
		if(group == null)
			return mDefaultFormat;
		
		group = mChatFormats.get(group);
		if(group == null)
			return mDefaultFormat;
		
		return group;
	}
	
	public String getChatFormatForUse(Player player)
	{
		return replaceKeywords(getChatFormat(player), player);
	}
	
	public static String replaceKeywords(String string, CommandSender sender)
	{
		string = string.replace("{DISPLAYNAME}", "%1$s");
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
