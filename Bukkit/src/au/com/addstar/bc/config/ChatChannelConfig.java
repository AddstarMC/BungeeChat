package au.com.addstar.bc.config;

import java.util.Map;

import au.com.addstar.bc.sync.SyncSerializable;

public class ChatChannelConfig implements SyncSerializable
{
	public String command;
	public String format;
	public String permission;
	public String listenPermission;
	
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
		
		return conf;
	}
}
