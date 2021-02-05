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

import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import au.com.addstar.bc.config.Config;
import au.com.addstar.bc.sync.packet.GlobalMutePacket;
import au.com.addstar.bc.util.Utilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class MuteHandler implements Listener
{
	private long mGMuteTime;
	private HashMap<InetAddress, Long> mIPMuteTime;
	private Set<String> mMutedCommands;
	
	private PlayerSettingsManager mPlayerManager;
	
	public MuteHandler(BungeeChat plugin)
	{
		mPlayerManager = plugin.getManager();
		mIPMuteTime = new HashMap<>();
		mMutedCommands = Collections.emptySet();
		mGMuteTime = 0;
		
		ProxyServer.getInstance().getScheduler().schedule(plugin, new UnmuteTimer(), 5, 5, TimeUnit.SECONDS);
		ProxyServer.getInstance().getPluginManager().registerListener(plugin, this);
	}
	
	public void updateSettings(Config config)
	{
		mMutedCommands = new HashSet<>(config.mutedCommands);
	}
	
	public void toggleGMute()
	{
		if (mGMuteTime == 0)
			mGMuteTime = Long.MAX_VALUE;
		else
			mGMuteTime = 0;
		
		if (mGMuteTime == 0)
			BungeeChat.audiences.all().sendMessage(Component.text("The global mute has ended").color(NamedTextColor.AQUA));
		else
			BungeeChat.audiences.all().sendMessage(
				Component.text("A ").color(NamedTextColor.AQUA)
				.append(Component.text("global").color(NamedTextColor.RED))
				.append(Component.text(" mute has been started")));
	}
	
	public void setGMute(long endTime)
	{
		if (mGMuteTime == endTime)
			return;
		
		if (endTime == 0)
			BungeeChat.audiences.all().sendMessage(Component.text("The global mute has ended").color(NamedTextColor.AQUA));
		else
		{
			Component message = Component.text("A ").color(NamedTextColor.AQUA)
				.append(Component.text("global").color(NamedTextColor.RED))
				.append(Component.text(" mute has been started"));
			
			if(endTime != Long.MAX_VALUE)
			{
				long timeLeft = Math.round((endTime - System.currentTimeMillis()) / 1000) * 1000L;
				message = message.append(Component.text(" for " + Utilities.timeDiffToString(timeLeft)));
			}
			BungeeChat.audiences.all().sendMessage(message);
		}
		
		mGMuteTime = endTime;
	}
	
	public boolean isGMuted()
	{
		return mGMuteTime != 0;
	}
	
	public void setIPMute(InetAddress address, long endTime)
	{
		Component message;
		if (endTime == 0)
		{
			if(mIPMuteTime.remove(address) == null)
				return;
			message = Component.text("You are no longer muted. You may talk again.").color(NamedTextColor.AQUA);
		}
		else
		{
			long timeLeft = Math.round((endTime - System.currentTimeMillis()) / 1000) * 1000L;
			message = Component.text("You have been muted for " + Utilities.timeDiffToString(timeLeft)).color(NamedTextColor.AQUA);
			mIPMuteTime.put(address, endTime);
		}
		
		// Send message to affected players
		for (ProxiedPlayer p : ProxyServer.getInstance().getPlayers())
		{
			if (p.getAddress().getAddress().equals(address))
			{
				BungeeChat.audiences.player(p).sendMessage(message);
				break;
			}
		}
	}
	
	public boolean isMuted(ProxiedPlayer player)
	{
		PlayerSettings settings = mPlayerManager.getSettings(player);
		InetAddress address = player.getAddress().getAddress();
		
		if(mGMuteTime > System.currentTimeMillis() && !player.hasPermission("bungeechat.globalmute.exempt"))
			return true;
		else if(settings.muteTime > System.currentTimeMillis())
			return true;
		else
			return mIPMuteTime.containsKey(address) && mIPMuteTime.get(address) > System.currentTimeMillis();
	}
	
	public boolean isMutedCommand(String commandString)
	{
		String command = commandString.split(" ")[0];
		if (command.startsWith("/"))
			command = command.substring(1);
		
		return mMutedCommands.contains(command);
	}
	
	@EventHandler
	public void onPlayerChat(ChatEvent event)
	{
		if(!(event.getSender() instanceof ProxiedPlayer))
			return;
		
		if(event.isCommand() && !isMutedCommand(event.getMessage()))
			return;
		
		ProxiedPlayer player = (ProxiedPlayer)event.getSender();
		if(!isMuted(player))
			return;
		
		boolean global = isGMuted();
		
		event.setCancelled(true);
		Component message;
		if (event.isCommand()) {
			if (global)
				message = Component.text("Everyone is muted. You may not use that command.").color(NamedTextColor.AQUA);
			else
				message = Component.text("You are muted. You may not use that command.").color(NamedTextColor.AQUA);
		} else {
			if (global)
				message = Component.text("Everyone is muted. You may not talk.").color(NamedTextColor.AQUA);
			else
				message = Component.text("You are muted. You may not talk.").color(NamedTextColor.AQUA);
		}
		BungeeChat.audiences.player(player).sendMessage(message);
	}
	
	private class UnmuteTimer implements Runnable
	{
		@Override
		public void run()
		{
			if(mGMuteTime > 0 && System.currentTimeMillis() >= mGMuteTime)
			{
				Component message = Component.text("The global mute has ended").color(NamedTextColor.AQUA);
				BungeeChat.audiences.all().sendMessage(message);
				mGMuteTime = 0;
				// TODO: Remove, this is only for legacy 
				BungeeChat.instance.getPacketManager().broadcast(new GlobalMutePacket(0));
			}
			
			Iterator<Entry<InetAddress, Long>> it = mIPMuteTime.entrySet().iterator();
			while(it.hasNext())
			{
				Entry<InetAddress, Long> entry = it.next();
				
				if (System.currentTimeMillis() >= entry.getValue())
				{
					it.remove();
					Component message = Component.text("You are no longer muted. You may talk again.").color(NamedTextColor.AQUA);
					for(ProxiedPlayer player : ProxyServer.getInstance().getPlayers())
					{
						if (player.getAddress().getAddress().equals(entry.getKey()))
							BungeeChat.audiences.player(player).sendMessage(message);
					}
				}
			}
			
			for(ProxiedPlayer player : ProxyServer.getInstance().getPlayers())
			{
				PlayerSettings settings = mPlayerManager.getSettings(player);
				if(settings.muteTime > 0 && System.currentTimeMillis() >= settings.muteTime)
				{
					settings.muteTime = 0;
					mPlayerManager.updateSettings(player);
					mPlayerManager.savePlayer(player);
					Component message = Component.text("You are no longer muted. You may talk again.").color(NamedTextColor.AQUA);
					BungeeChat.audiences.player(player).sendMessage(message);
				}
			}
		}
	}
}
