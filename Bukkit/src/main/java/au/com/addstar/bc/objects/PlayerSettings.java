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

package au.com.addstar.bc.objects;

/*-
 * #%L
 * BungeeChat-Bukkit
 * %%
 * Copyright (C) 2015 - 2020 AddstarMC
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.util.UUID;

import au.com.addstar.bc.BungeeChat;
import au.com.addstar.bc.utils.Utilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.command.CommandSender;

import au.com.addstar.bc.sync.packet.PlayerSettingsPacket;

public class PlayerSettings
{
	public int socialSpyState = 2;
	public boolean msgEnabled = true;
	public UUID lastMsgTarget = null;
	
	public String nickname = "";
	
	public long muteTime = 0;
	
	public String tabFormat = "";
	
	public long lastActiveTime = Long.MAX_VALUE;
	public long afkStartTime = 0;
	
	public boolean isAFK = false;

	public Component chatName = TextComponent.empty();

	public String defaultChannel = "";
	
	public CommandSender getLastMsgTarget()
	{
		return BungeeChat.getPlayerManager().getPlayer(lastMsgTarget);
	}
	
	public void read(PlayerSettingsPacket packet)
	{
		lastMsgTarget = packet.getLastMessageTarget();
		nickname = packet.getNickname();
		socialSpyState = packet.getSocialSpyState();
		msgEnabled = packet.getMsgToggle();
		muteTime = packet.getMuteTime();
		isAFK = packet.getAFK();
		chatName = Utilities.SERIALIZER.deserialize(packet.getChatName());
		defaultChannel = packet.getDefaultChannel();
	}
	
	public PlayerSettingsPacket toPacket(UUID id)
	{
		return new PlayerSettingsPacket(id, nickname, lastMsgTarget, socialSpyState, msgEnabled, muteTime, isAFK, Utilities.SERIALIZER.serialize(chatName), defaultChannel);
	}
}
