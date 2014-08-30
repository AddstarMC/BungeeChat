package au.com.addstar.bc.sync;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public abstract class ServerComLink
{
	public static final Charset UTF_8 = Charset.forName("UTF-8");
	
	private JedisPool mPool;
	
	public ServerComLink()
	{
	}
	
	public void init(String host, int port, String password)
	{
		if (password == null || password.isEmpty())
			mPool = new JedisPool(new JedisPoolConfig(), host, port, 0);
		else
			mPool = new JedisPool(new JedisPoolConfig(), host, port, 0, password);
	}
	
	public abstract void listenToChannel(String channel, IDataReceiver receiver);
	
	/**
	 * Subscribes to a channel. NOTE: This MUST be called on a thread as this WILL block until this is unsubscribed
	 */
	protected void subscribeChannel(String channel, IDataReceiver receiver)
	{
		Jedis jedis = mPool.getResource();
		
		jedis.subscribe(new ListenerWrapper(receiver), channel.getBytes(UTF_8));
		
		mPool.returnResource(jedis);
	}
	
	public void broadcastMessage(String channel, byte[] data)
	{
		ByteArrayOutputStream stream = new ByteArrayOutputStream(data.length + 2);
		DataOutputStream out = new DataOutputStream(stream);
		
		try
		{
			out.writeShort(getServerId());
			out.writeShort(0xFFFF);
			out.write(data);
		}
		catch(IOException e)
		{ // Cant happen
		}
		
		Jedis jedis = mPool.getResource();
		try
		{
			jedis.publish(channel.getBytes(UTF_8), stream.toByteArray());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			mPool.returnBrokenResource(jedis);
			return;
		}
		mPool.returnResource(jedis);
	}
	
	public void sendMessage(String channel, byte[] data, MessageSender target)
	{
		ByteArrayOutputStream stream = new ByteArrayOutputStream(data.length + 2);
		DataOutputStream out = new DataOutputStream(stream);
		
		try
		{
			out.writeShort(getServerId());
			out.writeShort((target != null ? target.getId() : 0));
			out.write(data);
		}
		catch(IOException e)
		{ // Cant happen
		}
		
		Jedis jedis = mPool.getResource();
		try
		{
			jedis.publish(channel.getBytes(UTF_8), stream.toByteArray());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			mPool.returnBrokenResource(jedis);
			return;
		}
		mPool.returnResource(jedis);
	}
	
	protected abstract MessageSender getSender(int id);
	public abstract int getServerId(); 
	
	private class ListenerWrapper extends BinaryJedisPubSub
	{
		private IDataReceiver mReceiver;
		public ListenerWrapper( IDataReceiver receiver )
		{
			mReceiver = receiver;
		}

		@Override
		public void onMessage( byte[] channel, byte[] data )
		{
			String channelName = new String(channel, UTF_8);
			try
			{
				ByteArrayInputStream stream = new ByteArrayInputStream(data);
				DataInputStream in = new DataInputStream(stream);
				
				int source = in.readUnsignedShort();
				int dest = in.readUnsignedShort();
				
				if(source != getServerId() && (dest == 0xFFFF || dest == getServerId()))
					mReceiver.onReceive(channelName, in, getSender(source));
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}

		@Override
		public void onPMessage( byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3 )
		{
		}

		@Override
		public void onSubscribe( byte[] channel, int paramInt )
		{
		}

		@Override
		public void onUnsubscribe( byte[] channel, int paramInt )
		{
		}

		@Override
		public void onPUnsubscribe( byte[] paramArrayOfByte, int paramInt )
		{
		}

		@Override
		public void onPSubscribe( byte[] paramArrayOfByte, int paramInt )
		{
		}
	}
}
