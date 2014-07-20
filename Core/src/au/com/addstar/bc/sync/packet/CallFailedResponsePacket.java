package au.com.addstar.bc.sync.packet;

import au.com.addstar.bc.sync.Packet;
import au.com.addstar.bc.sync.PacketSchema;

public class CallFailedResponsePacket extends Packet
{
	public static final PacketSchema schema = PacketSchema.from("id=Integer,errorName=String,errorMessage=String");
	
	public CallFailedResponsePacket(int id, String errorName, String errorMessage)
	{
		super(id, errorName, errorMessage);
	}
	
	protected CallFailedResponsePacket(Object[] data)
	{
		super(data);
	}
	
	public int getId()
	{
		return (Integer)getData(0);
	}
	
	public String getErrorName()
	{
		return (String)getData(1);
	}
	
	public String getErrorMessage()
	{
		return (String)getData(2);
	}
}
