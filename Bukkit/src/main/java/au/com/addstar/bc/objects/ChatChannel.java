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

package au.com.addstar.bc.objects;

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

import au.com.addstar.bc.BungeeChat;
import au.com.addstar.bc.PermissionSetting;
import au.com.addstar.bc.utils.Utilities;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

public class ChatChannel
{
	public String name;
	public String format;
	public String command;
	public String permission;
	public String listenPermission;
	public boolean subscribe;
	public boolean isRP;
	private final static String version = Bukkit.getBukkitVersion();


	public ChatChannel(String name, String command, String format, String permission, String listenPerm)
	{
		this(name, command,format,permission,listenPerm,false,false);
	}
	public ChatChannel(String name, String command, String format, String permission, String listenPerm, Boolean subscribe, Boolean rp)
	{
		this.name = name;
		this.command = command;
		this.format = Utilities.parseChatColors(format, version);
		if(permission != null && !permission.isEmpty())
			this.permission = permission;
		if(permission != null && !listenPerm.isEmpty())
			this.listenPermission = listenPerm;
		this.subscribe = subscribe != null && subscribe;
        isRP = rp != null && rp;

	}


	
	public void registerChannel()
	{
		try
		{
			if(listenPermission != null)
				Bukkit.getPluginManager().addPermission(new Permission(listenPermission, PermissionDefault.OP));
		}
		catch(IllegalArgumentException ignored) {}
		
		try
		{
			if(permission != null)
				Bukkit.getPluginManager().addPermission(new Permission(permission, PermissionDefault.OP));
		}
		catch(IllegalArgumentException ignored) {}
	}
	
	public void unregisterChannel()
	{
		if(listenPermission != null)
			Bukkit.getPluginManager().removePermission(listenPermission);
		
		if(permission != null)
			Bukkit.getPluginManager().removePermission(permission);
	}
	
	public void say(CommandSender sender, String message)
	{
		PermissionSetting level = Formatter.getPermissionLevel(sender);
		
		message = Utilities.colorize(message, sender);
		if (ChatColor.stripColor(message).trim().isEmpty())
			return;
		
		String newFormat = Formatter.replaceKeywordsPartial(format, sender, level);
		String finalMessage = String.format(newFormat, message);
		
		if(listenPermission != null)
			Bukkit.broadcast(finalMessage, listenPermission);
		else
			Bukkit.broadcastMessage(finalMessage);
		BungeeChat.mirrorChat(finalMessage, name);
	}
}
