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

import au.com.addstar.bc.commands.Debugger;
import au.com.addstar.bc.event.ProxyJoinEvent;
import au.com.addstar.bc.event.ProxyLeaveEvent;
import au.com.addstar.bc.objects.ChatChannel;
import au.com.addstar.bc.objects.Formatter;
import au.com.addstar.bc.objects.PlayerSettings;
import au.com.addstar.bc.objects.RemotePlayer;
import au.com.addstar.bc.sync.IPacketHandler;
import au.com.addstar.bc.sync.Packet;
import au.com.addstar.bc.sync.packet.FireEventPacket;
import au.com.addstar.bc.sync.packet.PlayerJoinPacket;
import au.com.addstar.bc.sync.packet.PlayerLeavePacket;
import au.com.addstar.bc.sync.packet.PlayerListPacket;
import au.com.addstar.bc.sync.packet.PlayerRefreshPacket;
import au.com.addstar.bc.sync.packet.PlayerSettingsPacket;
import au.com.addstar.bc.sync.packet.UpdateNamePacket;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

  public class PlayerManager implements Listener, IPacketHandler
{
	private BungeeChat plugin;
	private Map<UUID, CommandSender> mAllProxied = new HashMap<>();
	private Set<UUID> uuidProxied = new HashSet<>();
	private Map<UUID, String> mNicknames = new HashMap<>();
	private Map<UUID, PlayerSettings> mPlayerSettings = new HashMap<>();
	private Map<UUID, String> mDefaultChannel = new HashMap<>();

