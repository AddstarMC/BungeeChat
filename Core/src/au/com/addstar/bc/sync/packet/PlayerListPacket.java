package au.com.addstar.bc.sync.packet;

import java.util.List;
import java.util.UUID;

import au.com.addstar.bc.sync.Packet;
import au.com.addstar.bc.sync.PacketSchema;

@SuppressWarnings( "unchecked" )
public class PlayerListPacket extends Packet
{
	public static final PacketSchema schema = PacketSchema.from("ids=List<UUID>,names=List<String>,nicknames=List<String>");
	
	public PlayerListPacket(List<UUID> ids, List<String> names, List<String> nicknames)
	{
		super(ids, names, nicknames);
	}
	
	public List<UUID> getIDs()
	{
		return (List<UUID>)getData(0);
	}
	
	public List<String> getNames()
	{
		return (List<String>)getData(1);
	}
	
	public List<String> getNicknames()
	{
		return (List<String>)getData(2);
	}
}
