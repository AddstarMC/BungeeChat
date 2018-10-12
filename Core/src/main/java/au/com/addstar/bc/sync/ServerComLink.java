package au.com.addstar.bc.sync;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.AbstractMap;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

public abstract class ServerComLink
{
	public static final Charset UTF_8 = Charset.forName("UTF-8");
	
	private JedisPool mPool;
	private long mLastNotifyTime = 0;
	private ConnectionStateNotify mNotifyHandler;
	private LinkedBlockingQueue<Entry<String, byte[]>> mWaitingData;
	
	public ServerComLink()
	{
		mWaitingData = new LinkedBlockingQueue<>();
	}
	
	public void init(String host, int port, String password)
	{
		try {
			if (password == null || password.isEmpty())
				mPool = new JedisPool(new JedisPoolConfig(), host, port, 0);
			else
				mPool = new JedisPool(new JedisPoolConfig(), host, port, 0, password);
			if(mPool == null || mPool.getResource() == null)throw new ExceptionInInitializerError(
					"Jedis Pool did  not load");
			initializeQueueHandler(mWaitingData);
		}catch (Exception e){
			e.printStackTrace();
		}
		
	}
	
	public void disable(){
		mPool.close();
	}
	
	protected abstract void initializeQueueHandler(BlockingQueue<Entry<String, byte[]>> queue);
	
	public void setNotifyHandle(ConnectionStateNotify handler)
	{
		synchronized(this)
		{
			mNotifyHandler = handler;
		}
	}
	
	public abstract Future<Void> listenToChannel(String channel, IDataReceiver receiver);
	
	/**
	 * Subscribes to a channel. NOTE: This MUST be called on a thread as this WILL block until this is unsubscribed
	 */
	protected void subscribeChannel(String channel, IDataReceiver receiver, SubscribeFuture future)
	{
		boolean err = true;
		while(err)
		{
			Jedis jedis = null;
			try
			{
				jedis = mPool.getResource();
				jedis.clientSetname("BungeeChat-" + getServerId() + "-" + channel);
				notifyOnline();
				jedis.subscribe(new ListenerWrapper(receiver, future), channel.getBytes(UTF_8));
				err = false;
			}
			catch(JedisConnectionException e)
			{
				notifyFailure(e);
				try
				{
					Thread.sleep(2000);
				}
				catch (InterruptedException ex)
				{
					err = false;
				}
			}
			catch(JedisException e)
			{
				e.printStackTrace();
				err = false;
			}
			finally
			{
				if(jedis!=null)jedis.close();
			}
		}
	}
	
	private void notifyOnline()
	{
		synchronized(this)
		{
			if(mLastNotifyTime == 0)
				return;
			
			mLastNotifyTime = 0;
			
			if(mNotifyHandler != null)
				mNotifyHandler.onConnectionRestored();
		}
	}
	private void notifyFailure(Throwable e)
	{
		synchronized(this)
		{
			if (System.currentTimeMillis() - mLastNotifyTime < 60000)
				return;
			
			if(mLastNotifyTime == 0)
			{
				e.printStackTrace();
				if (mNotifyHandler != null)
					mNotifyHandler.onConnectionLost(e);
			}
			System.err.println("[BungeeChat] Warning: Redis connection is unavailble!");
			
			mLastNotifyTime = System.currentTimeMillis();
		}
	}
	
	protected void publish(String channel, byte[] data)
	{
		Jedis jedis = null;
		try
		{
			jedis = mPool.getResource();
			jedis.publish(channel.getBytes(UTF_8), data);
			notifyOnline();
		}
		catch(JedisConnectionException e)
		{
			notifyFailure(e);
		}
		catch(Throwable e)
		{
			e.printStackTrace();
		}
		finally
		{
			if(jedis!=null)jedis.close();
		}
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
		
		mWaitingData.offer(new AbstractMap.SimpleEntry<>(channel, stream.toByteArray()));
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
		
		mWaitingData.offer(new AbstractMap.SimpleEntry<>(channel, stream.toByteArray()));
	}
	
	protected abstract MessageSender getSender(int id);
	public abstract int getServerId(); 
	
	private class ListenerWrapper extends BinaryJedisPubSub
	{
		private IDataReceiver mReceiver;
		private SubscribeFuture mFuture;
		public ListenerWrapper( IDataReceiver receiver, SubscribeFuture future  )
		{
			mReceiver = receiver;
			mFuture = future;
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
			mFuture.setDone();
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
	
	public interface ConnectionStateNotify
	{
		/**
		 * Note, this must not do anything now. Schedule something
		 */
        void onConnectionLost(Throwable e);
		/**
		 * Note, this must not do anything now. Schedule something
		 */
        void onConnectionRestored();
	}
}
