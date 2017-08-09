package au.com.addstar.bc.sync.packet;

import java.util.UUID;

import au.com.addstar.bc.sync.Packet;
import au.com.addstar.bc.sync.PacketSchema;

public class AFKPacket extends Packet
{
	public static final PacketSchema schema = PacketSchema.from("id=UUID,afk=Boolean");
	
	public AFKPacket(UUID id, boolean afk)
	{
		super(id, afk);
	}
	
	protected AFKPacket(Object[] data)
	{
		super(data);
	}
	
	public UUID getID()
	{
		return (UUID)getData(0);
	}
	
	public boolean getAFK()
	{
		return (Boolean)getData(1);
	}
}
