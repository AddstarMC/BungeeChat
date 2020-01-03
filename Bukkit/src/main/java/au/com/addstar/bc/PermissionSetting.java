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

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

public class PermissionSetting implements Comparable<PermissionSetting>
{
	public PermissionSetting(String permission, int priority, String format, String color)
	{
		if(permission != null && !permission.isEmpty())
		{
			this.permission = permission;
			try
			{
				Bukkit.getPluginManager().addPermission(new Permission(permission, PermissionDefault.FALSE));
			}
			catch(IllegalArgumentException ignored)
			{
			}
		}
		
		this.format = ChatColor.translateAlternateColorCodes('&', format);
		this.priority = priority;
		
		this.color = "";
		for(int i = 0; i < color.length(); ++i)
		{
			char c = color.charAt(i);
			ChatColor col = ChatColor.getByChar(c);
			if(col != null) {
				this.color += col.toString();
			}
		}
	}
	
	public String format;
	public String color;
	public int priority;
	public String permission;
	
	@Override
	public int compareTo(@NotNull PermissionSetting o )
	{
		return Integer.compare(priority, o.priority);
	}
}
