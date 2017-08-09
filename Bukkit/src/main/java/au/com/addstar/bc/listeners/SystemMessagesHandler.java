package au.com.addstar.bc.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public class SystemMessagesHandler implements Listener
{
	public SystemMessagesHandler(Plugin plugin)
	{
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	private void onPlayerJoin(PlayerJoinEvent event)
	{
		event.setJoinMessage(null);
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	private void onPlayerQuit(PlayerQuitEvent event)
	{
		event.setQuitMessage(null);
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	private void onPlayerKicked(PlayerKickEvent event)
	{
		event.setLeaveMessage(null);
	}
	
	@EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
	private void onPlayerDeath(PlayerDeathEvent event)
	{
		String message = event.getDeathMessage();
		if(message == null)
			return;

		message = message.replace(event.getEntity().getName(), ChatColor.stripColor(event.getEntity().getDisplayName()));
		
		if(event.getEntity().getKiller() != null)
		{
			Player killer = event.getEntity().getKiller();
			message = message.replace(killer.getName(), ChatColor.stripColor(killer.getDisplayName()));
		}
		
		event.setDeathMessage(message);
	}
}
