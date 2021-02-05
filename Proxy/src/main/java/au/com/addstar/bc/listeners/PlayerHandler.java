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

package au.com.addstar.bc.listeners;

/*-
 * #%L
 * BungeeChat-Proxy
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

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import au.com.addstar.bc.BungeeChat;
import au.com.addstar.bc.ColourTabList;
import au.com.addstar.bc.Debugger;
import au.com.addstar.bc.PlayerSettings;
import au.com.addstar.bc.PlayerSettingsManager;
import au.com.addstar.bc.event.BCPlayerJoinEvent;
import au.com.addstar.bc.event.BCPlayerQuitEvent;
import au.com.addstar.bc.sync.PacketManager;
import au.com.addstar.bc.sync.packet.FireEventPacket;
import au.com.addstar.bc.sync.packet.PlayerJoinPacket;
import au.com.addstar.bc.sync.packet.PlayerLeavePacket;
import au.com.addstar.bc.sync.packet.PlayerRefreshPacket;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class PlayerHandler implements Listener
{
	private PlayerSettingsManager mSettings;
	private PacketManager mPackets;
	private ProxyServer mProxy;
	
	public PlayerHandler()
	{
		mSettings = BungeeChat.instance.getManager();
		mPackets = BungeeChat.instance.getPacketManager();
		mProxy = ProxyServer.getInstance();
	}
	
	private void loadSettingsAsync(final ProxiedPlayer player)
	{
		mProxy.getScheduler().runAsync(BungeeChat.instance, () -> {
			// Load this players settings
			PlayerSettings settings = mSettings.getSettings(player);

			if(settings.nickname.isEmpty())
				player.setDisplayName(player.getName());
			else
				player.setDisplayName(settings.nickname);

			Debugger.log("Applying nickname to PP %s: '%s'", player.getName(), settings.nickname);

			if (settings.skin != null && !settings.skin.isEmpty())
			{
				ColourTabList tablist = ((ColourTabList)player.getTabListHandler());
				tablist.setOverrideSkin(BungeeChat.instance.getSkinLibrary().getSkinWithLookupSync(UUID.fromString(settings.skin)));

				if (tablist.hasInited())
					BungeeChat.instance.getPacketManager().send(new PlayerRefreshPacket(player.getUniqueId()), player.getServer().getInfo());
			}
		});
	}
	
	@EventHandler
	public void onFinishLogin(final PostLoginEvent event)
	{
		Debugger.log("PP join %s", event.getPlayer().getName());
		
		event.getPlayer().setTabListHandler(new ColourTabList(event.getPlayer()));
		loadSettingsAsync(event.getPlayer());
	}
	
	@EventHandler
	public void onDisconnect(PlayerDisconnectEvent event)
	{
		ProxiedPlayer player = event.getPlayer();
		UUID id = player.getUniqueId();
		
		Debugger.log("PP disconnect %s", player.getName());
		
		if (player.getServer() != null)
		{
			boolean showQuitMessage = BungeeChat.instance.getSyncManager().getPropertyBoolean(player, "hasQuitMessage", true);
			TextComponent quitMessage = Component.text(player.getDisplayName() + " has left the game").color(NamedTextColor.GOLD);
			String sendMessage = GsonComponentSerializer.gson().serialize(quitMessage);

			if(!showQuitMessage)
				sendMessage = null;
			
			BCPlayerQuitEvent qevent = new BCPlayerQuitEvent(player, sendMessage);
			mProxy.getPluginManager().callEvent(qevent);
			sendMessage = qevent.getQuitMessage();
			if (sendMessage == null) {
				sendMessage = "";
			}
			mPackets.send(new FireEventPacket(FireEventPacket.EVENT_QUIT, id, sendMessage), player.getServer().getInfo());
		}
		else
			Debugger.log("Player %s never joined a server", player.getName());
		BungeeChat.instance.getSubHandler().unSubscribe(id);
		mPackets.broadcast(new PlayerLeavePacket(id));
		mSettings.unloadPlayer(id);
	}
	
	// Post connection established, pre login to server
	@EventHandler
	public void onFirstConnected(ServerConnectedEvent event)
	{
		if (!isOnline(event.getPlayer()))
		{
			Debugger.log("ServerConnected player not online %s", event.getPlayer().getName());
			return;
		}
		
		final ProxiedPlayer player = event.getPlayer();
		
		// First server join only
		if(player.getServer() != null)
			return;
		
		mPackets.broadcast(new PlayerJoinPacket(player.getUniqueId(), player.getName(), mSettings.getSettings(player).nickname, mSettings.getSettings(player).defaultChannel)
		);
		TextComponent quitMessage = Component.text(player.getDisplayName() + " joined the game.").color(NamedTextColor.GOLD);
		String sendMessage = GsonComponentSerializer.gson().serialize(quitMessage);
		BCPlayerJoinEvent jevent = new BCPlayerJoinEvent(player, sendMessage);
		mProxy.getPluginManager().callEvent(jevent);
		String message = jevent.getJoinMessage();
		if (message == null)
			message = "";
		mPackets.send(new FireEventPacket(FireEventPacket.EVENT_JOIN, player.getUniqueId(), message), event.getServer().getInfo());
		
		// Give 1 second for plugins on the server to apply tab groups to this player
		mProxy.getScheduler().schedule(BungeeChat.instance, () -> {
            if (!isOnline(player))
            {
                Debugger.log("ServerConnected-task player not online %s", player.getName());
                return;
            }
            
            if(player.getTabListHandler() instanceof ColourTabList)
            {
                ((ColourTabList)player.getTabListHandler()).onJoinPeriodComplete();
                BungeeChat.instance.getPacketManager().send(new PlayerRefreshPacket(player.getUniqueId()), player.getServer().getInfo());
            }
        }, 1, TimeUnit.SECONDS);
	}
	
	// Post login request, still before actual server login
	@EventHandler
	public void onServerSwitch(final ServerSwitchEvent event)
	{
		if (!isOnline(event.getPlayer()))
		{
			Debugger.log("ServerSwitch player not online %s", event.getPlayer().getName());
			return;
		}
		
		mProxy.getScheduler().schedule(BungeeChat.instance, () -> {
            if (!isOnline(event.getPlayer()))
                return;
            
            mSettings.updateSettings(event.getPlayer());
        }, 10, TimeUnit.MILLISECONDS);
		
		ColourTabList.updateAll();
	}
	
	private boolean isOnline(ProxiedPlayer player)
	{
		return mProxy.getPlayer(player.getUniqueId()) != null;
	}
}
