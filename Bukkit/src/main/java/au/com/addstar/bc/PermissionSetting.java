package au.com.addstar.bc;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

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
			catch(IllegalArgumentException e)
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
			if(col != null)
				this.color += col.toString();
		}
	}
	
	public String format;
	public String color;
	public int priority;
	public String permission;
	
	@Override
	public int compareTo( PermissionSetting o )
	{
		return Integer.valueOf(priority).compareTo(o.priority);
	}
}
