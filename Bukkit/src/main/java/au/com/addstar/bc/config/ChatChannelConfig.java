package au.com.addstar.bc.config;

import java.util.Map;

import au.com.addstar.bc.sync.SyncSerializable;

import static java.lang.Boolean.FALSE;

public class ChatChannelConfig implements SyncSerializable
{
	public String command;
	public String format;
	public String permission;
	public String listenPermission;
	public Boolean subscribe;
	public Boolean isRP;
	
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
		conf.subscribe = (map.containsKey("sub")||map.get("sub") == null)?(Boolean)map.get("sub"): FALSE;
		conf.isRP =(map.containsKey("rp")||map.get("rp") == null)?(Boolean)map.get("rp"):FALSE;
		
		return conf;
	}
}
