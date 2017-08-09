package au.com.addstar.bc.config;

import java.util.HashMap;
import java.util.Map;

import au.com.addstar.bc.sync.SyncSerializable;

public class KeywordHighlighterConfig implements SyncSerializable
{
	public String permission = "bungeechat.keywords";
	public boolean enabled = false;
	
	@Override
	public Map<String, Object> toMap()
	{
		HashMap<String, Object> map = new HashMap<>();
		map.put("perm", permission);
		map.put("enabled", enabled);
		
		return map;
	}
	
	public static KeywordHighlighterConfig fromMap(Map<String, Object> map)
	{
		KeywordHighlighterConfig settings = new KeywordHighlighterConfig();
		settings.permission = (String)map.get("perm");
		settings.enabled = (Boolean)map.get("enabled");
		
		return settings;
	}
}
