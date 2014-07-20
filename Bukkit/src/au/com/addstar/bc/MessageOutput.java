package au.com.addstar.bc;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

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

//	public byte[] toBytes()
//	{
//		return mStream.toByteArray();
//	}
//	
//	public void send(Plugin plugin)
//	{
//		Player[] players = Bukkit.getOnlinePlayers();
//		if(players.length > 0)
//			players[0].sendPluginMessage(plugin, mChannel, mStream.toByteArray());
//	}
//	
//	public void send(Player player, Plugin plugin)
//	{
//		player.sendPluginMessage(plugin, mChannel, mStream.toByteArray());
//	}
}
