package au.com.addstar.bc.sync;

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
