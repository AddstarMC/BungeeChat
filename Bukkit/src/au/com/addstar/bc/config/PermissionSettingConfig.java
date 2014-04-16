package au.com.addstar.bc.config;

import java.util.HashMap;
import java.util.Map;

import au.com.addstar.bc.PermissionSetting;
import au.com.addstar.bc.sync.SyncSerializable;

public class PermissionSettingConfig implements SyncSerializable
{
	public String format;
	public String color;
	public int priority;
	public String permission;
	
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
	
	public static PermissionSettingConfig fromMap(Map<String, Object> map)
	{
		PermissionSettingConfig setting = new PermissionSettingConfig();
		setting.format = (String)map.get("fmt");
		setting.color = (String)map.get("c");
		setting.permission = (String)map.get("perm");
		setting.priority = (Integer)map.get("pri");
		
		return setting;
	}
	
	public PermissionSetting convert()
	{
		return new PermissionSetting(permission, priority, format, color);
	}
}
