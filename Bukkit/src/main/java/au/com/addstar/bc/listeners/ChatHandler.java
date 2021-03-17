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
import au.com.addstar.bc.objects.ChannelType;
import au.com.addstar.bc.objects.ChatChannel;
import au.com.addstar.bc.objects.Formatter;
import au.com.addstar.bc.PermissionSetting;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.TabCompleteEvent;

import au.com.addstar.bc.event.ChatChannelEvent;
import au.com.addstar.bc.utils.Utilities;
import org.bukkit.permissions.Permissible;

public class ChatHandler implements Listener{

	private BungeeChat instance;

	public ChatHandler(BungeeChat instance) {
		this.instance = instance;
	}

	@EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
	private void onPlayerChatLowest(AsyncChatEvent event)
	{
        String channel = BungeeChat.getPlayerManager().getDefaultChatChannel(event.getPlayer());
        if(!channel.isEmpty()){
            if(instance.getChatChannelsManager().hasChannel(channel)){
                if(BungeeChat.forceGlobalprefix.equals(PlainComponentSerializer.plain().serialize(event.message()).substring(0,1))){
                    event.message(event.message().replaceText(builder -> builder.match("!").replacement("").once()));
                }else{
                	ChatChannel out = instance.getChatChannelsManager().getChatChannel(channel);
                	if(out != null) {
                		out.say(event.getPlayer(),event.message());
						event.setCancelled(true);
						//ChatChannelManager.callBungeeChatChannelEvent(event.getPlayer(),new HashSet<>(event.getRecipients()),Utilities.colorize(event.getMessage(), event.getPlayer()),out);
						return;
					}
                }
            }else{
                instance.getLogger().info("Channel Manager did not have the default channel...." + channel);
            }
		}
		PermissionSetting level = Formatter.getPermissionLevel(event.getPlayer());
		event.formatter(Formatter.getChatFormatForUse(event.getPlayer(), level));
		String plain = PlainComponentSerializer.plain().serialize(event.message());
		if(plain.trim().isEmpty())
			event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGHEST,ignoreCancelled = true)
	void onPlayerChatHighest(AsyncChatEvent event) {

		if(!Formatter.keywordsEnabled)
			return;

		Component newMessage = Formatter.highlightKeywords(event.message());
		if(newMessage == null)
			return;

		for(Permissible permissible : Bukkit.getPluginManager().getPermissionSubscriptions(Formatter.keywordPerm))
		{
			if(!(permissible instanceof Player) && permissible.hasPermission(Formatter.keywordPerm)) {
				continue;
			}
			event.recipients().remove(permissible);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR,ignoreCancelled = true)
	void onPlayerChatFinal(AsyncChatEvent event){
		if(!Formatter.keywordsEnabled)
			return;
		Component newMessage = Formatter.highlightKeywords(event.message());
		if(newMessage == null) {
			BungeeChat.mirrorChat(event.message(), ChannelType.KeywordHighlight.getName());
		} else {
			newMessage = event.formatter().chat(event.getPlayer().displayName(),newMessage);
			Utilities.localBroadCast(newMessage,Formatter.keywordPerm,Utilities.NO_CONSOLE);
			BungeeChat.mirrorChat(newMessage,ChannelType.KeywordHighlight.getName());
		}
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
