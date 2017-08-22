package au.com.addstar.bc.listeners;

import au.com.addstar.bc.BungeeChat;
import au.com.addstar.bc.objects.ChannelType;
import au.com.addstar.bc.objects.ChatChannel;
import au.com.addstar.bc.objects.Formatter;
import au.com.addstar.bc.PermissionSetting;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChatTabCompleteEvent;
import org.bukkit.permissions.Permissible;

import au.com.addstar.bc.event.ChatChannelEvent;
import au.com.addstar.bc.utils.Utilities;

public class ChatHandler implements Listener{

	private BungeeChat instance;

	public ChatHandler(BungeeChat instance) {
		this.instance = instance;
	}

	@EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
	private void onPlayerChatLowest(AsyncPlayerChatEvent event)
	{
        String channel = BungeeChat.getPlayerManager().getDefaultChatChannel(event.getPlayer());
        if(channel != null){
            if(instance.getChatChannelsManager().hasChannel(channel)){
                if(BungeeChat.forceGlobalprefix.equals
                        (event.getMessage().substring(0,1))){
                    event.setMessage(event.getMessage().substring(1));
                }else{
                    for (ChatChannel out : instance.getChatChannelsManager().getChannelObj().values()) {
                        if (out.name.equals(channel)) {
                            out.say(event.getPlayer(),event.getMessage());
                            event.setCancelled(true);
                            return;
                        }
                    }
                }
            }else{
                instance.getLogger().info("Channel Manager did not have the default channel...." + channel);
            }
        }
		PermissionSetting level = Formatter.getPermissionLevel(event.getPlayer());
		
		event.setFormat(Formatter.getChatFormatForUse(event.getPlayer(), level));
		event.setMessage(BungeeChat.colorize(event.getMessage(), event.getPlayer()));
		
		if(ChatColor.stripColor(event.getMessage()).trim().isEmpty())
			event.setCancelled(true);
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
		BungeeChat.mirrorChat(message, ChannelType.Default.getName());
		
		if(!Formatter.keywordsEnabled)
			return;
		
		String newMessage = Formatter.highlightKeywords(event.getMessage(), ChatColor.getLastColors(event.getFormat()));
		if(newMessage == null)
		{
			BungeeChat.mirrorChat(message, ChannelType.KeywordHighlight.getName());
		}
		else
		{
			newMessage = String.format(event.getFormat(), event.getPlayer().getDisplayName(), newMessage);
			Utilities.broadcast(newMessage, Formatter.keywordPerm, Utilities.NO_CONSOLE);
			BungeeChat.mirrorChat(newMessage, ChannelType.KeywordHighlight.getName());
		}
	}
	
	@EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
	private void onTabComplete(PlayerChatTabCompleteEvent event)
	{
		event.getTabCompletions().clear();
		event.getTabCompletions().addAll(BungeeChat.getPlayerManager().matchNames(event.getLastToken().toLowerCase()));
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	private void onChatChannel(ChatChannelEvent event)
	{
		switch(event.getChannelType())
		{
		case Default:
			Formatter.broadcastChat(event.getMessage());
			break;
		case KeywordHighlight:
			if(Formatter.keywordsEnabled)
				Bukkit.broadcast(event.getMessage(), Formatter.keywordPerm);
			break;
		case Broadcast:
			Utilities.broadcast(event.getMessage(), null, null);
			break;
		case AFKKick:
			Bukkit.broadcast(event.getMessage(), "bungeechat.afk.kick.notify");
			break;
		default:
			break;
		}
	}
}
