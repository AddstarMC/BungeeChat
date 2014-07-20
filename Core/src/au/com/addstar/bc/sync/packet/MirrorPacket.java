package au.com.addstar.bc.sync.packet;

import au.com.addstar.bc.sync.Packet;
import au.com.addstar.bc.sync.PacketSchema;

public class MirrorPacket extends Packet
{
	public static final PacketSchema schema = PacketSchema.from("channel=String,message=String");
	
	private static final int CHANNEL = 0;
	private static final int MESSAGE = 1;
	
	public MirrorPacket(String channel, String message)
	{
		super(channel, message);
	}
	
	protected MirrorPacket(Object... data)
	{
		super(data);
	}
	
	public String getChannel()
	{
		return (String)getData(CHANNEL);
	}
	
	public String getMessage()
	{
		return (String)getData(MESSAGE);
	}
}
