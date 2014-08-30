package au.com.addstar.bc.sync;

import java.util.HashMap;

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
	public void listenToChannel( final String channel, final IDataReceiver receiver )
	{
		Bukkit.getScheduler().runTaskAsynchronously(BungeeChat.getInstance(), new Runnable()
		{
			@Override
			public void run()
			{
				subscribeChannel(channel, receiver);
			}
		});
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
