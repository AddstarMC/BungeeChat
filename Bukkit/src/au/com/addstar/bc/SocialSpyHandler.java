package au.com.addstar.bc;

import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;

import au.com.addstar.bc.utils.Utilities;

public class SocialSpyHandler implements Listener, CommandExecutor
{
	private HashSet<String> mKeywords = new HashSet<String>();
	
	public SocialSpyHandler(Plugin plugin)
	{
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	private void onChatChannel(ChatChannelEvent event)
	{
		if(event.getChannelType() == ChannelType.SocialSpy)
		{
			Utilities.broadcast(event.getMessage(), "bungeechat.socialspy", Utilities.SOCIAL_SPY_ENABLED);
		}
	}
	
	@EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
	private void onPlayerCommand(PlayerCommandPreprocessEvent event)
	{
		String command = event.getMessage().split(" ")[0].substring(1);
		
		if(mKeywords.contains(command.toLowerCase()))
		{
			String message = event.getPlayer().getName() + ": " + event.getMessage();
			Utilities.broadcast(message, "bungeechat.socialspy", event.getPlayer(), Utilities.SOCIAL_SPY_ENABLED);
			BungeeChat.mirrorChat(message, ChannelType.SocialSpy.getName());
		}
	}
	
	public void clearKeywords()
	{
		mKeywords.clear();
	}
	
	public void addKeyword(String keyword)
	{
		mKeywords.add(keyword);
	}

	public void setStatus(CommandSender player, boolean on)
	{
		BungeeChat.getPlayerSettings(player).socialSpyState = (on ? 1 : 0);
		BungeeChat.updatePlayerSettings(player);
	}
	
	public void clearStatus(CommandSender player)
	{
		BungeeChat.getPlayerSettings(player).socialSpyState = 2;
		BungeeChat.updatePlayerSettings(player);
	}
	
	public boolean isEnabled(CommandSender player)
	{
		int state = BungeeChat.getPlayerSettings(player).socialSpyState;
		
		if(state == 2)
			return player.hasPermission("bungeechat.socialspy");
		
		return (state == 1);
	}
	
	@Override
	public boolean onCommand( CommandSender sender, Command command, String label, String[] args )
	{
		if(!(sender instanceof Player))
			return false;
		
		boolean on = isEnabled(sender);
		on = !on;
		
		if(on)
			sender.sendMessage(ChatColor.GREEN + "SocialSpy now on");
		else
			sender.sendMessage(ChatColor.GREEN + "SocialSpy now off");
		
		setStatus(sender, on);
		
		return true;
	}
}
