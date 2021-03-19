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
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

public class Debugger extends Command
{
	private static boolean mDebugEnabled = false;
	private static boolean mPacketDebugEnabled = false;

	public static void setGeneralDebugState(boolean on)
	{
		mDebugEnabled = on;
	}
	
	public static void setPacketDebugState(boolean on)
	{
		mPacketDebugEnabled = on;
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


	
	public Debugger()
	{
		super("!bchatdebug", "bungeechat.debug");
	}
	
	@Override
	public void execute( CommandSender sender, String[] args )
	{
		if (!onCommand(sender, args))
		{
			BungeeChat.audience.sender(sender).sendMessage(Component.text("/!bchatdebug general <true|false>"));
			BungeeChat.audience.sender(sender).sendMessage(Component.text("/!bchatdebug packet <true|false>"));
			BungeeChat.audience.sender(sender).sendMessage(Component.text("/!bchatdebug tab <true|false>"));
			BungeeChat.audience.sender(sender).sendMessage(Component.text("/!bchatdebug player <name>"));
			BungeeChat.audience.sender(sender).sendMessage(Component.text("/!bchatdebug allplayers"));
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
			BungeeChat.audience.sender(sender).sendMessage(Component.text("General debug is now " + (on ? "on" : "off")).color(NamedTextColor.GOLD));
		}
		else if (args[0].equalsIgnoreCase("packet"))
		{
			if (args.length != 2)
				return false;
			
			boolean on = Boolean.parseBoolean(args[1]);
			setPacketDebugState(on);
			BungeeChat.audience.sender(sender).sendMessage(Component.text("Packet debug is now " + (on ? "on" : "off")).color(NamedTextColor.GOLD));
		}
		else
			return false;
		
		return true;
	}
}
