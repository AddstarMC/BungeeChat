package au.com.addstar.bc;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatHandler implements Listener
{
	private Formatter mFormatter;
	
	public ChatHandler(Formatter formatter)
	{
		mFormatter = formatter;
	}
	
	@EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
	private void onPlayerChatLowest(AsyncPlayerChatEvent event)
	{
		event.setFormat(mFormatter.getChatFormatForUse(event.getPlayer()));
		event.setMessage(BungeeChat.colorize(event.getMessage(), event.getPlayer()));
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	private void onPlayerChatFinal(AsyncPlayerChatEvent event)
	{
		BungeeChat.mirrorChat(event.getPlayer(), String.format(event.getFormat(), event.getPlayer().getDisplayName(), event.getMessage()), ""); 
	}
}
