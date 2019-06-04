package au.com.addstar.bc.sync;

/*-
 * #%L
 * BungeeChat-Core
 * %%
 * Copyright (C) 2015 - 2019 AddstarMC
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

public class PacketCodec
{
	private HashMap<String, PacketSchema> mReadSchemas = new HashMap<>();
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
		for(int i = 0; i < count; ++i)
		{
			String type = in.readUTF();
			String schemaDef = in.readUTF();
			
			PacketSchema schema = PacketSchema.from(schemaDef);
			codec.mReadSchemas.put(type, schema);
		}
		
		codec.validate();
		
		return codec;
	}
}
