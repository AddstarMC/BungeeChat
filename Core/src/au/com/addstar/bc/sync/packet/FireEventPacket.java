package au.com.addstar.bc.sync.packet;

import java.util.UUID;

import au.com.addstar.bc.sync.Packet;
import au.com.addstar.bc.sync.PacketSchema;

public class FireEventPacket extends Packet
{
	public static final PacketSchema schema = PacketSchema.from("id=Byte,id=UUID,message=String");
	
	public static final int EVENT_JOIN = 0;
	public static final int EVENT_QUIT = 1;
	
	public FireEventPacket(int event, UUID id, String message)
	{
		super((byte)event, id, message);
	}
	
	public int getEvent()
	{
		return ((Byte)getData(0)).intValue();
	}
	
	public UUID getID()
	{
		return (UUID)getData(1);
	}
	
	public String getMessage()
	{
		return (String)getData(2);
	}
}