	public PlayerManager(BungeeChat plugin)
	{
		this.plugin = plugin;
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

    public static UUID getUniqueId(CommandSender sender) {
        if (sender instanceof Player)
            return ((Player) sender).getUniqueId();
        else if (sender instanceof RemotePlayer)
            return ((RemotePlayer) sender).getUniqueId();
        return null;
    }

	public CommandSender getPlayer(UUID id)
	{
		CommandSender player;
		player = Bukkit.getPlayer(id);
		if (player != null)
			return player;

		player = mAllProxied.get(id);
		if (player != null)
			Debugger.logCorrect(player);
		return player;
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

		CommandSender player = mAllProxied.get(bestId);
		if (player != null)
			Debugger.logCorrect(player);
		return player;
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
			{
				Debugger.logCorrect(player);
				return player;
			}

			if (!includeAliases)
				continue;

			String nick = mNicknames.get(getUniqueId(player));

			if (StringUtils.isNotBlank(nick))
			{
				if (nick.equalsIgnoreCase(name))
				{
					Debugger.logCorrect(player);
					return player;
				}
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
		List<String> matches = new ArrayList<>();

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

	public String getPlayerChatName(CommandSender player){
		if(player instanceof Player)
		{
			PlayerSettings settings = getPlayerSettings(player);
			return settings.chatName;
		}else if(player instanceof RemotePlayer){
            PlayerSettings settings = mPlayerSettings.get(((RemotePlayer) player).getUniqueId());
            return settings.chatName;
        }
		else {
			return null;
		}
	}

	public void setPlayerChatName(CommandSender player, String prefix){
		if(player instanceof Player)
		{
			PlayerSettings settings = getPlayerSettings(player);
			settings.chatName = prefix;
			mPlayerSettings.put(((Player) player).getUniqueId(),settings);
			updatePlayerSettings(player);
			Debugger.log("Setting chat name local %s to '%s'", player.getName(), prefix);

		}else {
			Debugger.log("Cannot set chat name local %s to '%s' as player is not local", player.getName(), prefix);
		}

	}

	public String getPlayerNickname(CommandSender player)
	{
		return mNicknames.get(getUniqueId(player));
	}

	public void setPlayerNickname(CommandSender player, String name)
	{
		if(player instanceof Player)
		{
			setPlayerNickname0(player, name);

			updatePlayerSettings(player);
		}
		else
		{
			BungeeChat.getPacketManager().broadcast(new UpdateNamePacket(getUniqueId(player), name));
			Debugger.log("Setting nickname remote %s to '%s'", player.getName(), name);

			onPlayerNameChange(getUniqueId(player), name);
		}
	}

	public void setPlayerNickname0(CommandSender player, String name)
	{
		if (!(player instanceof Player))
			return;

		PlayerSettings settings = getPlayerSettings(player);
		settings.nickname = name;

		if(name.isEmpty())
			((Player)player).setDisplayName(player.getName());
		else
			((Player)player).setDisplayName(name);

		Debugger.log("Setting nickname local %s to '%s'", player.getName(), name);

		onPlayerNameChange(getUniqueId(player), name);
	}

	public PlayerSettings getPlayerSettings(CommandSender player){
		return getPlayerSettings(player, false);
	}

    @SuppressWarnings("WeakerAccess")
    public PlayerSettings getPlayerSettings(CommandSender player, boolean noValidation) {
        if (!noValidation)
            Validate.isTrue(player instanceof Player, "Cannot get player settings of non local player");

        UUID id = getUniqueId(player);
        PlayerSettings settings = mPlayerSettings.get(id);
        if (noValidation) return settings;
        if (settings == null) {
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

	public boolean isPlayerMuted(CommandSender player) {
		return player instanceof Player && System.currentTimeMillis() < getPlayerSettings(player).muteTime;

    }

    private void refreshPlayer(final Player player) {
        Bukkit.getScheduler().runTaskLater(BungeeChat.getInstance(), () -> {
            for (final Player other : player.getWorld().getPlayers()) {
                if (other.canSee(player)) {
                    other.hidePlayer(plugin, player);
                    Bukkit.getScheduler().runTaskLater(BungeeChat.getInstance(), () -> other.showPlayer(plugin, player), 1);
                }
            }
        }, 10);
    }

    private void onPlayerJoin(PlayerJoinPacket packet) {
        uuidProxied.add(packet.getID());
        if (!packet.getDefaultChannel().isEmpty())
            mDefaultChannel.put(packet.getID(), packet.getDefaultChannel());

        if (!packet.getNickname().isEmpty())
            mNicknames.put(packet.getID(), packet.getNickname());

        Player local = Bukkit.getPlayer(packet.getID());
        if (local == null) {
            RemotePlayer current = new RemotePlayer(packet.getID(), packet.getName());
            Debugger.log("Proxy join %s. Add as remote player", packet.getName());
            mAllProxied.put(packet.getID(), current);
        } else {
            Debugger.log("Proxy join %s. Add as local player", packet.getName());
            mAllProxied.put(packet.getID(), local);

            if (!packet.getNickname().isEmpty())
                local.setDisplayName(packet.getNickname());
        }
    }

    private void onPlayerLeave(PlayerLeavePacket packet) {
        CommandSender player = mAllProxied.remove(packet.getID());
        uuidProxied.remove(packet.getID());
        mNicknames.remove(packet.getID());
        unsubscribeAll(packet.getID(), null, false);
        mDefaultChannel.remove(packet.getID());
        Debugger.log("Proxy leave %s. Remove as local/remote", (player != null ? player.getName() : packet.getID()));
    }

    private void onFireEvent(FireEventPacket packet) {
        Player player = Bukkit.getPlayer(packet.getID());
        String message = packet.getMessage();

        if (message.isEmpty())
            message = null;

        if (player != null) {
            switch (packet.getEvent()) {
                case FireEventPacket.EVENT_JOIN: {
                    ProxyJoinEvent event = new ProxyJoinEvent(player, message);
                    Bukkit.getPluginManager().callEvent(event);

                    message = event.getJoinMessage();
                    break;
                }
                case FireEventPacket.EVENT_QUIT: {
                    ProxyLeaveEvent event = new ProxyLeaveEvent(player, message);
                    Bukkit.getPluginManager().callEvent(event);

                    message = event.getQuitMessage();
                    break;
                }
            }
        }

        if (message != null)
            BungeeChat.broadcast(message);
    }

    private void onRefresh(PlayerRefreshPacket packet) {
        Player player = Bukkit.getPlayer(packet.getID());
        if (player != null)
            refreshPlayer(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onPlayerJoinServer(PlayerLoginEvent event) {
        final Player current = event.getPlayer();
        Debugger.log("Server join %s. Add as local", event.getPlayer().getName());
        mAllProxied.put(current.getUniqueId(), current);

        String nickname = mNicknames.get(current.getUniqueId());
        if (nickname != null)
            current.setDisplayName(nickname);
        String chan = mDefaultChannel.getOrDefault(current.getUniqueId(), "");
        if (!chan.isEmpty()) {
            if (BungeeChat.getInstance().getChatChannelsManager().hasChannel(chan)) {
                mDefaultChannel.put(current.getUniqueId(), chan);
                BungeeChat.permissionManager.playerAdd(null, current,
                        BungeeChat.getInstance().getChatChannelsManager().getChannelSpeakPerm(chan));
                event.getPlayer().recalculatePermissions();
            }
        }
        Bukkit.getScheduler().runTaskLater(BungeeChat.getInstance(), () -> updateTabColor(current), 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onPlayerLeaveServer(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        mPlayerSettings.remove(player.getUniqueId());
        String channel = mDefaultChannel.get(player.getUniqueId());
        if (BungeeChat.getInstance().getChatChannelsManager().hasChannel(channel)) {
            BungeeChat.permissionManager.playerRemove(null, player,
                    BungeeChat.getInstance().getChatChannelsManager().getChannelSpeakPerm(channel));
        }

        // Prevent re-adding the player when they leave the proxy
        if (uuidProxied.contains(player.getUniqueId())) {
            CommandSender current = new RemotePlayer(player.getUniqueId(), player.getName());
            mAllProxied.put(player.getUniqueId(), current);
            Debugger.log("Server leave %s. Add as remote", event.getPlayer().getName());
        } else {
            mAllProxied.remove(player.getUniqueId());
            mDefaultChannel.remove(player.getUniqueId());
            // dont resend the update packet if the player has left the proxy
            unsubscribeAll(player, false, true);
            Debugger.log("Server leave %s. Not on proxy. Remove completely", event.getPlayer().getName());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onPlayerChat(AsyncPlayerChatEvent event) {
        updateTabColor(event.getPlayer());
    }

    private void updateTabColor(Player player) {
        PermissionSetting level = Formatter.getPermissionLevel(player);
        String colour = "";
        if (level != null)
            colour = level.color;

        PlayerSettings settings = getPlayerSettings(player);

        if (!settings.tabFormat.equals(colour)) {
            Debugger.log("Tab colour change %s: '%s'-'%s'", player.getName(), settings.tabFormat.replace(ChatColor.COLOR_CHAR, '&'), colour.replace(ChatColor.COLOR_CHAR, '&'));
            settings.tabFormat = colour;
            BungeeChat.getSyncManager().callSyncMethod("bchat:setTabColor", null, player.getUniqueId(), settings.tabFormat);
        }
    }

    private void onUpdatePlayers(PlayerListPacket packet) {
        mAllProxied.clear();
        mNicknames.clear();

        Debugger.log("Resetting online players:");

        List<UUID> ids = packet.getIDs();
        List<String> names = packet.getNames();
        List<String> nicknames = packet.getNicknames();

        for (Player player : Bukkit.getOnlinePlayers()) {
            mAllProxied.put(player.getUniqueId(), player);
            Debugger.log("Add %s as local", player.getName());

            PlayerSettings settings = getPlayerSettings(player);
            if (!settings.nickname.isEmpty()) {
                mNicknames.put(player.getUniqueId(), settings.nickname);
                player.setDisplayName(settings.nickname);
            } else
                player.setDisplayName(player.getName());
        }

        for (int i = 0; i < ids.size(); ++i) {
            UUID id = ids.get(i);
            String name = names.get(i);
            String nickname = nicknames.get(i);

            if (mAllProxied.containsKey(id))
                continue;

            RemotePlayer player = new RemotePlayer(id, name);
            mAllProxied.put(id, player);

            Debugger.log("Add %s as remote", player.getName());

            if (!nickname.isEmpty())
                mNicknames.put(id, nickname);

        }
    }

    private void onPlayerSettings(final PlayerSettingsPacket packet) {
        Player player = Bukkit.getPlayer(packet.getID());
        if (player != null) {
            // Player is online so handle the packet now
            doPlayerSettings(packet);
        } else {
            // Player is not online so try again in 5 ticks (in case it's sent while joining this server)
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> doPlayerSettings(packet), 5L);
        }
    }

    private void doPlayerSettings(PlayerSettingsPacket packet) {
        Player player = Bukkit.getPlayer(packet.getID());
        if (player != null) {
            Debugger.log("Updating settings for %s", player.getName());
            PlayerSettings settings = getPlayerSettings(player);
            settings.read(packet);

            if (settings.nickname.isEmpty()) {
                player.setDisplayName(player.getName());
                onPlayerNameChange(player.getUniqueId(), "");
            } else {
                player.setDisplayName(settings.nickname);
                onPlayerNameChange(player.getUniqueId(), settings.nickname);
            }
            if (settings.defaultChannel.isEmpty()) {
                unsubscribeAll(player);
            } else {
                mDefaultChannel.put(player.getUniqueId(), settings.defaultChannel);
            }
        }
    }

    public void updatePlayerSettings(CommandSender player) {
        if (!(player instanceof Player))
            return;
        PlayerSettings settings = getPlayerSettings(player);
        BungeeChat.getPacketManager().sendNoQueue(settings.toPacket(getUniqueId(player)));
    }

    private void onPlayerNameChange(UUID uuid, String newName) {
        if (newName.isEmpty())
            mNicknames.remove(uuid);
        else
            mNicknames.put(uuid, newName);

        // Force the player to spawn again so that the name will update
        Player player = Bukkit.getPlayer(uuid);
        if (player != null)
            refreshPlayer(player);
    }

    @Override
    public void handle(Packet packet) {
        if (packet instanceof PlayerJoinPacket)
            onPlayerJoin((PlayerJoinPacket) packet);
        else if (packet instanceof PlayerLeavePacket)
            onPlayerLeave((PlayerLeavePacket) packet);
        else if (packet instanceof PlayerListPacket)
            onUpdatePlayers((PlayerListPacket) packet);
        else if (packet instanceof UpdateNamePacket)
            onPlayerNameChange(((UpdateNamePacket) packet).getID(), ((UpdateNamePacket) packet).getName());
        else if (packet instanceof PlayerSettingsPacket)
            onPlayerSettings((PlayerSettingsPacket) packet);
        else if (packet instanceof FireEventPacket)
            onFireEvent((FireEventPacket) packet);
        else if (packet instanceof PlayerRefreshPacket)
            onRefresh((PlayerRefreshPacket) packet);
    }

    public void setDefaultChannel(Player sender, String channel) {
        PlayerSettings settings = getPlayerSettings(sender);
        settings.defaultChannel = channel;
        mPlayerSettings.put(sender.getUniqueId(), settings);
        if (!channel.isEmpty()) {
            mDefaultChannel.put(sender.getUniqueId(), channel);
        } else {
            mDefaultChannel.remove(sender.getUniqueId());
        }
        updatePlayerSettings(sender);
    }

    public String getDefaultChatChannel(Player sender) {
        return mDefaultChannel.getOrDefault(sender.getUniqueId(), "");
    }

    public void unsubscribeAll(CommandSender sender) {
        unsubscribeAll(sender, false, false);
    }

    public void unsubscribeAll(CommandSender sender, boolean forceSync) {
        unsubscribeAll(sender, forceSync, false);
    }

    public void unsubscribeAll(CommandSender sender, boolean forceSync, boolean playerOffline) {
        if (sender instanceof Player) {
            unsubscribeAll(((Player) sender).getUniqueId(), null, forceSync);
            PlayerSettings settings = getPlayerSettings(sender);
            settings.defaultChannel = "";
            // dont resend the update packet if the player has left the proxy
            if (!(playerOffline) && ((Player) sender).isOnline()) {
                updatePlayerSettings(sender);
            }
        }
        if (sender instanceof RemotePlayer) {
            unsubscribeAll(((RemotePlayer) sender).getUniqueId(), null, false);
        }
    }

    private void unsubscribeAll(UUID uuid, String world, boolean forceSync) {
        if (forceSync) {
            doUnsubscribeAll(uuid, null);
        }
	    else if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> doUnsubscribeAll(uuid, null));
        } else {
            doUnsubscribeAll(uuid, null);
        }
    }

    private void doUnsubscribeAll(UUID uuid, String world) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        for (ChatChannel channels : BungeeChat.getInstance().getChatChannelsManager().getChannelObj().values()) {
            if (channels.subscribe) {
                if (BungeeChat.permissionManager.playerHas(world, player, channels.permission)) {
                    if (BungeeChat.permissionManager.playerRemove(world, player, channels.permission)) {
                        Debugger.log(channels.permission + " was removed ");
                    } else {
                        Debugger.log(channels.permission + " removal failed ");
                    }
                }
            }
        }
        mDefaultChannel.remove(uuid);
    }
}