package au.com.addstar.bc;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.config.ServerInfo;

public class MessageOutput
{
	private ByteArrayOutputStream mStream;
	private DataOutputStream mOutput;
	private String mChannel;
	
	public MessageOutput(String channel, String subChannel)
	{
		mChannel = channel;
		mStream = new ByteArrayOutputStream();
		mOutput = new DataOutputStream(mStream);
		
		try
		{
			mOutput.writeUTF(subChannel);
		}
		catch(IOException e)
		{
		}
	}
	public MessageOutput write( int b )
	{
		try
		{
			mOutput.write(b);
		}
		catch(IOException e)
		{
		}
		
		return this;
	}

	public MessageOutput write( byte[] b )
	{
		try
		{
			mOutput.write(b);
		}
		catch(IOException e)
		{
		}
		
		return this;
	}

	public MessageOutput write( byte[] b, int off, int len )
	{
		try
		{
			mOutput.write(b, off, len);
		}
		catch(IOException e)
		{
		}
		
		return this;
	}

	public MessageOutput writeBoolean( boolean v )
	{
		try
		{
			mOutput.writeBoolean(v);
		}
		catch(IOException e)
		{
		}
		
		return this;
	}

	public MessageOutput writeByte( int v )
	{
		try
		{
			mOutput.writeByte(v);
		}
		catch(IOException e)
		{
		}
		
		return this;
	}

	public MessageOutput writeShort( int v )
	{
		try
		{
			mOutput.writeShort(v);
		}
		catch(IOException e)
		{
		}
		
		return this;
	}

	public MessageOutput writeChar( int v )
	{
		try
		{
			mOutput.writeChar(v);
		}
		catch(IOException e)
		{
		}
		
		return this;
	}

	public MessageOutput writeInt( int v )
	{
		try
		{
			mOutput.writeInt(v);
		}
		catch(IOException e)
		{
		}
		
		return this;
	}

	public MessageOutput writeLong( long v )
	{
		try
		{
			mOutput.writeLong(v);
		}
		catch(IOException e)
		{
		}
		
		return this;
	}

	public MessageOutput writeFloat( float v )
	{
		try
		{
			mOutput.writeFloat(v);
		}
		catch(IOException e)
		{
		}
		
		return this;
	}

	public MessageOutput writeDouble( double v )
	{
		try
		{
			mOutput.writeDouble(v);
		}
		catch(IOException e)
		{
		}
		
		return this;
	}

	public MessageOutput writeBytes( String s )
	{
		try
		{
			mOutput.writeBytes(s);
		}
		catch(IOException e)
		{
		}
		
		return this;
	}

	public MessageOutput writeChars( String s )
	{
		try
		{
			mOutput.writeChars(s);
		}
		catch(IOException e)
		{
		}
		
		return this;
	}

	public MessageOutput writeUTF( String s )
	{
		try
		{
			mOutput.writeUTF(s);
		}
		catch(IOException e)
		{
		}
		
		return this;
	}
	
	public DataOutput asDataOutput()
	{
		return mOutput;
	}

	public byte[] toBytes()
	{
		return mStream.toByteArray();
	}
	
	public void send()
	{
		send(false);
	}
	
	public void send(boolean queue)
	{
		byte[] data = mStream.toByteArray();
		
		for(ServerInfo server : BungeeCord.getInstance().getServers().values())
		{
			if(queue || !server.getPlayers().isEmpty())
				server.sendData(mChannel, data);
		}
	}
	
	public void send(ServerInfo server)
	{
		server.sendData(mChannel, mStream.toByteArray());
	}
}
