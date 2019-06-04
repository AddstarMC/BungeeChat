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

import java.util.List;
import java.util.UUID;

import au.com.addstar.bc.sync.Packet;
import au.com.addstar.bc.sync.PacketSchema;

@SuppressWarnings( "unchecked" )
public class PlayerListPacket extends Packet
{
	public static final PacketSchema schema = PacketSchema.from("ids=List<UUID>,names=List<String>,nicknames=List<String>");
	
	public PlayerListPacket(List<UUID> ids, List<String> names, List<String> nicknames)
	{
		super(ids, names, nicknames);
	}
	
	protected PlayerListPacket(Object[] data)
	{
		super(data);
	}
	
	public List<UUID> getIDs()
	{
		return (List<UUID>)getData(0);
	}
	
	public List<String> getNames()
	{
		return (List<String>)getData(1);
	}
	
	public List<String> getNicknames()
	{
		return (List<String>)getData(2);
	}
}
