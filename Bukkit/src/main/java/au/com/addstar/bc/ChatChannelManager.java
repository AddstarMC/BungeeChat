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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import au.com.addstar.bc.commands.Debugger;
import au.com.addstar.bc.objects.ChannelType;
import au.com.addstar.bc.objects.ChatChannel;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.jetbrains.annotations.Nullable;

import au.com.addstar.bc.config.ChatChannelConfig;
import au.com.addstar.bc.event.ChatChannelEvent;
import au.com.addstar.bc.sync.SyncConfig;



public class ChatChannelManager implements Listener, CommandExecutor
{
	private HashMap<String, ChatChannel> mChannels = new HashMap<>();
	public ChatChannelManager(BungeeChat plugin)
	{
	    Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	private void onChatChannel(ChatChannelEvent event)
	{
		if(event.getChannelType() != ChannelType.Custom)
			return;
		
		ChatChannel channelObj = mChannels.get(event.getChannelName());
		if(channelObj != null) {
			Debugger.log(event.getEventName() + " Chat EVENT RCV  ->" + event.getMessage());
                if (channelObj.listenPermission != null)
                    Bukkit.broadcast(event.getMessage(), channelObj.listenPermission);
                else
                    Bukkit.broadcastMessage(event.getMessage());
        }
	}
	
	@EventHandler(priority=EventPriority.LOW, ignoreCancelled=true)
	private void onPlayerCommand(PlayerCommandPreprocessEvent event)
	{
		if(processCommands(event.getPlayer(), event.getMessage().substring(1)))
		{
			Debugger.log(event.getEventName() + " Preproccess  CANCELLED - BC processed ->" + event.getMessage());
			event.setMessage("/nullcmd");
			event.setCancelled(true);
		}
	}
	
	@EventHandler(priority=EventPriority.LOW, ignoreCancelled=true)
	private void onServerCommand(ServerCommandEvent event)
	{
		if(processCommands(event.getSender(), event.getCommand()))
		{
			Debugger.log(event.getCommand()+ " SRV CMD CANCELLED - BC processed ->" + event.getCommand());
			event.setCommand("/nullcmd");
		}
	}
	
	private boolean processCommands(CommandSender sender, String message)
	{
		String command;
		int pos = message.indexOf(' ');
		if(pos == -1)
		{
			command = message;
			message = null;
		}
		else
		{
			command = message.substring(0, pos);
			message = message.substring(pos+1);
		}

		for(ChatChannel channel : mChannels.values())
		{
			if((channel.command != null) && !channel.command.isEmpty() && channel.command.equals(command))
			{
				if(channel.permission != null && !sender.hasPermission(channel.permission)){
				    break;
				}
				if(message != null) {
                    channel.say(sender, message);
                    return true;
                }
				Debugger.log("BC : " + channel.name + ": NULL MSG");
			}
		}
		return false;
	}
	
	@Override
	public boolean onCommand( CommandSender sender, Command command, String label, String[] args )
	{
		if(args.length < 2)
			return false;
		
		String channelCmd = args[0];
		
		String message = StringUtils.join(args, ' ', 1, args.length);
		for(ChatChannel channel : mChannels.values())
		{
			if(!channel.command.isEmpty() && channel.command.equalsIgnoreCase(channelCmd)) {
				BungeeChat.getInstance().getLogger().info("Command: " + channelCmd + " matched");
				if (channel.permission != null)
					if (sender.hasPermission(channel.permission)) {
                        channel.say(sender, message);
					} else {
						Debugger.log("BC : " + channel.name + ":" + message);
						return true;
					}
				}
			}
			return false;
	}

	
	public void unregisterAll()
	{
		for(ChatChannel channel : mChannels.values())
			channel.unregisterChannel();
		
		mChannels.clear();
	}
	
	public void register(String name, String command, String format, String permission, String listenPerm, Boolean subscribe, Boolean isRp)
	{
		ChatChannel channel = new ChatChannel(name, command, format, permission, listenPerm, subscribe, isRp);
		channel.registerChannel();
		mChannels.put(channel.name, channel);
	}
	
	public void unregister(String name)
	{
		ChatChannel channel = mChannels.remove(name);
		if(channel != null)
			channel.unregisterChannel();
	}
	
	public void load(SyncConfig config)
	{
		unregisterAll();
		
		SyncConfig channels = config.getSection("channels");
		
		for (String key : channels.getKeys())
		{
			ChatChannelConfig setting = (ChatChannelConfig) channels.get(key, null);
			register(key, setting.command, setting.format, setting.permission, setting.listenPermission, setting.subscribe, setting.isRP);
		}
	}

	public List<String> getChannelNames(boolean sub){
		List<String> channels = new ArrayList<>();
		for (Map.Entry<String, ChatChannel> channel: mChannels.entrySet()){
			if(sub) {
				if (channel.getValue().subscribe) {
					channels.add(channel.getKey());
				}
			} else {
				channels.add(channel.getKey());
			}
		}
		return channels;
	}

	public HashMap<String, ChatChannel> getChannelObj() {
		return mChannels;
	}

	public boolean isSubscribable(String channelName) {
		String channelNameToFind = getChannelNameIgnoreCase(channelName);
		return hasChannel(channelNameToFind) && mChannels.get(channelNameToFind).subscribe;
    }

	public boolean isRolePlay(String channelName) {
		String channelNameToFind = getChannelNameIgnoreCase(channelName);
		return hasChannel(channelNameToFind) && mChannels.get(channelNameToFind).isRP;
	}

    public String getChannelSpeakPerm(String channelName) {
		String channelNameToFind = getChannelNameIgnoreCase(channelName);

		if(mChannels.containsKey(channelNameToFind)) {
			return mChannels.get(channelNameToFind).permission;
		}
		return null;
    }

    private String getChannelNameIgnoreCase(String channelName) {
		for (Map.Entry<String, ChatChannel> channel: mChannels.entrySet()) {
			String capitalizedChannelName = channel.getKey();
			if (capitalizedChannelName.equalsIgnoreCase(channelName)) {
				return capitalizedChannelName;
			}
		}
		return channelName;
	}

	public boolean hasChannel(String channelName) {
		String channelNameToFind = getChannelNameIgnoreCase(channelName);
		return mChannels.containsKey(channelNameToFind);
	}

	@Nullable
	public ChatChannel getChatChannel(String channelName) {
		String channelNameToFind = getChannelNameIgnoreCase(channelName);
		return mChannels.getOrDefault(channelNameToFind, null);
	}

}

