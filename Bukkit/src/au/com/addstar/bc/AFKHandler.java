package au.com.addstar.bc;

import java.io.DataInput;
import java.io.IOException;
import java.util.List;
import java.util.Set;

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

import au.com.addstar.bc.utils.Utilities;

import com.google.common.collect.HashMultimap;

public class AFKHandler implements CommandExecutor, TabCompleter, Listener, IDataReceiver
{
	public int delay = 30;
	public int kickTime = 30;
	public boolean kickEnabled = false;
	public String kickMessage = "You have been kicked for idling %d minutes.";
	
	private HashMultimap<String, AFKCheck> mAFKChecks;
	
	public AFKHandler(Plugin plugin)
	{
		Bukkit.getScheduler().runTaskTimer(plugin, new AFKTimer(), 20, 20);
		Bukkit.getPluginManager().registerEvents(this, plugin);
		BungeeChat.getInstance().addListener(this);
		mAFKChecks = HashMultimap.create();
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
			settings.isAFK = !settings.isAFK;

			settings.lastActiveTime = System.currentTimeMillis();
			
			if(settings.isAFK)
				settings.afkStartTime = System.currentTimeMillis();
			else
				settings.afkStartTime = Long.MAX_VALUE;
			
			new MessageOutput("BungeeChat", "AFK")
				.writeUTF(target.getName())
				.writeBoolean(settings.isAFK)
				.send(BungeeChat.getInstance());
			
			onAFKChange((Player)target, settings.isAFK);
		}
		else
		{
			new MessageOutput("BungeeChat", "ToggleAFK")
			.writeUTF(target.getName())
			.send(BungeeChat.getInstance());
		}
		
		if(target != sender)
			sender.sendMessage(ChatColor.GREEN + "Toggled " + target.getName() + "'s AFK state");
		return true;
	}
	
	@Override
	public void onMessage( String channel, DataInput data ) throws IOException
	{
		if(channel.equals("IsAFK"))
		{
			String player = data.readUTF();
			boolean afk = data.readBoolean();
			
			Set<AFKCheck> checks = mAFKChecks.removeAll(player);
			if(checks != null && afk)
			{
				for(AFKCheck check : checks)
					check.run();
			}
		}
		else if(channel.equals("AFK"))
		{
			String player = data.readUTF();
			boolean afk = data.readBoolean();
			
			Player pplayer = Bukkit.getPlayerExact(player);
			if(pplayer != null)
			{
				PlayerSettings settings = BungeeChat.getPlayerManager().getPlayerSettings(pplayer);
				settings.isAFK = afk;

				settings.lastActiveTime = System.currentTimeMillis();
				
				if(settings.isAFK)
					settings.afkStartTime = System.currentTimeMillis();
				else
					settings.afkStartTime = Long.MAX_VALUE;
				
				onAFKChange(pplayer, settings.isAFK);
			}
		}
	}
	
	public void checkAFK(CommandSender sender, CommandSender player, String message)
	{
		if(player instanceof Player)
		{
			if(isAFK((Player)player))
				sender.sendMessage(message);
		}
		else
			scheduleAfkCheck(sender, player.getName(), message);
	}
	
	private void scheduleAfkCheck(CommandSender sender, String player, String message)
	{
		AFKCheck check = new AFKCheck(sender, message);
		mAFKChecks.put(player, check);
		
		new MessageOutput("BungeeChat", "IsAFK")
			.writeUTF(player)
			.send(BungeeChat.getInstance());
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
		PlayerSettings settings = BungeeChat.getPlayerManager().getPlayerSettings(player);
		settings.isAFK = false;
		settings.afkStartTime = Long.MAX_VALUE;
		settings.lastActiveTime = System.currentTimeMillis();
		
		new MessageOutput("BungeeChat", "AFK")
			.writeUTF(player.getName())
			.writeBoolean(false)
			.send(BungeeChat.getInstance());
		
		onAFKChange(player, false);
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
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=false)
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
						player.kickPlayer(String.format(kickMessage, kickTime));
						// TODO: Broadcast if needed
					}
				}
				else if(!settings.isAFK)
				{
					if(time - settings.lastActiveTime >= delay * 1000)
					{
						settings.isAFK = true;
						settings.afkStartTime = time;
						new MessageOutput("BungeeChat", "AFK")
							.writeUTF(player.getName())
							.writeBoolean(true)
							.send(BungeeChat.getInstance());
						
						onAFKChange(player, true);
					}
				}
				
			}
		}
	}
	
	private class AFKCheck
	{
		private CommandSender mSender;
		private String mMessage;
		
		public AFKCheck(CommandSender sender, String message)
		{
			mSender = sender;
			mMessage = message;
		}
		
		public void run()
		{
			mSender.sendMessage(mMessage);
		}
	}

}
