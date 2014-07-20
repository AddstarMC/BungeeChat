package au.com.addstar.bc.sync;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

public class PacketCodec
{
	private HashMap<String, PacketSchema> mReadSchemas = new HashMap<String, PacketSchema>();
	private boolean mEnabled;
	
	public PacketCodec()
	{
		mEnabled = true;
	}
	
	public void write(Packet packet, DataOutput out) throws IOException
	{
		if(mEnabled)
			PacketRegistry.write(packet, out);
	}
	
	public Packet read(DataInput in) throws IOException
	{
		if(!mEnabled)
			return null;
		
		String type = in.readUTF();
		
		PacketSchema schema = mReadSchemas.get(type);
		if(schema == null)
			throw new IOException("Cannot read packet " + type);
		
		Object[] data = schema.decode(in);
		return PacketRegistry.make(type, data, schema);
	}
	
	public void setReadSchema(String type, PacketSchema schema)
	{
		mReadSchemas.put(type, schema);
	}
	
	public void setEnabled(boolean value)
	{
		mEnabled = value;
	}
	
	public void validate()
	{
		for(Entry<String, PacketSchema> entry : mReadSchemas.entrySet())
		{
			if(!PacketRegistry.isCompatible(entry.getKey(), entry.getValue()))
			{
				System.out.println("Disabling packet codec. Packet " + entry.getKey() + " is not compatible with our version");
				mEnabled = false;
				return;
			}
		}
	}
	
	public static PacketCodec fromSchemaData(DataInput in) throws IOException
	{
		PacketCodec codec = new PacketCodec();
		
		int count = in.readByte();
		System.out.println("Loading decoder: " + count + " packets");
		for(int i = 0; i < count; ++i)
		{
			String type = in.readUTF();
			String schemaDef = in.readUTF();
			
			System.out.println("* Packet: " + type + ": " + schemaDef);
			PacketSchema schema = PacketSchema.from(schemaDef);
			codec.mReadSchemas.put(type, schema);
		}
		
		codec.validate();
		
		return codec;
	}
}
