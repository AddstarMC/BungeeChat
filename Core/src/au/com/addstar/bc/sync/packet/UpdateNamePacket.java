package au.com.addstar.bc.sync.packet;

import java.util.UUID;

import au.com.addstar.bc.sync.Packet;
import au.com.addstar.bc.sync.PacketSchema;

public class UpdateNamePacket extends Packet
{
	public static final PacketSchema schema = PacketSchema.from("id=UUID,name=String");
	
	public UpdateNamePacket(UUID id, String name)
	{
		super(id,name);
	}
	
	public UUID getID()
	{
		return (UUID)getData(0);
	}
	
	public String getName()
	{
		return (String)getData(1);
	}
}
