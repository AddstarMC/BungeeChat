package au.com.addstar.bc;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.permissions.Permissible;

public class ChatHandler implements Listener
{
	@EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
	private void onPlayerChatLowest(AsyncPlayerChatEvent event)
	{
		PermissionSetting level = Formatter.getPermissionLevel(event.getPlayer());
		
		event.setFormat(Formatter.getChatFormatForUse(event.getPlayer(), level));
		event.setMessage(BungeeChat.colorize(event.getMessage(), event.getPlayer()));
	}
	
	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
	private void onPlayerChatHighest(AsyncPlayerChatEvent event)
	{
		if(!Formatter.keywordsEnabled)
			return;
		
		String newMessage = Formatter.highlightKeywords(event.getMessage(), ChatColor.getLastColors(event.getFormat()));
		if(newMessage == null)
			return;
		
		for(Permissible permissible : Bukkit.getPluginManager().getPermissionSubscriptions(Formatter.keywordPerm))
		{
			if(!(permissible instanceof Player) && permissible.hasPermission(Formatter.keywordPerm))
				continue;
			
			event.getRecipients().remove(permissible);
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	private void onPlayerChatFinal(AsyncPlayerChatEvent event)
	{
		String message = String.format(event.getFormat(), event.getPlayer().getDisplayName(), event.getMessage());
		BungeeChat.mirrorChat(event.getPlayer(), message, "");
		
		if(!Formatter.keywordsEnabled)
			return;
		
		String newMessage = Formatter.highlightKeywords(event.getMessage(), ChatColor.getLastColors(event.getFormat()));
		if(newMessage == null)
		{
			BungeeChat.mirrorChat(event.getPlayer(), message, "~");
			return;
		}
		else
		{
			newMessage = String.format(event.getFormat(), event.getPlayer().getDisplayName(), newMessage);
			Formatter.broadcastNoConsole(newMessage, Formatter.keywordPerm);
			BungeeChat.mirrorChat(event.getPlayer(), newMessage, "~");
		}
	}
}
