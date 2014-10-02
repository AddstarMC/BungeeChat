package au.com.addstar.bc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

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

import com.google.common.collect.HashBiMap;

public class PlayerManager implements Listener, IPacketHandler
{
	private HashBiMap<UUID, CommandSender> mAllProxied = HashBiMap.create();
	private HashMap<String, UUID> mNameMap = new HashMap<String, UUID>();
	private HashMap<UUID, String> mReverseNickMapping = new HashMap<UUID, String>();
	private HashMap<CommandSender, PlayerSettings> mPlayerSettings = new HashMap<CommandSender, PlayerSettings>();
	
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
		
		for(Entry<String, UUID> entry : mNameMap.entrySet())
		{
			String nick = mReverseNickMapping.get(entry.getValue());
			
			if(!includeAliases && nick != null && nick.equalsIgnoreCase(entry.getKey()))
				continue;
			
			if(entry.getKey().contains(name))
			{
				int diff = entry.getKey().length() - name.length();
				if(diff < best)
				{
					best = diff;
					bestId = entry.getValue();
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

		UUID id = mNameMap.get(name.toLowerCase());
		if(id == null)
			return null;
		
		CommandSender sender = mAllProxied.get(id);
		if(!includeAliases && sender != null && !name.equalsIgnoreCase(sender.getName()))
			return null;
		return sender;
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
		for(Entry<String, UUID> entry : mNameMap.entrySet())
		{
			String nick = mReverseNickMapping.get(entry.getValue());
			
			if(!includeAliases && nick != null && nick.equalsIgnoreCase(entry.getKey()))
				continue;
			
			if(entry.getKey().startsWith(name))
			{
				if(entry.getKey().equalsIgnoreCase(nick))
					matches.add(nick);
				else
					matches.add(mAllProxied.get(entry.getValue()).getName());
			}
		}
		
		return matches;
	}
	
	public String getPlayerNickname(CommandSender player)
	{
		return mReverseNickMapping.get(getUniqueId(player));
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
		
		PlayerSettings settings = mPlayerSettings.get(player);
		if(settings == null)
		{
			settings = new PlayerSettings();
			mPlayerSettings.put(player, settings);
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
		mNameMap.put(packet.getName().toLowerCase(), packet.getID());
		
		if(!packet.getNickname().isEmpty())
		{
			mNameMap.put(packet.getNickname().toLowerCase(), packet.getID());
			mReverseNickMapping.put(packet.getID(), packet.getNickname());
		}
	}
	
	private void onPlayerLeave(PlayerLeavePacket packet)
	{
		CommandSender original = mAllProxied.remove(packet.getID());
		mNameMap.remove(original.getName().toLowerCase());
		
		// Remove nickname registration
		String nickname = mReverseNickMapping.remove(packet.getID());
		if(nickname != null)
			mNameMap.remove(nickname.toLowerCase());
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
		
		// Update nickname registration
		String nickname = mReverseNickMapping.get(current.getUniqueId());
		if(nickname != null)
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
		if (mAllProxied.containsKey(player.getUniqueId()))
		{
			RemotePlayer current = new RemotePlayer(player.getUniqueId(), player.getName());
			mAllProxied.put(player.getUniqueId(), current);
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	private void onPlayerLeaveServer(PlayerKickEvent event)
	{
		Player player = event.getPlayer();
		mPlayerSettings.remove(player.getUniqueId());
		RemotePlayer current = new RemotePlayer(player.getUniqueId(), player.getName());
		mAllProxied.put(player.getUniqueId(), current);
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
		mNameMap.clear();
		mReverseNickMapping.clear();
		
		List<UUID> ids = packet.getIDs();
		List<String> names = packet.getNames();
		List<String> nicknames = packet.getNicknames();
		
		for(Player player : Bukkit.getOnlinePlayers())
		{
			mAllProxied.put(player.getUniqueId(), player);
			mNameMap.put(player.getName().toLowerCase(), player.getUniqueId());
			
			PlayerSettings settings = getPlayerSettings(player);
			if(!settings.nickname.isEmpty())
			{
				mNameMap.put(settings.nickname.toLowerCase(), player.getUniqueId());
				mReverseNickMapping.put(player.getUniqueId(), settings.nickname);
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
			mNameMap.put(name.toLowerCase(), id);
			
			if(!nickname.isEmpty())
			{
				mNameMap.put(nickname.toLowerCase(), id);
				mReverseNickMapping.put(id, nickname);
			}
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
	
	private void onPlayerNameChange(UUID uuid, String newName)
	{
		String oldName = mReverseNickMapping.remove(uuid);
		if(oldName != null)
			mNameMap.remove(oldName.toLowerCase());
		
		if(!newName.isEmpty())
		{
			mNameMap.put(newName.toLowerCase(), uuid);
			mReverseNickMapping.put(uuid, newName);
		}
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
