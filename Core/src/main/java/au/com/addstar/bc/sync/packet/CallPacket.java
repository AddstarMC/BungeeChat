package au.com.addstar.bc.sync.packet;

import java.util.Arrays;
import java.util.List;

import au.com.addstar.bc.sync.Packet;
import au.com.addstar.bc.sync.PacketSchema;

public class CallPacket extends Packet
{
	public static final PacketSchema schema = PacketSchema.from("method=String,id=Integer,args=List<Object>");
	
	public CallPacket(String method, int id, Object[] args)
	{
		super(method, id, Arrays.asList(args));
	}
	
	protected CallPacket(Object[] data)
	{
		super(data);
	}
	
	public String getMethod()
	{
		return (String)getData(0);
	}
	
	public int getId()
	{
		return (Integer)getData(1);
	}
	
	@SuppressWarnings( "unchecked" )
	public List<Object> getArgs()
	{
		return (List<Object>)getData(2);
	}
}
