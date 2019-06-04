package au.com.addstar.bc.sync.packet;

/*-
 * #%L
 * BungeeChat-Core
 * %%
 * Copyright (C) 2015 - 2019 AddstarMC
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

import au.com.addstar.bc.sync.Packet;
import au.com.addstar.bc.sync.PacketSchema;

public class PlayerSettingsPacket extends Packet
{
	public static final PacketSchema schema = PacketSchema.from("id=UUID,nickname=String,lmt=UUID,socialspy=Byte,msgtoggle=Boolean,mute=Long,afk=Boolean,roleplayprefix=String,defaultChannel=String");
	
	public PlayerSettingsPacket(UUID id, String nickname, UUID lastMessageTarget, int socialSpyState, boolean msgToggle, long muteTime, boolean afk, String roleplayprefix, String defaultChannel)
	{
		super(id, nickname, lastMessageTarget, (byte)socialSpyState, msgToggle, muteTime, afk, roleplayprefix,defaultChannel);
	}
	
	protected PlayerSettingsPacket(Object[] data)
	{
		super(data);
	}
	
	public UUID getID()
	{
		return (UUID)getData(0);
	}
	
	public String getNickname()
	{
		return (String)getData(1);
	}
	
	public UUID getLastMessageTarget()
	{
		return (UUID)getData(2);
	}
	
	public int getSocialSpyState()
	{
		return ((Byte)getData(3)).intValue();
	}
	
	public boolean getMsgToggle()
	{
		return (Boolean)getData(4);
	}
	
	public long getMuteTime()
	{
		return (Long)getData(5);
	}
	
	public boolean getAFK()
	{
		return (Boolean)getData(6);
	}

	public String getChatName() {
		return (String)getData(7);
	}

	public String getDefaultChannel() {
		return (String) getData(8);
	}
}
