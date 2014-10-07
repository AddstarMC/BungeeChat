package au.com.addstar.bc;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;

import au.com.addstar.bc.event.AFKChangeEvent;
import au.com.addstar.bc.sync.IMethodCallback;
import au.com.addstar.bc.sync.IPacketHandler;
import au.com.addstar.bc.sync.Packet;
import au.com.addstar.bc.sync.SyncConfig;
import au.com.addstar.bc.sync.packet.AFKPacket;
import au.com.addstar.bc.utils.Utilities;

public class AFKHandler implements CommandExecutor, TabCompleter, Listener, IPacketHandler
{
	public int delay = 30;
	public int kickTime = 30;
	public boolean kickEnabled = false;
	public String kickMessage = "You have been kicked for idling more than %d minutes.";
	
	@SuppressWarnings( "unchecked" )
	public AFKHandler(Plugin plugin)
	{
		Bukkit.getScheduler().runTaskTimer(plugin, new AFKTimer(), 20, 20);
		Bukkit.getPluginManager().registerEvents(this, plugin);
		BungeeChat.getPacketManager().addHandler(this, AFKPacket.class);
	}
	
	@Override
	public List<String> onTabComplete( CommandSender sender, Command command, String label, String[] args )
	{
		if(args.length == 1 && sender.hasPermission("bungeechat.afk.others"))
			return BungeeChat.getPlayerManager().matchNames(args[0]);
		return null;
	}

	@Override
	public boolean onCommand( CommandSender sender, Command command, String label, String[] args )
	{
		if(args.length != 0 && args.length != 1)
			return false;
		
		if(args.length == 0 && !(sender instanceof Player))
			return false;
		
		CommandSender target = sender;
		
		if(args.length == 1)
		{
			if(!sender.hasPermission("bungeechat.afk.others"))
			{
				sender.sendMessage(ChatColor.RED + "You do not have permission to change other players AFK state.");
				return true;
			}
			
			target = BungeeChat.getPlayerManager().getPlayer(args[0]);
			if(target == null)
			{
				sender.sendMessage(ChatColor.RED + "Unknown player " + args[0]);
				return true;
			}
		}
		
		if(target instanceof Player)
		{
			PlayerSettings settings = BungeeChat.getPlayerManager().getPlayerSettings(target);
			
			// Prevent re-enabling afk when you leave afk with /afk
			if(!settings.isAFK && System.currentTimeMillis() - settings.afkStartTime < 100)
				return true;
			
			settings.isAFK = !settings.isAFK;

			settings.lastActiveTime = System.currentTimeMillis();
			
			if(settings.isAFK)
				settings.afkStartTime = System.currentTimeMillis();
			else
				settings.afkStartTime = Long.MAX_VALUE;
			
			BungeeChat.getSyncManager().callSyncMethod("bchat:setAFK", null, PlayerManager.getUniqueId(target), settings.isAFK);
			onAFKChange((Player)target, settings.isAFK);
		}
		else
			BungeeChat.getSyncManager().callSyncMethod("bchat:toggleAFK", null, PlayerManager.getUniqueId(target));
		
		if(target != sender)
			sender.sendMessage(ChatColor.GREEN + "Toggled " + target.getName() + "'s AFK state");
		return true;
	}
	
	@Override
	public void handle( Packet raw )
	{
		AFKPacket packet = (AFKPacket)raw;
		Player player = Bukkit.getPlayer(packet.getID());
		if(player != null)
		{
			PlayerSettings settings = BungeeChat.getPlayerManager().getPlayerSettings(player);
			settings.isAFK = packet.getAFK();

			settings.lastActiveTime = System.currentTimeMillis();
			
			if(settings.isAFK)
				settings.afkStartTime = System.currentTimeMillis();
			else
				settings.afkStartTime = Long.MAX_VALUE;
			
			onAFKChange(player, settings.isAFK);
		}
	}
	
	public void checkAFK(final CommandSender sender, CommandSender player, final String message)
	{
		if(player instanceof Player)
		{
			if(isAFK((Player)player))
				sender.sendMessage(message);
		}
		else
		{
			BungeeChat.getSyncManager().callSyncMethod("bchat:isAFK", new IMethodCallback<Boolean>()
			{
				@Override
				public void onError( String type, String message )
				{
					throw new RuntimeException(type + ": " + message);
				}
				
				@Override
				public void onFinished( Boolean data )
				{
					if(data)
						sender.sendMessage(message);
				}
				
			}, PlayerManager.getUniqueId(player));
		}
	}
	
	public void load(SyncConfig config)
	{
		delay = config.getInt("afk-delay", 30);
		kickEnabled = config.getBoolean("afk-kick-enabled", false);
		kickTime = config.getInt("afk-kick-delay", 30);
		kickMessage = config.getString("afk-kick-message", "You have been kicked for idling for %d minutes");
	}
	
