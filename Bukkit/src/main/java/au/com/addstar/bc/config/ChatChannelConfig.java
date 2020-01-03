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
