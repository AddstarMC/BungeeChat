/*
 * BungeeChat
 *
 * Copyright (c) 2015 - 2020.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy   of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is *
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR  IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
 * EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
