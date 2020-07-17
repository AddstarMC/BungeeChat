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

import au.com.addstar.bc.BungeeChat;
import au.com.addstar.bc.ChatChannelManager;
import au.com.addstar.bc.objects.ChatChannel;
import au.com.addstar.bc.objects.Formatter;
import au.com.addstar.bc.PermissionSetting;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.server.TabCompleteEvent;

import au.com.addstar.bc.event.ChatChannelEvent;
import au.com.addstar.bc.utils.Utilities;

import java.util.HashSet;

public class ChatHandler implements Listener{

	private BungeeChat instance;

	public ChatHandler(BungeeChat instance) {
		this.instance = instance;
	}

	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
	private void onPlayerChat(AsyncPlayerChatEvent event)
	{
        String channel = BungeeChat.getPlayerManager().getDefaultChatChannel(event.getPlayer());

        if(!channel.isEmpty()){
            if(instance.getChatChannelsManager().hasChannel(channel)){
                if(BungeeChat.forceGlobalprefix.equals(event.getMessage().substring(0,1))){
                    event.setMessage(event.getMessage().substring(1));
                }else{
                	ChatChannel out = instance.getChatChannelsManager().getChatChannel(channel);
                	if(out != null) {
						event.setCancelled(true);
						ChatChannelManager.callBungeeChatChannelEvent(event.getPlayer(),new HashSet<>(event.getRecipients()),Utilities.colorize(event.getMessage(), event.getPlayer()),out);
						return;
					}
                }
            }else{
                instance.getLogger().info("Channel Manager did not have the default channel...." + channel);
            }
        }
        //this is now a global chat message.

		PermissionSetting level = Formatter.getPermissionLevel(event.getPlayer());
		Component format = Formatter.getChatFormatForUse(event.getPlayer(),level);
		Component mess = Utilities.colorize(event.getMessage(),event.getPlayer());
		ChatChannelManager.callBungeeChatEvent(event.getPlayer(),new HashSet<>(event.getRecipients()),format,mess);
		event.getRecipients().clear();
	}

	@EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
	private void onTabComplete(TabCompleteEvent event)
	{
		event.getCompletions().addAll(BungeeChat.getPlayerManager().matchNames(event.getBuffer().toLowerCase()));
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	private void onMirrorChatChannel(ChatChannelEvent event)
	{
		switch(event.getChannelType())
		{
		case Default:
			Formatter.localBroadcast(event.getMessage());
			break;
		case KeywordHighlight:
			if(Formatter.keywordsEnabled)
				Utilities.localBroadCast(event.getMessage(),Formatter.keywordPerm);
			break;
		case Broadcast:
			Utilities.localBroadCast(event.getMessage(), (String) null);
			break;
		case AFKKick:
			Utilities.localBroadCast(event.getMessage(), "bungeechat.afk.kick.notify");
			break;
		default:
			break;
		}
	}
}
