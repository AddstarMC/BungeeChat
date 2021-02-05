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

import java.util.HashSet;
import java.util.List;

import au.com.addstar.bc.objects.ChannelType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;

import au.com.addstar.bc.event.ChatChannelEvent;
import au.com.addstar.bc.sync.SyncConfig;
import au.com.addstar.bc.utils.Utilities;

public class SocialSpyHandler implements Listener, CommandExecutor
{
	private HashSet<String> mKeywords = new HashSet<>();
	
	public SocialSpyHandler(Plugin plugin)
	{
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	private void onChatChannel(ChatChannelEvent event)
	{
		if(event.getChannelType() == ChannelType.SocialSpy)
		{
			Utilities.broadcast(MiniMessage.get().parse(event.getMessage()), "bungeechat.socialspy", Utilities.SOCIAL_SPY_ENABLED);
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	private void onPlayerCommand(PlayerCommandPreprocessEvent event)
	{
		String command = event.getMessage().split(" ")[0].substring(1);
		
		if(mKeywords.contains(command.toLowerCase()))
		{
			Component message = TextComponent.of(event.getPlayer().getName() + ": ")
				.append(MiniMessage.get().parse(event.getMessage()));
			Utilities.broadcast(message, "bungeechat.socialspy", event.getPlayer(), Utilities.SOCIAL_SPY_ENABLED);
			BungeeChat.mirrorChat(message, ChannelType.SocialSpy.getName());
		}
	}
	
	public void clearKeywords()
	{
		mKeywords.clear();
	}
	
	public void addKeyword(String keyword)
	{
		mKeywords.add(keyword);
	}

	public void setStatus(CommandSender player, boolean on)
	{
		BungeeChat.getPlayerManager().getPlayerSettings(player).socialSpyState = (on ? 1 : 0);
		BungeeChat.getPlayerManager().updatePlayerSettings(player);
	}
	
	public void clearStatus(CommandSender player)
	{
		BungeeChat.getPlayerManager().getPlayerSettings(player).socialSpyState = 2;
		BungeeChat.getPlayerManager().updatePlayerSettings(player);
	}
	
	public boolean isEnabled(CommandSender player)
	{
		int state = BungeeChat.getPlayerManager().getPlayerSettings(player).socialSpyState;
		
		if(state == 2)
			return player.hasPermission("bungeechat.socialspy");
		
		return (state == 1);
	}
	
	@Override
	public boolean onCommand( CommandSender sender, Command command, String label, String[] args )
	{
		if(!(sender instanceof Player))
			return false;
		
		boolean on = isEnabled(sender);
		on = !on;
		
		if(on)
			BungeeChat.audiences.sender(sender).sendMessage(TextComponent.of("SocialSpy now on")
				.color(NamedTextColor.GREEN));
		else
			BungeeChat.audiences.sender(sender).sendMessage(TextComponent.of("SocialSpy now off")
				.color(NamedTextColor.GREEN));
		setStatus(sender, on);
		
		return true;
	}
	
	@SuppressWarnings( "unchecked" )
	public void load(SyncConfig config)
	{
		clearKeywords();
		mKeywords.addAll((List<String>)config.get("socialspykeywords", null));
	}
}
