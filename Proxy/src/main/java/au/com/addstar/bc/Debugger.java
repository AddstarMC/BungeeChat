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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.protocol.packet.PlayerListItem;
import net.md_5.bungee.protocol.packet.PlayerListItem.Action;
import net.md_5.bungee.protocol.packet.PlayerListItem.Item;

public class Debugger extends Command
{
	private static boolean mDebugEnabled = false;
	private static boolean mPacketDebugEnabled = false;
	private static boolean mTabDebugEnabled = false;
	
	public static void setGeneralDebugState(boolean on)
	{
		mDebugEnabled = on;
	}
	
	public static void setPacketDebugState(boolean on)
	{
		mPacketDebugEnabled = on;
	}
	
	public static void setTabDebugState(boolean on)
	{
		mTabDebugEnabled = on;
	}
	
	private static void log0(String category, String message)
	{
		if (category == null || category.isEmpty())
			BungeeChat.instance.getLogger().info(String.format("[Debug] %s", message));
		else
			BungeeChat.instance.getLogger().info(String.format("[Debug-%s] %s", category, message));
	}
	
	/**
	 * General logging
	 */
	public static void log(String message, Object... params)
	{
		if (mDebugEnabled)
			log0(null, String.format(message, params));
	}
	
	/**
	 * Packet logging
	 */
	public static void logp(String message, Object... params)
	{
		if (mPacketDebugEnabled)
			log0("Packet", String.format(message, params));
	}
	
	/**
	 * TabList logging
	 */
	public static void logt(String message, Object... params)
	{
		if (mTabDebugEnabled)
			log0("TabList", LegacyComponentSerializer.legacyAmpersand().serialize(
				LegacyComponentSerializer.legacySection().deserialize(String.format(message, params))));
	}
	
	public static void logTabItem(PlayerListItem packet, ProxiedPlayer to)
	{
		if (!mTabDebugEnabled)
			return;
		
		boolean newTab = ColourTabList.isNewTab(to);
		for (Item item : packet.getItems())
		{
			String name = item.getUsername();
			if (!newTab)
				name = BaseComponent.toLegacyText(item.getDisplayName());
			
			String message = null;
			switch(packet.getAction())
			{
			case ADD_PLAYER:
				message = String.format("%d,%d,%s", item.getPing(), item.getGamemode(), BaseComponent.toLegacyText(item.getDisplayName()));
				break;
			case REMOVE_PLAYER:
				message = item.getUuid().toString();
				break;
			case UPDATE_DISPLAY_NAME:
				message = BaseComponent.toLegacyText(item.getDisplayName());
				break;
			case UPDATE_GAMEMODE:
				message = String.valueOf(item.getGamemode());
				break;
			case UPDATE_LATENCY:
				message = String.valueOf(item.getPing());
				break;
			}
			
			if (packet.getAction() == Action.ADD_PLAYER)
				message = String.format("%s %s-%s: %s", packet.getAction().name(), name, item.getUuid().toString(), message);
			else if (name == null)
				message = String.format("%s %s: %s", packet.getAction().name(), item.getUuid().toString(), message);
			else
				message = String.format("%s %s: %s", packet.getAction().name(), name, message);
			
			log0("TabList", LegacyComponentSerializer.legacyAmpersand().serialize(LegacyComponentSerializer.legacySection().deserialize(String.format("to %s: %s", to.getName(), message))));
		}
	}
	
	public static void logTrue(boolean expression, String message, Object... params)
	{
		if (!expression)
			log(message, params);
	}
	
	public Debugger()
	{
		super("!bchatdebug", "bungeechat.debug");
	}
	
	@Override
	public void execute( CommandSender sender, String[] args )
	{
		if (!onCommand(sender, args))
		{
			TextComponent message = Component.text().color(NamedTextColor.RED)
				.append(Component.text("/!bchatdebug general <true|false>"))
				.append(Component.newline())
				.append(Component.text("/!bchatdebug packet <true|false>"))
				.append(Component.newline())
				.append(Component.text("/!bchatdebug tab <true|false>"))
				.append(Component.newline())
				.append(Component.text("/!bchatdebug player <name>"))
				.append(Component.newline())
				.append(Component.text("/!bchatdebug allplayers"))
				.append(Component.newline())
				.build();
			BungeeChat.audiences.sender(sender).sendMessage(message);
		}
	}
	
	public boolean onCommand( CommandSender sender, String[] args )
	{
		if (args.length == 0)
			return false;
		
		if (args[0].equalsIgnoreCase("general"))
		{
			if (args.length != 2)
				return false;
			
			boolean on = Boolean.parseBoolean(args[1]);
			setGeneralDebugState(on);
			TextComponent message = Component.text("General debug is now " + (on ? "on" : "off")).color(NamedTextColor.GOLD);
			BungeeChat.audiences.sender(sender).sendMessage(message);

		}
		else if (args[0].equalsIgnoreCase("packet"))
		{
			if (args.length != 2)
				return false;
			
			boolean on = Boolean.parseBoolean(args[1]);
			setPacketDebugState(on);
			TextComponent message = Component.text( "Packet debug is now " + (on ? "on" : "off")).color(NamedTextColor.GOLD);
			BungeeChat.audiences.sender(sender).sendMessage(message);
		}
		else if (args[0].equalsIgnoreCase("tab"))
		{
			if (args.length != 2)
				return false;
			
			boolean on = Boolean.parseBoolean(args[1]);
			setTabDebugState(on);
			net.kyori.adventure.text.TextComponent message = net.kyori.adventure.text.Component.text( "TabList debug is now " + (on ? "on" : "off")).color(NamedTextColor.GOLD);
			BungeeChat.audiences.sender(sender).sendMessage(message);
		}
		else
			return false;
		
		return true;
	}
}
