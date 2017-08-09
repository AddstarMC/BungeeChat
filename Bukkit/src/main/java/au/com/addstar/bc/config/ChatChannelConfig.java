package au.com.addstar.bc.config;

import java.util.Map;

import au.com.addstar.bc.sync.SyncSerializable;

public class ChatChannelConfig implements SyncSerializable
{
	public String command;
	public String format;
	public String permission;
	public String listenPermission;
	public boolean subscribe;
	public boolean isRP;
	
	@Override
	public Map<String, Object> toMap()
	{
		// Dont need to serialize it
		return null;
	}

	public static ChatChannelConfig fromMap(Map<String, Object> map)
	{
		ChatChannelConfig conf = new ChatChannelConfig();
		conf.command = (String)map.get("cmd");
		conf.format = (String)map.get("fmt");
		conf.permission = (String)map.get("perm");
		conf.listenPermission = (String)map.get("lperm");
		conf.subscribe = (map.containsKey("sub"))?(Boolean)map.get("sub"):false;
		conf.isRP =(map.containsKey("rp"))?(Boolean)map.get("rp"):false;
		
		return conf;
	}
}
