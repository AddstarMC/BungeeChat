package au.com.addstar.bc;

import java.util.HashMap;

import org.bukkit.entity.Player;

public class Formatter
{
	private HashMap<String, String> mChatFormats = new HashMap<String, String>();
	private String mDefaultFormat = "[{GROUP}] <{DISPLAYNAME}> {MESSAGE}"; 
	
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
		String format = getChatFormat(player);
		
		format = format.replace("{DISPLAYNAME}", "%1$s");
		format = format.replace("{MESSAGE}", "%2$s");
		
		String group = BungeeChat.getPrimaryGroup(player);
		format = format.replace("{GROUP}", (group != null ? group : "Default"));
		format = format.replace("{SERVER}", BungeeChat.serverName);
		format = format.replace("{WORLD}", player.getWorld().getName());
		
		return format;
	}
}
