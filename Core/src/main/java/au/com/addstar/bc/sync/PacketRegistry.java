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

package au.com.addstar.bc.sync;

/*-
 * #%L
 * BungeeChat-Core
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

import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map.Entry;

import au.com.addstar.bc.sync.packet.*;

public class PacketRegistry
{
	private static HashMap<String, PacketDefinition> mDefinitions = new HashMap<>();
	private static HashMap<Class<? extends Packet>, String> mReverseDef = new HashMap<>();
	
	public static void registerPacket(String type, Class<? extends Packet> packetClass, PacketSchema schema)
	{
		mDefinitions.put(type, new PacketDefinition(packetClass, schema));
		mReverseDef.put(packetClass, type);
	}
	
	public static boolean isCompatible(String type, PacketSchema readSchema)
	{
		PacketDefinition def = mDefinitions.get(type);
		if(def == null)
			return false;
		
		return def.schema.isCompatible(readSchema);
	}
	
	public static Packet make(String type, Object[] data, PacketSchema schema)
	{
		PacketDefinition def = mDefinitions.get(type);
		if(def == null)
			throw new IllegalArgumentException("Unknown packet type " + type);
		
		return def.build(def.schema.translate(data, schema));
	}
	
	public static void write(Packet packet, DataOutput out) throws IOException
	{
		String type = mReverseDef.get(packet.getClass());
		if(type == null)
			throw new IOException("Packet is unregistered: " + packet.getClass().getName());
		out.writeUTF(type);
		PacketDefinition def = mDefinitions.get(type);
		
		def.schema.encode(packet.getData(), out);
	}
	
	public static void writeSchemaPacket(DataOutput out) throws IOException
	{
		out.writeUTF("Schema");
		out.writeByte(mDefinitions.size());
		
		for(Entry<String, PacketDefinition> entry : mDefinitions.entrySet())
		{
			out.writeUTF(entry.getKey());
			out.writeUTF(entry.getValue().schema.toString());
		}
	}
	
	static
	{
		registerPacket("Mirror", MirrorPacket.class, MirrorPacket.schema);
		registerPacket("FEvent", FireEventPacket.class, FireEventPacket.schema);
		registerPacket("GMute", GlobalMutePacket.class, GlobalMutePacket.schema);
		registerPacket("Send", SendPacket.class, SendPacket.schema);
		registerPacket("Pl+", PlayerJoinPacket.class, PlayerJoinPacket.schema);
		registerPacket("Pl-", PlayerLeavePacket.class, PlayerLeavePacket.schema);
		registerPacket("Pl*", PlayerListPacket.class, PlayerListPacket.schema);
		registerPacket("UPl", PlayerListRequestPacket.class, PlayerListRequestPacket.schema);
		registerPacket("Pl", PlayerSettingsPacket.class, PlayerSettingsPacket.schema);
		registerPacket("Name", UpdateNamePacket.class, UpdateNamePacket.schema);
		registerPacket("AFK", AFKPacket.class, AFKPacket.schema);
		registerPacket("R", PlayerRefreshPacket.class, PlayerRefreshPacket.schema);
		
		// Remote method related
		registerPacket("CR-", CallFailedResponsePacket.class, CallFailedResponsePacket.schema);
		registerPacket("CR+", CallSuccessResponsePacket.class, CallSuccessResponsePacket.schema);
		registerPacket("C", CallPacket.class, CallPacket.schema);
		
		registerPacket("Cfg", ConfigPacket.class, ConfigPacket.schema);
		registerPacket("CfgR", ConfigRequestPacket.class, ConfigRequestPacket.schema);
		
	}
	
	private static class PacketDefinition
	{
		public Constructor<? extends Packet> constructor;
		public PacketSchema schema;
		
		public PacketDefinition(Class<? extends Packet> packetClass, PacketSchema schema)
		{
			try
			{
				constructor = packetClass.getDeclaredConstructor(Object[].class);
				constructor.setAccessible(true);
			}
			catch(Exception e)
			{
				throw new IllegalArgumentException("Cannot use " + packetClass.getName() + " as a Packet:", e);
			}
			
			this.schema = schema;
		}
		
		public Packet build(Object[] data)
		{
			try
			{
				return constructor.newInstance(new Object[]{data});
			}
			catch(Exception e)
			{
				throw new IllegalArgumentException(e);
			}
		}
		
	}
}
