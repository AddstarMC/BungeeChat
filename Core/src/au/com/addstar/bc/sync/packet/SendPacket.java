package au.com.addstar.bc.sync.packet;

import java.util.UUID;

import au.com.addstar.bc.sync.Packet;
import au.com.addstar.bc.sync.PacketSchema;

public class SendPacket extends Packet
{
	public static final PacketSchema schema = PacketSchema.from("id=UUID,message=String");
	
	private static final int ID = 0;
	private static final int MESSAGE = 1;
	
	public SendPacket(UUID id, String message)
	{
		super(id, message);
	}
	
	public UUID getUUID()
	{
		return (UUID)getData(ID);
	}
	
	public String getMessage()
	{
		return (String)getData(MESSAGE);
	}
}
