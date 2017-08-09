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
	public boolean subscribe = false;
	public boolean isRp = false;
	
	public ChatChannel() {}
	public ChatChannel(String cmd, String prefix, String perm, String listenPerm){
		this(cmd,prefix, perm, listenPerm, false, false);
	}
	public ChatChannel(String cmd, String prefix, String perm, String listenPerm,Boolean subscribe, boolean isRP)
	{
		this.command = cmd;
		this.format = prefix;
		this.permission = perm;
		this.listenPermission = listenPerm;
		this.subscribe = subscribe;
		this.isRp = isRP;
	}
	
	@Override
	public Map<String, Object> toMap()
	{
		HashMap<String, Object> map = new HashMap<>();
		
		map.put("cmd", command);
		map.put("fmt", format);
		map.put("perm", permission);
		map.put("lperm", listenPermission);
		map.put("sub", subscribe);
		map.put("rp",isRp);
		
		return map;
	}
}
