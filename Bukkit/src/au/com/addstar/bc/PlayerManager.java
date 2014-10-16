package au.com.addstar.bc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.StringUtil;

import au.com.addstar.bc.event.ProxyJoinEvent;
import au.com.addstar.bc.event.ProxyLeaveEvent;
import au.com.addstar.bc.sync.IPacketHandler;
import au.com.addstar.bc.sync.Packet;
import au.com.addstar.bc.sync.packet.FireEventPacket;
import au.com.addstar.bc.sync.packet.PlayerJoinPacket;
import au.com.addstar.bc.sync.packet.PlayerLeavePacket;
import au.com.addstar.bc.sync.packet.PlayerListPacket;
import au.com.addstar.bc.sync.packet.PlayerSettingsPacket;
import au.com.addstar.bc.sync.packet.UpdateNamePacket;

public class PlayerManager implements Listener, IPacketHandler
{
	private HashMap<UUID, CommandSender> mAllProxied = new HashMap<UUID, CommandSender>();
	private HashSet<UUID> mProxied = new HashSet<UUID>();
	private HashMap<UUID, String> mNicknames = new HashMap<UUID, String>();
	private HashMap<UUID, PlayerSettings> mPlayerSettings = new HashMap<UUID, PlayerSettings>();
	
	public PlayerManager(BungeeChat plugin)
	{
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	public CommandSender getPlayer(UUID id)
	{
		return mAllProxied.get(id);
	}
	
	public CommandSender getPlayer(String name)
	{
		return getPlayer(name, true);
	}
	
	public CommandSender getPlayer(String name, boolean includeAliases)
	{
		int best = Integer.MAX_VALUE;
		UUID bestId = null;
		
		name = name.toLowerCase();
		
		for(CommandSender player : mAllProxied.values())
		{
			if(StringUtils.containsIgnoreCase(player.getName(), name))
			{
				int diff = player.getName().length() - name.length();
				if(diff < best)
				{
					best = diff;
					bestId = getUniqueId(player);
				}
			}
			
			if (!includeAliases)
				continue;
			
			String nick = mNicknames.get(getUniqueId(player));
			
			if (StringUtils.isNotBlank(nick))
			{
				if(StringUtils.containsIgnoreCase(nick, name))
				{
					int diff = nick.length() - name.length();
					if(diff < best)
					{
						best = diff;
						bestId = getUniqueId(player);
					}
				}
			}
		}
		
		return mAllProxied.get(bestId);
	}
	
	public CommandSender getPlayerExact(String name)
	{
		return getPlayerExact(name, true);
	}
	
	public CommandSender getPlayerExact(String name, boolean includeAliases)
	{
		if(name.equalsIgnoreCase("console"))
			return Bukkit.getConsoleSender();

		for(CommandSender player : mAllProxied.values())
		{
			if (player.getName().equalsIgnoreCase(name))
				return player;
			
			if (!includeAliases)
				continue;
			
			String nick = mNicknames.get(getUniqueId(player));
			
			if (StringUtils.isNotBlank(nick))
			{
				if (nick.equalsIgnoreCase(name))
					return player;
			}
		}
		
		return null;
	}
	
	public Collection<CommandSender> getPlayers()
	{
		return Collections.unmodifiableCollection(mAllProxied.values());
	}
	
	public List<String> matchNames(String name)
	{
		return matchNames(name, true);
	}
	
	public List<String> matchNames(String name, boolean includeAliases)
	{
		name = name.toLowerCase();
		ArrayList<String> matches = new ArrayList<String>();
		
		for(CommandSender player : mAllProxied.values())
		{
			if(StringUtil.startsWithIgnoreCase(player.getName(), name))
				matches.add(player.getName());
			
			if (!includeAliases)
				continue;
			
			String nick = mNicknames.get(getUniqueId(player));
			
			if (StringUtils.isNotBlank(nick))
			{
				if(StringUtil.startsWithIgnoreCase(nick, name))
					matches.add(nick);
			}
		}
		
		return matches;
	}
	
	public String getPlayerNickname(CommandSender player)
	{
		return mNicknames.get(getUniqueId(player));
	}
	
	public void setPlayerNickname(CommandSender player, String name)
	{
		if(player instanceof Player)
		{
			PlayerSettings settings = getPlayerSettings(player);
			settings.nickname = name;
			
			if(name.isEmpty())
				((Player)player).setDisplayName(player.getName());
			else
				((Player)player).setDisplayName(name);
			
			updatePlayerSettings(player);
		}
		else
		{
			BungeeChat.getPacketManager().send(new UpdateNamePacket(getUniqueId(player), name));
		}
		
		onPlayerNameChange(getUniqueId(player), name);
	}
	
	public PlayerSettings getPlayerSettings(CommandSender player)
	{
		Validate.isTrue(player instanceof Player, "Cannot get player settins of non local player");
		
		UUID id = getUniqueId(player);
		PlayerSettings settings = mPlayerSettings.get(id);
		if(settings == null)
		{
			settings = new PlayerSettings();
			mPlayerSettings.put(id, settings);
		}
		
		return settings;
	}
	
	public void setPlayerMuteTime(CommandSender player, long endTime)
	{
		if(player instanceof Player)
		{
			PlayerSettings settings = getPlayerSettings(player);
			settings.muteTime = endTime;
			updatePlayerSettings(player);
		}
		else
			BungeeChat.getSyncManager().callSyncMethod("bchat:setMute", null, PlayerManager.getUniqueId(player), endTime);
	}
	
	public boolean isPlayerMuted(CommandSender player)
	{
		if(player instanceof Player)
			return System.currentTimeMillis() < getPlayerSettings(player).muteTime;
		
		return false;
	}
	
	private void onPlayerJoin(PlayerJoinPacket packet)
	{
		RemotePlayer current = new RemotePlayer(packet.getID(), packet.getName());
		mAllProxied.put(packet.getID(), current);
		mProxied.add(packet.getID());
		
		if(!packet.getNickname().isEmpty())
			mNicknames.put(packet.getID(), packet.getNickname());
	}
	
	private void onPlayerLeave(PlayerLeavePacket packet)
	{
		mAllProxied.remove(packet.getID());
		mProxied.remove(packet.getID());
		mNicknames.remove(packet.getID());
	}
	
	private void onFireEvent(FireEventPacket packet)
	{
		Player player = Bukkit.getPlayer(packet.getID());
		String message = packet.getMessage();
		
		if(player == null)
			return;
		
		if(message.isEmpty())
			message = null;
		
		switch(packet.getEvent())
		{
		case FireEventPacket.EVENT_JOIN:
		{
			ProxyJoinEvent event = new ProxyJoinEvent(player, message);
			Bukkit.getPluginManager().callEvent(event);
			
			if(event.getJoinMessage() != null)
				BungeeChat.broadcast(event.getJoinMessage());
			break;
		}
		case FireEventPacket.EVENT_QUIT:
		{
			ProxyLeaveEvent event = new ProxyLeaveEvent(player, message);
			Bukkit.getPluginManager().callEvent(event);
			
			if(event.getQuitMessage() != null)
				BungeeChat.broadcast(event.getQuitMessage());
			break;
		}
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	private void onPlayerJoinServer(PlayerLoginEvent event)
	{
		final Player current = event.getPlayer();
		mAllProxied.put(current.getUniqueId(), current);
		
		String nickname = mNicknames.get(current.getUniqueId());
		if (nickname != null)
			current.setDisplayName(nickname);
		
		Bukkit.getScheduler().runTaskLater(BungeeChat.getInstance(), new Runnable()
		{
			@Override
			public void run()
			{
				updateTabColor(current);
			}
		}, 2L);
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	private void onPlayerLeaveServer(PlayerQuitEvent event)
	{
		Player player = event.getPlayer();
		mPlayerSettings.remove(player.getUniqueId());
		
		// Prevent re-adding the player when they leave the proxy
		if (mProxied.contains(player.getUniqueId()))
		{
			RemotePlayer current = new RemotePlayer(player.getUniqueId(), player.getName());
			mAllProxied.put(player.getUniqueId(), current);
		}
		else
			mAllProxied.remove(player.getUniqueId());
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	private void onPlayerLeaveServer(PlayerKickEvent event)
	{
		Player player = event.getPlayer();
		mPlayerSettings.remove(player.getUniqueId());
		
		// Prevent re-adding the player when they leave the proxy
		if (mProxied.contains(player.getUniqueId()))
		{
			RemotePlayer current = new RemotePlayer(player.getUniqueId(), player.getName());
			mAllProxied.put(player.getUniqueId(), current);
		}
		else
			mAllProxied.remove(player.getUniqueId());
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	private void onPlayerChat(AsyncPlayerChatEvent event)
	{
		updateTabColor(event.getPlayer());
	}
	
	private void updateTabColor(Player player)
	{
		PermissionSetting level = Formatter.getPermissionLevel(player);
		String colour = "";
		if(level != null)
			colour = level.color;
		
		PlayerSettings settings = getPlayerSettings(player);
		
		if(!settings.tabFormat.equals(colour))
		{
			settings.tabFormat = colour;
			BungeeChat.getSyncManager().callSyncMethod("bchat:setTabColor", null, player.getUniqueId(), settings.tabFormat);
		}
	}
	
	private void onUpdatePlayers(PlayerListPacket packet)
	{
		mAllProxied.clear();
		mNicknames.clear();
		
		List<UUID> ids = packet.getIDs();
		List<String> names = packet.getNames();
		List<String> nicknames = packet.getNicknames();
		
		for(Player player : Bukkit.getOnlinePlayers())
		{
			mAllProxied.put(player.getUniqueId(), player);
			
			PlayerSettings settings = getPlayerSettings(player);
			if(!settings.nickname.isEmpty())
			{
				mNicknames.put(player.getUniqueId(), settings.nickname);
				player.setDisplayName(settings.nickname);
			}
			else
				player.setDisplayName(player.getName());
		}
		
		for(int i = 0; i < ids.size(); ++i)
		{
			UUID id = ids.get(i);
			String name = names.get(i);
			String nickname = nicknames.get(i);
			
			if(mAllProxied.containsKey(id))
				continue;
			
			RemotePlayer player = new RemotePlayer(id, name);
			mAllProxied.put(id, player);
			
			if(!nickname.isEmpty())
				mNicknames.put(id, nickname);
		}
	}
	
	private void onPlayerSettings(PlayerSettingsPacket packet)
	{
		Player player = Bukkit.getPlayer(packet.getID());
		if(player != null)
		{
			PlayerSettings settings = getPlayerSettings(player);
			settings.read(packet);
			
			if(settings.nickname.isEmpty())
			{
				player.setDisplayName(player.getName());
				onPlayerNameChange(player.getUniqueId(), "");
			}
			else
			{
				player.setDisplayName(settings.nickname);
				onPlayerNameChange(player.getUniqueId(), settings.nickname);
			}
		}
	}
	
	public void updatePlayerSettings(CommandSender player)
	{
		if(!(player instanceof Player))
			return;
		
		PlayerSettings settings = getPlayerSettings(player);
		BungeeChat.getPacketManager().sendNoQueue(settings.toPacket(getUniqueId(player)));
	}
	
	protected void onPlayerNameChange(UUID uuid, String newName)
	{
		if (newName.isEmpty())
			mNicknames.remove(uuid);
		else
			mNicknames.put(uuid, newName);
	}
	
	@Override
	public void handle( Packet packet )
	{
		if(packet instanceof PlayerJoinPacket)
			onPlayerJoin((PlayerJoinPacket)packet);
		else if(packet instanceof PlayerLeavePacket)
			onPlayerLeave((PlayerLeavePacket)packet);
		else if(packet instanceof PlayerListPacket)
			onUpdatePlayers((PlayerListPacket)packet);
		else if(packet instanceof UpdateNamePacket)
			onPlayerNameChange(((UpdateNamePacket) packet).getID(), ((UpdateNamePacket) packet).getName());
		else if(packet instanceof PlayerSettingsPacket)
			onPlayerSettings((PlayerSettingsPacket)packet);
		else if(packet instanceof FireEventPacket)
			onFireEvent((FireEventPacket)packet);
	}
	
	public static UUID getUniqueId(CommandSender sender)
	{
		if(sender instanceof Player)
			return ((Player)sender).getUniqueId();
		else if(sender instanceof RemotePlayer)
			return ((RemotePlayer)sender).getUniqueId();
		return null;
	}
}
