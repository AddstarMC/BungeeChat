package au.com.addstar.bc.config;

import java.util.HashMap;
import java.util.Map;

import au.com.addstar.bc.sync.SyncSerializable;

public class ChatChannel extends net.cubespace.Yamler.Config.Config implements SyncSerializable
{
	public String command;
	public String format;
	public String permission;
	public String listenPermission;
	
	public ChatChannel() {}
	public ChatChannel(String cmd, String prefix, String perm, String listenPerm)
	{
		this.command = cmd;
		this.format = prefix;
		this.permission = perm;
		this.listenPermission = listenPerm;
	}
	
	@Override
	public Map<String, Object> toMap()
	{
		HashMap<String, Object> map = new HashMap<String, Object>();
		
		map.put("cmd", command);
		map.put("fmt", format);
		map.put("perm", permission);
		map.put("lperm", listenPermission);
		
		return map;
	}
}
