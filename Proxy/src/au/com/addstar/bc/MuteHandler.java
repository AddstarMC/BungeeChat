package au.com.addstar.bc;

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
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
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
		mIPMuteTime = new HashMap<InetAddress, Long>();
		mMutedCommands = Collections.emptySet();
		mGMuteTime = 0;
		
		ProxyServer.getInstance().getScheduler().schedule(plugin, new UnmuteTimer(), 5, 5, TimeUnit.SECONDS);
		ProxyServer.getInstance().getPluginManager().registerListener(plugin, this);
	}
	
	public void updateSettings(Config config)
	{
		mMutedCommands = new HashSet<String>(config.mutedCommands);
	}
	
	public void toggleGMute()
	{
		if (mGMuteTime == 0)
			mGMuteTime = Long.MAX_VALUE;
		else
			mGMuteTime = 0;
		
		if (mGMuteTime == 0)
			ProxyServer.getInstance().broadcast(TextComponent.fromLegacyText(ChatColor.AQUA + "The global mute has ended"));
		else
			ProxyServer.getInstance().broadcast(TextComponent.fromLegacyText(ChatColor.AQUA + "A " + ChatColor.RED + "global" + ChatColor.AQUA + " mute has been started"));
	}
	
	public void setGMute(long endTime)
	{
		if (mGMuteTime == endTime)
			return;
		
		if (endTime == 0)
			ProxyServer.getInstance().broadcast(TextComponent.fromLegacyText(ChatColor.AQUA + "The global mute has ended"));
		else
		{
			String message = ChatColor.AQUA + "A " + ChatColor.RED + "global" + ChatColor.AQUA + " mute has been started";
			
			if(endTime != Long.MAX_VALUE)
			{
				long timeLeft = Math.round((endTime - System.currentTimeMillis()) / 1000) * 1000L;
				message += " for " + Utilities.timeDiffToString(timeLeft);
			}
			
			ProxyServer.getInstance().broadcast(TextComponent.fromLegacyText(message));
		}
		
		mGMuteTime = endTime;
	}
	
	public boolean isGMuted()
	{
		return mGMuteTime != 0;
	}
	
	public void setIPMute(InetAddress address, long endTime)
	{
		BaseComponent[] message;
		if (endTime == 0)
		{
			if(mIPMuteTime.remove(address) == null)
				return;
			
			message = TextComponent.fromLegacyText(ChatColor.AQUA + "You are no longer muted. You may talk again.");
		}
		else
		{
			long timeLeft = Math.round((endTime - System.currentTimeMillis()) / 1000) * 1000L;
			message = TextComponent.fromLegacyText(ChatColor.AQUA + "You have been muted for " + Utilities.timeDiffToString(timeLeft));
			
			mIPMuteTime.put(address, endTime);
		}
		
		// Send message to affected players
		for (ProxiedPlayer p : ProxyServer.getInstance().getPlayers())
		{
			if (p.getAddress().getAddress().equals(address))
			{
				p.sendMessage(message);
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
		else if(mIPMuteTime.containsKey(address) && mIPMuteTime.get(address) > System.currentTimeMillis())
			return true;
		return false;
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
		if (event.isCommand())
		{
			if (global)
				player.sendMessage(TextComponent.fromLegacyText(ChatColor.AQUA + "Everyone is muted. You may not use that command."));
			else
				player.sendMessage(TextComponent.fromLegacyText(ChatColor.AQUA + "You are muted. You may not use that command."));
		}
		else
		{
			if (global)
				player.sendMessage(TextComponent.fromLegacyText(ChatColor.AQUA + "Everyone is muted. You may not talk."));
			else
				player.sendMessage(TextComponent.fromLegacyText(ChatColor.AQUA + "You are muted. You may not talk."));
		}
	}
	
	private class UnmuteTimer implements Runnable
	{
		@Override
		public void run()
		{
			if(mGMuteTime > 0 && System.currentTimeMillis() >= mGMuteTime)
			{
				ProxyServer.getInstance().broadcast(TextComponent.fromLegacyText(ChatColor.AQUA + "The global mute has ended"));
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
					BaseComponent[] message = TextComponent.fromLegacyText(ChatColor.AQUA + "You are no longer muted. You may talk again.");
					for(ProxiedPlayer player : ProxyServer.getInstance().getPlayers())
					{
						if (player.getAddress().getAddress().equals(entry.getKey()))
							player.sendMessage(message);
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
					
					player.sendMessage(TextComponent.fromLegacyText(ChatColor.AQUA + "You are no longer muted. You may talk again."));
				}
			}
		}
	}
}
