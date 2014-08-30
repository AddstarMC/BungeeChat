package au.com.addstar.bc.sync;

import java.util.HashMap;
import java.util.concurrent.Future;

import org.bukkit.Bukkit;

public class BukkitComLink extends ServerComLink
{
	private HashMap<Integer, RemoteServer> mServers;
	
	public BukkitComLink()
	{
		mServers = new HashMap<Integer, RemoteServer>();
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
	
	private class RemoteServer implements MessageSender
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
}
