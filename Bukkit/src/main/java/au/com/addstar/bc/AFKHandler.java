/*
 * BungeeChat
 *
 * Copyright (c) 2015 - 2020.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy   of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is *
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR  IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
 * EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package au.com.addstar.bc;

/*-
 * #%L
 * BungeeChat-Bukkit
 * %%
 * Copyright (C) 2015 - 2020 AddstarMC
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.util.List;

import au.com.addstar.bc.objects.ChannelType;
import au.com.addstar.bc.objects.PlayerSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
import org.bukkit.event.player.PlayerJoinEvent;
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
				Utilities.getAudienceProvider().sender(sender)
					.sendMessage(Component.text("You do not have permission to change other players AFK state.")
						.color(NamedTextColor.RED));
				return true;
			}
			
			target = BungeeChat.getPlayerManager().getPlayer(args[0]);
			if(target == null)
			{
				Utilities.getAudienceProvider().sender(sender)
					.sendMessage(Component.text("Unknown player " + args[0])
						.color(NamedTextColor.RED));
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
			Utilities.getAudienceProvider().sender(sender)
				.sendMessage(Component.text("Toggled " + target.getName() + "'s AFK state")
					.color(NamedTextColor.GREEN));
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
	
	public void checkAFK(final CommandSender sender, CommandSender player, Component message)
	{
		if(player instanceof Player)
		{
			if(isAFK((Player)player))
				Utilities.getAudienceProvider().sender(sender).sendMessage(message);
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
						Utilities.getAudienceProvider().sender(sender).sendMessage(message);
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
		Component message;
		Component name = LegacyComponentSerializer.legacySection().deserialize(player.getDisplayName()).color(NamedTextColor.GRAY);
		TextComponent.Builder builder = Component.text().content("* ").color(NamedTextColor.GRAY).append(name);
		if(isAFK)
			builder.append(Component.text(" is now AFK."));
		else
			builder.append(Component.text(" is no longer AFK."));
		message = builder.build();
		BungeeChat.mirrorChat(message, ChannelType.Broadcast.getName());
		
		Utilities.localBroadCast(message, null,object -> true);
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
			
			if (!event.isSilent())
				onAFKChange(player, false);
		}
		else
			settings.lastActiveTime = System.currentTimeMillis();
	}
	
	private void updateActiveTime(Player player)
	{
		BungeeChat.getPlayerManager().getPlayerSettings(player).lastActiveTime = System.currentTimeMillis();
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	private void onPlayerLogin(PlayerJoinEvent event)
	{
		updateActiveTime(event.getPlayer());
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
			for(Player player : Bukkit.getServer().getOnlinePlayers())
			{
				if(player.hasPermission("bungeechat.afk.exempt"))
					continue;
				
				PlayerSettings settings = BungeeChat.getPlayerManager().getPlayerSettings(player);
				
				if(settings.isAFK && kickEnabled && !player.hasPermission("bungeechat.afk.kick.exempt"))
				{
					if(time - settings.afkStartTime >= kickTime * 60000)
					{
						BungeeChat.getSyncManager().callSyncMethod("bchat:kick", null, player.getUniqueId(), String.format(kickMessage, kickTime));

						Component consoleKickMessage = LegacyComponentSerializer.legacySection().deserialize(player.getDisplayName())
							.append(Component.text(" has been kicked for idling more than " + kickTime + " minutes"))
							.colorIfAbsent(NamedTextColor.GOLD);
						BungeeChat.mirrorChat(consoleKickMessage, ChannelType.AFKKick.getName());
						Utilities.localBroadCast(consoleKickMessage, "bungeechat.afk.kick.notify",object -> true);
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
	
							if (!event.isSilent())
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
