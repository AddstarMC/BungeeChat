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

package au.com.addstar.bc;

import java.io.File;
import java.lang.reflect.Field;
import java.util.UUID;

import au.com.addstar.bc.sync.packet.PlayerSettingsPacket;
import net.cubespace.Yamler.Config.YamlConfig;

public class PlayerSettings extends YamlConfig
{
	public PlayerSettings(File file)
	{
		CONFIG_FILE = file;
	}
	
	public int socialSpyState = 2;
	public boolean msgEnabled = true;
	
	public String nickname = "";
	
	public String skin = null;
	
	@NoSave
	public UUID lastMsgTarget = null;
	
	public long muteTime = 0;

	/**
	 * A hex or legacy color code (eg &C) or formatted as per RRGGBB (no leading #) or 0xrrggbb
	 * It can also be a named colour string ie Red
	 */
	@NoSave
	public String tabColor = "";

	@NoSave
	public boolean isAFK = false;

	@NoSave
	public String chatName = "";

	@NoSave
	public String defaultChannel = "";

	@Override
	protected boolean doSkip( Field field )
	{
		return (field.getAnnotation(NoSave.class) != null);
	}
	
	public void read(PlayerSettingsPacket packet)
	{
		lastMsgTarget = packet.getLastMessageTarget();
		nickname = packet.getNickname();
		socialSpyState = packet.getSocialSpyState();
		msgEnabled = packet.getMsgToggle();
		muteTime = packet.getMuteTime();
		isAFK = packet.getAFK();
		chatName = packet.getChatName();
		defaultChannel = packet.getDefaultChannel();
	}
	
	public PlayerSettingsPacket getUpdatePacket(UUID id)
	{
		return new PlayerSettingsPacket(id, nickname, lastMsgTarget, socialSpyState, msgEnabled, muteTime, isAFK, chatName,defaultChannel);
	}
}
