package au.com.addstar.bc.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import au.com.addstar.bc.sync.SyncSerializable;

import net.cubespace.Yamler.Config.Config;

public class KeywordHighlighterSettings extends Config implements SyncSerializable
{
	public String permission = "bungeechat.keywords";
	public boolean enabled = false;
	public List<String> allowInChannels = new ArrayList<>();
	public String keywordFile = "keywords.txt";
	
	@Override
	public Map<String, Object> toMap()
	{
		HashMap<String, Object> map = new HashMap<>();
		map.put("perm", permission);
		map.put("enabled", enabled);
		
		return map;
	}
}
