package au.com.addstar.bc.sync.packet;

import java.util.UUID;

import au.com.addstar.bc.sync.Packet;
import au.com.addstar.bc.sync.PacketSchema;

public class QuitMessagePacket extends Packet
{
	public static final PacketSchema schema = PacketSchema.from("id=UUID,message=String");
	
	public QuitMessagePacket(UUID id, String message)
	{
		super(id, message);
	}
	
	protected QuitMessagePacket(Object[] data)
	{
		super(data);
	}
	
	public UUID getID()
	{
		return (UUID)getData(0);
	}
	
	public String getMessage()
	{
		return (String)getData(1);
	}
}
