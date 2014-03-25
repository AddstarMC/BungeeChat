package au.com.addstar.bc;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatHandler implements Listener
{
	@EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
	private void onPlayerChatLowest(AsyncPlayerChatEvent event)
	{
		PermissionSetting level = Formatter.getPermissionLevel(event.getPlayer());
		
		event.setFormat(Formatter.getChatFormatForUse(event.getPlayer(), level));
		event.setMessage(BungeeChat.colorize(event.getMessage(), event.getPlayer()));
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	private void onPlayerChatFinal(AsyncPlayerChatEvent event)
	{
		BungeeChat.mirrorChat(event.getPlayer(), String.format(event.getFormat(), event.getPlayer().getDisplayName(), event.getMessage()), ""); 
	}
}
