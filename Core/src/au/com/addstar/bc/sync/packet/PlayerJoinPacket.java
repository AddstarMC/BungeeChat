package au.com.addstar.bc.sync.packet;

import java.util.UUID;

import au.com.addstar.bc.sync.Packet;
import au.com.addstar.bc.sync.PacketSchema;

public class PlayerJoinPacket extends Packet
{
	public static final PacketSchema schema = PacketSchema.from("id=UUID,name=String,nickname=String");
	
	public PlayerJoinPacket(UUID id, String name, String nickname)
	{
		super(id, name, nickname);
	}
	
	protected PlayerJoinPacket(Object[] data)
	{
		super(data);
	}
	
	public UUID getID()
	{
		return (UUID)getData(0);
	}
	
	public String getName()
	{
		return (String)getData(1);
	}
	
	public String getNickname()
	{
		return (String)getData(2);
	}
}
