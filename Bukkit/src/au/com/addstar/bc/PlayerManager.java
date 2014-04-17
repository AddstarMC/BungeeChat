package au.com.addstar.bc;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import net.minecraft.util.org.apache.commons.lang3.Validate;

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

public class PlayerManager implements Listener, IDataReceiver
{
	private HashMap<CommandSender, String> mReverseNickMapping = new HashMap<CommandSender, String>();
	private HashMap<String, CommandSender> mAllProxied = new HashMap<String, CommandSender>();
	private HashMap<CommandSender, PlayerSettings> mPlayerSettings = new HashMap<CommandSender, PlayerSettings>();
	
	public PlayerManager(BungeeChat plugin)
	{
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	public CommandSender getPlayer(String name)
	{
		return getPlayer(name, true);
	}
	
	public CommandSender getPlayer(String name, boolean includeAliases)
	{
		int best = Integer.MAX_VALUE;
		CommandSender bestCS = null;
		
		name = name.toLowerCase();
		
		for(Entry<String, CommandSender> entry : mAllProxied.entrySet())
		{
			if(!includeAliases && !entry.getKey().equalsIgnoreCase(entry.getValue().getName()))
				continue;
			
			if(entry.getKey().contains(name))
			{
				int diff = entry.getKey().length() - name.length();
				if(diff < best)
				{
					best = diff;
					bestCS = entry.getValue();
				}
			}
		}
		
		return bestCS;
	}
	
	public CommandSender getPlayerExact(String name)
	{
		return getPlayerExact(name, true);
	}
	
	public CommandSender getPlayerExact(String name, boolean includeAliases)
	{
		if(name.equalsIgnoreCase("console"))
			return Bukkit.getConsoleSender();

		CommandSender sender = mAllProxied.get(name.toLowerCase());
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
		for(Entry<String, CommandSender> entry : mAllProxied.entrySet())
		{
			if(!includeAliases && !entry.getKey().equalsIgnoreCase(entry.getValue().getName()))
				continue;
			
			if(entry.getKey().startsWith(name))
			{
				if(entry.getKey().equalsIgnoreCase(entry.getValue().getName()))
					matches.add(entry.getValue().getName());
				else
					matches.add(mReverseNickMapping.get(entry.getValue()));
			}
		}
		
		return matches;
	}
	
	public String getPlayerNickname(CommandSender player)
	{
		return mReverseNickMapping.get(player);
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
				.writeUTF(player.getName())
				.writeUTF(name)
				.send(BungeeChat.getInstance());
		}
		
		onPlayerNameChange(player.getName(), name);
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
	
	private void onPlayerJoin(String player, String nickname)
	{
		RemotePlayer current = new RemotePlayer(player);
		mAllProxied.put(player.toLowerCase(), current);
		
		if(!nickname.isEmpty())
		{
			mAllProxied.put(nickname.toLowerCase(), current);
			mReverseNickMapping.put(current, nickname);
		}
		
		BungeeChat.getSysMsgHandler().onPlayerGlobalJoin(player, nickname);
	}
	
	private void onPlayerLeave(String player)
	{
		CommandSender original = mAllProxied.remove(player.toLowerCase());
		
		// Remove nickname registration
		String nickname = mReverseNickMapping.remove(original);
		if(nickname != null)
			mAllProxied.remove(nickname.toLowerCase());
		
		BungeeChat.getSysMsgHandler().onPlayerGlobalLeave(player, nickname == null ? "" : nickname);
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	private void onPlayerJoinServer(PlayerLoginEvent event)
	{
		final Player current = event.getPlayer();
		CommandSender original = mAllProxied.put(event.getPlayer().getName().toLowerCase(), current);
		
		// Update nickname registration
		String nickname = mReverseNickMapping.get(original);
		if(nickname != null)
		{
			mAllProxied.put(nickname.toLowerCase(), current);
			mReverseNickMapping.put(current, nickname);
			current.setDisplayName(nickname);
		}
		
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
		mPlayerSettings.remove(event.getPlayer());
		RemotePlayer current = new RemotePlayer(event.getPlayer().getName());
		CommandSender original = mAllProxied.put(event.getPlayer().getName().toLowerCase(), current);
		
		// Update nickname registration
		String nickname = mReverseNickMapping.get(original);
		if(nickname != null)
		{
			mAllProxied.put(nickname.toLowerCase(), current);
			mReverseNickMapping.put(current, nickname);
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	private void onPlayerLeaveServer(PlayerKickEvent event)
	{
		mPlayerSettings.remove(event.getPlayer());
		RemotePlayer current = new RemotePlayer(event.getPlayer().getName());
		CommandSender original = mAllProxied.put(event.getPlayer().getName().toLowerCase(), current);
		
		// Update nickname registration
		String nickname = mReverseNickMapping.get(original);
		if(nickname != null)
		{
			mAllProxied.put(nickname.toLowerCase(), current);
			mReverseNickMapping.put(current, nickname);
		}
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
			BungeeChat.getSyncManager().callSyncMethod("bchat:setTabColor", null, player.getName(), settings.tabFormat);
		}
	}
	
	private void onUpdatePlayers(DataInput input) throws IOException
	{
		mAllProxied.clear();
		
		for(Player player : Bukkit.getOnlinePlayers())
		{
			mAllProxied.put(player.getName().toLowerCase(), player);
			PlayerSettings settings = getPlayerSettings(player);
			if(!settings.nickname.isEmpty())
			{
				mAllProxied.put(settings.nickname.toLowerCase(), player);
				mReverseNickMapping.put(player, settings.nickname);
				player.setDisplayName(settings.nickname);
			}
			else
				player.setDisplayName(player.getName());
		}
		
		int count = input.readShort();
		for(int i = 0; i < count; ++i)
		{
			String name = input.readUTF();
			String nickname = input.readUTF();
			
			if(mAllProxied.containsKey(name.toLowerCase()))
				continue;
			
			RemotePlayer player = new RemotePlayer(name);
			mAllProxied.put(name.toLowerCase(), player);
			
			if(!nickname.isEmpty())
			{
				mAllProxied.put(nickname.toLowerCase(), player);
				mReverseNickMapping.put(player, nickname);
			}
		}
	}
	
	public void updatePlayerSettings(CommandSender player)
	{
		if(!(player instanceof Player))
			return;
		
		PlayerSettings settings = getPlayerSettings(player);
		
		new MessageOutput("BungeeChat", "SyncPlayer")
			.writeUTF(player.getName())
			.writeUTF(settings.lastMsgTarget == null ? "" : settings.lastMsgTarget)
			.writeUTF(settings.nickname)
			.writeByte(settings.socialSpyState)
			.writeBoolean(settings.msgEnabled)
			.writeLong(settings.muteTime)
			.send((Player)player, BungeeChat.getInstance());
	}
	
	private void onPlayerNameChange(String player, String newName)
	{
		CommandSender current = mAllProxied.get(player.toLowerCase());
		String oldName = mReverseNickMapping.remove(current);
		if(oldName != null)
			mAllProxied.remove(oldName.toLowerCase());
		
		if(!newName.isEmpty())
		{
			mAllProxied.put(newName.toLowerCase(), current);
			mReverseNickMapping.put(current, newName);
		}
	}
	
	@Override
	public void onMessage( String channel, DataInput input ) throws IOException
	{
		if(channel.equals("Player+"))
			onPlayerJoin(input.readUTF(), input.readUTF());
		else if(channel.equals("Player-"))
			onPlayerLeave(input.readUTF());
		else if(channel.equals("Player*"))
			onUpdatePlayers(input);
		else if(channel.equals("UpdateName"))
			onPlayerNameChange(input.readUTF(), input.readUTF());
		else if(channel.equals("SyncPlayer"))
		{
			String playerName = input.readUTF();
			Player player = Bukkit.getPlayerExact(playerName);
			if(player != null)
			{
				PlayerSettings settings = getPlayerSettings(player);
				settings.read(input);
				if(settings.nickname.isEmpty())
					player.setDisplayName(player.getName());
				else
				{
					player.setDisplayName(settings.nickname);
					onPlayerNameChange(playerName, settings.nickname);
				}
			}
		}
	}
}
