package au.com.addstar.bc.sync.packet;

import au.com.addstar.bc.sync.Packet;
import au.com.addstar.bc.sync.PacketSchema;

public class PlayerListRequestPacket extends Packet
{
	public static final PacketSchema schema = PacketSchema.from("");
	
	public PlayerListRequestPacket()
	{
	}
	
	protected PlayerListRequestPacket(Object[] data)
	{
		super(data);
	}
}
