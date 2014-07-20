package au.com.addstar.bc.sync.packet;

import au.com.addstar.bc.sync.Packet;
import au.com.addstar.bc.sync.PacketSchema;

public class GlobalMutePacket extends Packet
{
	public static final PacketSchema schema = PacketSchema.from("time=Long");
	
	public GlobalMutePacket(long time)
	{
		super(time);
	}
	
	public long getTime()
	{
		return (Long)getData(0);
	}
}
