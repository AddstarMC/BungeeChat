package au.com.addstar.bc.sync;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;

import org.bukkit.Bukkit;

import au.com.addstar.bc.BungeeChat;

public class BukkitComLink extends ServerComLink
{
	private HashMap<Integer, RemoteServer> mServers;
	
	public BukkitComLink()
	{
		mServers = new HashMap<Integer, RemoteServer>();
	}
	
	@Override
	protected void initializeQueueHandler( BlockingQueue<Entry<String, byte[]>> queue )
	{
		Bukkit.getScheduler().runTaskAsynchronously(BungeeChat.getInstance(), new DataSender(queue));
	}
	
	@Override
	public Future<Void> listenToChannel( final String channel, final IDataReceiver receiver )
	{
		final SubscribeFuture future = new SubscribeFuture();
		Thread thread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				subscribeChannel(channel, receiver, future);
			}
		});
		
		thread.start();
		
		return future;
	}

	@Override
	protected MessageSender getSender( int id )
	{
		if (id == 0)
			return null;
		
		RemoteServer server = mServers.get(id);
		if(server == null)
		{
			server = new RemoteServer(id);
			mServers.put(id, server);
		}
		
		return server;
	}

	@Override
	public int getServerId()
	{
		return Bukkit.getPort();
	}

	public void sendMessage( String channel, byte[] data)
	{
		super.sendMessage(channel, data, null);
	}
	
	private static class RemoteServer implements MessageSender
	{
		private int mId;
		
		public RemoteServer(int id)
		{
			mId = id;
		}
		
		@Override
		public int getId()
		{
			return mId;
		}

		@Override
		public String getName()
		{
			return null;
		}
		
	}
	
	private class DataSender implements Runnable
	{
		private BlockingQueue<Entry<String, byte[]>> mQueue;
		public DataSender(BlockingQueue<Entry<String, byte[]>> queue)
		{
			mQueue = queue;
		}
		
		@Override
		public void run()
		{
			try
			{
				while(true)
				{
					Entry<String, byte[]> item = mQueue.take();
					publish(item.getKey(), item.getValue());
				}
			}
			catch(InterruptedException e)
			{
			}
		}
	}
}
