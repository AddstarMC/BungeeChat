package au.com.addstar.bc.sync.packet;

import au.com.addstar.bc.sync.Packet;
import au.com.addstar.bc.sync.PacketSchema;

public class ConfigRequestPacket extends Packet
{
	public static final PacketSchema schema = PacketSchema.from("name=String");
	
	public ConfigRequestPacket(String name)
	{
		super(name);
	}
	
	protected ConfigRequestPacket(Object[] data)
	{
		super(data);
	}
	
	public String getName()
	{
		return (String)getData(0);
	}
}
