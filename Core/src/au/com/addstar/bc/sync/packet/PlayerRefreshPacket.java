package au.com.addstar.bc.sync.packet;

import java.util.UUID;

import au.com.addstar.bc.sync.Packet;
import au.com.addstar.bc.sync.PacketSchema;

public class PlayerRefreshPacket extends Packet
{
	public static final PacketSchema schema = PacketSchema.from("id=UUID");
	
	public PlayerRefreshPacket(UUID id)
	{
		super(id);
	}
	
	protected PlayerRefreshPacket(Object[] data)
	{
		super(data);
	}
	
	public UUID getID()
	{
		return (UUID)getData(0);
	}
}
