package au.com.addstar.bc.sync.packet;

import au.com.addstar.bc.sync.Packet;
import au.com.addstar.bc.sync.PacketSchema;

public class CallSuccessResponsePacket extends Packet
{
	public static final PacketSchema schema = PacketSchema.from("id=Integer,data=Object");
	
	public CallSuccessResponsePacket(int id, Object data)
	{
		super(id, data);
	}
	
	public int getId()
	{
		return (Integer)getData(0);
	}
	
	public Object getResult()
	{
		return getData(1);
	}
}
