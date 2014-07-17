package au.com.addstar.bc;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.google.common.collect.HashBiMap;

public class PlayerManager implements Listener, IDataReceiver
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
			new MessageOutput("BungeeChat", "UpdateName")
				.writeUTF(getUniqueId(player).toString())
				.writeUTF(name)
				.send(BungeeChat.getInstance());
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
			BungeeChat.getSyncManager().callSyncMethod("bchat:setMute", null, player.getName(), endTime);
	}
	
	public boolean isPlayerMuted(CommandSender player)
	{
		if(player instanceof Player)
			return System.currentTimeMillis() < getPlayerSettings(player).muteTime;
		
		return false;
	}
	
	private void onPlayerJoin(UUID id, String name, String nickname)
	{
		RemotePlayer current = new RemotePlayer(id, name);
		mAllProxied.put(id, current);
		mNameMap.put(name.toLowerCase(), id);
		
		if(!nickname.isEmpty())
		{
			mNameMap.put(nickname.toLowerCase(), id);
			mReverseNickMapping.put(id, nickname);
		}
	}
	
	private void onPlayerJoinFirst(UUID uuid , String player, String nickname)
	{
		String message;
		if(nickname.isEmpty())
			message = ChatColor.YELLOW + player + " joined the game.";
		else
			message = ChatColor.YELLOW + nickname + " joined the game.";
		
		ProxyJoinEvent event = new ProxyJoinEvent(Bukkit.getPlayer(uuid), message);
		Bukkit.getPluginManager().callEvent(event);
		
		if(event.getJoinMessage() != null)
			BungeeChat.getSysMsgHandler().onPlayerGlobalJoin(event.getJoinMessage());
	}
	
	private void onPlayerLeave(UUID uuid)
	{
		CommandSender original = mAllProxied.remove(uuid);
		mNameMap.remove(original.getName().toLowerCase());
		
		// Remove nickname registration
		String nickname = mReverseNickMapping.remove(uuid);
		if(nickname != null)
			mNameMap.remove(nickname.toLowerCase());
	}
	
	private void onPlayerLeaveProxy(UUID id, String displayName, String message)
	{
		if(message.isEmpty())
			message = null;
		
		ProxyLeaveEvent event = new ProxyLeaveEvent(Bukkit.getOfflinePlayer(id), displayName, message);
		Bukkit.getPluginManager().callEvent(event);
		
		message = event.getQuitMessage();
		if(message == null)
			message = "";
		
		new MessageOutput("BungeeChat", "QuitMessage")
			.writeUTF(id.toString())
			.writeUTF(message)
			.send(BungeeChat.getInstance());
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
		RemotePlayer current = new RemotePlayer(player.getUniqueId(), player.getName());
		mAllProxied.put(player.getUniqueId(), current);
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
	
	private void onUpdatePlayers(DataInput input) throws IOException
	{
		mAllProxied.clear();
		
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
		
		int count = input.readShort();
		for(int i = 0; i < count; ++i)
		{
			UUID id = UUID.fromString(input.readUTF());
			String name = input.readUTF();
			String nickname = input.readUTF();
			
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
	
	public void updatePlayerSettings(CommandSender player)
	{
		if(!(player instanceof Player))
			return;
		
		PlayerSettings settings = getPlayerSettings(player);
		
		new MessageOutput("BungeeChat", "SyncPlayer")
			.writeUTF(getUniqueId(player).toString())
			.writeUTF(settings.lastMsgTarget == null ? "" : settings.lastMsgTarget.toString())
			.writeUTF(settings.nickname)
			.writeByte(settings.socialSpyState)
			.writeBoolean(settings.msgEnabled)
			.writeLong(settings.muteTime)
			.send((Player)player, BungeeChat.getInstance());
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
	public void onMessage( String channel, DataInput input ) throws IOException
	{
		if(channel.equals("Player+"))
			onPlayerJoin(UUID.fromString(input.readUTF()), input.readUTF(), input.readUTF());
		else if(channel.equals("Player-"))
			onPlayerLeave(UUID.fromString(input.readUTF()));
		else if(channel.equals("Player*"))
			onUpdatePlayers(input);
		else if(channel.equals("UpdateName"))
			onPlayerNameChange(UUID.fromString(input.readUTF()), input.readUTF());
		else if(channel.equals("SyncPlayer"))
		{
			Player player = Bukkit.getPlayer(UUID.fromString(input.readUTF()));
			if(player != null)
			{
				PlayerSettings settings = getPlayerSettings(player);
				settings.read(input);
				if(settings.nickname.isEmpty())
					player.setDisplayName(player.getName());
				else
				{
					player.setDisplayName(settings.nickname);
					onPlayerNameChange(player.getUniqueId(), settings.nickname);
				}
			}
		}
		else if(channel.equals("ProxyJoin"))
			onPlayerJoinFirst(UUID.fromString(input.readUTF()), input.readUTF(), input.readUTF());
		else if(channel.equals("ProxyLeave"))
			onPlayerLeaveProxy(UUID.fromString(input.readUTF()), input.readUTF(), input.readUTF());
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