	private void onAFKChange(Player player, boolean isAFK)
	{
		String message = null;
		if(isAFK)
			message = ChatColor.GRAY + "* " + ChatColor.stripColor(player.getDisplayName()) + " is now AFK.";
		else
			message = ChatColor.GRAY + "* " + ChatColor.stripColor(player.getDisplayName()) + " is no longer AFK.";
		
		BungeeChat.mirrorChat(message, ChannelType.Broadcast.getName());
		
		Utilities.broadcast(message, null, null);
	}
	
	private boolean isAFK(Player player)
	{
		return BungeeChat.getPlayerManager().getPlayerSettings(player).isAFK;
	}
	
	private void cancelAFK(Player player)
	{
		AFKChangeEvent event = new AFKChangeEvent(player, true);
		Bukkit.getPluginManager().callEvent(event);
		PlayerSettings settings = BungeeChat.getPlayerManager().getPlayerSettings(player);
		
		if(!event.isCancelled())
		{
			settings.isAFK = false;
			settings.afkStartTime = System.currentTimeMillis();
			settings.lastActiveTime = System.currentTimeMillis();
			
			BungeeChat.getSyncManager().callSyncMethod("bchat:setAFK", null, PlayerManager.getUniqueId(player), false);
			
			onAFKChange(player, false);
		}
		else
			settings.lastActiveTime = System.currentTimeMillis();
	}
	
	private void updateActiveTime(Player player)
	{
		BungeeChat.getPlayerManager().getPlayerSettings(player).lastActiveTime = System.currentTimeMillis();
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	private void onPlayerMove(PlayerMoveEvent event)
	{
		if(!isAFK(event.getPlayer()))
		{
			updateActiveTime(event.getPlayer());
			return;
		}
		
		if(event.getTo().distanceSquared(event.getFrom()) > 0.0001f)
		{
			cancelAFK(event.getPlayer());
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	private void onPlayerClick(PlayerInteractEvent event)
	{
		if(!isAFK(event.getPlayer()))
		{
			updateActiveTime(event.getPlayer());
			return;
		}
		
		cancelAFK(event.getPlayer());
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	private void onPlayerClick(PlayerInteractEntityEvent event)
	{
		if(!isAFK(event.getPlayer()))
		{
			updateActiveTime(event.getPlayer());
			return;
		}
		
		cancelAFK(event.getPlayer());
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	private void onPlayerChat(PlayerCommandPreprocessEvent event)
	{
		if(!isAFK(event.getPlayer()))
		{
			updateActiveTime(event.getPlayer());
			return;
		}
		
		cancelAFK(event.getPlayer());
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
	private void onPlayerChat(AsyncPlayerChatEvent event)
	{
		if(!isAFK(event.getPlayer()))
		{
			updateActiveTime(event.getPlayer());
			return;
		}
		
		cancelAFK(event.getPlayer());
	}
	
	private class AFKTimer implements Runnable
	{
		@Override
		public void run()
		{
			long time = System.currentTimeMillis();
			for(Player player : Bukkit.getOnlinePlayers())
			{
				if(player.hasPermission("bungeechat.afk.exempt"))
					continue;
				
				PlayerSettings settings = BungeeChat.getPlayerManager().getPlayerSettings(player);
				
				if(settings.isAFK && kickEnabled && !player.hasPermission("bungeechat.afk.kick.exempt"))
				{
					if(time - settings.afkStartTime >= kickTime * 60000)
					{
						BungeeChat.getSyncManager().callSyncMethod("bchat:kick", null, player.getUniqueId(), String.format(kickMessage, kickTime));
						String consoleKickMessage = ChatColor.AQUA + player.getDisplayName() + " has been kicked for AFKing more than " + kickTime + " minutes";
						BungeeChat.mirrorChat(consoleKickMessage, ChannelType.AFKKick.getName());
						Bukkit.broadcast(consoleKickMessage, "bungeechat.afk.kick.notify");
					}
				}
				else if(!settings.isAFK)
				{
					if(time - settings.lastActiveTime >= delay * 1000)
					{
						AFKChangeEvent event = new AFKChangeEvent(player, true);
						Bukkit.getPluginManager().callEvent(event);
						if(!event.isCancelled())
						{
							settings.isAFK = true;
							settings.afkStartTime = time;
							BungeeChat.getSyncManager().callSyncMethod("bchat:setAFK", null, player.getUniqueId(), true);
	
							onAFKChange(player, true);
						}
						else
							settings.lastActiveTime = time;
					}
				}
				
			}
		}
	}
}
