package au.com.addstar.bc.config;

import java.util.HashMap;
import java.util.Map;

import au.com.addstar.bc.sync.SyncSerializable;

public class PermissionSetting extends net.cubespace.Yamler.Config.Config implements SyncSerializable
{
	public PermissionSetting() {}
	public PermissionSetting(String format, String color, int priority)
	{
		this.format = format;
		this.color = color;
		this.priority = priority;
	}
	
	public String format;
	public String color;
	public int priority;
	public String permission;
	
	@Override
	public String toString()
	{
		return "Permission Setting: " + String.format("perm:%s pri:%d col:%s fmt:%s", permission, priority, color, format);
	}
	
	@Override
	public Map<String, Object> toMap()
	{
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("fmt", format);
		map.put("c", color);
		map.put("pri", priority);
		map.put("perm", permission);
		
		return map;
	}
}
