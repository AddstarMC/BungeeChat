package au.com.addstar.bc.sync.packet;

import java.util.UUID;

import au.com.addstar.bc.sync.Packet;
import au.com.addstar.bc.sync.PacketSchema;

public class PlayerLeavePacket extends Packet
{
	public static final PacketSchema schema = PacketSchema.from("id=UUID");
	
	public PlayerLeavePacket(UUID id)
	{
		super(id);
	}
	
	protected PlayerLeavePacket(Object[] data)
	{
		super(data);
	}
	
	public UUID getID()
	{
		return (UUID)getData(0);
	}
}
